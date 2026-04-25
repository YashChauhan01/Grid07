package com.grid07.assignment.controller;

import com.grid07.assignment.dto.CommentCreateRequest;
import com.grid07.assignment.dto.PostCreateRequest;
import com.grid07.assignment.model.Comment;
import com.grid07.assignment.model.Post;
import com.grid07.assignment.service.InteractionService;
import com.grid07.assignment.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final InteractionService interactionService;

    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody PostCreateRequest request) {
        Post createdPost = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> createComment(
            @PathVariable Long postId,
            @RequestBody CommentCreateRequest request) {

        Comment createdComment = interactionService.createComment(postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> likePost(@PathVariable Long postId) {
        interactionService.likePost(postId);
        return ResponseEntity.ok().build();
    }
}