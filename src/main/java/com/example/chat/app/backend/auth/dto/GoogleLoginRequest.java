package com.example.chat.app.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "Google credential is required")
        String credential
) {
}
