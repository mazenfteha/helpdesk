package com.mazen.helpdesk.entity;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;


        public boolean canTransitionTo(TicketStatus target) {
        return switch (this) {
            case OPEN -> target == IN_PROGRESS || target == CLOSED;
            case IN_PROGRESS -> target == RESOLVED;
            case RESOLVED -> target == CLOSED || target == IN_PROGRESS;
            case CLOSED -> false;
        };
    }
}
