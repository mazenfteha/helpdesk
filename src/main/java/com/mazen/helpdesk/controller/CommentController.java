package com.mazen.helpdesk.controller;

import org.springframework.web.bind.annotation.RestController;
import com.mazen.helpdesk.dto.CommentResponse;
import com.mazen.helpdesk.dto.CreateCommentRequest;
import com.mazen.helpdesk.security.CurrentUser;
import com.mazen.helpdesk.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;


import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(@PathVariable UUID ticketId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return commentService.addComment(ticketId, request, CurrentUser.from(jwt));
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable UUID ticketId, @AuthenticationPrincipal Jwt jwt) {
        return commentService.listComments(ticketId, CurrentUser.from(jwt));
    }
}