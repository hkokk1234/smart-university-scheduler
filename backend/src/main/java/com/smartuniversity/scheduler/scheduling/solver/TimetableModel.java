package com.smartuniversity.scheduler.scheduling.solver;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

/** Immutable timetable values shared by schedule generation and the solver. */
public final class TimetableModel {
    private TimetableModel() {}
    // Adjacent meetings are allowed because an end time is excluded from its interval.
    public record Slot(DayOfWeek weekday, LocalTime startTime, LocalTime endTime) {
        public Slot {
            Objects.requireNonNull(weekday); Objects.requireNonNull(startTime); Objects.requireNonNull(endTime);
            if (!startTime.isBefore(endTime)) throw new IllegalArgumentException("Meeting start must precede end");
        }
        public boolean overlaps(Slot other) {
            return weekday ==other.weekday && startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
        }
    }
    public record Option(UUID id, UUID groupId, UUID courseId, UUID semesterId, List<Slot> meetings) {
        public Option {
            Objects.requireNonNull(id); Objects.requireNonNull(groupId); Objects.requireNonNull(courseId);
            Objects.requireNonNull(semesterId); meetings =List.copyOf(meetings);
            if (meetings.isEmpty()) throw new IllegalArgumentException("Sections require meetings");
        }
    }
    public record Group(UUID id, UUID courseId, List<Option> options) {
        public Group { Objects.requireNonNull(id); Objects.requireNonNull(courseId); options =List.copyOf(options); }
    }
    public record Weights(double gaps, double campusDays, double preferredTimes, double freeDays, double balance) {
        public Weights {
            for (double weight : new double[]{gaps, campusDays, preferredTimes, freeDays, balance})
                if (!Double.isFinite(weight) || weight < 0 || weight > 1000)
                    throw new IllegalArgumentException("Weights must be finite and between 0 and 1000");
        }
        public static Weights defaults() { return new Weights(1, 1, 1, 1, 0.2); }
    }
    public enum TimeOfDay { ANY, MORNING, AFTERNOON }
    public record Preferences(List<Slot> unavailableTimes, LocalTime preferredStart, LocalTime preferredEnd,
                              Set<DayOfWeek> preferredFreeDays, Integer maximumCampusDays,
                              TimeOfDay preferredPeriod, Weights weights) {
        public Preferences {
            unavailableTimes =List.copyOf(unavailableTimes); preferredFreeDays =Set.copyOf(preferredFreeDays);
            Objects.requireNonNull(preferredPeriod); Objects.requireNonNull(weights);
            if ((preferredStart ==null) !=(preferredEnd ==null) ||
                    (preferredStart !=null && !preferredStart.isBefore(preferredEnd)))
                throw new IllegalArgumentException("Provide a valid preferred time window");
            if (maximumCampusDays !=null && (maximumCampusDays < 1 || maximumCampusDays > 7))
                throw new IllegalArgumentException("Maximum campus days must be between 1 and 7");
        }
        public Preferences(List<Slot> unavailableTimes, LocalTime preferredStart, LocalTime preferredEnd,
                           Set<DayOfWeek> preferredFreeDays, Weights weights) {
            this(unavailableTimes, preferredStart, preferredEnd, preferredFreeDays, null, TimeOfDay.ANY, weights);
        }
        public static Preferences defaults() {
            return new Preferences(List.of(), null, null, Set.of(), null, TimeOfDay.ANY, Weights.defaults());
        }
    }
    public record Problem(UUID semesterId, Set<UUID> courseIds, List<Group> groups, Preferences preferences) {
        public Problem {
            Objects.requireNonNull(semesterId); courseIds =Set.copyOf(courseIds); groups =List.copyOf(groups);
            Objects.requireNonNull(preferences);
            if (courseIds.isEmpty() || groups.isEmpty()) throw new IllegalArgumentException("Courses require section groups");
            // Reject repeated sections and groups before exploring possible schedules.
            Set<UUID> groupIds =new HashSet<>(), sectionIds =new HashSet<>(), covered =new HashSet<>();
            for (Group group : groups) {
                if (!groupIds.add(group.id) || !courseIds.contains(group.courseId))
                    throw new IllegalArgumentException("Invalid required group");
                covered.add(group.courseId);
                for (Option option : group.options)
                    if (!sectionIds.add(option.id) || !option.groupId.equals(group.id) ||
                            !option.courseId.equals(group.courseId) || !option.semesterId.equals(semesterId))
                        throw new IllegalArgumentException("Section does not belong to its course,group and semester");
            }
            if (!covered.equals(courseIds)) throw new IllegalArgumentException("Every selected course requires groups");
        }
    }
    public enum Status { SOLVED, UNSAT, TIMEOUT, CANCELLED }
    public record ScorePart(String criterion, double penalty, double weight, double contribution, String explanation) {}
    public record Solution(List<UUID> selectedSectionIds, List<Slot> weeklyMeetings, double optimizationScore,
                           List<ScorePart> scoreExplanation) {
        public Solution { selectedSectionIds =List.copyOf(selectedSectionIds); weeklyMeetings =List.copyOf(weeklyMeetings); scoreExplanation =List.copyOf(scoreExplanation); }
    }
    public record Metrics(long nodesVisited, long backtracks, long domainPruned, long solutionsFound,
                          long executionMillis, boolean complete) {}
    public record Result(Status status, List<Solution> schedules, Metrics metrics) {
        public Result { schedules =List.copyOf(schedules); }
    }
}
