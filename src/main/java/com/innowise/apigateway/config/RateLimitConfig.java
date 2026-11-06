package com.innowise.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Configuration
public class RateLimitConfig {

    @Bean
    public RedisRateLimiter redisRateLimiter(
            @Value("${app.rate-limit.replenish-rate}") int replenishRate,
            @Value("${app.rate-limit.burst-capacity}") int burstCapacity
    ) {
        return new RedisRateLimiter(replenishRate, burstCapacity);
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(Principal::getName)
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    var remoteAddress = exchange.getRequest()
                            .getRemoteAddress();

                    if (remoteAddress == null) {
                        return "unknown";
                    }

                    return remoteAddress.getAddress().getHostAddress();
                }));
    }
}