package com.example.chat.app.backend.payload;

import com.example.chat.app.backend.entities.Message;

public record MessageEvent(MessageEventType type, String messageId, Message message) {
    public static MessageEvent updated(Message message) {
        return new MessageEvent(MessageEventType.UPDATED, message.getId(), message);
    }

    public static MessageEvent deleted(String messageId) {
        return new MessageEvent(MessageEventType.DELETED, messageId, null);
    }
}
