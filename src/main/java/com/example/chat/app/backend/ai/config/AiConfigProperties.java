package com.example.chat.app.backend.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.ai")
public class AiConfigProperties {

    private int recentMessageLimit = 25;
    private int maxMessageLength = 1000;
    private int requestTimeoutSeconds = 15;
    private int suggestionsPerRequest = 3;
    private int draftRateLimitPerMinute = 20;
    private int suggestionRateLimitPerMinute = 5;
}
