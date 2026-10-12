package com.smartuniversity.scheduler.digitaltwin.simulation;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import com.smartuniversity.scheduler.digitaltwin.simulation.CampusSimulation.*;

class CampusSimulationTest {
    private final Room room = new Room(UUID.randomUUID(), "Science", "101", 30);
    private Slot slot(String start, String end, int attendees) {
        return new Slot(UUID.randomUUID(), "CS101", DayOfWeek.MONDAY, LocalTime.parse(start), LocalTime.parse(end), room.id(), attendees);
    }
    @Test void overlappingMeetingsCountUnionAndReportCapacity() {
        var report = CampusSimulation.simulate(List.of(room), List.of(slot("09:00", "11:00", 40), slot("10:00", "12:00", 20)));
        assertThat(report.occupiedRoomHours()).isEqualTo(3);
        assertThat(report.alerts()).extracting(Alert::type).containsExactly("CAPACITY", "ROOM_CONFLICT");
        assertThat(report.peaks().getFirst().estimatedAttendance()).isEqualTo(60);
        assertThat(report.peaks().getFirst().peakTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(report.peaks().getFirst().simultaneousMeetings()).isEqualTo(2);
    }
    @Test void touchingMeetingsDoNotOverlapOrInflatePeaks() {
        var report = CampusSimulation.simulate(List.of(room), List.of(slot("09:00", "10:00", 30), slot("10:00", "11:00", 20)));
        assertThat(report.alerts()).isEmpty();
        assertThat(report.peaks().getFirst().estimatedAttendance()).isEqualTo(30);
        assertThat(report.occupiedRoomHours()).isEqualTo(2);
    }
    @Test void includesWeekendAndUnassignedMeetingsAndUnusedRooms() {
        var meeting = new Slot(UUID.randomUUID(), "Lab", DayOfWeek.SUNDAY, LocalTime.NOON, LocalTime.of(13, 0), null, 12);
        var report = CampusSimulation.simulate(List.of(room), List.of(meeting));
        assertThat(report.unassignedMeetings()).isEqualTo(1);
        assertThat(report.rooms().getFirst().occupiedHours()).isZero();
        assertThat(report.peaks().getLast().estimatedAttendance()).isEqualTo(12);
    }
    @Test void rejectsInvalidIntervalsAndUnknownRooms() {
        assertThatThrownBy(() -> CampusSimulation.simulate(List.of(room), List.of(slot("11:00", "10:00", 10)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CampusSimulation.simulate(List.of(), List.of(slot("09:00", "10:00", 10)))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void deterministicAndDoesNotMutateInputs() {
        var slots = List.of(slot("09:00", "11:00", 10));
        assertThat(CampusSimulation.simulate(List.of(room), slots)).isEqualTo(CampusSimulation.simulate(List.of(room), slots));
        assertThat(slots.getFirst().startTime()).isEqualTo(LocalTime.of(9, 0));
    }
}
