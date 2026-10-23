package com.smartuniversity.scheduler.platform.security;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix ="security.jwt")
public record JwtProperties(@NotBlank String secretBase64, @NotBlank String issuer, @NotBlank String audience,
                            @Min(60) @Max(3600) long ttlSeconds) {}
