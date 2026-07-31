package com.example.chat.app.backend.auth.security;

import java.security.Principal;

public record ChatUserPrincipal(String id, String name, String email) implements Principal {
    @Override
    public String getName() {
        return name;
    }
}
