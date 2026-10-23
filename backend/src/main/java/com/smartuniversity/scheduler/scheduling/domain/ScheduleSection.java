package com.smartuniversity.scheduler.scheduling.domain;

import com.smartuniversity.scheduler.platform.persistence.UuidEntity;
import com.smartuniversity.scheduler.platform.persistence.EntityValues;
import jakarta.persistence.*;
import com.smartuniversity.scheduler.catalog.domain.Section;
import com.smartuniversity.scheduler.catalog.domain.SectionGroup;
import com.smartuniversity.scheduler.catalog.domain.Semester;

@Entity
@Table(name ="schedule_sections")
public class ScheduleSection extends UuidEntity {
    @ManyToOne(fetch =FetchType.LAZY, optional =false)
    @JoinColumn(name ="saved_schedule_id", nullable =false)
    private SavedSchedule savedSchedule;
    @ManyToOne(fetch =FetchType.LAZY, optional =false)
    @JoinColumn(name ="section_id", nullable =false)
    private Section section;
    @ManyToOne(fetch =FetchType.LAZY, optional =false)
    @JoinColumn(name ="section_group_id", nullable =false)
    private SectionGroup sectionGroup;
    @ManyToOne(fetch =FetchType.LAZY, optional =false)
    @JoinColumn(name ="semester_id", nullable =false)
    private Semester semester;

    protected ScheduleSection() {}

    ScheduleSection(SavedSchedule savedSchedule, Section section) {
        this.savedSchedule =savedSchedule;
        this.section =section;
        this.sectionGroup =section.getSectionGroup();
        this.semester =section.getSemester();
    }

    public SavedSchedule getSavedSchedule() { return savedSchedule; }
    public Section getSection() { return section; }
    public SectionGroup getSectionGroup() { return sectionGroup; }
    public Semester getSemester() { return semester; }
}
