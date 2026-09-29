package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;

public class TicketActionNotAllowedException extends ApiException {

    public TicketActionNotAllowedException() {
        super(HttpStatus.FORBIDDEN, "You are not allowed to perform this action on this ticket");
    }
}
