package com.grid07.assignment.service;

import com.grid07.assignment.dto.PostCreateRequest;
import com.grid07.assignment.model.Post;
import com.grid07.assignment.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    @Transactional
    public Post createPost(PostCreateRequest request) {
        Post post = new Post();
        post.setAuthorId(request.getAuthorId());
        post.setAuthorType(request.getAuthorType());
        post.setContent(request.getContent());

        return postRepository.save(post);
    }
}