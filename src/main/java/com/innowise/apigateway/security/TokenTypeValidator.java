package com.innowise.apigateway.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class TokenTypeValidator implements OAuth2TokenValidator<Jwt> {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";

    private static final OAuth2Error INVALID_TOKEN_TYPE = new OAuth2Error(
            "invalid_token",
            "Only access tokens are allowed",
            null
    );

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String tokenType = jwt.getClaimAsString(TOKEN_TYPE_CLAIM);

        if (!ACCESS_TOKEN_TYPE.equals(tokenType)) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN_TYPE);
        }

        return OAuth2TokenValidatorResult.success();
    }
}