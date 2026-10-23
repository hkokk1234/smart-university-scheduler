package com.smartuniversity.scheduler.notifications;

import com.smartuniversity.scheduler.catalog.api.SectionDto;
import com.smartuniversity.scheduler.platform.api.PageResponse;
import java.time.*;
import java.util.*;

public final class ChangeModels {
    private ChangeModels() {}
    public record MeetingSnapshot(DayOfWeek weekday, LocalTime startTime, LocalTime endTime,
                                  UUID roomId, String room, UUID instructorId, String instructor) {}
    public record Snapshot(String code, int capacity, List<MeetingSnapshot> meetings) {
        public static Snapshot of(SectionDto section) {
            List<MeetingSnapshot> meetings =section.meetings().stream().map(m -> new MeetingSnapshot(
                    m.weekday(), m.startTime(), m.endTime(), m.room() ==null ? null : m.room().id(),
                    m.room() ==null ? null : m.room().building() + " · " + m.room().code(),
                    m.instructor() ==null ? null : m.instructor().id(), m.instructor() ==null ? null : m.instructor().displayName()))
                    .sorted(Comparator.comparing(MeetingSnapshot::weekday).thenComparing(MeetingSnapshot::startTime)
                            .thenComparing(MeetingSnapshot::endTime).thenComparing(m -> Objects.toString(m.roomId(), ""))
                            .thenComparing(m -> Objects.toString(m.instructorId(), ""))).toList();
            return new Snapshot(section.code(), section.capacity(), meetings);
        }
    }
    public record Change(UUID id, UUID sectionId, UUID courseId, String courseCode, String courseTitle,
                         String actorName, Instant createdAt, Snapshot before, Snapshot after) {}
    public record Notification(UUID id, Instant readAt, Change change) {}
    public record Inbox(PageResponse<Notification> items, long unreadCount) {}
    public record CommittedChange(UUID courseId, Set<UUID> recipients) {
        public CommittedChange { recipients =Set.copyOf(recipients); }
    }
    public record InboxChanged(UUID userId) {}
}
