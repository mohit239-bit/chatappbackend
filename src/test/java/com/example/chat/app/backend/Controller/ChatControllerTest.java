package com.example.chat.app.backend.Controller;

import com.example.chat.app.backend.Respository.RoomRepository;
import com.example.chat.app.backend.Service.RoomService;
import com.example.chat.app.backend.auth.security.ChatUserPrincipal;
import com.example.chat.app.backend.entities.Message;
import com.example.chat.app.backend.entities.Room;
import com.example.chat.app.backend.payload.EditMessageRequest;
import com.example.chat.app.backend.payload.MessageEvent;
import com.example.chat.app.backend.payload.MessageEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomService roomService;

    private ChatController chatController;
    private Room room;
    private Message message;

    @BeforeEach
    void setUp() {
        chatController = new ChatController(roomRepository, roomService);
        message = new Message();
        message.setId("message-1");
        message.setSenderId("user-1");
        message.setSender("Alice");
        message.setContent("Original message");
        message.setTimeStamp(Instant.now());

        room = new Room();
        room.setRoomId("room-1");
        room.setMessages(new ArrayList<>(java.util.List.of(message)));
        when(roomService.getRoom("room-1")).thenReturn(room);
    }

    @Test
    void editMessageUpdatesOwnedMessageAndPublishesEvent() {
        MessageEvent event = chatController.editMessage(
                "room-1",
                "message-1",
                new EditMessageRequest("Updated message"),
                principal("user-1", "Alice"));

        assertEquals(MessageEventType.UPDATED, event.type());
        assertEquals("message-1", event.messageId());
        assertEquals("Updated message", message.getContent());
        assertNotNull(message.getUpdatedAt());
        verify(roomRepository).save(room);
    }

    @Test
    void deleteMessageRejectsAnotherUsersMessage() {
        assertThrows(SecurityException.class,
                () -> chatController.deleteMessage("room-1", "message-1", principal("user-2", "Bob")));

        verify(roomRepository, never()).save(room);
    }

    @Test
    void deleteMessageRemovesOwnedMessageAndPublishesEvent() {
        MessageEvent event = chatController.deleteMessage(
                "room-1",
                "message-1",
                principal("user-1", "Alice"));

        assertEquals(MessageEventType.DELETED, event.type());
        assertEquals("message-1", event.messageId());
        assertNull(event.message());
        assertEquals(0, room.getMessages().size());
        verify(roomRepository).save(room);
    }

    private UsernamePasswordAuthenticationToken principal(String id, String name) {
        return new UsernamePasswordAuthenticationToken(new ChatUserPrincipal(id, name, name + "@example.com"), null);
    }
}
