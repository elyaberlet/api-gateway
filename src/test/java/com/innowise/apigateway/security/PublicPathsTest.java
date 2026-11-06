package com.innowise.apigateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicPathsTest {

    @Test
    void shouldAllowPostLoginWithoutToken() {
        assertTrue(PublicPath.isPublic(
                "/api/auth/login",
                HttpMethod.POST
        ));
    }

    @Test
    void shouldAllowPostRegisterWithoutToken() {
        assertTrue(PublicPath.isPublic(
                "/api/auth/register",
                HttpMethod.POST
        ));
    }

    @Test
    void shouldAllowPostRefreshWithoutToken() {
        assertTrue(PublicPath.isPublic(
                "/api/auth/refresh",
                HttpMethod.POST
        ));
    }

    @Test
    void shouldRejectGetLoginAsPublic() {
        assertFalse(PublicPath.isPublic(
                "/api/auth/login",
                HttpMethod.GET
        ));
    }

    @Test
    void shouldRejectUnknownAuthEndpointAsPublic() {
        assertFalse(PublicPath.isPublic(
                "/api/auth/logout",
                HttpMethod.POST
        ));
    }

    @Test
    void shouldRejectUserEndpointAsPublic() {
        assertFalse(PublicPath.isPublic(
                "/api/users/me",
                HttpMethod.GET
        ));
    }
}