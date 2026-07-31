package com.example.chat.app.backend.auth.dto;

import com.example.chat.app.backend.entities.AuthProvider;
import com.example.chat.app.backend.entities.User;

import java.time.Instant;

public record UserResponse(
        String id,
        String name,
        String email,
        String profilePicture,
        AuthProvider provider,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getProfilePicture(),
                user.getProvider(),
                user.getCreatedAt()
        );
    }
}
