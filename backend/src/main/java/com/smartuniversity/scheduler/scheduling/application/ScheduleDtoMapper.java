package com.smartuniversity.scheduler.scheduling.application;

import com.smartuniversity.scheduler.catalog.application.CatalogDtoMapper;
import com.smartuniversity.scheduler.scheduling.api.SavedScheduleDto;
import com.smartuniversity.scheduler.scheduling.api.ScheduleSectionDto;
import com.smartuniversity.scheduler.scheduling.domain.SavedSchedule;
import com.smartuniversity.scheduler.scheduling.domain.ScheduleSection;
import java.util.Comparator;

public final class ScheduleDtoMapper {
    private ScheduleDtoMapper() {}

    public static ScheduleSectionDto toDto(ScheduleSection selection) {
        return new ScheduleSectionDto(selection.getId(), selection.getSavedSchedule().getId(),
                CatalogDtoMapper.toDto(selection.getSection()));
    }

    public static SavedScheduleDto toDto(SavedSchedule schedule) {
        var selections =schedule.getSelections().stream()
                .sorted(Comparator.comparing(selection -> selection.getSectionGroup().getId()))
                .map(ScheduleDtoMapper::toDto).toList();
        return new SavedScheduleDto(schedule.getId(), schedule.getUser().getId(), schedule.getSemester().getId(),
                schedule.getName(), schedule.getCreatedAt(), selections);
    }
}
