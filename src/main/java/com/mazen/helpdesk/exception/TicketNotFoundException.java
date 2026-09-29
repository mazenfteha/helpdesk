package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;

public class TicketNotFoundException extends ApiException {

    public TicketNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Ticket not found");
    }
}
