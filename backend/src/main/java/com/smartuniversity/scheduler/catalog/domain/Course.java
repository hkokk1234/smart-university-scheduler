package com.smartuniversity.scheduler.catalog.domain;

import com.smartuniversity.scheduler.platform.persistence.UuidEntity;
import com.smartuniversity.scheduler.platform.persistence.EntityValues;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Entity
@Table(name ="courses")
public class Course extends UuidEntity {
    @ManyToOne(fetch =FetchType.LAZY, optional =false)
    @JoinColumn(name ="semester_id", nullable =false)
    private Semester semester;
    @Column(nullable =false, length =32)
    private String code;
    @Column(nullable =false, length =200)
    private String title;
    @Column(nullable =false, precision =4, scale =1)
    private BigDecimal credits;

    protected Course() {}

    public Course(Semester semester, String code, String title, BigDecimal credits) {
        this.semester =Objects.requireNonNull(semester, "semester");
        this.code =EntityValues.requiredText(code, 32, "code");
        this.title =EntityValues.requiredText(title, 200, "title");
        this.credits =Objects.requireNonNull(credits, "credits").setScale(1, RoundingMode.UNNECESSARY);
        if (credits.signum() <=0 || credits.compareTo(BigDecimal.valueOf(60)) > 0) {
            throw new IllegalArgumentException("Credits must be greater than zero and at most 60");
        }
    }

    public Semester getSemester() { return semester; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public BigDecimal getCredits() { return credits; }
}
