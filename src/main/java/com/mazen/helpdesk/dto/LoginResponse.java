package com.mazen.helpdesk.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {

}
