package com.smartuniversity.scheduler.catalog.api;

import java.util.UUID;
import java.math.BigDecimal;

public record CourseDto(UUID id, UUID semesterId, String code, String title, BigDecimal credits) {
}
