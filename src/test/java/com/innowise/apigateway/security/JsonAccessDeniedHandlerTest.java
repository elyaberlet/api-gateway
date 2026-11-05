package com.innowise.apigateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    private final JsonAccessDeniedHandler handler =
            new JsonAccessDeniedHandler(objectMapper);

    @Test
    void shouldReturn403WithJsonErrorResponse() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        org.springframework.mock.http.server.reactive.MockServerHttpRequest
                                .get("/api/users")
                                .build()
                );

        handler.handle(
                exchange,
                new AccessDeniedException("Access denied")
        ).block();

        assertEquals(
                HttpStatus.FORBIDDEN,
                exchange.getResponse().getStatusCode()
        );

        assertEquals(
                "application/json",
                exchange.getResponse().getHeaders().getContentType().toString()
        );

        String responseBody = exchange.getResponse()
                .getBodyAsString()
                .block();

        System.out.println(responseBody);
        assertTrue(responseBody.contains("403"));
        assertTrue(responseBody.contains("Forbidden"));
        assertTrue(responseBody.contains(
                "You do not have permission to access this resource"
        ));
    }
}
