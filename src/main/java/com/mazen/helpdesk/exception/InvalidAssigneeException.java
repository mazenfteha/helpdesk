package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidAssigneeException extends RuntimeException {

    public InvalidAssigneeException() {
        super("Assignee must be an existing agent");
    }
}
