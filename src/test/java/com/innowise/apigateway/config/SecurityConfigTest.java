package com.innowise.apigateway.config;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "app.security.jwks-uri=http://localhost:8082/.well-known/jwks.json",
        "app.services.auth-uri=http://localhost:8082",
        "app.services.user-uri=http://localhost:8081",
        "management.health.redis.enabled=false"
})
@AutoConfigureWebTestClient
@Import(SecurityConfigTest.TestRoutesConfiguration.class)
class SecurityConfigTest {

    private static final MockWebServer MOCK_WEB_SERVER = startMockWebServer();

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    private static MockWebServer startMockWebServer() {
        MockWebServer server = new MockWebServer();

        try {
            server.start();
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @DynamicPropertySource
    static void configureMockWebServer(DynamicPropertyRegistry registry) {
        registry.add(
                "test.mock-server.url",
                () -> MOCK_WEB_SERVER.url("/").toString()
        );
    }

    @AfterAll
    static void stopMockWebServer() throws IOException {
        MOCK_WEB_SERVER.shutdown();
    }

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

        when(jwtDecoder.decode("abc.def"))
                .thenReturn(Mono.error(
                        new BadJwtException("Malformed token")
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
    void shouldForwardRequestWithValidTokenToDownstreamService() throws InterruptedException {
        MOCK_WEB_SERVER.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setBody("forwarded")
        );

        webTestClient.get()
                .uri("/protected-test")
                .headers(headers ->
                        headers.setBearerAuth("valid-test-token")
                )
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("forwarded");

        RecordedRequest recordedRequest =
                MOCK_WEB_SERVER.takeRequest(1, TimeUnit.SECONDS);

        assertNotNull(recordedRequest);
        assertEquals(
                "Bearer valid-test-token",
                recordedRequest.getHeader(HttpHeaders.AUTHORIZATION)
        );
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
                        headers.setBearerAuth("abc.def")
                )
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestRoutesConfiguration {

        @Bean
        RouteLocator testRouteLocator(
                RouteLocatorBuilder builder,
                @Value("${test.mock-server.url}") String mockServerUrl
        ) {
            return builder.routes()
                    .route("protected-test", route -> route
                            .path("/protected-test")
                            .uri(mockServerUrl)
                    )
                    .build();
        }
    }
}