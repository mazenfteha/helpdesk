package com.mazen.helpdesk.dto;

import com.mazen.helpdesk.entity.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        String content,
        UserSummary author,
        LocalDateTime createdAt
) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                UserSummary.from(comment.getUser()),
                comment.getCreatedAt()
        );
    }
}
