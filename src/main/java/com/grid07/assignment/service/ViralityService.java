package com.grid07.assignment.service;

import com.grid07.assignment.model.InteractionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViralityService {

    private final StringRedisTemplate redisTemplate;

    public void recordInteraction(Long postId, InteractionType type) {
        String key = "post:" + postId + ":virality_score";

        // This executes an atomic INCRBY operation in Redis
        Long updatedScore = redisTemplate.opsForValue().increment(key, type.getScore());

        log.info("Updated Virality Score for Post ID {}: +{} (New Total: {})",
                postId, type.getScore(), updatedScore);
    }
}