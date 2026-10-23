package com.smartuniversity.scheduler.scheduling.api;

import com.smartuniversity.scheduler.catalog.api.SectionDto;
import com.smartuniversity.scheduler.scheduling.solver.TimetableModel;
import java.util.List;
import java.util.UUID;

public record RepairResponse(UUID originalScheduleId, TimetableModel.Status status,
        List<RepairedSchedule> schedules, TimetableModel.Metrics metrics) {
    public record SectionChange(UUID groupId, SectionDto before, SectionDto after) {}
    public record RepairedSchedule(GenerationResponse.GeneratedSchedule schedule, int changedSectionCount,
                                   List<SectionChange> changes) {}
}
