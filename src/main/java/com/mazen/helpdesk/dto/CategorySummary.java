package com.mazen.helpdesk.dto;

import com.mazen.helpdesk.entity.TicketCategory;

import java.util.UUID;

public record CategorySummary(UUID id, String name) {

    public static CategorySummary from(TicketCategory category) {
        return new CategorySummary(category.getId(), category.getName());
    }
}
