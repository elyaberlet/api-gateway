package com.innowise.apigateway.dto;

import java.time.Instant;

public record SecurityErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message
) {
}
