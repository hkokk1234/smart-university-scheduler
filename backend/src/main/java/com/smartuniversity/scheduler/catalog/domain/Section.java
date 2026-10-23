package com.smartuniversity.scheduler.catalog.domain;

import com.smartuniversity.scheduler.platform.persistence.UuidEntity;
import com.smartuniversity.scheduler.platform.persistence.EntityValues;
import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name ="sections")
public class Section extends UuidEntity {
    @ManyToOne(fetch =FetchType.LAZY, optional =false)
    @JoinColumn(name ="section_group_id", nullable =false)
    private SectionGroup sectionGroup;
    @ManyToOne(fetch =FetchType.LAZY, optional =false)
    @JoinColumn(name ="semester_id", nullable =false)
    private Semester semester;
    @Column(nullable =false, length =32)
    private String code;
    @Column(nullable =false)
    private int capacity;
    @Version
    @Column(nullable =false)
    private long version;
    @OneToMany(mappedBy ="section", cascade =CascadeType.ALL, orphanRemoval =true)
    @OrderBy("weekday ASC, startTime ASC, id ASC")
    private List<Meeting> meetings =new ArrayList<>();

    protected Section() {}

    public Section(SectionGroup sectionGroup, String code, int capacity) {
        this.sectionGroup =Objects.requireNonNull(sectionGroup, "sectionGroup");
        this.semester =sectionGroup.getSemester();
        this.code =EntityValues.requiredText(code, 32, "code");
        this.capacity =EntityValues.positive(capacity, "capacity");
    }

    public SectionGroup getSectionGroup() { return sectionGroup; }
    public Semester getSemester() { return semester; }
    public String getCode() { return code; }
    public int getCapacity() { return capacity; }
    public long getVersion() { return version; }
    public void updateDetails(String code, int capacity) {
        this.code =EntityValues.requiredText(code, 32, "code");
        this.capacity =EntityValues.positive(capacity, "capacity");
    }

    public void clearMeetings() { meetings.clear(); }
    public List<Meeting> getMeetings() { return Collections.unmodifiableList(meetings); }

    public Meeting addMeeting(DayOfWeek weekday, LocalTime startTime, LocalTime endTime,
                              Instructor instructor, Room room) {
        Meeting meeting =new Meeting(this, weekday, startTime, endTime, instructor, room);
        meetings.add(meeting);
        return meeting;
    }

    public void removeMeeting(Meeting meeting) { meetings.remove(meeting); }
}
