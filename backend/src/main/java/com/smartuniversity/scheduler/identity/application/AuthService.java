package com.smartuniversity.scheduler.identity.application;

import com.smartuniversity.scheduler.identity.api.*;
import com.smartuniversity.scheduler.identity.domain.User;
import com.smartuniversity.scheduler.identity.infrastructure.UserRepository;
import com.smartuniversity.scheduler.platform.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwords, TokenService tokens) {
        this.users =users;
        this.passwords =passwords;
        this.tokens =tokens;
        this.dummyHash =passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.findByEmail(request.email()).isPresent()) throw ApiException.conflict("Email is already registered");
        User user =users.saveAndFlush(new User(request.email(), request.displayName(), passwords.encode(request.password())));
        return tokens.issue(user);
    }

    @Transactional(readOnly =true)
    public AuthResponse login(LoginRequest request) {
        User user =users.findByEmail(request.email()).orElse(null);
        String hash =user ==null || user.getPasswordHash() ==null ? dummyHash : user.getPasswordHash();
        boolean matches =passwords.matches(request.password(), hash);
        if (user ==null || user.getPasswordHash() ==null || !matches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }
        return tokens.issue(user);
    }
}
