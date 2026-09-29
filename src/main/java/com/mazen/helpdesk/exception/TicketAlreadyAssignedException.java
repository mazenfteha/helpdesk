package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class TicketAlreadyAssignedException extends RuntimeException {

    public TicketAlreadyAssignedException() {
        super("Ticket is already assigned");
    }
}
