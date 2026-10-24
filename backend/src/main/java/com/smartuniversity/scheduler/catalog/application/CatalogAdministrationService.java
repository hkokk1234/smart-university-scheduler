package com.smartuniversity.scheduler.catalog.application;

import com.smartuniversity.scheduler.catalog.api.*;
import com.smartuniversity.scheduler.catalog.domain.*;
import com.smartuniversity.scheduler.catalog.infrastructure.*;
import com.smartuniversity.scheduler.platform.api.ApiException;
<<<<<<< HEAD
import com.smartuniversity.scheduler.notifications.ChangeService;
=======
>>>>>>> origin/main
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.UUID;

@Service
@Transactional
@PreAuthorize("hasRole('ADMIN')")
public class CatalogAdministrationService {
    private final CourseRepository courses;
    private final SemesterRepository semesters;
    private final SectionRepository sections;
    private final InstructorRepository instructors;
    private final RoomRepository rooms;
<<<<<<< HEAD
    private final ChangeService changes;

    public CatalogAdministrationService(CourseRepository courses, SemesterRepository semesters, SectionRepository sections,
                                        InstructorRepository instructors, RoomRepository rooms, ChangeService changes) {
        this.courses =courses; this.semesters =semesters; this.sections =sections;
        this.instructors =instructors; this.rooms =rooms;
        this.changes =changes;
=======

    public CatalogAdministrationService(CourseRepository courses, SemesterRepository semesters, SectionRepository sections,
                                        InstructorRepository instructors, RoomRepository rooms) {
        this.courses =courses; this.semesters =semesters; this.sections =sections;
        this.instructors =instructors; this.rooms =rooms;
>>>>>>> origin/main
    }

    public CourseDto createCourse(CreateCourseRequest request) {
        Semester semester =semesters.findById(request.semesterId()).orElseThrow(() -> ApiException.notFound("Semester"));
        Course course =courses.saveAndFlush(new Course(semester, request.code(), request.title(), request.credits()));
        return CatalogDtoMapper.toDto(course);
    }

    @Transactional(readOnly =true)
    public java.util.List<RoomDto> rooms() {
        return rooms.findAll(org.springframework.data.domain.Sort.by("building", "code"))
                .stream().map(CatalogDtoMapper::toDto).toList();
    }

    @Transactional(readOnly =true)
    public java.util.List<InstructorDto> instructors() {
        return instructors.findAll(org.springframework.data.domain.Sort.by("displayName", "id"))
                .stream().map(CatalogDtoMapper::toDto).toList();
    }

<<<<<<< HEAD
    public SectionDto updateSection(UUID id, UpdateSectionRequest request, UUID actorId) {
        Section section =sections.findById(id).orElseThrow(() -> ApiException.notFound("Section"));
        if (section.getVersion() !=request.version()) throw ApiException.conflict("Section was changed; reload it before updating");
        SectionDto before =CatalogDtoMapper.toDto(section);
=======
    public SectionDto updateSection(UUID id, UpdateSectionRequest request) {
        Section section =sections.findById(id).orElseThrow(() -> ApiException.notFound("Section"));
        if (section.getVersion() !=request.version()) throw ApiException.conflict("Section was changed; reload it before updating");
>>>>>>> origin/main
        var slots =new HashSet<String>();
        for (MeetingRequest meeting : request.meetings()) {
            String slot =meeting.weekday() + "/" + meeting.startTime() + "/" + meeting.endTime();
            if (!slots.add(slot)) throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Duplicate meeting slot");
        }
        var assignments =request.meetings().stream().map(meeting -> new Assignment(meeting,
                meeting.instructorId() ==null ? null : instructors.findById(meeting.instructorId())
                        .orElseThrow(() -> ApiException.notFound("Instructor")),
                meeting.roomId() ==null ? null : rooms.findById(meeting.roomId())
                        .orElseThrow(() -> ApiException.notFound("Room")))).toList();
        section.updateDetails(request.code(), request.capacity());
        section.clearMeetings();
        // Delete old slots before inserting replacements; the deferred minimum-meeting check runs at commit.
        sections.flush();
        for (Assignment assignment : assignments) {
            MeetingRequest meeting =assignment.request();
            section.addMeeting(meeting.weekday(), meeting.startTime(), meeting.endTime(), assignment.instructor(), assignment.room());
        }
        sections.saveAndFlush(section);
<<<<<<< HEAD
        SectionDto after =CatalogDtoMapper.toDto(section);
        changes.record(before, after, actorId);
        return after;
=======
        return CatalogDtoMapper.toDto(section);
>>>>>>> origin/main
    }

    private record Assignment(MeetingRequest request, Instructor instructor, Room room) {}
}
