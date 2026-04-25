package com.grid07.assignment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSweeper {

    private final StringRedisTemplate redisTemplate;

    @Scheduled(cron = "0 */5 * * * *")
    public void processBatchedNotifications() {
        log.info("🧹 Waking up: Sweeping Redis for batched notifications...");

        Set<String> keys = new HashSet<>();

        // 1. Safe SCAN implementation instead of the blocking KEYS command
        ScanOptions options = ScanOptions.scanOptions().match("user:*:pending_notifs").count(100).build();

        redisTemplate.execute((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next()));
                }
            } catch (Exception e) {
                log.error("Error scanning Redis keys", e);
            }
            return null;
        });

        if (keys.isEmpty()) {
            log.info("No pending notifications found. Going back to sleep.");
            return;
        }

        // 2. Process each user's queue
        for (String key : keys) {
            String userId = key.split(":")[1];
            List<String> messages = redisTemplate.opsForList().range(key, 0, -1);

            if (messages != null && !messages.isEmpty()) {

                String firstMsg = messages.get(0);
                String botName = "A Bot";
                String[] parts = firstMsg.split(" ");
                if (parts.length >= 2 && parts[0].equals("Bot")) {
                    botName = parts[0] + " " + parts[1]; // Evaluates to "Bot 101"
                }

                int othersCount = messages.size() - 1;

                // 3. Print the exact summarized digest format required by the assignment
                log.info("[DIGEST FOR USER {}] Summarized Push Notification: {} and [{}] others interacted with your posts.",
                        userId, botName, othersCount);

                // 4. Clear the queue
                redisTemplate.delete(key);
            }
        }

        log.info("Sweep complete. Cleared queues for {} users.", keys.size());
    }
}