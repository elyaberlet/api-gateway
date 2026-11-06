package com.innowise.apigateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    private final JsonAuthenticationEntryPoint entryPoint =
            new JsonAuthenticationEntryPoint(objectMapper);

    @Test
    void shouldReturn401WithJsonErrorResponse() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        org.springframework.mock.http.server.reactive.MockServerHttpRequest
                                .get("/api/users/me")
                                .build()
                );

        entryPoint.commence(
                exchange,
                new BadCredentialsException("Invalid credentials")
        ).block();

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exchange.getResponse().getStatusCode()
        );

        assertEquals(
                "application/json",
                exchange.getResponse().getHeaders()
                        .getContentType().toString()
        );

        assertEquals(
                "Bearer",
                exchange.getResponse().getHeaders()
                        .getFirst(HttpHeaders.WWW_AUTHENTICATE)
        );

        String responseBody = exchange.getResponse()
                .getBodyAsString()
                .block();

        assertTrue(responseBody.contains("\"status\":401"));
        assertTrue(responseBody.contains("\"error\":\"Unauthorized\""));
        assertTrue(responseBody.contains(
                "Authentication is required or the access token is invalid"
        ));
    }
}
