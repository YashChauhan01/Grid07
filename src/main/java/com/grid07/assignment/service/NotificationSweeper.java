package com.grid07.assignment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSweeper {

    private final StringRedisTemplate redisTemplate;

    // This CRON expression means: "Run at minute 0, 5, 10, 15... etc., of every hour"
    @Scheduled(cron = "0 */5 * * * *")
    public void processBatchedNotifications() {
        log.info("Waking up: Sweeping Redis for batched notifications...");

        Set<String> keys = redisTemplate.keys("user:*:pending_notifs");

        if (keys == null || keys.isEmpty()) {
            log.info("No pending notifications found.");
            return;
        }

        for (String key : keys) {
            String userId = key.split(":")[1];

            List<String> messages = redisTemplate.opsForList().range(key, 0, -1);

            if (messages != null && !messages.isEmpty()) {
                log.info("[DIGEST FOR USER {}] You had {} new bot interactions:", userId, messages.size());
                for (String msg : messages) {
                    log.info("   -> {}", msg);
                }

                redisTemplate.delete(key);
            }
        }

        log.info("Sweep complete.");
    }
}