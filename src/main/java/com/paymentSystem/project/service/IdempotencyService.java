package com.paymentSystem.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentSystem.project.dto.response.CachedResponse;
import com.paymentSystem.project.entity.IdempotencyRecord;
import com.paymentSystem.project.entity.Payments;
import com.paymentSystem.project.repos.IdempotencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Service
public class IdempotencyService {

    @Autowired
    RedisTemplate<String, String> redisTemplate ;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    IdempotencyRepository idempotencyRepository;

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

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = DataIntegrityViolationException.class
    )
    public void saveIdempotencyRecordSafely(String key, Payments payments) {
        try {
            // Create idempotency record
            IdempotencyRecord record = new IdempotencyRecord();
            record.setIdempotencyKey(key);
            record.setPayments(payments);
            record.setStatus(payments.getStatus());
            record.setResponse(objectMapper.writeValueAsString(payments));

            // Try to save to database
            idempotencyRepository.save(record);

        } catch (DataIntegrityViolationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save idempotency record", e);
        }
    }
}
