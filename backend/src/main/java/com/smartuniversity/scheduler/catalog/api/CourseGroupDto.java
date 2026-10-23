package com.smartuniversity.scheduler.catalog.api;
import java.util.List;
public record CourseGroupDto(SectionGroupDto group, List<SectionDto> sections) {
    public CourseGroupDto { sections =List.copyOf(sections); }
}
