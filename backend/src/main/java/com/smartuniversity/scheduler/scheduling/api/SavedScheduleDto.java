package com.smartuniversity.scheduler.scheduling.api;

import java.util.UUID;
import java.time.Instant;
import java.util.List;

public record SavedScheduleDto(UUID id, UUID userId, UUID semesterId, String name, Instant createdAt,
                               List<ScheduleSectionDto> sections) {
    public SavedScheduleDto {
        sections =List.copyOf(sections);
    }
}
