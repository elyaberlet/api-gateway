package com.innowise.apigateway.dto;

import java.time.Instant;
import java.util.Map;

public record SecurityErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> validationErrors
) {
}
