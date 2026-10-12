package com.smartuniversity.scheduler.catalog.api;
import java.util.List;
public record CourseDetailDto(CourseDto course, List<CourseGroupDto> sectionGroups) {
    public CourseDetailDto { sectionGroups =List.copyOf(sectionGroups); }
}
