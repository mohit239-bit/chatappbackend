package com.example.chat.app.backend.payload;

import com.example.chat.app.backend.entities.Attachment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {

    private String content;
    private String sender;
    private String roomId;
    private List<Attachment> attachments;
}
