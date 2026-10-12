package com.smartuniversity.scheduler.catalog.domain;

import com.smartuniversity.scheduler.platform.persistence.UuidEntity;
import com.smartuniversity.scheduler.platform.persistence.EntityValues;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

@Entity
@Table(name ="semesters")
public class Semester extends UuidEntity {
    @Column(nullable =false, length =32, unique =true)
    private String code;
    @Column(nullable =false, length =120)
    private String name;
    @Column(name ="starts_on", nullable =false)
    private LocalDate startsOn;
    @Column(name ="ends_on", nullable =false)
    private LocalDate endsOn;
    @Column(name ="time_zone", nullable =false, length =64)
    private String timeZone;

    protected Semester() {}

    public Semester(String code, String name, LocalDate startsOn, LocalDate endsOn, String timeZone) {
        this.code =EntityValues.requiredText(code, 32, "code");
        this.name =EntityValues.requiredText(name, 120, "name");
        this.startsOn =Objects.requireNonNull(startsOn, "startsOn");
        this.endsOn =Objects.requireNonNull(endsOn, "endsOn");
        if (!startsOn.isBefore(endsOn)) throw new IllegalArgumentException("Semester start must precede end");
        this.timeZone =ZoneId.of(EntityValues.requiredText(timeZone, 64, "timeZone")).getId();
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public LocalDate getStartsOn() { return startsOn; }
    public LocalDate getEndsOn() { return endsOn; }
    public String getTimeZone() { return timeZone; }
}
