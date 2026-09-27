package com.mazen.helpdesk.dto;

import java.util.UUID;

import com.mazen.helpdesk.entity.TicketPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        @Size(max = 5000)
        String description,

        @NotNull
        UUID categoryId,

        TicketPriority priority
) {

}
