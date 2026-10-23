package com.smartuniversity.scheduler.scheduling.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record RepairScheduleRequest(
        @Size(max =100) List<@NotNull UUID> excludedSectionIds,
        @Min(1) @Max(5) Integer maxResults,
        @Min(1) @Max(60000) Long timeLimitMillis,
        @Valid GenerateScheduleRequest.PreferenceRequest preferences) {
    public List<UUID> exclusions() { return excludedSectionIds ==null ? List.of() : List.copyOf(excludedSectionIds); }
}
