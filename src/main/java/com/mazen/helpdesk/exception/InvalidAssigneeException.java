package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;

public class InvalidAssigneeException extends ApiException {

    public InvalidAssigneeException() {
        super(HttpStatus.BAD_REQUEST, "Assignee must be an existing agent");
    }
}
