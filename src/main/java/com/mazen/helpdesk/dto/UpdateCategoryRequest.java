package com.mazen.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(

        @NotBlank
        @Size(max = 100)
        String name
) {
}
