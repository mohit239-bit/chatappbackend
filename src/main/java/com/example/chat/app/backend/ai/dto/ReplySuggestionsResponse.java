package com.example.chat.app.backend.ai.dto;

import java.util.List;

public record ReplySuggestionsResponse(
        List<String> suggestions
) {
}
