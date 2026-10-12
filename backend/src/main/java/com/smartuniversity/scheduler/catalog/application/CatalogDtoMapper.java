package com.smartuniversity.scheduler.catalog.application;

import com.smartuniversity.scheduler.catalog.api.*;
import com.smartuniversity.scheduler.catalog.domain.*;
import java.util.Comparator;

public final class CatalogDtoMapper {
    private CatalogDtoMapper() {}

    public static SemesterDto toDto(Semester semester) {
        return new SemesterDto(semester.getId(), semester.getCode(), semester.getName(),
                semester.getStartsOn(), semester.getEndsOn(), semester.getTimeZone());
    }

    public static CourseDto toDto(Course course) {
        return new CourseDto(course.getId(), course.getSemester().getId(), course.getCode(),
                course.getTitle(), course.getCredits());
    }

    public static SectionGroupDto toDto(SectionGroup group) {
        return new SectionGroupDto(group.getId(), group.getCourse().getId(), group.getSemester().getId(),
                group.getCode(), group.getName(), group.getType().name());
    }

    public static InstructorDto toDto(Instructor instructor) {
        return new InstructorDto(instructor.getId(), instructor.getStaffCode(),
                instructor.getDisplayName(), instructor.getEmail());
    }

    public static RoomDto toDto(Room room) {
        return new RoomDto(room.getId(), room.getBuilding(), room.getCode(), room.getCapacity());
    }

    public static MeetingDto toDto(Meeting meeting) {
        return new MeetingDto(meeting.getId(), meeting.getSection().getId(), meeting.getWeekday(),
                meeting.getStartTime(), meeting.getEndTime(),
                meeting.getInstructor() ==null ? null : toDto(meeting.getInstructor()),
                meeting.getRoom() ==null ? null : toDto(meeting.getRoom()));
    }

    public static SectionDto toDto(Section section) {
        var meetings =section.getMeetings().stream()
                .sorted(Comparator.comparing(Meeting::getWeekday).thenComparing(Meeting::getStartTime)
                        .thenComparing(Meeting::getId))
                .map(CatalogDtoMapper::toDto).toList();
        return new SectionDto(section.getId(), section.getSectionGroup().getId(), section.getSemester().getId(),
                section.getCode(), section.getCapacity(), section.getVersion(),
                section.getSectionGroup().getCourse().getId(), section.getSectionGroup().getCourse().getCode(),
                section.getSectionGroup().getCourse().getTitle(), meetings);
    }
}
