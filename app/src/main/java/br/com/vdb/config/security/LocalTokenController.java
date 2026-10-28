package br.com.vdb.config.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Issues test tokens in the "local" profile only. Never active in production.
 */
@RestController
@Profile("local")
@Tag(name = "Local token", description = "Issues JWTs for local testing (local profile only)")
public class LocalTokenController {

    private static final Duration TTL = Duration.ofHours(1);

    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    public LocalTokenController(JwtEncoder jwtEncoder, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
    }

    @PostMapping("/dev/token")
    @Operation(summary = "Issue a local JWT",
            description = "Simulates a client credentials token: the calling system goes in client_id (and sub)")
    public Map<String, Object> issueToken(
            @RequestParam(defaultValue = "local-client") String clientId,
            @RequestParam(defaultValue = "requests:read requests:write") String scope) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("product-activation-api-local")
                .subject(clientId)
                .issuedAt(now)
                .expiresAt(now.plus(TTL))
                .claim("client_id", clientId)
                .claim("scope", scope)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return Map.of("access_token", token, "token_type", "Bearer", "expires_in", TTL.toSeconds());
    }
}
