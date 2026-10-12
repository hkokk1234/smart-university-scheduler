package com.smartuniversity.scheduler.scheduling.solver;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import static com.smartuniversity.scheduler.scheduling.solver.TimetableModel.*;

/** Reentrant solver. All mutable search state belongs to one invocation. */
public final class TimetableSolver {
    private static final Comparator<UUID> IDS =Comparator.comparing(UUID::toString);
    private static final Comparator<Solution> RANK =Comparator.comparingDouble(Solution::optimizationScore)
            .thenComparing(s -> s.selectedSectionIds().toString());
    public Result solve(Problem problem, int topK, long timeLimitMillis) {
        return solve(problem, topK, timeLimitMillis, System::nanoTime, () -> Thread.currentThread().isInterrupted());
    }
    public Result solve(Problem problem, int topK, long timeLimitMillis, LongSupplier clock, BooleanSupplier cancelled) {
        if (topK < 1 || topK > 5 || timeLimitMillis < 1 || timeLimitMillis > 60000)
            throw new IllegalArgumentException("Invalid search limits");
        return new Search(problem, topK, timeLimitMillis, clock, cancelled).run();
    }
    private static boolean conflicts(Option a, Option b) {
        for (Slot first : a.meetings()) for (Slot second : b.meetings()) if (first.overlaps(second)) return true;
        return false;
    }
    private static boolean permitted(Option option, Preferences preferences) {
        List<Slot> meetings =option.meetings();
        for (int i =0; i < meetings.size(); i++) {
            for (Slot unavailable : preferences.unavailableTimes()) if (meetings.get(i).overlaps(unavailable)) return false;
            for (int j =0; j < i; j++) if (meetings.get(i).overlaps(meetings.get(j))) return false;
        }
        return true;
    }
    private static final class Search {
        final Problem problem; final int topK; final long budgetNanos, start;
        final LongSupplier clock; final BooleanSupplier cancelled;
        final PriorityQueue<Solution> best =new PriorityQueue<>(RANK.reversed());
        long nodes, backtracks, pruned, found; Status stopped;
        Search(Problem problem, int topK, long milliseconds, LongSupplier clock, BooleanSupplier cancelled) {
            this.problem =problem; this.topK =topK; this.clock =clock; this.cancelled =cancelled;
            budgetNanos =milliseconds * 1_000_000L; start =clock.getAsLong();
        }
        boolean stop() {
            if (stopped !=null) return true;
            if (cancelled.getAsBoolean()) stopped =Status.CANCELLED;
            else if (clock.getAsLong() - start >=budgetNanos) stopped =Status.TIMEOUT;
            return stopped !=null;
        }
        Result run() {
            Map<UUID, List<Option>> domains =new TreeMap<>(IDS);
            for (Group group : problem.groups()) {
                List<Option> allowed =new ArrayList<>();
                for (Option option : group.options()) {
                    if (stop()) return result();
                    if (permitted(option, problem.preferences())) allowed.add(option); else pruned++;
                }
                allowed.sort(Comparator.comparing(Option::id, IDS)); domains.put(group.id(), allowed);
            }
            visit(domains, new ArrayList<>());
            return result();
        }
        Result result() {
            List<Solution> ranked =new ArrayList<>(best); ranked.sort(RANK);
            Status status =stopped !=null ? stopped : (found ==0 ? Status.UNSAT : Status.SOLVED);
            return new Result(status, ranked, new Metrics(nodes, backtracks, pruned, found,
                    Math.max(0, (clock.getAsLong() - start) / 1_000_000), stopped ==null));
        }
        void visit(Map<UUID, List<Option>> domains, List<Option> chosen) {
            if (stop()) return;
            nodes++;
            if (domains.isEmpty()) {
                if (found !=Long.MAX_VALUE) found++;
                Solution solution =score(chosen, problem.preferences());
                if (best.size() < topK) best.add(solution);
                else if (RANK.compare(solution, best.peek()) < 0) { best.poll(); best.add(solution); }
                return;
            }
            // TreeMap iteration supplies the stable UUID tie-break for MRV.
            var next =domains.entrySet().stream().min(Comparator.comparingInt(e -> e.getValue().size())).orElseThrow();
            if (next.getValue().isEmpty()) { backtracks++; return; }
            for (Option candidate : next.getValue()) {
                if (stop()) return;
                Map<UUID, List<Option>> remaining =new TreeMap<>(IDS);
                boolean viable =true;
                for (var entry : domains.entrySet()) {
                    if (entry.getKey().equals(next.getKey())) continue;
                    List<Option> allowed =new ArrayList<>();
                    for (Option option : entry.getValue()) {
                        if (stop()) return;
                        if (conflicts(candidate, option)) pruned++; else allowed.add(option);
                    }
                    if (allowed.isEmpty()) { viable =false; backtracks++; break; }
                    remaining.put(entry.getKey(), allowed);
                }
                if (viable) {
                    chosen.add(candidate); visit(remaining, chosen); chosen.removeLast();
                }
            }
        }
    }
    private static double minutes(Slot slot) { return (slot.endTime().toNanoOfDay() - slot.startTime().toNanoOfDay()) / 60_000_000_000.0; }
    private static double normalized(double value, double denominator) {
        return denominator <=0 ? 0 : Math.min(1, value / denominator);
    }
    private static Solution score(List<Option> options, Preferences preferences) {
        List<UUID> ids =options.stream().map(Option::id).sorted(IDS).toList();
        List<Slot> meetings =options.stream().flatMap(o -> o.meetings().stream())
                .sorted(Comparator.comparing(Slot::weekday).thenComparing(Slot::startTime).thenComparing(Slot::endTime)).toList();
        double[] loads =new double[7]; double gaps =0, outside =0, free =0, totalMinutes =0; int days =0;
        LocalTime periodStart =switch (preferences.preferredPeriod()) {
            case MORNING -> LocalTime.of(8, 0);
            case AFTERNOON -> LocalTime.NOON;
            case ANY -> null;
        };
        LocalTime periodEnd =switch (preferences.preferredPeriod()) {
            case MORNING -> LocalTime.NOON;
            case AFTERNOON -> LocalTime.of(18, 0);
            case ANY -> null;
        };
        LocalTime preferredStart =preferences.preferredStart(), preferredEnd =preferences.preferredEnd();
        if (periodStart !=null) {
            preferredStart =preferredStart ==null || preferredStart.isBefore(periodStart) ? periodStart : preferredStart;
            preferredEnd =preferredEnd ==null || preferredEnd.isAfter(periodEnd) ? periodEnd : preferredEnd;
        }
        Slot previous =null;
        for (Slot slot : meetings) {
            double duration =minutes(slot);
            totalMinutes +=duration;
            loads[slot.weekday().ordinal()] +=duration;
            if (previous !=null && previous.weekday() ==slot.weekday())
                gaps +=(slot.startTime().toNanoOfDay() - previous.endTime().toNanoOfDay()) / 60_000_000_000.0;
            if (preferences.preferredFreeDays().contains(slot.weekday())) free +=minutes(slot);
            if (preferredStart !=null) {
                // Intersect the two preferences so each outside minute is counted once.
                long inside =Math.max(0, Math.min(slot.endTime().toNanoOfDay(), preferredEnd.toNanoOfDay())
                        - Math.max(slot.startTime().toNanoOfDay(), preferredStart.toNanoOfDay()));
                outside +=duration - inside / 60_000_000_000.0;
            }
            previous =slot;
        }
        double mean =Arrays.stream(loads).sum() / DayOfWeek.values().length, imbalance =0;
        for (double load : loads) { if (load > 0) days++; imbalance +=Math.abs(load - mean); }
        Weights w =preferences.weights();
        int preferredMaximum =preferences.maximumCampusDays() ==null ? 7 : preferences.maximumCampusDays();
        double excessDays =Math.max(0, days - preferredMaximum);
        List<ScorePart> parts =List.of(part("gaps", normalized(gaps, totalMinutes), w.gaps(),
                        "Gap minutes as a fraction of total meeting time"),
                part("campusDays", days / 7.0, w.campusDays(), "Campus days as a fraction of the seven-day week"),
                part("maximumCampusDays", normalized(excessDays, 7 - preferredMaximum), w.campusDays(),
                        "Days above the preferred maximum, normalized to the remaining week"),
                part("preferredTimes", normalized(outside, totalMinutes), w.preferredTimes(),
                        "Meeting minutes outside the preferred time window and period, as a fraction of total meeting time"),
                part("freeDays", normalized(free, totalMinutes), w.freeDays(),
                        "Meeting minutes on preferred free days as a fraction of total meeting time"),
                part("balance", normalized(imbalance, 2 * totalMinutes), w.balance(),
                        "Seven-day workload imbalance as a fraction of its maximum"));
        return new Solution(ids, meetings, parts.stream().mapToDouble(ScorePart::contribution).sum(), parts);
    }
    private static ScorePart part(String name, double penalty, double weight, String explanation) {
        return new ScorePart(name, penalty, weight, penalty * weight, explanation);
    }
}
