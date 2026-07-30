package com.example.chat.app.backend.ai.dto;

import com.example.chat.app.backend.ai.model.DetectedLanguage;
import com.example.chat.app.backend.ai.model.ProcessingAction;

public record ProcessedMessageResponse(
        String originalText,
        String processedText,
        DetectedLanguage detectedLanguage,
        ProcessingAction action,
        boolean changed
) {
}
