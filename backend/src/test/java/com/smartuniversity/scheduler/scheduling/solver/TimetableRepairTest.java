package com.smartuniversity.scheduler.scheduling.solver;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static com.smartuniversity.scheduler.scheduling.solver.TimetableModel.*;
import static org.assertj.core.api.Assertions.*;

class TimetableRepairTest {
    final UUID semester =id(1), course =id(2);
    final TimetableSolver solver =new TimetableSolver();
    UUID id(int n) { return new UUID(0, n); }
    Option option(int n, int group, int hour) {
        return new Option(id(n), id(group), course, semester,
                List.of(new Slot(DayOfWeek.MONDAY, LocalTime.of(hour, 0), LocalTime.of(hour + 1, 0))));
    }
    Group group(int n, Option... options) { return new Group(id(n), course, List.of(options)); }
    Problem problem(Preferences prefs, Group... groups) { return new Problem(semester, Set.of(course), List.of(groups), prefs); }

    @Test void keepsAValidBaselineEvenWhenPreferencesStronglyFavorAnAlternative() {
        Preferences prefs =new Preferences(List.of(), LocalTime.of(8, 0), LocalTime.NOON,
                Set.of(), new Weights(0, 0, 1000, 0, 0));
        Problem p =problem(prefs, group(10, option(20, 10, 16), option(21, 10, 9)));
        Result result =solver.repair(p, Map.of(id(10), id(20)), 5, 10000);
        assertThat(result.metrics().complete()).isTrue();
        assertThat(result.schedules().getFirst().selectedSectionIds()).containsExactly(id(20));
        assertThat(result.schedules().getFirst().optimizationScore()).isGreaterThan(result.schedules().get(1).optimizationScore());
    }

    @Test void repairsANewConflictWithoutChangingAnUnrelatedGroup() {
        Problem p =problem(Preferences.defaults(),
                group(10, option(20, 10, 9), option(21, 10, 10)),
                group(11, option(22, 11, 9)),
                group(12, option(24, 12, 12), option(25, 12, 13)));
        Result result =solver.repair(p, Map.of(id(10), id(20), id(11), id(22), id(12), id(24)), 5, 10000);
        assertThat(result.status()).isEqualTo(Status.SOLVED);
        assertThat(result.schedules().getFirst().selectedSectionIds()).containsExactly(id(21), id(22), id(24));
    }

    @Test void replacesAnExcludedOriginalAndAccountsForCascadingConflicts() {
        // Original 20 was removed. Its only replacement now conflicts with original 22.
        Problem p =problem(Preferences.defaults(), group(10, option(21, 10, 10)),
                group(11, option(22, 11, 10), option(23, 11, 11)));
        Result result =solver.repair(p, Map.of(id(10), id(20), id(11), id(22)), 5, 10000);
        assertThat(result.schedules().getFirst().selectedSectionIds()).containsExactly(id(21), id(23));
    }

    @Test void handlesImpossibleRepairPartialBaselineAndCancellation() {
        Problem p =problem(Preferences.defaults(), group(10));
        Map<UUID, UUID> baseline =Map.of(id(10), id(20));
        Result result =solver.repair(p, baseline, 5, 10000);
        assertThat(result.status()).isEqualTo(Status.UNSAT);
        assertThat(result.metrics().complete()).isTrue();
        assertThatIllegalArgumentException().isThrownBy(() -> solver.repair(p, Map.of(), 5, 10000));
        assertThat(solver.repair(p, baseline, 5, 1000, () -> 0L, () -> true).status()).isEqualTo(Status.CANCELLED);
        AtomicLong clock =new AtomicLong();
        Result timedOut =solver.repair(p, baseline, 5, 1, () -> clock.getAndAdd(1_000_000), () -> false);
        assertThat(timedOut.status()).isEqualTo(Status.TIMEOUT);
        assertThat(timedOut.metrics().complete()).isFalse();
    }

    @Test void branchAndBoundMatchesExhaustiveEnumerationForRandomSmallProblems() {
        Random random =new Random(7123);
        for (int sample =0; sample < 60; sample++) {
            List<Group> groups =new ArrayList<>();
            Map<UUID, UUID> baseline =new HashMap<>();
            for (int g =0; g < 4; g++) {
                List<Option> options =new ArrayList<>();
                for (int o =0; o < 3; o++) options.add(option(100 + g * 3 + o, 10 + g, 8 + random.nextInt(7)));
                groups.add(new Group(id(10 + g), course, options));
                baseline.put(id(10 + g), options.get(random.nextInt(3)).id());
            }
            Problem p =new Problem(semester, Set.of(course), groups, Preferences.defaults());
            // Enumerate every assignment independently of the solver's pruning.
            List<List<UUID>> feasible =new ArrayList<>();
            enumerate(groups, 0, new ArrayList<>(), feasible);
            // Score each known-valid assignment with fixed domains, then rank by the public objective.
            List<Solution> exhaustive =new ArrayList<>();
            for (List<UUID> ids : feasible) {
                List<Group> fixed =groups.stream().map(g -> new Group(g.id(), course,
                        g.options().stream().filter(o -> ids.contains(o.id())).toList())).toList();
                exhaustive.add(solver.solve(new Problem(semester, Set.of(course), fixed, p.preferences()), 1, 10000).schedules().getFirst());
            }
            exhaustive.sort(Comparator.<Solution>comparingLong(s -> baseline.values().stream()
                            .filter(id -> !s.selectedSectionIds().contains(id)).count())
                    .thenComparingDouble(Solution::optimizationScore).thenComparing(s -> s.selectedSectionIds().toString()));
            for (int k : new int[]{1, 3, 5}) {
                Result result =solver.repair(p, baseline, k, 10000);
                assertThat(result.metrics().complete()).isTrue();
                assertThat(result.schedules()).isEqualTo(exhaustive.stream().limit(k).toList());
            }
        }
    }
    void enumerate(List<Group> groups, int index, List<Option> chosen, List<List<UUID>> result) {
        if (index ==groups.size()) {
            result.add(chosen.stream().map(Option::id).sorted(Comparator.comparing(UUID::toString)).toList());
            return;
        }
        for (Option option : groups.get(index).options()) {
            boolean conflict =chosen.stream().anyMatch(other -> other.meetings().stream()
                    .anyMatch(first -> option.meetings().stream().anyMatch(first::overlaps)));
            if (!conflict) { chosen.add(option); enumerate(groups, index + 1, chosen, result); chosen.removeLast(); }
        }
    }
}
