package com.innowise.apigateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicPathsTest {

    @Test
    void shouldAllowPostLoginWithoutToken() {
        assertTrue(PublicPaths.isPublic(
                "/api/auth/login",
                HttpMethod.POST
        ));
    }

    @Test
    void shouldAllowPostRegisterWithoutToken() {
        assertTrue(PublicPaths.isPublic(
                "/api/auth/register",
                HttpMethod.POST
        ));
    }

    @Test
    void shouldAllowPostRefreshWithoutToken() {
        assertTrue(PublicPaths.isPublic(
                "/api/auth/refresh",
                HttpMethod.POST
        ));
    }

    @Test
    void shouldRejectGetLoginAsPublic() {
        assertFalse(PublicPaths.isPublic(
                "/api/auth/login",
                HttpMethod.GET
        ));
    }

    @Test
    void shouldRejectUnknownAuthEndpointAsPublic() {
        assertFalse(PublicPaths.isPublic(
                "/api/auth/logout",
                HttpMethod.POST
        ));
    }

    @Test
    void shouldRejectUserEndpointAsPublic() {
        assertFalse(PublicPaths.isPublic(
                "/api/users/me",
                HttpMethod.GET
        ));
    }
}