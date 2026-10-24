package com.smartuniversity.scheduler.scheduling.application;

import com.smartuniversity.scheduler.catalog.api.*;
import com.smartuniversity.scheduler.catalog.application.CourseService;
import com.smartuniversity.scheduler.scheduling.api.*;
import com.smartuniversity.scheduler.scheduling.solver.*;
<<<<<<< HEAD
import com.smartuniversity.scheduler.platform.api.ApiException;
import org.springframework.http.HttpStatus;
=======
>>>>>>> origin/main
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
<<<<<<< HEAD
        CatalogProblem loaded =load(request);
        Result result =solver.solve(loaded.problem(), limit(request.maxResults()), budget(request.timeLimitMillis()));
        return new GenerationResponse(result.status(), result.schedules().stream()
                .map(solution -> generated(solution, loaded.sections())).toList(), result.metrics());
    }
    public RepairResponse repair(SavedScheduleDto original, RepairScheduleRequest request) {
        if (original.sections().isEmpty()) throw invalidRepair("An empty draft cannot be repaired");
        List<UUID> courses =original.sections().stream().map(s -> s.section().courseId()).distinct().toList();
        if (courses.size() > 20) throw invalidRepair("Repair supports at most 20 courses");
        CatalogProblem loaded =load(new GenerateScheduleRequest(original.semesterId(), courses,
                request.maxResults(), request.timeLimitMillis(), request.preferences()));
        Map<UUID, SectionDto> baseline =new HashMap<>();
        for (ScheduleSectionDto entry : original.sections()) {
            SectionDto section =entry.section();
            if (baseline.put(section.sectionGroupId(), section) !=null)
                throw invalidRepair("Repair requires exactly one section per required group");
        }
        Set<UUID> required =new HashSet<>();
        loaded.problem().groups().forEach(group -> required.add(group.id()));
        if (!required.equals(baseline.keySet()))
            throw invalidRepair("This is a partial draft. Select one section for every required group before repairing");
        Set<UUID> excluded =new HashSet<>(request.exclusions());
        if (excluded.size() !=request.exclusions().size() || !loaded.sections().keySet().containsAll(excluded))
            throw invalidRepair("Excluded sections must be unique and belong to this schedule's courses");
        Problem problem =loaded.problem();
        Problem filtered =new Problem(problem.semesterId(), problem.courseIds(), problem.groups().stream()
                .map(group -> new Group(group.id(), group.courseId(), group.options().stream()
                        .filter(option -> !excluded.contains(option.id())).toList())).toList(), problem.preferences());
        Map<UUID, UUID> originalIds =new HashMap<>();
        baseline.forEach((group, section) -> originalIds.put(group, section.id()));
        Result result =solver.repair(filtered, originalIds, limit(request.maxResults()), budget(request.timeLimitMillis()));
        return new RepairResponse(original.id(), result.status(), result.schedules().stream().map(solution -> {
            GenerationResponse.GeneratedSchedule schedule =generated(solution, loaded.sections());
            List<RepairResponse.SectionChange> changes =schedule.sections().stream()
                    .filter(section -> !section.id().equals(originalIds.get(section.sectionGroupId())))
                    .map(section -> new RepairResponse.SectionChange(section.sectionGroupId(),
                            baseline.get(section.sectionGroupId()), section)).toList();
            return new RepairResponse.RepairedSchedule(schedule, changes.size(), changes);
        }).toList(), result.metrics());
    }
    private int limit(Integer requested) { return requested ==null ? 5 : requested; }
    private ApiException invalidRepair(String message) { return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REPAIR", message); }
    private long budget(Long requested) { return requested ==null ? maxMillis : Math.min(maxMillis, requested); }
    private record CatalogProblem(Problem problem, Map<UUID, SectionDto> sections) {}
    private CatalogProblem load(GenerateScheduleRequest request) {
=======
>>>>>>> origin/main
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
<<<<<<< HEAD
        return new CatalogProblem(new Problem(request.semesterId(), courseIds, groups, request.preferenceModel()), sections);
    }
    private GenerationResponse.GeneratedSchedule generated(Solution solution, Map<UUID, SectionDto> sections) {
=======
        Result result =solver.solve(new Problem(request.semesterId(), courseIds, groups, request.preferenceModel()),
                request.maxResults() ==null ? 5 : request.maxResults(),
                request.timeLimitMillis() ==null ? maxMillis : Math.min(maxMillis, request.timeLimitMillis()));
        return new GenerationResponse(result.status(), result.schedules().stream().map(solution -> {
>>>>>>> origin/main
            List<SectionDto> selected =solution.selectedSectionIds().stream().map(sections::get).toList();
            List<MeetingDto> meetings =selected.stream().flatMap(s -> s.meetings().stream()).sorted(
                    Comparator.comparing(MeetingDto::weekday).thenComparing(MeetingDto::startTime).thenComparing(m -> m.id().toString())).toList();
            return new GenerationResponse.GeneratedSchedule(solution.selectedSectionIds(), selected, meetings,
                    solution.optimizationScore(), solution.scoreExplanation());
<<<<<<< HEAD
=======
        }).toList(), result.metrics());
>>>>>>> origin/main
    }
}
