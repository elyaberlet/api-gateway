package com.innowise.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8082/.well-known/jwks.json",
                "app.services.auth-uri=http://localhost:8082",
                "app.services.user-uri=http://localhost:8081"
        }
)
@AutoConfigureWebTestClient
@Import(AuthEndpointsSecurityTest.TestAuthConfig.class)
class AuthEndpointsSecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldAllowLoginWithoutToken() {
        testPublicEndpoint("/api/auth/login");
    }

    @Test
    void shouldAllowRegistrationWithoutToken() {
        testPublicEndpoint("/api/auth/register");
    }

    @Test
    void shouldAllowRefreshWithoutToken() {
        testPublicEndpoint("/api/auth/refresh");
    }

    private void testPublicEndpoint(String uri) {
        webTestClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Request reached test auth endpoint");
    }

    @TestConfiguration
    static class TestAuthConfig {

        @Bean
        TestAuthController testAuthController() {
            return new TestAuthController();
        }
    }

    @RestController
    static class TestAuthController {

        @PostMapping({
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/refresh"
        })
        ResponseEntity<String> authEndpoint() {
            return ResponseEntity.ok("Request reached test auth endpoint");
        }
    }
}
