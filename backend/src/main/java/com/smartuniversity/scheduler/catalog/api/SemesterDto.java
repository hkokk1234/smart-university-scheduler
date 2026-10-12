package com.smartuniversity.scheduler.catalog.api;

import java.util.UUID;
import java.time.LocalDate;

public record SemesterDto(UUID id, String code, String name, LocalDate startsOn, LocalDate endsOn, String timeZone) {
}
