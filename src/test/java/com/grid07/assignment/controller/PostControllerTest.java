package com.grid07.assignment.controller;

import com.grid07.assignment.dto.CommentCreateRequest;
import com.grid07.assignment.model.AuthorType;
import com.grid07.assignment.model.Post;
import com.grid07.assignment.repository.CommentRepository;
import com.grid07.assignment.repository.PostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PostControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Long testPostId;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        Post post = new Post();
        post.setAuthorId(1L);
        post.setAuthorType(AuthorType.USER);
        post.setContent("Concurrency Test Post");
        Post savedPost = postRepository.save(post);
        this.testPostId = savedPost.getId();
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void horizontalCap_ShouldBlockTraffic_UnderHeavyConcurrency() throws InterruptedException {
        int totalRequests = 200;

        ExecutorService executorService = Executors.newFixedThreadPool(totalRequests);

        CountDownLatch readyLatch = new CountDownLatch(totalRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalRequests);

        String url = "/api/posts/" + testPostId + "/comments";

        for (int i = 0; i < totalRequests; i++) {
            final long uniqueBotId = 1000L + i; // 1000, 1001, 1002... ensuring we bypass the Cooldown Cap

            executorService.submit(() -> {
                try {
                    CommentCreateRequest request = new CommentCreateRequest();
                    request.setAuthorId(uniqueBotId);
                    request.setAuthorType(AuthorType.BOT);
                    request.setContent("Concurrent spam attack!");

                    readyLatch.countDown();
                    startLatch.await();

                    restTemplate.postForEntity(url, request, String.class);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();

        executorService.shutdown();

        // Acceptance criteria assertions
        long dbCommentCount = commentRepository.countByPostId(testPostId);
        assertThat(dbCommentCount).isEqualTo(100);

        String redisBotCount = redisTemplate.opsForValue().get("post:" + testPostId + ":bot_count");
        assertThat(redisBotCount).isEqualTo("100");

        System.out.println("✅ CONCURRENCY TEST PASSED! DB Count: " + dbCommentCount + " | Redis Count: " + redisBotCount);
    }
}