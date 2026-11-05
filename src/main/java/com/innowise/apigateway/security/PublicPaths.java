package com.innowise.apigateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

public final class PublicPaths {

    private static final PathPatternParser PARSER =
            new PathPatternParser();

    private static final PathPattern LOGIN =
            PARSER.parse("/api/auth/login");

    private static final PathPattern REGISTER =
            PARSER.parse("/api/auth/register");

    private static final PathPattern REFRESH =
            PARSER.parse("/api/auth/refresh");

    private PublicPaths() {
    }

    public static boolean isPublic(String path, HttpMethod method) {
        if (!HttpMethod.POST.equals(method)) {
            return false;
        }

        PathContainer pathContainer = PathContainer.parsePath(path);

        return LOGIN.matches(pathContainer)
                || REGISTER.matches(pathContainer)
                || REFRESH.matches(pathContainer);
    }
}