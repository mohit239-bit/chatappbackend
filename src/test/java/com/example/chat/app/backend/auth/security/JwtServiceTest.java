package com.example.chat.app.backend.auth.security;

import com.example.chat.app.backend.auth.config.AuthProperties;
import com.example.chat.app.backend.entities.AuthProvider;
import com.example.chat.app.backend.entities.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtServiceTest {

    @Test
    void createsDistinctAccessAndWebSocketTokens() {
        AuthProperties properties = new AuthProperties();
        properties.setJwtSecret("test-only-secret-that-is-long-enough-for-hmac-signing");
        properties.setJwtExpirationMinutes(60);
        properties.setWebSocketTokenExpirationMinutes(2);

        User user = new User();
        user.setId("user-1");
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setProvider(AuthProvider.LOCAL);

        JwtService jwtService = new JwtService(properties);

        assertEquals("access", jwtService.parseClaims(jwtService.createToken(user)).get("tokenType", String.class));
        assertEquals("websocket", jwtService.parseClaims(jwtService.createWebSocketToken(user)).get("tokenType", String.class));
    }
}
