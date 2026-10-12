package com.smartuniversity.scheduler.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.Locale;

public record LoginRequest(
        @NotBlank @Email @Size(max =254) String email,
        @NotBlank @Size(max =72) @Utf8Password
        @Schema(accessMode =Schema.AccessMode.WRITE_ONLY, format ="password") String password) {
    public LoginRequest {
        if (email !=null) email =email.strip().toLowerCase(Locale.ROOT);
    }
}
