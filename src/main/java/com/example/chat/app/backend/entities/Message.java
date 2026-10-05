package com.example.chat.app.backend.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "messages")
public class Message {

    private String id;
    private String senderId;
    private String sender;
    private String content;
    private List<Attachment> attachments = new ArrayList<>();
    private Instant timeStamp;
    private Instant updatedAt;

    public Message(String sender, String content, LocalDateTime timeStamp) {
        this.sender = sender;
        this.content = content;
        this.timeStamp = Instant.now();
    }
}
