package com.example.chat.app.backend.Controller;


import com.example.chat.app.backend.Respository.RoomRepository;
import com.example.chat.app.backend.Service.RoomService;
import com.example.chat.app.backend.auth.security.ChatUserPrincipal;
import com.example.chat.app.backend.entities.Message;
import com.example.chat.app.backend.entities.Room;
import com.example.chat.app.backend.payload.EditMessageRequest;
import com.example.chat.app.backend.payload.MessageEvent;
import com.example.chat.app.backend.payload.MessageRequest;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import java.security.Principal;

import java.time.Instant;
import java.util.UUID;

@Controller
public class ChatController {

    private final RoomRepository roomRepository;
    private final RoomService roomService;

    public ChatController(RoomRepository roomRepository, RoomService roomService) {
        this.roomRepository = roomRepository;
        this.roomService = roomService;
    }

    @MessageMapping("/sendMessage/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public Message sendMessage(@DestinationVariable String roomId,
                               MessageRequest request,
                               Principal principal) throws Exception {

        ChatUserPrincipal user = authenticatedUser(principal);
        Room room = roomService.getRoom(roomId);

        Message message = new Message();
        message.setId(UUID.randomUUID().toString());
        message.setContent(request.getContent());
        message.setSender(user.name());
        message.setSenderId(user.id());
        message.setTimeStamp(Instant.now());

        if(room != null){
            room.getMessages().add(message);
            roomRepository.save(room);
        }else {
            throw new RuntimeException("Room not found");
        }

        return message;
    }

    @MessageMapping("/editMessage/{roomId}/{messageId}")
    @SendTo("/topic/room/{roomId}/events")
    public MessageEvent editMessage(@DestinationVariable String roomId,
                                    @DestinationVariable String messageId,
                                    @Valid EditMessageRequest request,
                                    Principal principal) {
        ChatUserPrincipal user = authenticatedUser(principal);
        Room room = requireRoom(roomId);
        Message message = findOwnedMessage(room, messageId, user);
        String content = request.content().trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Message content is required");
        }

        message.setContent(content);
        message.setUpdatedAt(Instant.now());
        roomRepository.save(room);
        return MessageEvent.updated(message);
    }

    @MessageMapping("/deleteMessage/{roomId}/{messageId}")
    @SendTo("/topic/room/{roomId}/events")
    public MessageEvent deleteMessage(@DestinationVariable String roomId,
                                      @DestinationVariable String messageId,
                                      Principal principal) {
        ChatUserPrincipal user = authenticatedUser(principal);
        Room room = requireRoom(roomId);
        Message message = findOwnedMessage(room, messageId, user);
        room.getMessages().remove(message);
        roomRepository.save(room);
        return MessageEvent.deleted(messageId);
    }

    private Message findOwnedMessage(Room room, String messageId, ChatUserPrincipal user) {
        Message message = room.getMessages().stream()
                .filter(candidate -> messageId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        if (!user.id().equals(message.getSenderId())) {
            throw new SecurityException("You can only modify your own messages");
        }
        return message;
    }

    private Room requireRoom(String roomId) {
        Room room = roomService.getRoom(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        return room;
    }

    private ChatUserPrincipal authenticatedUser(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof ChatUserPrincipal user) {
            return user;
        }
        if (principal instanceof ChatUserPrincipal user) {
            return user;
        }
        throw new SecurityException("Authentication is required to modify messages");
    }
}
