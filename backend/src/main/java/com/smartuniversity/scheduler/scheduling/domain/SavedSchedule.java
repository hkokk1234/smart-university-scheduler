package com.smartuniversity.scheduler.scheduling.domain;

import com.smartuniversity.scheduler.platform.persistence.UuidEntity;
import com.smartuniversity.scheduler.platform.persistence.EntityValues;
import jakarta.persistence.*;
import com.smartuniversity.scheduler.identity.domain.User;
import com.smartuniversity.scheduler.catalog.domain.Semester;
import com.smartuniversity.scheduler.catalog.domain.Section;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name ="saved_schedules")
public class SavedSchedule extends UuidEntity {
    @ManyToOne(fetch =FetchType.LAZY, optional =false)
    @JoinColumn(name ="user_id", nullable =false)
    private User user;
    @ManyToOne(fetch =FetchType.LAZY, optional =false)
    @JoinColumn(name ="semester_id", nullable =false)
    private Semester semester;
    @Column(nullable =false, length =120)
    private String name;
    @Column(name ="created_at", nullable =false, updatable =false)
    private Instant createdAt;
    @OneToMany(mappedBy ="savedSchedule", cascade =CascadeType.ALL, orphanRemoval =true)
    private Set<ScheduleSection> selections =new LinkedHashSet<>();

    protected SavedSchedule() {}

    public SavedSchedule(User user, Semester semester, String name) {
        this.user =Objects.requireNonNull(user, "user");
        this.semester =Objects.requireNonNull(semester, "semester");
        this.name =EntityValues.requiredText(name, 120, "name");
        this.createdAt =Instant.now();
    }

    public User getUser() { return user; }
    public Semester getSemester() { return semester; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
    public Set<ScheduleSection> getSelections() { return Collections.unmodifiableSet(selections); }

    public ScheduleSection addSection(Section section) {
        Objects.requireNonNull(section, "section");
        if (!semester.hasSameIdentity(section.getSemester())) {
            throw new IllegalArgumentException("Selected section must belong to the schedule semester");
        }
        if (selections.stream().anyMatch(selection ->
                selection.getSectionGroup().hasSameIdentity(section.getSectionGroup()))) {
            throw new IllegalArgumentException("Only one alternative per section group can be selected");
        }
        ScheduleSection selection =new ScheduleSection(this, section);
        selections.add(selection);
        return selection;
    }

    public void removeSection(ScheduleSection selection) { selections.remove(selection); }
}
