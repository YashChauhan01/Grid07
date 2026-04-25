package com.grid07.assignment.service;

import com.grid07.assignment.exception.CapExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuardrailService {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_BOT_REPLIES = 100;
    private static final int MAX_DEPTH_LEVEL = 20;
    private static final Duration COOLDOWN_DURATION = Duration.ofMinutes(10);

    // Master method to chain all checks in order
    public void enforceBotGuardrails(Long postId, int depthLevel, Long botId, Long targetHumanId) {
        checkHorizontalCap(postId);
        checkVerticalCap(depthLevel);

        // Only enforce cooldown if the bot is interacting with a human
        if (targetHumanId != null) {
            checkCooldownCap(botId, targetHumanId);
        }
    }

    public void checkHorizontalCap(Long postId) {
        String key = "post:" + postId + ":bot_count";
        Long currentBotCount = redisTemplate.opsForValue().increment(key);

        if (currentBotCount != null && currentBotCount > MAX_BOT_REPLIES) {
            redisTemplate.opsForValue().decrement(key);
            log.warn("Horizontal Cap Exceeded for Post ID {}. Request blocked.", postId);
            throw new CapExceededException("A single post cannot have more than " + MAX_BOT_REPLIES + " bot replies.");
        }
    }

    public void checkVerticalCap(int depthLevel) {
        if (depthLevel > MAX_DEPTH_LEVEL) {
            log.warn("Vertical Cap Exceeded: Attempted depth level {}", depthLevel);
            throw new CapExceededException("A comment thread cannot go deeper than " + MAX_DEPTH_LEVEL + " levels.");
        }
    }

    public void checkCooldownCap(Long botId, Long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;

        // Atomic 'SET NX EX' equivalent in Spring Boot
        // Returns true if the key was set (meaning no cooldown existed)
        // Returns false if the key already exists (meaning they are still on cooldown)
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(key, "locked", COOLDOWN_DURATION);

        if (Boolean.FALSE.equals(lockAcquired)) {
            log.warn("Cooldown Cap Exceeded for Bot {} attempting to interact with Human {}", botId, humanId);
            throw new CapExceededException("Bot is in a 10-minute cooldown period for this user.");
        }
    }
}