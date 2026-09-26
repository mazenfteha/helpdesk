package com.mazen.helpdesk.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mazen.helpdesk.dto.LoginRequest;
import com.mazen.helpdesk.dto.LoginResponse;
import com.mazen.helpdesk.dto.RegisterRequest;
import com.mazen.helpdesk.dto.UserResponse;
import com.mazen.helpdesk.entity.User;
import com.mazen.helpdesk.entity.Role;
import com.mazen.helpdesk.exception.EmailAlreadyUsedException;
import com.mazen.helpdesk.exception.InvalidCredentialsException;
import com.mazen.helpdesk.repository.UserRepository;
import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email))
            throw new EmailAlreadyUsedException();

        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(Role.CUSTOMER);

        user = userRepository.saveAndFlush(user);
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        return tokenService.issueToken(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
