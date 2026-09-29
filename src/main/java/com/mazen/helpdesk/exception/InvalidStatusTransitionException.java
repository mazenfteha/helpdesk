package com.mazen.helpdesk.exception;

import com.mazen.helpdesk.entity.TicketStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(TicketStatus from, TicketStatus to) {
        super("Cannot change ticket status from " + from + " to " + to);
    }
}
