package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class TicketClosedException extends RuntimeException {

    public TicketClosedException() {
        super("Ticket is closed");
    }
}
