package com.grid07.assignment.service;

import com.grid07.assignment.dto.CommentCreateRequest;
import com.grid07.assignment.model.AuthorType;
import com.grid07.assignment.model.Comment;
import com.grid07.assignment.model.InteractionType;
import com.grid07.assignment.model.Post;
import com.grid07.assignment.repository.CommentRepository;
import com.grid07.assignment.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ViralityService viralityService;
    private final GuardrailService guardrailService;
    private final NotificationService notificationService;

    // Notice there is NO @Transactional here. We keep the Redis network calls outside the DB lock.
    public Comment createComment(Long postId, CommentCreateRequest request) {

        // 1. Fetch Post & Calculate Depth (Simple DB Reads)
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + postId));

        int depthLevel = 1;
        Long targetAuthorId = post.getAuthorId(); // Default to post author
        AuthorType targetAuthorType = post.getAuthorType();

        if (request.getParentCommentId() != null) {
            Comment parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found with ID: " + request.getParentCommentId()));
            depthLevel = parent.getDepthLevel() + 1;
            targetAuthorId = parent.getAuthorId(); // Update target to parent comment author
            targetAuthorType = parent.getAuthorType();
        }

        // 2. Enforce Guardrails (Redis Network Calls happen safely OUTSIDE the DB transaction!)

        // Vertical Cap applies to EVERYONE (Humans and Bots)
        guardrailService.checkVerticalCap(depthLevel);

        // Horizontal and Cooldown caps ONLY apply to Bots
        if (request.getAuthorType() == AuthorType.BOT) {
            guardrailService.checkHorizontalCap(postId);

            Long targetHumanId = (targetAuthorType == AuthorType.USER) ? targetAuthorId : null;
            if (targetHumanId != null) {
                guardrailService.checkCooldownCap(request.getAuthorId(), targetHumanId);
            }
        }

        // 3. Persist to DB (commentRepository.save() handles the transaction internally)
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(request.getAuthorId());
        comment.setAuthorType(request.getAuthorType());
        comment.setContent(request.getContent());
        comment.setDepthLevel(depthLevel);

        Comment savedComment = commentRepository.save(comment);

        // 4. Update Virality Score in Redis
        InteractionType interactionType = (request.getAuthorType() == AuthorType.BOT)
                ? InteractionType.BOT_REPLY
                : InteractionType.HUMAN_COMMENT;
        viralityService.recordInteraction(postId, interactionType);

        // 5. Trigger Smart Notifications (Task 8 - Notification Throttler)
        if (request.getAuthorType() == AuthorType.BOT && targetAuthorType == AuthorType.USER) {
            String notificationMessage = "Bot " + request.getAuthorId() + " commented: " + request.getContent();
            notificationService.processBotInteraction(targetAuthorId, notificationMessage);
        }

        return savedComment;
    }

    public void likePost(Long postId) {
        log.info("Registered a 'LIKE' interaction for Post ID: {}", postId);

        // Update Redis Virality Score (Assuming likes are only from humans for this assignment)
        viralityService.recordInteraction(postId, InteractionType.HUMAN_LIKE);
    }


}