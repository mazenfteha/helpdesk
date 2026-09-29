package com.mazen.helpdesk.dto;

import com.mazen.helpdesk.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(

        @NotNull
        TicketStatus status
) {
}
