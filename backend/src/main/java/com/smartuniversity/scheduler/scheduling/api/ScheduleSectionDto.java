package com.smartuniversity.scheduler.scheduling.api;

import java.util.UUID;
import com.smartuniversity.scheduler.catalog.api.SectionDto;

public record ScheduleSectionDto(UUID id, UUID savedScheduleId, SectionDto section) {
}
