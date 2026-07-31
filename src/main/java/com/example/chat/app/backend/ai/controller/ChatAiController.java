package com.example.chat.app.backend.ai.controller;

import com.example.chat.app.backend.ai.dto.ApiErrorResponse;
import com.example.chat.app.backend.ai.dto.ProcessMessageRequest;
import com.example.chat.app.backend.ai.dto.ProcessedMessageResponse;
import com.example.chat.app.backend.ai.dto.ReplySuggestionsResponse;
import com.example.chat.app.backend.ai.model.DetectedLanguage;
import com.example.chat.app.backend.ai.model.ProcessingAction;
import com.example.chat.app.backend.ai.ratelimit.RateLimiterService;
import com.example.chat.app.backend.ai.service.ChatAiService;
import com.example.chat.app.backend.ai.service.ChatContextService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class ChatAiController {

    private final ChatAiService chatAiService;
    private final ChatContextService chatContextService;
    private final RateLimiterService rateLimiterService;

    public ChatAiController(ChatAiService chatAiService,
                            ChatContextService chatContextService,
                            RateLimiterService rateLimiterService) {
        this.chatAiService = chatAiService;
        this.chatContextService = chatContextService;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Endpoint 1: Generate 3 reply suggestions based on room context.
     * GET /api/v1/ai/rooms/{roomId}/suggestions
     */
    @GetMapping("/rooms/{roomId}/suggestions")
    public ResponseEntity<?> getReplySuggestions(
            @PathVariable String roomId,
            java.security.Principal currentUser
    ) {
        String userName = currentUser.getName();
        if (!rateLimiterService.tryAcquireSuggestion(userName, roomId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiErrorResponse("RATE_LIMIT_EXCEEDED", "Too many suggestion requests. Please try again in a minute."));
        }

        String conversationContext = chatContextService.getFormattedRecentMessages(roomId, userName);
        ReplySuggestionsResponse suggestions = chatAiService.generateReplySuggestions(conversationContext, userName);

        return ResponseEntity.ok(suggestions);
    }

    /**
     * Endpoint 2: Process message draft (Language detection, Hindi/Hinglish translation, English grammar correction).
     * POST /api/v1/ai/process-message
     */
    @PostMapping("/process-message")
    public ResponseEntity<?> processMessage(
            @Valid @RequestBody ProcessMessageRequest request,
            java.security.Principal currentUser
    ) {
        String userName = currentUser.getName();
        String draft = request.message().trim();

        // Guard against unnecessary AI processing (too short, single emoji, pure URL)
        if (draft.length() < 3 || isPureUrl(draft)) {
            return ResponseEntity.ok(new ProcessedMessageResponse(
                    draft, draft, DetectedLanguage.OTHER, ProcessingAction.NO_CHANGE, false
            ));
        }

        if (!rateLimiterService.tryAcquireDraftProcessing(userName)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiErrorResponse("RATE_LIMIT_EXCEEDED", "Too many draft processing requests. Please try again in a minute."));
        }

        ProcessedMessageResponse response = chatAiService.processMessage(draft);
        return ResponseEntity.ok(response);
    }

    private boolean isPureUrl(String text) {
        return text.matches("^https?://\\S+$");
    }
}
