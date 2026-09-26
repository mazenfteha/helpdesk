package com.mazen.helpdesk.service;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.mazen.helpdesk.config.JwtProperties;
import com.mazen.helpdesk.dto.LoginResponse;
import com.mazen.helpdesk.entity.User;

import java.time.Instant;


@Service 
public class TokenService {
        private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

        public LoginResponse issueToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("helpdesk-api")
                .issuedAt(now)
                .expiresAt(now.plus(properties.expiration()))
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new LoginResponse(token, "Bearer", properties.expiration().toSeconds());
    }
}
