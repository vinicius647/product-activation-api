package br.com.vdb.fakeprovider;

import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Fake activation provider, active only in the "local" profile, so the full flow can be run
 * without the real system. The answer depends on the productCode:
 * <ul>
 *     <li>contains REJECT: 400</li>
 *     <li>contains ERROR: 500</li>
 *     <li>contains BUSY: 503 with Retry-After: 10</li>
 *     <li>contains SLOW: answers after 5 seconds (more than the read timeout)</li>
 *     <li>anything else: 200</li>
 * </ul>
 */
@Hidden
@Profile("local")
@RestController
public class FakeProviderController {

    private static final Logger log = LoggerFactory.getLogger(FakeProviderController.class);

    @PostMapping("/provider/v1/activate")
    public ResponseEntity<Map<String, String>> activate(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                        @RequestBody Map<String, Object> body)
            throws InterruptedException {
        String productCode = String.valueOf(body.get("productCode"));
        log.info("Fake provider received activation {} for product {}", idempotencyKey, productCode);

        if (productCode.contains("REJECT")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid product"));
        }
        if (productCode.contains("ERROR")) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Unexpected error"));
        }
        if (productCode.contains("BUSY")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.RETRY_AFTER, "10")
                    .body(Map.of("error", "Try again later"));
        }
        if (productCode.contains("SLOW")) {
            Thread.sleep(5_000);
        }
        return ResponseEntity.ok(Map.of("status", "ACTIVATED"));
    }
}
