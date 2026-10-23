package com.smartuniversity.scheduler.scheduling.api;

import com.smartuniversity.scheduler.scheduling.solver.TimetableModel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

public record GenerateScheduleRequest(@NotNull UUID semesterId,
        @NotEmpty @Size(max =20) List<@NotNull UUID> courseIds,
        @Min(1) @Max(5) Integer maxResults, @Min(1) @Max(60000) Long timeLimitMillis,
        @Valid PreferenceRequest preferences) {
    public record UnavailableTime(@NotNull DayOfWeek weekday, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {
        TimetableModel.Slot slot() { return new TimetableModel.Slot(weekday, startTime, endTime); }
    }
    public record PreferenceRequest(@Size(max =100) List<@NotNull @Valid UnavailableTime> unavailableTimes,
            LocalTime preferredStart, LocalTime preferredEnd, Set<@NotNull DayOfWeek> preferredFreeDays,
            @Min(1) @Max(7) Integer maximumCampusDays, TimetableModel.TimeOfDay preferredPeriod,
            WeightRequest weights) {
        TimetableModel.Preferences model() {
            return new TimetableModel.Preferences(unavailableTimes ==null ? List.of() : unavailableTimes.stream().map(UnavailableTime::slot).toList(),
                    preferredStart, preferredEnd, preferredFreeDays ==null ? Set.of() : preferredFreeDays,
                    maximumCampusDays, preferredPeriod ==null ? TimetableModel.TimeOfDay.ANY : preferredPeriod,
                    weights ==null ? TimetableModel.Weights.defaults() : weights.model());
        }
    }
    public record WeightRequest(double gaps, double campusDays, double preferredTimes, double freeDays, double balance) {
        TimetableModel.Weights model() { return new TimetableModel.Weights(gaps, campusDays, preferredTimes, freeDays, balance); }
    }
    public TimetableModel.Preferences preferenceModel() {
        return preferences ==null ? TimetableModel.Preferences.defaults() : preferences.model();
    }
}
