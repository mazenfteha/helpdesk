package com.mazen.helpdesk.dto;

import com.mazen.helpdesk.entity.Ticket;
import com.mazen.helpdesk.entity.TicketPriority;
import com.mazen.helpdesk.entity.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        CategorySummary category,
        UserSummary customer,
        UserSummary assignedTo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                CategorySummary.from(ticket.getCategory()),
                UserSummary.from(ticket.getCustomer()),
                UserSummary.from(ticket.getAssignedTo()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
