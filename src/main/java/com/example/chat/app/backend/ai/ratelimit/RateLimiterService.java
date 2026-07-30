package com.example.chat.app.backend.ai.ratelimit;

import com.example.chat.app.backend.ai.config.AiConfigProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final AiConfigProperties properties;
    private final Map<String, Bucket> draftBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> suggestionBuckets = new ConcurrentHashMap<>();

    public RateLimiterService(AiConfigProperties properties) {
        this.properties = properties;
    }

    public boolean tryAcquireDraftProcessing(String username) {
        Bucket bucket = draftBuckets.computeIfAbsent(username, this::createDraftBucket);
        return bucket.tryConsume(1);
    }

    public boolean tryAcquireSuggestion(String username, String roomId) {
        String key = username + ":" + roomId;
        Bucket bucket = suggestionBuckets.computeIfAbsent(key, k -> createSuggestionBucket());
        return bucket.tryConsume(1);
    }

    private Bucket createDraftBucket(String key) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(properties.getDraftRateLimitPerMinute())
                .refillIntervally(properties.getDraftRateLimitPerMinute(), Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createSuggestionBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(properties.getSuggestionRateLimitPerMinute())
                .refillIntervally(properties.getSuggestionRateLimitPerMinute(), Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
