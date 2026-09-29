package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;

public class TicketNotEditableException extends ApiException {

    public TicketNotEditableException() {
        super(HttpStatus.CONFLICT, "Only open tickets can be edited");
    }
}
