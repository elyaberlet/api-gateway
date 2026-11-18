package com.innowise.apigateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenTypeValidatorTest {

    private final TokenTypeValidator validator = new TokenTypeValidator();

    @Test
    void shouldAcceptAccessToken() {
        Jwt jwt = createJwt("access");

        var result = validator.validate(jwt);

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void shouldRejectRefreshToken() {
        Jwt jwt = createJwt("refresh");

        var result = validator.validate(jwt);

        assertTrue(result.hasErrors());
        assertEquals(
                "invalid_token",
                result.getErrors().iterator().next().getErrorCode()
        );
    }

    @Test
    void shouldRejectTokenWithoutTokenType() {
        Jwt jwt = createJwt(null);

        var result = validator.validate(jwt);

        assertTrue(result.hasErrors());
    }

    private Jwt createJwt(String tokenType) {
        Instant now = Instant.now();

        var builder = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("1002")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300));

        if (tokenType != null) {
            builder.claim("token_type", tokenType);
        }

        return builder.build();
    }
}
