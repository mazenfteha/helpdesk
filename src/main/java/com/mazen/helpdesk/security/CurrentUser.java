package com.mazen.helpdesk.security;

import com.mazen.helpdesk.entity.Role;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public record CurrentUser(UUID id, Role role) {

    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(
                UUID.fromString(jwt.getSubject()),
                Role.valueOf(jwt.getClaimAsString("role"))
        );
    }
}
