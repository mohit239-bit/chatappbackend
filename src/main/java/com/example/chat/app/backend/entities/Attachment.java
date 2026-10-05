package com.example.chat.app.backend.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    private String name;
    private long size;
    private String type;
    private String dataUrl;
    private boolean isImage;
}
