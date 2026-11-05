package com.innowise.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitConfigTest {

    private final RateLimitConfig config = new RateLimitConfig();

    @Test
    void shouldCreateRedisRateLimiter() {
        RedisRateLimiter rateLimiter = config.redisRateLimiter();

        assertNotNull(rateLimiter);
    }

    @Test
    void shouldResolveKeyByAuthenticatedUser() {
        KeyResolver keyResolver = config.userKeyResolver();

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        Principal principal = () -> "test-user";

        when(exchange.getPrincipal())
                .thenReturn(Mono.just(principal));

        String key = keyResolver.resolve(exchange).block();

        assertEquals("test-user", key);
    }

    @Test
    void shouldResolveKeyByIpAddressWhenUserIsNotAuthenticated() {
        KeyResolver keyResolver = config.userKeyResolver();

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);

        when(exchange.getPrincipal()).thenReturn(Mono.empty());
        when(exchange.getRequest()).thenReturn(request);
        when(request.getRemoteAddress())
                .thenReturn(new InetSocketAddress("192.168.1.10", 8080));

        String key = keyResolver.resolve(exchange).block();

        assertEquals("192.168.1.10", key);
    }

    @Test
    void shouldResolveKeyAsUnknownWhenRemoteAddressIsNull() {
        KeyResolver keyResolver = config.userKeyResolver();

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);

        when(exchange.getPrincipal()).thenReturn(Mono.empty());
        when(exchange.getRequest()).thenReturn(request);
        when(request.getRemoteAddress()).thenReturn(null);

        String key = keyResolver.resolve(exchange).block();

        assertEquals("unknown", key);
    }

    @Test
    void shouldPreferAuthenticatedUserOverIpAddress() {
        KeyResolver keyResolver = config.userKeyResolver();

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        Principal principal = () -> "authenticated-user";

        when(exchange.getPrincipal())
                .thenReturn(Mono.just(principal));

        String key = keyResolver.resolve(exchange).block();

        assertEquals("authenticated-user", key);
    }

    @Test
    void shouldResolveKeyByIpAddressWhenPrincipalIsEmpty() {
        KeyResolver keyResolver = config.userKeyResolver();

        ServerWebExchange exchange = MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest
                        .get("/api/auth/login")
                        .remoteAddress(new InetSocketAddress("10.0.0.5", 8080))
                        .build()
        );

        String key = keyResolver.resolve(exchange).block();

        assertEquals("10.0.0.5", key);
    }
}
