package com.smartuniversity.scheduler.catalog.domain;

import com.smartuniversity.scheduler.platform.persistence.UuidEntity;
import com.smartuniversity.scheduler.platform.persistence.EntityValues;
import jakarta.persistence.*;
import com.smartuniversity.scheduler.catalog.infrastructure.WeekdayConverter;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name ="meetings")
public class Meeting extends UuidEntity {
    @ManyToOne(fetch =FetchType.LAZY, optional =false)
    @JoinColumn(name ="section_id", nullable =false)
    private Section section;
    @Convert(converter =WeekdayConverter.class)
    @Column(nullable =false)
    private DayOfWeek weekday;
    @Column(name ="start_time", nullable =false)
    private LocalTime startTime;
    @Column(name ="end_time", nullable =false)
    private LocalTime endTime;
    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name ="instructor_id")
    private Instructor instructor;
    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name ="room_id")
    private Room room;

    protected Meeting() {}

    Meeting(Section section, DayOfWeek weekday, LocalTime startTime, LocalTime endTime,
            Instructor instructor, Room room) {
        this.section =Objects.requireNonNull(section, "section");
        this.weekday =Objects.requireNonNull(weekday, "weekday");
        this.startTime =Objects.requireNonNull(startTime, "startTime");
        this.endTime =Objects.requireNonNull(endTime, "endTime");
        if (!startTime.isBefore(endTime)) throw new IllegalArgumentException("Meeting start must precede end");
        this.instructor =instructor;
        this.room =room;
    }

    public Section getSection() { return section; }
    public DayOfWeek getWeekday() { return weekday; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Instructor getInstructor() { return instructor; }
    public Room getRoom() { return room; }
}
