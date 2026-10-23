package com.smartuniversity.scheduler.identity.api;

import java.util.UUID;
import java.time.Instant;

public record UserDto(UUID id, String email, String displayName, Instant createdAt, String role) {
}
