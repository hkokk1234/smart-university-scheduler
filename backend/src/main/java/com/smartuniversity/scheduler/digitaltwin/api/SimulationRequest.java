package com.smartuniversity.scheduler.digitaltwin.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record SimulationRequest(@NotNull UUID semesterId, @NotNull @Size(max = 100) List<@NotNull @Valid Change> changes) {
    public record Change(@NotNull UUID meetingId, @NotNull DayOfWeek weekday,
                         @NotNull LocalTime startTime, @NotNull LocalTime endTime,
                         UUID roomId, @NotNull @Min(0) @Max(10000) Integer attendance) {}
}
