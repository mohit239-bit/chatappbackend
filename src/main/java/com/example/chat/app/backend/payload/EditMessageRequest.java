package com.example.chat.app.backend.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditMessageRequest(
        @NotBlank(message = "Message content is required")
        @Size(max = 1000, message = "Message cannot exceed 1000 characters")
        String content
) {
}
