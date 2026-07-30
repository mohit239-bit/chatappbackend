package com.example.chat.app.backend.ai.service;

import com.example.chat.app.backend.ai.dto.ProcessedMessageResponse;
import com.example.chat.app.backend.ai.dto.ReplySuggestionsResponse;
import com.example.chat.app.backend.ai.exception.AiProcessingException;
import com.example.chat.app.backend.ai.model.DetectedLanguage;
import com.example.chat.app.backend.ai.model.ProcessingAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ChatAiServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @InjectMocks
    private ChatAiService chatAiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void processMessage_emptyString_returnsNoChange() {
        ProcessedMessageResponse response = chatAiService.processMessage("");
        assertNotNull(response);
        assertEquals(ProcessingAction.NO_CHANGE, response.action());
        assertFalse(response.changed());
    }

    @Test
    void processMessage_success() {
        ProcessedMessageResponse expected = new ProcessedMessageResponse(
                "Mujhe kal test Dena hai",
                "I have to give a test tomorrow.",
                DetectedLanguage.HINGLISH,
                ProcessingAction.TRANSLATED,
                true
        );

        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .entity(ProcessedMessageResponse.class))
                .thenReturn(expected);

        ProcessedMessageResponse response = chatAiService.processMessage("Mujhe kal test Dena hai");

        assertNotNull(response);
        assertEquals("I have to give a test tomorrow.", response.processedText());
        assertEquals(DetectedLanguage.HINGLISH, response.detectedLanguage());
        assertEquals(ProcessingAction.TRANSLATED, response.action());
        assertTrue(response.changed());
    }

    @Test
    void generateReplySuggestions_success() {
        ReplySuggestionsResponse expected = new ReplySuggestionsResponse(
                List.of("Can you share the log?", "I can check that.", "Let me investigate.")
        );

        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .entity(ReplySuggestionsResponse.class))
                .thenReturn(expected);

        ReplySuggestionsResponse response = chatAiService.generateReplySuggestions("Alice: Having an issue", "Bob");

        assertNotNull(response);
        assertEquals(3, response.suggestions().size());
        assertEquals("Can you share the log?", response.suggestions().get(0));
    }

    @Test
    void generateReplySuggestions_aiError_throwsAiProcessingException() {
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .entity(ReplySuggestionsResponse.class))
                .thenThrow(new RuntimeException("API connection refused"));

        assertThrows(AiProcessingException.class, () ->
                chatAiService.generateReplySuggestions("Alice: Hi", "Bob")
        );
    }
}
