package com.example.chat.app.backend.ai.service;

import com.example.chat.app.backend.Respository.RoomRepository;
import com.example.chat.app.backend.ai.config.AiConfigProperties;
import com.example.chat.app.backend.ai.exception.RoomNotFoundException;
import com.example.chat.app.backend.entities.Message;
import com.example.chat.app.backend.entities.Room;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatContextService {

    private final RoomRepository roomRepository;
    private final AiConfigProperties properties;

    public ChatContextService(RoomRepository roomRepository, AiConfigProperties properties) {
        this.roomRepository = roomRepository;
        this.properties = properties;
    }

    /**
     * Retrieves recent messages from the room formatted chronologically for prompt context.
     */
    public String getFormattedRecentMessages(String roomId, String requestingUser) {
        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) {
            throw new RoomNotFoundException("Room with ID '" + roomId + "' not found.");
        }

        List<Message> messages = room.getMessages();
        if (messages == null || messages.isEmpty()) {
            return "No prior messages in room.";
        }

        int limit = properties.getRecentMessageLimit();
        int maxMsgLen = properties.getMaxMessageLength();

        int start = Math.max(0, messages.size() - limit);
        List<Message> recentMessages = messages.subList(start, messages.size());

        StringBuilder contextBuilder = new StringBuilder();
        for (Message msg : recentMessages) {
            if (msg.getContent() == null || msg.getContent().isBlank()) {
                continue;
            }
            String sender = msg.getSender() != null ? msg.getSender() : "Unknown";
            String content = msg.getContent().trim();
            if (content.length() > maxMsgLen) {
                content = content.substring(0, maxMsgLen) + "...";
            }
            contextBuilder.append(sender).append(": ").append(content).append("\n");
        }

        return contextBuilder.toString().trim();
    }
}
