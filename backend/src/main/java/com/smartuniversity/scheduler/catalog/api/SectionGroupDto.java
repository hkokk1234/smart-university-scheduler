package com.smartuniversity.scheduler.catalog.api;

import java.util.UUID;


public record SectionGroupDto(UUID id, UUID courseId, UUID semesterId, String code, String name, String type) {
}
