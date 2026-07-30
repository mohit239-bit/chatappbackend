package com.example.chat.app.backend.ai.dto;

import java.time.Instant;

public record ApiErrorResponse(
        String code,
        String message,
        Instant timestamp
) {
    public ApiErrorResponse(String code, String message) {
        this(code, message, Instant.now());
    }
}
