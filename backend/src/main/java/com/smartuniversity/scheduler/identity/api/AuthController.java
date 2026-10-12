package com.smartuniversity.scheduler.identity.api;

import com.smartuniversity.scheduler.identity.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name ="Authentication")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth =auth; }

    @PostMapping("/register")
    @Operation(summary ="Register a STUDENT and issue a short-lived access token")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).header("Cache-Control", "no-store").header("Pragma", "no-cache")
                .body(auth.register(request));
    }

    @PostMapping("/login")
    @Operation(summary ="Authenticate with email and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").header("Pragma", "no-cache").body(auth.login(request));
    }
}
