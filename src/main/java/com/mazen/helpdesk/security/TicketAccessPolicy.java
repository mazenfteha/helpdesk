package com.mazen.helpdesk.security;

import org.springframework.stereotype.Component;

import com.mazen.helpdesk.entity.Ticket;

@Component 
public class TicketAccessPolicy {

        public boolean canView(Ticket ticket, CurrentUser currentUser) {
        return switch (currentUser.role()) {
            case ADMIN -> true;
            case CUSTOMER -> isCustomer(ticket, currentUser);
            case AGENT -> ticket.getAssignedTo() == null || isAssignee(ticket, currentUser);
        };
    }

        public boolean canComment(Ticket ticket, CurrentUser currentUser) {
        return switch (currentUser.role()) {
            case ADMIN -> true;
            case CUSTOMER -> isCustomer(ticket, currentUser);
            case AGENT -> isAssignee(ticket, currentUser);
        };
    }

    private boolean isCustomer(Ticket ticket, CurrentUser currentUser) {
        return ticket.getCustomer().getId().equals(currentUser.id());
    }

    private boolean isAssignee(Ticket ticket, CurrentUser currentUser) {
        return ticket.getAssignedTo() != null
                && ticket.getAssignedTo().getId().equals(currentUser.id());
    }

}
