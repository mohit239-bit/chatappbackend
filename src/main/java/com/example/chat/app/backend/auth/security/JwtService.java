package com.example.chat.app.backend.auth.security;

import com.example.chat.app.backend.auth.config.AuthProperties;
import com.example.chat.app.backend.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final AuthProperties properties;

    public JwtService(AuthProperties properties) {
        this.properties = properties;
    }

    public String createToken(User user) {
        return createToken(user, "access", properties.getJwtExpirationMinutes());
    }

    public String createWebSocketToken(User user) {
        return createToken(user, "websocket", properties.getWebSocketTokenExpirationMinutes());
    }

    private String createToken(User user, String tokenType, long expirationMinutes) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(user.getId())
                .claim("tokenType", tokenType)
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("provider", user.getProvider().name())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }
}
