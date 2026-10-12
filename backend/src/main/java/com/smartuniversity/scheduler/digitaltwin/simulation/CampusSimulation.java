package com.smartuniversity.scheduler.digitaltwin.simulation;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

/** Pure, deterministic weekly simulation. Intervals are half-open; attendance is an estimate. */
public final class CampusSimulation {
    private CampusSimulation() {}
    public record Room(UUID id, String building, String code, int capacity) {}
    public record Slot(UUID id, String label, DayOfWeek weekday, LocalTime startTime,
                       LocalTime endTime, UUID roomId, int attendance) {}
    public record Alert(String type, UUID meetingId, UUID otherMeetingId, String message) {}
    public record RoomUsage(Room room, double occupiedHours, double utilizationPercent) {}
    public record DayPeak(DayOfWeek weekday, int simultaneousMeetings, int estimatedAttendance, LocalTime peakTime) {}
    public record Report(List<RoomUsage> rooms, List<DayPeak> peaks, List<Alert> alerts,
                         int unassignedMeetings, double occupiedRoomHours) {}

    public static Report simulate(List<Room> rooms, List<Slot> slots) {
        Map<UUID, Room> known = new HashMap<>();
        rooms.forEach(room -> known.put(room.id(), room));
        for (Slot slot : slots) {
            if (!slot.startTime().isBefore(slot.endTime()) || slot.attendance() < 0)
                throw new IllegalArgumentException("Invalid meeting interval or attendance");
            if (slot.roomId() != null && !known.containsKey(slot.roomId()))
                throw new IllegalArgumentException("Unknown room");
        }
        List<Alert> alerts = new ArrayList<>();
        for (Slot slot : slots) {
            Room room = known.get(slot.roomId());
            if (room != null && slot.attendance() > room.capacity())
                alerts.add(new Alert("CAPACITY", slot.id(), null, slot.label() + " exceeds room capacity by " + (slot.attendance() - room.capacity())));
        }
        List<RoomUsage> usage = new ArrayList<>();
        for (Room room : rooms) {
            int occupiedSeconds = 0;
            for (DayOfWeek day : DayOfWeek.values()) {
                List<Slot> daily = slots.stream().filter(s -> room.id().equals(s.roomId()) && s.weekday() == day)
                        .sorted(Comparator.comparing(Slot::startTime).thenComparing(Slot::id)).toList();
                int unionEnd = -1;
                for (int i = 0; i < daily.size(); i++) {
                    Slot slot = daily.get(i);
                    int start = slot.startTime().toSecondOfDay(), end = slot.endTime().toSecondOfDay();
                    occupiedSeconds += Math.max(0, end - Math.max(start, unionEnd));
                    unionEnd = Math.max(unionEnd, end);
                    for (int j = i + 1; j < daily.size() && daily.get(j).startTime().isBefore(slot.endTime()); j++) {
                        Slot other = daily.get(j);
                        alerts.add(new Alert("ROOM_CONFLICT", slot.id(), other.id(), slot.label() + " overlaps " + other.label() + " in " + room.building() + " " + room.code()));
                        if (alerts.size() > 10000) throw new IllegalArgumentException("Scenario exceeds the 10000 alert safety limit");
                    }
                }
            }
            usage.add(new RoomUsage(room, occupiedSeconds / 3600.0, occupiedSeconds * 100.0 / (7 * 86400)));
        }
        List<DayPeak> peaks = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            TreeMap<LocalTime, int[]> events = new TreeMap<>();
            slots.stream().filter(s -> s.weekday() == day).forEach(s -> {
                int[] start = events.computeIfAbsent(s.startTime(), ignored -> new int[2]);
                start[0]++; start[1] += s.attendance();
                int[] end = events.computeIfAbsent(s.endTime(), ignored -> new int[2]);
                end[0]--; end[1] -= s.attendance();
            });
            int count = 0, attendance = 0, maxCount = 0, maxAttendance = 0;
            LocalTime peakTime = null;
            for (var entry : events.entrySet()) {
                int[] event = entry.getValue();
                count += event[0]; attendance += event[1];
                if (attendance > maxAttendance) peakTime = entry.getKey();
                maxCount = Math.max(maxCount, count); maxAttendance = Math.max(maxAttendance, attendance);
            }
            peaks.add(new DayPeak(day, maxCount, maxAttendance, peakTime));
        }
        return new Report(List.copyOf(usage), List.copyOf(peaks), List.copyOf(alerts),
                (int) slots.stream().filter(s -> s.roomId() == null).count(), usage.stream().mapToDouble(RoomUsage::occupiedHours).sum());
    }
}
