package com.smartuniversity.scheduler.digitaltwin.application;

import com.smartuniversity.scheduler.catalog.application.*;
import com.smartuniversity.scheduler.digitaltwin.api.SimulationRequest;
import com.smartuniversity.scheduler.digitaltwin.simulation.CampusSimulation;
import com.smartuniversity.scheduler.digitaltwin.simulation.CampusSimulation.*;
import com.smartuniversity.scheduler.platform.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.util.*;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class DigitalTwinService {
    private final CourseService courses;
    private final SemesterReadService semesters;
    private final CatalogAdministrationService administration;
    public DigitalTwinService(CourseService courses, SemesterReadService semesters, CatalogAdministrationService administration) {
        this.courses = courses; this.semesters = semesters; this.administration = administration;
    }
    public record Comparison(UUID semesterId, List<Room> rooms, List<Slot> meetings, Report baseline, Report scenario) {}
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Comparison simulate(SimulationRequest request) {
        semesters.get(request.semesterId());
        List<Slot> slots = new ArrayList<>();
        for (int page = 0; ; page++) {
            var result = courses.search(request.semesterId(), null, null, page, 100, "code,asc");
            if (result.totalElements() > 500) throw invalid("Simulation supports at most 500 courses per semester");
            for (var course : result.content()) {
                var detail = courses.details(course.id());
                for (var group : detail.sectionGroups()) for (var section : group.sections()) for (var meeting : section.meetings()) {
                    slots.add(new Slot(meeting.id(), course.code() + " / " + group.group().code() + " / " + section.code(),
                            meeting.weekday(), meeting.startTime(), meeting.endTime(), meeting.room() == null ? null : meeting.room().id(), section.capacity()));
                    if (slots.size() > 5000) throw invalid("Simulation supports at most 5000 meetings");
                }
            }
            if (page + 1 >= result.totalPages()) break;
        }
        var rooms = administration.rooms().stream().map(r -> new Room(r.id(), r.building(), r.code(), r.capacity())).toList();
        Map<UUID, Slot> scenario = new LinkedHashMap<>();
        slots.forEach(s -> scenario.put(s.id(), s));
        Set<UUID> changed = new HashSet<>();
        for (var change : request.changes()) {
            if (!changed.add(change.meetingId())) throw invalid("Duplicate meeting change");
            Slot original = scenario.get(change.meetingId());
            if (original == null) throw invalid("Meeting does not belong to this semester");
            scenario.put(original.id(), new Slot(original.id(), original.label(), change.weekday(), change.startTime(), change.endTime(), change.roomId(), change.attendance()));
        }
        try {
            return new Comparison(request.semesterId(), rooms, List.copyOf(slots), CampusSimulation.simulate(rooms, slots),
                    CampusSimulation.simulate(rooms, List.copyOf(scenario.values())));
        } catch (IllegalArgumentException e) { throw invalid(e.getMessage()); }
    }
    private static ApiException invalid(String message) { return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SIMULATION", message); }
}
