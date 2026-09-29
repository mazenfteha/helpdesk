package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;

public class CommentNotAllowedException extends ApiException {
    public CommentNotAllowedException() {
        super(HttpStatus.FORBIDDEN, "Comment not allowed");
    }

}
