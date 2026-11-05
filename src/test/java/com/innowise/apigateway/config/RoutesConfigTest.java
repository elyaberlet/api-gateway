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
        "app.security.jwks-uri=http://localhost:8082/.well-known/jwks.json",
        "app.services.auth-uri=http://localhost:8082",
        "app.services.user-uri=http://localhost:8081"
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

        assertEquals(2, routes.size());

        assertEquals(
                URI.create("http://localhost:8082"),
                routes.get("auth-service").getUri()
        );

        assertEquals(
                URI.create("http://localhost:8081"),
                routes.get("user-service").getUri()
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

    private Route getRoute(String routeId) {
        return routeLocator.getRoutes()
                .filter(route -> route.getId().equals(routeId))
                .blockFirst();
    }
}
