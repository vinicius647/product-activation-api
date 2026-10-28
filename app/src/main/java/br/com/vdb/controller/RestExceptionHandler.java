package br.com.vdb.controller;

import br.com.vdb.config.CorrelationIdFilter;
import br.com.vdb.core.domain.exception.ActivationNotFoundException;
import br.com.vdb.core.domain.exception.DuplicateActivationException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;

/**
 * Every error is returned as RFC 9457 Problem Details with a traceId (the correlation id),
 * which is also in the logs. Stack traces and internal messages are never returned.
 */
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);
    private static final String TYPE_BASE = "https://product-activation-api/problems/";

    @ExceptionHandler(ActivationNotFoundException.class)
    ProblemDetail handleNotFound(ActivationNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Activation request not found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateActivationException.class)
    ProblemDetail handleDuplicate(DuplicateActivationException ex) {
        return problem(HttpStatus.CONFLICT, "duplicate-request", "Duplicate externalId", ex.getMessage());
    }

    @ExceptionHandler(RequestNotPermitted.class)
    ProblemDetail handleRateLimited(RequestNotPermitted ex) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "rate-limit", "Too many requests",
                "Rate limit exceeded, try again later.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Unexpected error",
                "An unexpected error happened. Use the traceId to find it in the logs.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return validationProblem(ex, errors, headers, status, request);
    }

    /** Invalid query or path parameters (for example size=500). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<FieldError> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldError(result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return validationProblem(ex, errors, headers, status, request);
    }

    /** Wrong type in a parameter, for example an id that is not a UUID or an unknown status. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        var errors = List.of(new FieldError(ex.getPropertyName(), "has an invalid value"));
        return validationProblem(ex, errors, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "malformed-request", "Malformed request",
                "The request body is missing or is not valid JSON.");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /** Also used by the errors handled by Spring itself (404 of unknown paths, 405, 415...). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            addTraceId(problem);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private ResponseEntity<Object> validationProblem(Exception ex, List<FieldError> errors, HttpHeaders headers,
                                                     HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation-error", "Invalid data",
                "One or more fields are invalid.");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_BASE + type));
        problem.setTitle(title);
        addTraceId(problem);
        return problem;
    }

    private static void addTraceId(ProblemDetail problem) {
        String traceId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
    }

    record FieldError(String field, String message) {
    }
}
