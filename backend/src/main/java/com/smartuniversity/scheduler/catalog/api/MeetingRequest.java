package com.smartuniversity.scheduler.catalog.api;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;
public record MeetingRequest(@NotNull DayOfWeek weekday, @NotNull LocalTime startTime, @NotNull LocalTime endTime,
                             UUID instructorId, UUID roomId) {
    @AssertTrue(message ="startTime must precede endTime")
    @JsonIgnore
    public boolean isTimeRangeValid() {
        return startTime ==null || endTime ==null || startTime.isBefore(endTime);
    }
}
