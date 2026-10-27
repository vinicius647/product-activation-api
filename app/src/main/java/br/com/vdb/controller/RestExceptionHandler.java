package br.com.vdb.controller;

import br.com.vdb.core.domain.exception.ActivationAlreadyExistsException;
import br.com.vdb.core.domain.exception.ActivationNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Translates domain and resilience exceptions into RFC 9457 problem details.
 */
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ActivationNotFoundException.class)
    ProblemDetail handleNotFound(ActivationNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ActivationAlreadyExistsException.class)
    ProblemDetail handleConflict(ActivationAlreadyExistsException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(RequestNotPermitted.class)
    ProblemDetail handleRateLimited(RequestNotPermitted ex) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded, try again later");
    }

    @ExceptionHandler(CallNotPermittedException.class)
    ProblemDetail handleCircuitOpen(CallNotPermittedException ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable, try again later");
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
