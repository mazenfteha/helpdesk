package com.mazen.helpdesk.exception;

import org.springframework.http.HttpStatus;

public class TicketAlreadyAssignedException extends ApiException {

    public TicketAlreadyAssignedException() {
        super(HttpStatus.CONFLICT, "Ticket is already assigned");
    }
}
