package com.innowise.apigateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.apigateway.dto.SecurityErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class JsonAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> commence(
            ServerWebExchange exchange,
            AuthenticationException exception
    ) {
        var response = exchange.getResponse();

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(
                HttpHeaders.WWW_AUTHENTICATE,
                "Bearer"
        );

        SecurityErrorResponse errorResponse = new SecurityErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Authentication is required or the access token is invalid"
        );

        return response.writeWith(
                Mono.fromCallable(() ->
                        response.bufferFactory().wrap(
                                objectMapper.writeValueAsBytes(errorResponse)
                        )
                )
        );
    }
}