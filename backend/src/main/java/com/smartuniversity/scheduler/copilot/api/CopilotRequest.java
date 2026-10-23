package com.smartuniversity.scheduler.copilot.api;

import com.smartuniversity.scheduler.scheduling.api.GenerateScheduleRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public record CopilotRequest(@NotNull UUID semesterId, @NotBlank @Size(max=2000) String message,
        @Size(max=8) List<@NotNull UUID> courseIds,
        @Size(max=6) List<@NotNull @Valid Turn> history,
        @Valid GenerateScheduleRequest.PreferenceRequest preferences) {
    public enum Role { USER, ASSISTANT }
    public record Turn(@NotNull Role role, @NotBlank @Size(max=8000) String content) {}
}
