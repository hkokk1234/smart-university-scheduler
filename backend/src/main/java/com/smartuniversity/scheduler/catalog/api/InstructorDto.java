package com.smartuniversity.scheduler.catalog.api;

import java.util.UUID;


public record InstructorDto(UUID id, String staffCode, String displayName, String email) {
}
