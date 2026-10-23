package com.smartuniversity.scheduler.notifications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartuniversity.scheduler.platform.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.*;
import static com.smartuniversity.scheduler.notifications.ChangeModels.*;

@Repository
public class ChangeStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public ChangeStore(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc =jdbc; this.json =json; }
    public void insert(Change change, UUID actorId) {
        jdbc.update("""
                INSERT INTO section_changes(id,section_id,course_id,course_code,course_title,actor_id,actor_name,created_at,before_snapshot,after_snapshot)
                VALUES(?,?,?,?,?,?,?,?,?,?)
                """, change.id(), change.sectionId(), change.courseId(), change.courseCode(), change.courseTitle(), actorId,
                change.actorName(), Timestamp.from(change.createdAt()), encode(change.before()), encode(change.after()));
    }
    public Set<UUID> recipients(UUID sectionId) {
        return new HashSet<>(jdbc.query("""
                SELECT DISTINCT s.user_id FROM saved_schedules s JOIN schedule_sections ss ON ss.saved_schedule_id=s.id
                WHERE ss.section_id=?
                """, (rs, index) -> rs.getObject(1, UUID.class), sectionId));
    }
    public void notify(Set<UUID> users, UUID changeId) {
        jdbc.batchUpdate("INSERT INTO user_notifications(id,user_id,change_id) VALUES(?,?,?)", users, 100,
                (statement, user) -> { statement.setObject(1, UUID.randomUUID()); statement.setObject(2, user); statement.setObject(3, changeId); });
    }
    public PageResponse<Change> history(UUID sectionId, int page, int size) {
        long total =jdbc.queryForObject("SELECT count(*) FROM section_changes WHERE section_id=?", Long.class, sectionId);
        var rows =jdbc.query("SELECT * FROM section_changes WHERE section_id=? ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",
                (rs, index) -> change(rs), sectionId, size, (long) page * size);
        return new PageResponse<>(rows, page, size, total, (int) ((total + size - 1) / size));
    }
    public Inbox inbox(UUID userId, int page, int size) {
        long total =jdbc.queryForObject("SELECT count(*) FROM user_notifications WHERE user_id=?", Long.class, userId);
        long unread =jdbc.queryForObject("SELECT count(*) FROM user_notifications WHERE user_id=? AND read_at IS NULL", Long.class, userId);
        var rows =jdbc.query("""
                SELECT c.*,n.id AS notification_id,n.read_at FROM user_notifications n
                JOIN section_changes c ON c.id=n.change_id WHERE n.user_id=?
                ORDER BY c.created_at DESC,c.id DESC LIMIT ? OFFSET ?
                """, (rs, index) -> new Notification(rs.getObject("notification_id", UUID.class),
                rs.getTimestamp("read_at") ==null ? null : rs.getTimestamp("read_at").toInstant(), change(rs)), userId, size, (long) page * size);
        return new Inbox(new PageResponse<>(rows, page, size, total, (int) ((total + size - 1) / size)), unread);
    }
    public void markRead(UUID id, UUID userId) {
        if (jdbc.update("UPDATE user_notifications SET read_at=COALESCE(read_at,CURRENT_TIMESTAMP) WHERE id=? AND user_id=?", id, userId) ==0)
            throw ApiException.notFound("Notification");
    }
    public void markAllRead(UUID userId) {
        jdbc.update("UPDATE user_notifications SET read_at=CURRENT_TIMESTAMP WHERE user_id=? AND read_at IS NULL", userId);
    }
    private Change change(ResultSet rs) throws SQLException {
        return new Change(rs.getObject("id", UUID.class), rs.getObject("section_id", UUID.class), rs.getObject("course_id", UUID.class),
                rs.getString("course_code"), rs.getString("course_title"), rs.getString("actor_name"), rs.getTimestamp("created_at").toInstant(),
                decode(rs.getString("before_snapshot")), decode(rs.getString("after_snapshot")));
    }
    private String encode(Snapshot snapshot) {
        try { return json.writeValueAsString(snapshot); } catch (JsonProcessingException error) { throw new IllegalStateException("Cannot serialize section history", error); }
    }
    private Snapshot decode(String value) {
        try { return json.readValue(value, Snapshot.class); } catch (JsonProcessingException error) { throw new IllegalStateException("Cannot read section history", error); }
    }
}
