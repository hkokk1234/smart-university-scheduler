package com.smartuniversity.scheduler.catalog.domain;

import com.smartuniversity.scheduler.platform.persistence.UuidEntity;
import com.smartuniversity.scheduler.platform.persistence.EntityValues;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name ="section_groups")
public class SectionGroup extends UuidEntity {
    @ManyToOne(fetch =FetchType.LAZY, optional =false)
    @JoinColumn(name ="course_id", nullable =false)
    private Course course;
    @ManyToOne(fetch =FetchType.LAZY, optional =false)
    @JoinColumn(name ="semester_id", nullable =false)
    private Semester semester;
    @Column(nullable =false, length =32)
    private String code;
    @Column(nullable =false, length =120)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable =false, length =16)
    private SectionGroupType type;

    protected SectionGroup() {}

    public SectionGroup(Course course, String code, String name, SectionGroupType type) {
        this.course =Objects.requireNonNull(course, "course");
        this.semester =course.getSemester();
        this.code =EntityValues.requiredText(code, 32, "code");
        this.name =EntityValues.requiredText(name, 120, "name");
        this.type =Objects.requireNonNull(type, "type");
    }

    public Course getCourse() { return course; }
    public Semester getSemester() { return semester; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public SectionGroupType getType() { return type; }
}
