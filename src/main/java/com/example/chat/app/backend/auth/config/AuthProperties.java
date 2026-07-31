package com.example.chat.app.backend.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import jakarta.annotation.PostConstruct;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    private String jwtSecret;
    private long jwtExpirationMinutes = 60;
    private long webSocketTokenExpirationMinutes = 2;
    private String cookieName = "chat_access_token";
    private boolean cookieSecure = false;
    private String cookieSameSite = "Lax";
    private String googleClientId;

    @PostConstruct
    void validate() {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("APP_AUTH_JWT_SECRET must contain at least 32 bytes");
        }
        if (jwtExpirationMinutes <= 0 || webSocketTokenExpirationMinutes <= 0) {
            throw new IllegalStateException("JWT expiration values must be greater than zero");
        }
    }
}
