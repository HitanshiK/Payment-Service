package com.paymentSystem.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentSystem.project.dto.response.CachedResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class IdempotencyService {

    @Autowired
    RedisTemplate<String, String> redisTemplate ;
    @Autowired
    ObjectMapper objectMapper;

    private static final String PREFIX = "idem:payments:";

    public Optional<CachedResponse> getCachedResponse(String key) {
        String redisKey = PREFIX + key;

        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null) return Optional.empty();

        try {
            return Optional.of(objectMapper.readValue(json, CachedResponse.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize cached response");
        }
    }

    public void cacheResponse(String key, CachedResponse response) {
        String redisKey = PREFIX + key;

        try {
            String json = objectMapper.writeValueAsString(response);

            redisTemplate.opsForValue().set(
                    redisKey,
                    json,
                    Duration.ofHours(24)   // TTL
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize cached response");
        }
    }
}
