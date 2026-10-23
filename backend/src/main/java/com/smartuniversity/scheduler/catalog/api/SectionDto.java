package com.smartuniversity.scheduler.catalog.api;

import java.util.UUID;
import java.util.List;

public record SectionDto(UUID id, UUID sectionGroupId, UUID semesterId, String code, int capacity, long version,
                         UUID courseId, String courseCode, String courseTitle,
                         List<MeetingDto> meetings) {
    public SectionDto {
        meetings =List.copyOf(meetings);
    }
}
