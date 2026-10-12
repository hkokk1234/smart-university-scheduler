package com.smartuniversity.scheduler.catalog.api;

import java.util.UUID;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record MeetingDto(UUID id, UUID sectionId, DayOfWeek weekday, LocalTime startTime, LocalTime endTime,
                         InstructorDto instructor, RoomDto room) {
}
