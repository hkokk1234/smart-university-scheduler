package com.smartuniversity.scheduler.scheduling.application;

import com.smartuniversity.scheduler.catalog.api.*;
import com.smartuniversity.scheduler.catalog.application.CourseService;
import com.smartuniversity.scheduler.scheduling.api.*;
import com.smartuniversity.scheduler.scheduling.solver.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
import static com.smartuniversity.scheduler.scheduling.solver.TimetableModel.*;

@Service
public class ScheduleGenerationService {
    private final CourseService catalog;
    private final long maxMillis;
    private final int maxGroups, maxOptions;
    private final TimetableSolver solver =new TimetableSolver();
    public ScheduleGenerationService(CourseService catalog,
            @Value("${scheduler.solver.max-time-millis:3000}") long maxMillis,
            @Value("${scheduler.solver.max-groups:100}") int maxGroups,
            @Value("${scheduler.solver.max-options:5000}") int maxOptions) {
        if (maxMillis < 1 || maxMillis > 60000 || maxGroups < 1 || maxGroups > 100 || maxOptions < 1 || maxOptions > 10000)
            throw new IllegalArgumentException("Invalid solver configuration");
        this.catalog =catalog; this.maxMillis =maxMillis; this.maxGroups =maxGroups; this.maxOptions =maxOptions;
    }
    public GenerationResponse generate(GenerateScheduleRequest request) {
        Set<UUID> courseIds =new HashSet<>(request.courseIds());
        if (courseIds.size() !=request.courseIds().size()) throw new IllegalArgumentException("Duplicate courses");
        List<Group> groups =new ArrayList<>(); Map<UUID, SectionDto> sections =new HashMap<>();
        for (UUID courseId : courseIds.stream().sorted(Comparator.comparing(UUID::toString)).toList()) {
            CourseDetailDto detail =catalog.details(courseId);
            if (!detail.course().semesterId().equals(request.semesterId()) || detail.sectionGroups().isEmpty())
                throw new IllegalArgumentException("Course must belong to this semester and have required groups");
            for (CourseGroupDto group : detail.sectionGroups()) {
                List<Option> options =new ArrayList<>();
                for (SectionDto section : group.sections()) {
                    sections.put(section.id(), section);
                    options.add(new Option(section.id(), section.sectionGroupId(), section.courseId(), section.semesterId(),
                            section.meetings().stream().map(m -> new Slot(m.weekday(), m.startTime(), m.endTime())).toList()));
                    if (sections.size() > maxOptions) throw new IllegalArgumentException("Too many available sections");
                }
                groups.add(new Group(group.group().id(), group.group().courseId(), options));
                if (groups.size() > maxGroups) throw new IllegalArgumentException("Too many required groups");
            }
        }
        Result result =solver.solve(new Problem(request.semesterId(), courseIds, groups, request.preferenceModel()),
                request.maxResults() ==null ? 5 : request.maxResults(),
                request.timeLimitMillis() ==null ? maxMillis : Math.min(maxMillis, request.timeLimitMillis()));
        return new GenerationResponse(result.status(), result.schedules().stream().map(solution -> {
            List<SectionDto> selected =solution.selectedSectionIds().stream().map(sections::get).toList();
            List<MeetingDto> meetings =selected.stream().flatMap(s -> s.meetings().stream()).sorted(
                    Comparator.comparing(MeetingDto::weekday).thenComparing(MeetingDto::startTime).thenComparing(m -> m.id().toString())).toList();
            return new GenerationResponse.GeneratedSchedule(solution.selectedSectionIds(), selected, meetings,
                    solution.optimizationScore(), solution.scoreExplanation());
        }).toList(), result.metrics());
    }
}
