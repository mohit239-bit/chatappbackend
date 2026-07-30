package com.example.chat.app.backend.ai.service;

import com.example.chat.app.backend.Respository.RoomRepository;
import com.example.chat.app.backend.ai.config.AiConfigProperties;
import com.example.chat.app.backend.ai.exception.RoomNotFoundException;
import com.example.chat.app.backend.entities.Message;
import com.example.chat.app.backend.entities.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ChatContextServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private AiConfigProperties properties;

    @InjectMocks
    private ChatContextService chatContextService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(properties.getRecentMessageLimit()).thenReturn(25);
        when(properties.getMaxMessageLength()).thenReturn(1000);
    }

    @Test
    void getFormattedRecentMessages_success() {
        Room room = new Room();
        room.setRoomId("room1");
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("Alice", "Hello", LocalDateTime.now()));
        messages.add(new Message("Bob", "Hi Alice!", LocalDateTime.now()));
        room.setMessages(messages);

        when(roomRepository.findByRoomId("room1")).thenReturn(room);

        String context = chatContextService.getFormattedRecentMessages("room1", "Alice");

        assertNotNull(context);
        assertTrue(context.contains("Alice: Hello"));
        assertTrue(context.contains("Bob: Hi Alice!"));
    }

    @Test
    void getFormattedRecentMessages_roomNotFound_throwsException() {
        when(roomRepository.findByRoomId("nonexistent")).thenReturn(null);

        assertThrows(RoomNotFoundException.class, () ->
                chatContextService.getFormattedRecentMessages("nonexistent", "Alice")
        );
    }

    @Test
    void getFormattedRecentMessages_limitsToConfiguredCount() {
        Room room = new Room();
        room.setRoomId("room2");
        List<Message> messages = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            messages.add(new Message("User" + i, "Message " + i, LocalDateTime.now()));
        }
        room.setMessages(messages);

        when(properties.getRecentMessageLimit()).thenReturn(5);
        when(roomRepository.findByRoomId("room2")).thenReturn(room);

        String context = chatContextService.getFormattedRecentMessages("room2", "User30");

        assertFalse(context.contains("User1: Message 1"));
        assertTrue(context.contains("User26: Message 26"));
        assertTrue(context.contains("User30: Message 30"));
    }
}
