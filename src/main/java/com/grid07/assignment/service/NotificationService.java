package com.grid07.assignment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final StringRedisTemplate redisTemplate;
    private static final Duration NOTIF_COOLDOWN = Duration.ofSeconds(900); // 15 minutes

    public void processBotInteraction(Long userId, String message) {
        if (userId == null) return;

        String cooldownKey = "notif:cooldown:user_" + userId;
        String pendingListKey = "user:" + userId + ":pending_notifs";

        // Atomic check-and-set: SET NX EX 900
        // Returns true if the key did not exist and was successfully set.
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "locked", NOTIF_COOLDOWN);

        if (Boolean.TRUE.equals(lockAcquired)) {
            // Key didn't exist -> User is not on cooldown
            log.info("Push Notification Sent to User {}", userId);
        } else {
            // Key existed -> User is on cooldown, batch the notification
            redisTemplate.opsForList().rightPush(pendingListKey, message);
            log.info("User {} on cooldown. Notification added to pending list.", userId);
        }
    }
}