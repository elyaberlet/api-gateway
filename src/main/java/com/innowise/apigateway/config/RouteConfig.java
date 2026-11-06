package com.innowise.apigateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ServicesProperties.class)
public class RouteConfig {

    @Bean
    public RouteLocator gatewayRoutes(
            RouteLocatorBuilder builder,
            ServicesProperties servicesProperties,
            RedisRateLimiter redisRateLimiter,
            KeyResolver userKeyResolver
    ) {
        return builder.routes()

                .route("auth-service", route -> route
                        .path("/api/auth/**")
                        .filters(f -> f.requestRateLimiter(config -> config
                                .setRateLimiter(redisRateLimiter)
                                .setKeyResolver(userKeyResolver)
                        ))
                        .uri(servicesProperties.authUri())
                )

                .route("user-service", route -> route
                        .path("/api/users/**", "/api/cards/**")
                        .filters(f -> f.requestRateLimiter(config -> config
                                .setRateLimiter(redisRateLimiter)
                                .setKeyResolver(userKeyResolver)
                        ))
                        .uri(servicesProperties.userUri())
                )

                .build();
    }
}