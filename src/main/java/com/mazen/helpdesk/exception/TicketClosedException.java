package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;

public class TicketClosedException extends ApiException {

    public TicketClosedException() {
        super(HttpStatus.CONFLICT, "Ticket is closed");
    }
}
