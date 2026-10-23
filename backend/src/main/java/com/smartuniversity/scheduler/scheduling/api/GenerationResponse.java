package com.smartuniversity.scheduler.scheduling.api;

import com.smartuniversity.scheduler.catalog.api.*;
import com.smartuniversity.scheduler.scheduling.solver.TimetableModel;
import java.util.*;

public record GenerationResponse(TimetableModel.Status status, List<GeneratedSchedule> schedules, TimetableModel.Metrics metrics) {
    public record GeneratedSchedule(List<UUID> selectedSectionIds, List<SectionDto> sections,
            List<MeetingDto> weeklyMeetings, double optimizationScore, List<TimetableModel.ScorePart> scoreExplanation) {}
}
