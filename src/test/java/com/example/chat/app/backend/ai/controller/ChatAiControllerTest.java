package com.example.chat.app.backend.ai.controller;

import com.example.chat.app.backend.ai.dto.ProcessMessageRequest;
import com.example.chat.app.backend.ai.dto.ProcessedMessageResponse;
import com.example.chat.app.backend.ai.dto.ReplySuggestionsResponse;
import com.example.chat.app.backend.ai.exception.GlobalExceptionHandler;
import com.example.chat.app.backend.ai.model.DetectedLanguage;
import com.example.chat.app.backend.ai.model.ProcessingAction;
import com.example.chat.app.backend.ai.ratelimit.RateLimiterService;
import com.example.chat.app.backend.ai.service.ChatAiService;
import com.example.chat.app.backend.ai.service.ChatContextService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatAiController.class)
@ContextConfiguration(classes = {ChatAiController.class, GlobalExceptionHandler.class})
class ChatAiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatAiService chatAiService;

    @MockBean
    private ChatContextService chatContextService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        when(rateLimiterService.tryAcquireSuggestion(anyString(), anyString())).thenReturn(true);
        when(rateLimiterService.tryAcquireDraftProcessing(anyString())).thenReturn(true);
    }

    @Test
    void getReplySuggestions_success() throws Exception {
        when(chatContextService.getFormattedRecentMessages("room123", "Alice"))
                .thenReturn("Bob: Hi Alice");
        when(chatAiService.generateReplySuggestions("Bob: Hi Alice", "Alice"))
                .thenReturn(new ReplySuggestionsResponse(List.of("Hello Bob!", "How are you?", "What's up?")));

        mockMvc.perform(get("/api/v1/ai/rooms/room123/suggestions")
                        .header("X-Current-User", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0]").value("Hello Bob!"))
                .andExpect(jsonPath("$.suggestions.length()").value(3));
    }

    @Test
    void getReplySuggestions_rateLimitExceeded_returns429() throws Exception {
        when(rateLimiterService.tryAcquireSuggestion(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(get("/api/v1/ai/rooms/room123/suggestions")
                        .header("X-Current-User", "Alice"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void processMessage_success() throws Exception {
        ProcessMessageRequest req = new ProcessMessageRequest("Mujhe help chahiye");
        ProcessedMessageResponse resp = new ProcessedMessageResponse(
                "Mujhe help chahiye", "I need help", DetectedLanguage.HINGLISH, ProcessingAction.TRANSLATED, true
        );

        when(chatAiService.processMessage("Mujhe help chahiye")).thenReturn(resp);

        mockMvc.perform(post("/api/v1/ai/process-message")
                        .header("X-Current-User", "Alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedText").value("I need help"))
                .andExpect(jsonPath("$.detectedLanguage").value("HINGLISH"))
                .andExpect(jsonPath("$.action").value("TRANSLATED"));
    }

    @Test
    void processMessage_invalidInput_returns400() throws Exception {
        ProcessMessageRequest req = new ProcessMessageRequest("");

        mockMvc.perform(post("/api/v1/ai/process-message")
                        .header("X-Current-User", "Alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
