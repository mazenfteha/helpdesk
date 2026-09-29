package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class CommentNotAllowedException extends RuntimeException {
    public CommentNotAllowedException() {
        super("Comment not allowed");
    }

}
