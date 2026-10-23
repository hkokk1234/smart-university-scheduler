package com.smartuniversity.scheduler.scheduling.application;

import com.smartuniversity.scheduler.catalog.api.*;
import com.smartuniversity.scheduler.catalog.application.CourseService;
import com.smartuniversity.scheduler.scheduling.api.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduleRepairServiceTest {
    final UUID semester =id(1), course =id(2), schedule =id(3), user =id(4);
    final CourseService catalog =mock(CourseService.class);
    final ScheduleGenerationService service =new ScheduleGenerationService(catalog, 3000, 100, 5000);
    UUID id(int n) { return new UUID(0, n); }
    SectionDto section(int n, int group, int hour) {
        return new SectionDto(id(n), id(group), semester, "S" + n, 30, 0, course, "CS101", "Algorithms",
                List.of(new MeetingDto(id(100 + n), id(n), DayOfWeek.MONDAY,
                        LocalTime.of(hour, 0), LocalTime.of(hour + 1, 0), null, null)));
    }
    CourseGroupDto group(int n, SectionDto... sections) {
        return new CourseGroupDto(new SectionGroupDto(id(n), course, semester, "G" + n, "Group " + n, "LECTURE"), List.of(sections));
    }
    void catalog(CourseGroupDto... groups) {
        when(catalog.details(course)).thenReturn(new CourseDetailDto(
                new CourseDto(course, semester, "CS101", "Algorithms", BigDecimal.valueOf(6)), List.of(groups)));
    }
    SavedScheduleDto saved(SectionDto... sections) {
        return new SavedScheduleDto(schedule, user, semester, "Original", Instant.EPOCH,
                Arrays.stream(sections).map(section -> new ScheduleSectionDto(UUID.randomUUID(), schedule, section)).toList());
    }
    @Test void returnsExactChangesAndPreservesAllRequiredGroups() {
        SectionDto original =section(20, 10, 9), replacement =section(21, 10, 10), unchanged =section(22, 11, 12);
        catalog(group(10, original, replacement), group(11, unchanged));
        SavedScheduleDto saved =saved(original, unchanged);
        RepairResponse response =service.repair(saved, new RepairScheduleRequest(List.of(original.id()), 5, null, null));
        assertThat(response.originalScheduleId()).isEqualTo(schedule);
        assertThat(response.schedules()).hasSize(1);
        var repair =response.schedules().getFirst();
        assertThat(repair.changedSectionCount()).isEqualTo(1);
        assertThat(repair.changes()).containsExactly(new RepairResponse.SectionChange(id(10), original, replacement));
        assertThat(repair.schedule().selectedSectionIds()).containsExactly(replacement.id(), unchanged.id());
        assertThat(saved.sections()).extracting(s -> s.section().id()).containsExactly(original.id(), unchanged.id());
    }
    @Test void rejectsEmptyPartialAndDuplicateGroupDrafts() {
        SectionDto a =section(20, 10, 9), b =section(21, 11, 10);
        catalog(group(10, a), group(11, b));
        var request =new RepairScheduleRequest(null, null, null, null);
        assertThatThrownBy(() -> service.repair(saved(), request)).hasMessageContaining("empty draft");
        assertThatThrownBy(() -> service.repair(saved(a), request)).hasMessageContaining("partial draft");
        assertThatThrownBy(() -> service.repair(saved(a, a, b), request)).hasMessageContaining("exactly one section");
    }
    @Test void rejectsForeignOrDuplicateExclusionsAndReportsImpossibleRepair() {
        SectionDto a =section(20, 10, 9);
        catalog(group(10, a));
        assertThatThrownBy(() -> service.repair(saved(a), new RepairScheduleRequest(List.of(id(999)), null, null, null)))
                .hasMessageContaining("Excluded sections");
        assertThatThrownBy(() -> service.repair(saved(a), new RepairScheduleRequest(List.of(a.id(), a.id()), null, null, null)))
                .hasMessageContaining("Excluded sections");
        var response =service.repair(saved(a), new RepairScheduleRequest(List.of(a.id()), null, null, null));
        assertThat(response.status().name()).isEqualTo("UNSAT");
        assertThat(response.metrics().complete()).isTrue();
        assertThat(response.schedules()).isEmpty();
    }
}
