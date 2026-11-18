package com.innowise.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8082/.well-known/jwks.json",
        "app.services.auth-uri=http://localhost:8082",
        "app.services.user-uri=http://localhost:8081",
        "app.services.order-uri=http://localhost:8080"
})
class RoutesConfigTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void shouldConfigureCorrectRoutes() {
        Map<String, Route> routes = routeLocator.getRoutes()
                .collectList()
                .block()
                .stream()
                .collect(Collectors.toMap(
                        Route::getId,
                        Function.identity()
                ));

        assertEquals(3, routes.size());

        assertEquals(
                URI.create("http://localhost:8082"),
                routes.get("auth-service").getUri()
        );

        assertEquals(
                URI.create("http://localhost:8081"),
                routes.get("user-service").getUri()
        );

        assertEquals(
                URI.create("http://localhost:8080"),
                routes.get("order-service").getUri()
        );
    }

    @Test
    void shouldRouteAuthRequestsToAuthService() {
        Route authRoute = getRoute("auth-service");

        StepVerifier.create(
                        authRoute.getPredicate().apply(
                                MockServerWebExchange.from(
                                        MockServerHttpRequest.get("/api/auth/login").build()
                                )
                        )
                ).expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldRouteUserRequestsToUserService() {
        Route userRoute = getRoute("user-service");

        StepVerifier.create(
                        userRoute.getPredicate().apply(
                                MockServerWebExchange.from(
                                        MockServerHttpRequest.get("/api/users/me").build()
                                )
                        )
                ).expectNext(true)
                .verifyComplete();

        StepVerifier.create(
                        userRoute.getPredicate().apply(
                                MockServerWebExchange.from(
                                        MockServerHttpRequest.get("/api/cards").build()
                                )
                        )
                ).expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldRouteOrderRequestsToOrderService() {
        Route orderRoute = getRoute("order-service");

        StepVerifier.create(
                orderRoute.getPredicate().apply(
                        MockServerWebExchange.from(
                                MockServerHttpRequest.get("/api/orders").build()
                        )
                )
        ).expectNext(true).verifyComplete();

        StepVerifier.create(
                orderRoute.getPredicate().apply(
                        MockServerWebExchange.from(
                                MockServerHttpRequest.get("/api/items").build()
                        )
                )
        ).expectNext(true).verifyComplete();
    }

    private Route getRoute(String routeId) {
        return routeLocator.getRoutes()
                .filter(route -> route.getId().equals(routeId))
                .blockFirst();
    }
}
