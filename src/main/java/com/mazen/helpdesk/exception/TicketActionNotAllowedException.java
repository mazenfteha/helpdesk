package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TicketActionNotAllowedException extends RuntimeException {

    public TicketActionNotAllowedException() {
        super("You are not allowed to perform this action on this ticket");
    }
}
