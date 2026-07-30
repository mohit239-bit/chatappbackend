package com.example.chat.app.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProcessMessageRequest(
        @NotBlank(message = "Message must not be blank")
        @Size(max = 1000, message = "Message exceeds maximum allowed length of 1000 characters")
        String message
) {
}
