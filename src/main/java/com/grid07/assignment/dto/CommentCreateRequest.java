package com.grid07.assignment.dto;

import com.grid07.assignment.model.AuthorType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentCreateRequest {
    private Long authorId;
    private AuthorType authorType;
    private String content;
    private Long parentCommentId;
}