package com.mazen.helpdesk.exception;

import com.mazen.helpdesk.entity.TicketStatus;
import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends ApiException {

    public InvalidStatusTransitionException(TicketStatus from, TicketStatus to) {
        super(HttpStatus.CONFLICT, "Cannot change ticket status from " + from + " to " + to);
    }
}
