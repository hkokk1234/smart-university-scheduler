package com.smartuniversity.scheduler.scheduling.solver;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.*;
import static com.smartuniversity.scheduler.scheduling.solver.TimetableModel.*;

class TimetableSolverTest {
    final UUID semester =id(1), course =id(2);
    final TimetableSolver solver =new TimetableSolver();
    UUID id(int n) { return new UUID(0, n); }
    Slot slot(DayOfWeek day, String start, String end) { return new Slot(day, LocalTime.parse(start), LocalTime.parse(end)); }
    Slot monday(String start, String end) { return slot(DayOfWeek.MONDAY, start, end); }
    Option option(int n, int group, Slot... meetings) { return new Option(id(n), id(group), course, semester, List.of(meetings)); }
    Group group(int n, Option... options) { return new Group(id(n), course, List.of(options)); }
    Problem problem(Preferences preferences, Group... groups) { return new Problem(semester, Set.of(course), List.of(groups), preferences); }
    Result solve(Group... groups) { return solver.solve(problem(Preferences.defaults(), groups), 5, 10000); }

    @Test void detectsConflictInEveryMeetingAndAllowsTouchingBoundaries() {
        Option lecture =option(20, 10, monday("09:00", "10:00"), slot(DayOfWeek.WEDNESDAY, "09:00", "10:00"));
        Option conflict =option(21, 11, slot(DayOfWeek.WEDNESDAY, "09:30", "11:00"));
        Option touching =option(22, 11, monday("10:00", "11:00"));
        Result result =solve(group(10, lecture), group(11, conflict, touching));
        assertThat(result.status()).isEqualTo(Status.SOLVED);
        assertThat(result.schedules()).hasSize(1);
        assertThat(result.schedules().getFirst().selectedSectionIds()).containsExactly(id(20), id(22));
        assertThat(result.schedules().getFirst().weeklyMeetings()).hasSize(3);
        assertThat(result.metrics().domainPruned()).isPositive();
    }
    @Test void impossibleCombinationsAreUnsat() {
        Result result =solve(group(10, option(20, 10, monday("09:00", "10:00"))), group(11, option(21, 11, monday("09:30", "10:30"))));
        assertThat(result.status()).isEqualTo(Status.UNSAT);
        assertThat(result.metrics().complete()).isTrue();
        assertThat(result.schedules()).isEmpty();
    }
    @Test void prunesInternalConflictsAndMandatoryUnavailability() {
        Preferences prefs =new Preferences(List.of(monday("09:30", "10:30")), null, null, Set.of(), Weights.defaults());
        Result result =solver.solve(problem(prefs, group(10,
                option(20, 10, monday("09:00", "10:00")),
                option(21, 10, monday("12:00", "13:00"), monday("12:30", "13:30")),
                option(22, 10, monday("14:00", "15:00")))), 5, 10000);
        assertThat(result.schedules().getFirst().selectedSectionIds()).containsExactly(id(22));
        assertThat(result.metrics().domainPruned()).isEqualTo(2);
    }
    @Test void retainsOnlyFiveAndRanksDeterministicallyRegardlessOfInputOrder() {
        List<Option> options =new ArrayList<>();
        for (int n =20; n < 30; n++) options.add(option(n, 10, monday("09:00", "10:00")));
        Result first =solve(new Group(id(10), course, options));
        Collections.reverse(options);
        Result second =solve(new Group(id(10), course, options));
        assertThat(first.schedules()).hasSize(5).isEqualTo(second.schedules());
        assertThat(first.metrics().solutionsFound()).isEqualTo(10);
        assertThat(first.schedules().getFirst().selectedSectionIds()).containsExactly(id(20));
    }
    @Test void mrvChoosesEmptyDomainBeforeLargerDomains() {
        Result result =solve(group(10, option(20, 10, monday("09:00", "10:00"))), group(11));
        assertThat(result.status()).isEqualTo(Status.UNSAT);
        assertThat(result.metrics().nodesVisited()).isEqualTo(1);
    }
    @Test void timeoutNeverClaimsUnsatAndCancellationIsDistinct() {
        Problem p =problem(Preferences.defaults(), group(10, option(20, 10, monday("09:00", "10:00"))));
        AtomicLong clock =new AtomicLong();
        Result timeout =solver.solve(p, 5, 1, () -> clock.getAndAdd(1_000_000), () -> false);
        assertThat(timeout.status()).isEqualTo(Status.TIMEOUT);
        assertThat(timeout.metrics().complete()).isFalse();
        assertThat(solver.solve(p, 5, 1000, () -> 0L, () -> true).status()).isEqualTo(Status.CANCELLED);
    }
    @Test void enforcesCourseGroupSemesterAndRequiredGroupCoverage() {
        Option wrong =new Option(id(20), id(10), course, id(999), List.of(monday("09:00", "10:00")));
        assertThatIllegalArgumentException().isThrownBy(() -> problem(Preferences.defaults(), group(10, wrong)));
        assertThatIllegalArgumentException().isThrownBy(() -> new Problem(semester, Set.of(course, id(99)), List.of(group(10)), Preferences.defaults()));
        assertThatIllegalArgumentException().isThrownBy(() -> problem(Preferences.defaults(), group(10), group(10)));
    }
    @Test void weightedPreferencesRankAndExplainAllSixPenalties() {
        Preferences prefs =new Preferences(List.of(), LocalTime.of(9, 0), LocalTime.of(17, 0),
                Set.of(DayOfWeek.MONDAY), new Weights(1, 10, 2, 3, 0));
        Result result =solver.solve(problem(prefs, group(10,
                option(20, 10, monday("08:00", "09:00")),
                option(21, 10, slot(DayOfWeek.TUESDAY, "10:00", "11:00")))), 5, 10000);
        assertThat(result.schedules().getFirst().selectedSectionIds()).containsExactly(id(21));
        assertThat(result.schedules().getFirst().optimizationScore()).isCloseTo(10.0 / 7, within(0.00001));
        assertThat(result.schedules().get(1).optimizationScore()).isCloseTo(45.0 / 7, within(0.00001));
        assertThat(result.schedules().getFirst().scoreExplanation()).hasSize(6)
                .allSatisfy(part -> assertThat(part.penalty()).isBetween(0.0, 1.0));
    }
    @Test void measuresGapsAndSevenDayWorkloadDeviation() {
        Preferences prefs =new Preferences(List.of(), null, null, Set.of(), new Weights(1, 0, 0, 0, 1));
        var result =solver.solve(problem(prefs, group(10, option(20, 10, monday("09:00", "10:00"), monday("11:00", "12:00")))), 5, 10000);
        var explanation =result.schedules().getFirst().scoreExplanation();
        assertThat(explanation.get(0).penalty()).isEqualTo(0.5);
        assertThat(explanation.get(5).penalty()).isCloseTo(6.0 / 7, within(0.00001));
    }
    @Test void maximumCampusDaysAndPreferredPeriodChangeRanking() {
        Preferences campusLimit =new Preferences(List.of(), null, null, Set.of(), 1, TimeOfDay.ANY,
                new Weights(0, 1, 0, 0, 0));
        Result campusResult =solver.solve(problem(campusLimit,
                group(10, option(20, 10, monday("09:00", "10:00"))),
                group(11, option(21, 11, monday("11:00", "12:00")),
                        option(22, 11, slot(DayOfWeek.TUESDAY, "11:00", "12:00")))), 5, 10000);
        assertThat(campusResult.schedules().getFirst().selectedSectionIds()).containsExactly(id(20), id(21));
        assertThat(campusResult.schedules().getFirst().scoreExplanation().get(2).penalty()).isZero();
        assertThat(campusResult.schedules().get(1).scoreExplanation().get(2).penalty()).isEqualTo(1.0 / 6);

        Preferences morning =new Preferences(List.of(), null, null, Set.of(), null, TimeOfDay.MORNING,
                new Weights(0, 0, 1, 0, 0));
        Result periodResult =solver.solve(problem(morning, group(10,
                option(20, 10, monday("14:00", "15:00")),
                option(21, 10, monday("09:00", "10:00")))), 5, 10000);
        assertThat(periodResult.schedules().getFirst().selectedSectionIds()).containsExactly(id(21));
        assertThat(periodResult.schedules().getFirst().scoreExplanation().get(3).penalty()).isZero();
        assertThat(periodResult.schedules().get(1).scoreExplanation().get(3).penalty()).isEqualTo(1.0);
    }
    @Test void combinedTimePreferencesCountEachOutsideMinuteOnlyOnce() {
        Preferences prefs =new Preferences(List.of(), LocalTime.of(9, 0), LocalTime.of(17, 0),
                Set.of(), null, TimeOfDay.MORNING, new Weights(0, 0, 1, 0, 0));
        Result result =solver.solve(problem(prefs, group(10, option(20, 10,
                monday("07:00", "08:00"), monday("10:00", "11:00")))), 5, 10000);
        assertThat(result.schedules().getFirst().optimizationScore()).isEqualTo(0.5);
        assertThat(result.schedules().getFirst().scoreExplanation().get(3).penalty()).isEqualTo(0.5);
    }
    @Test void disjointTimePreferencesTreatEveryMeetingMinuteAsOutside() {
        Preferences prefs =new Preferences(List.of(), LocalTime.of(14, 0), LocalTime.of(17, 0),
                Set.of(), null, TimeOfDay.MORNING, new Weights(0, 0, 1, 0, 0));
        Result result =solver.solve(problem(prefs, group(10, option(20, 10, monday("10:00", "11:00")))), 5, 10000);
        assertThat(result.schedules().getFirst().optimizationScore()).isEqualTo(1);
    }
    @Test void rejectsInvalidTimeAndWeightInputs() {
        assertThatIllegalArgumentException().isThrownBy(() -> monday("10:00", "09:00"));
        assertThatIllegalArgumentException().isThrownBy(() -> new Weights(Double.NaN, 1, 1, 1, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> new Preferences(List.of(), LocalTime.NOON, null, Set.of(), Weights.defaults()));
        assertThatIllegalArgumentException().isThrownBy(() -> new Preferences(List.of(), null, null, Set.of(), 8, TimeOfDay.ANY, Weights.defaults()));
    }
    @Test void timeoutRetainsValidPartialSolutions() {
        List<Option> alternatives =new ArrayList<>();
        for (int n =100; n < 200; n++) alternatives.add(option(n, 10, monday("09:00", "10:00")));
        AtomicLong calls =new AtomicLong();
        Result result =solver.solve(problem(Preferences.defaults(), new Group(id(10), course, alternatives)), 5, 1,
                () -> calls.incrementAndGet() <=106 ? 0 : 1_000_000L, () -> false);
        assertThat(result.status()).isEqualTo(Status.TIMEOUT);
        assertThat(result.schedules()).hasSize(2);
        assertThat(result.metrics().complete()).isFalse();
        assertThat(result.schedules().getFirst().weeklyMeetings()).containsExactly(monday("09:00", "10:00"));
    }
    @Test void groupOrderingDoesNotAffectResultsAndSelectsOnePerGroup() {
        Group a =group(10, option(20, 10, monday("09:00", "10:00")), option(21, 10, monday("10:00", "11:00")));
        Group b =group(11, option(22, 11, slot(DayOfWeek.TUESDAY, "09:00", "10:00")), option(23, 11, slot(DayOfWeek.WEDNESDAY, "09:00", "10:00")));
        Result first =solve(a, b), second =solve(b, a);
        assertThat(first.schedules()).hasSize(4).isEqualTo(second.schedules());
        for (Solution solution : first.schedules()) {
            assertThat(solution.selectedSectionIds()).hasSize(2);
            assertThat(solution.weeklyMeetings()).hasSize(2);
        }
    }
    @Test void defaultCancellationRespectsAndPreservesThreadInterruption() {
        Thread.currentThread().interrupt();
        try {
            Result result =solve(group(10, option(20, 10, monday("09:00", "10:00"))));
            assertThat(result.status()).isEqualTo(Status.CANCELLED);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally { Thread.interrupted(); }
    }
}
