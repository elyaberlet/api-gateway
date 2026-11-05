package com.innowise.apigateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "app.security.jwks-uri=http://localhost:8082/.well-known/jwks.json",
        "app.services.auth-uri=http://localhost:8082",
        "app.services.user-uri=http://localhost:8081",
        "management.health.redis.enabled=false"
})
@AutoConfigureWebTestClient
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();

        Jwt validJwt = Jwt.withTokenValue("valid-test-token")
                .header("alg", "RS256")
                .subject("1002")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("role", "USER")
                .build();

        when(jwtDecoder.decode("valid-test-token"))
                .thenReturn(Mono.just(validJwt));

        when(jwtDecoder.decode("invalid-token"))
                .thenReturn(Mono.error(
                        new BadJwtException("Invalid token")
                ));

        when(jwtDecoder.decode("refresh-test-token"))
                .thenReturn(Mono.error(
                        new BadJwtException("Only access tokens are allowed")
                ));
    }

    @Test
    void shouldReturn401WhenTokenIsMissing() {
        webTestClient.get()
                .uri("/api/users/me")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader()
                .contentTypeCompatibleWith("application/json")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void shouldReturn401WhenTokenIsInvalid() {
        webTestClient.get()
                .uri("/api/users/me")
                .headers(headers ->
                        headers.setBearerAuth("invalid-token")
                )
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void shouldReturn401WhenRefreshTokenIsUsed() {
        webTestClient.get()
                .uri("/api/users/me")
                .headers(headers ->
                        headers.setBearerAuth("refresh-test-token")
                )
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void shouldAllowHealthEndpointWithoutToken() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void shouldAllowRequestWithValidTokenThroughSecurity() {
        webTestClient.get()
                .uri("/protected-test")
                .headers(headers ->
                        headers.setBearerAuth("valid-test-token")
                )
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturn401WhenBearerTokenIsEmpty() {
        webTestClient.get()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn401WhenAuthorizationHeaderHasWrongScheme() {
        webTestClient.get()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Basic dGVzdA==")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn401WhenBearerTokenIsMalformed() {
        webTestClient.get()
                .uri("/api/users/me")
                .headers(headers ->
                        headers.setBearerAuth("invalid-token")
                )
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
