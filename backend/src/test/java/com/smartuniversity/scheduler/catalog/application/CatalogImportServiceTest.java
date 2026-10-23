package com.smartuniversity.scheduler.catalog.application;

import com.smartuniversity.scheduler.catalog.api.*;
import com.smartuniversity.scheduler.catalog.domain.*;
import com.smartuniversity.scheduler.catalog.infrastructure.*;
import com.smartuniversity.scheduler.platform.api.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogImportServiceTest {
    final SemesterRepository semesters =mock(SemesterRepository.class);
    final CourseRepository courses =mock(CourseRepository.class);
    final SectionGroupRepository groups =mock(SectionGroupRepository.class);
    final SectionRepository sections =mock(SectionRepository.class);
    final CatalogImportService importer =new CatalogImportService(semesters, courses, groups, sections);
    final UUID semesterId =UUID.randomUUID();
    final Semester semester =new Semester("TEST", "Test semester", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 1, 31), "Europe/Athens");
    CatalogImportRequest.Row row(String day, String start, String end) {
        return new CatalogImportRequest.Row("CS101", "Algorithms", "6,0", "LEC", "Θεωρία", "A", "30", day, start, end);
    }
    CatalogImportRequest request(CatalogImportRequest.Row... rows) { return new CatalogImportRequest(semesterId, List.of(rows)); }
    @BeforeEach void setup() {
        when(semesters.findById(semesterId)).thenReturn(Optional.of(semester));
        when(courses.findBySemesterIdOrderByCodeAsc(semesterId)).thenReturn(List.of());
        when(courses.save(any())).thenAnswer(call -> call.getArgument(0));
        when(groups.save(any())).thenAnswer(call -> call.getArgument(0));
    }
    @Test void validationNormalizesGreekDaysDecimalCreditsAndTimesWithoutWrites() {
        var report =importer.validate(request(row("Δευτέρα", "9.00", "11.00"), row("Τετάρτη", "09:00", "10:00")));
        assertThat(report.valid()).isTrue();
        assertThat(report.courseCount()).isEqualTo(1);
        assertThat(report.sectionCount()).isEqualTo(1);
        assertThat(report.meetingCount()).isEqualTo(2);
        verify(courses, never()).save(any()); verifyNoInteractions(groups, sections);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test void commitBuildsOneSectionWithAllMeetingsAndRevalidates() {
        var summary =importer.commit(request(row("Δευτέρα", "9.00", "11.00"), row("3", "09:00", "10:00")));
        assertThat(summary).isEqualTo(new CatalogImportReport.Imported(1, 1, 1, 2));
        ArgumentCaptor<Iterable<Section>> capture =ArgumentCaptor.forClass((Class) Iterable.class);
        verify(sections).saveAll(capture.capture()); verify(sections).flush();
        Section section =capture.getValue().iterator().next();
        assertThat(section.getMeetings()).hasSize(2);
        assertThat(section.getMeetings().getFirst().getWeekday()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(section.getMeetings().getFirst().getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(section.getSectionGroup().getType()).isEqualTo(SectionGroupType.LECTURE);
        verify(courses).findBySemesterIdOrderByCodeAsc(semesterId);
    }
    @Test void reportsCellErrorsAndRejectsInvalidBatchBeforeWritingAnything() {
        var invalid =new CatalogImportRequest.Row("", "", "no", "", "unknown", "", "-1", "bad", "25:00", "09:00");
        var request =request(row("MON", "09:00", "11:00"), invalid);
        var report =importer.validate(request);
        assertThat(report.valid()).isFalse();
        assertThat(report.errors().getFirst().rowIndex()).isEqualTo(1);
        assertThat(report.errors().getFirst().fields()).containsKeys("courseCode", "title", "credits", "groupType", "capacity", "weekday", "startTime");
        assertThatThrownBy(() -> importer.commit(request)).isInstanceOf(ApiException.class).hasMessageContaining("invalid");
        verify(courses, never()).save(any()); verifyNoInteractions(groups, sections);
    }
    @Test void detectsDuplicatesOverlapsAndInconsistentCourseMetadata() {
        var report =importer.validate(request(row("MON", "09:00", "11:00"), row("MONDAY", "10:00", "12:00")));
        assertThat(report.errors()).hasSize(2).allSatisfy(error -> assertThat(error.fields()).containsKey("startTime"));
        var different =new CatalogImportRequest.Row("CS101", "Different title", "5", "LEC", "LECTURE", "B", "30", "MON", "09:00", "11:00");
        assertThat(importer.validate(request(row("MON", "09:00", "11:00"), different)).errors().getFirst().fields()).containsKey("title");
        var duplicate =row("MON", "09:00", "11:00");
        assertThat(importer.validate(request(duplicate, duplicate)).valid()).isFalse();
        assertThat(importer.validate(request(row("MON", "09:00", "11:00"), row("MON", "11:00", "12:00"))).valid()).isTrue();
        assertThat(importer.validate(request(row("MON", "11:00", "09:00"))).errors().getFirst().fields()).containsKey("endTime");
    }
    @Test void reusesMatchingCourseAndGroupButRejectsExistingSectionsAndMetadataChanges() {
        Course course =new Course(semester, "CS101", "Algorithms", new BigDecimal("6.0"));
        UUID courseId =UUID.randomUUID(); ReflectionTestUtils.setField(course, "id", courseId);
        SectionGroup group =new SectionGroup(course, "LEC", "Lectures", SectionGroupType.LECTURE);
        UUID groupId =UUID.randomUUID(); ReflectionTestUtils.setField(group, "id", groupId);
        when(courses.findBySemesterIdOrderByCodeAsc(semesterId)).thenReturn(List.of(course));
        when(groups.findByCourseIdOrderByCodeAsc(courseId)).thenReturn(List.of(group));
        when(sections.findBySectionGroupIdOrderByCodeAsc(groupId)).thenReturn(List.of());
        assertThat(importer.commit(request(row("MON", "09:00", "11:00"))))
                .isEqualTo(new CatalogImportReport.Imported(0, 0, 1, 1));
        verify(courses, never()).save(any()); verify(groups, never()).save(any());
        when(sections.findBySectionGroupIdOrderByCodeAsc(groupId)).thenReturn(List.of(new Section(group, "A", 30)));
        assertThat(importer.validate(request(row("MON", "09:00", "11:00"))).errors().getFirst().fields()).containsKey("sectionCode");
        var metadata =new CatalogImportRequest.Row("CS101", "Other", "6", "LEC", "LAB", "B", "30", "MON", "09:00", "11:00");
        assertThat(importer.validate(request(metadata)).errors().getFirst().fields()).containsKeys("title", "groupType");
    }
}
