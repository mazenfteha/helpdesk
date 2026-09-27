package com.mazen.helpdesk.dto;

import com.mazen.helpdesk.entity.User;

import java.util.UUID;

public record UserSummary(UUID id, String name) {

    public static UserSummary from(User user) {
        return user == null ? null : new UserSummary(user.getId(), user.getName());
    }
}
