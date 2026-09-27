package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class TicketNotEditableException extends RuntimeException {

    public TicketNotEditableException() {
        super("Only open tickets can be edited");
    }
}
