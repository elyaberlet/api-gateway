package com.innowise.apigateway.config;

import com.innowise.apigateway.security.JsonAuthenticationEntryPoint;
import com.innowise.apigateway.security.PublicPath;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JsonAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(
            JsonAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http
    ) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(
                                HttpMethod.GET,
                                "/actuator/health"
                        ).permitAll()
                        .anyExchange().access((authentication, context) -> {
                            var request = context.getExchange().getRequest();

                            boolean isPublic = PublicPath.isPublic(
                                    request.getURI().getPath(),
                                    request.getMethod()
                            );

                            return authentication
                                    .map(auth -> new AuthorizationDecision(
                                            isPublic || auth.isAuthenticated()
                                    ))
                                    .defaultIfEmpty(
                                            new AuthorizationDecision(isPublic)
                                    );
                        })
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .bearerTokenConverter(bearerTokenConverter())
                        .jwt(jwt -> {})
                )
                .build();
    }

    private ServerAuthenticationConverter bearerTokenConverter() {
        ServerBearerTokenAuthenticationConverter defaultConverter =
                new ServerBearerTokenAuthenticationConverter();

        return exchange -> {
            var request = exchange.getRequest();

            boolean isPublic = PublicPath.isPublic(
                    request.getURI().getPath(),
                    request.getMethod()
            );

            if (isPublic) {
                return Mono.empty();
            }

            return defaultConverter.convert(exchange);
        };
    }
}
