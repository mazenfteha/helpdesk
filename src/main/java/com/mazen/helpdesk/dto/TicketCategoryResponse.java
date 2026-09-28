package com.mazen.helpdesk.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.mazen.helpdesk.entity.TicketCategory;

public record TicketCategoryResponse(
        UUID id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TicketCategoryResponse from(TicketCategory category) {
        return new TicketCategoryResponse(category.getId(), category.getName(), category.getCreatedAt(),
                category.getUpdatedAt());
    }

}
