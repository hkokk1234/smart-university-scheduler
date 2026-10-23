package com.smartuniversity.scheduler.catalog.domain;

import com.smartuniversity.scheduler.platform.persistence.UuidEntity;
import com.smartuniversity.scheduler.platform.persistence.EntityValues;
import jakarta.persistence.*;


@Entity
@Table(name ="instructors")
public class Instructor extends UuidEntity {
    @Column(name ="staff_code", nullable =false, length =32, unique =true)
    private String staffCode;
    @Column(name ="display_name", nullable =false, length =120)
    private String displayName;
    @Column(length =254, unique =true)
    private String email;

    protected Instructor() {}

    public Instructor(String staffCode, String displayName, String email) {
        this.staffCode =EntityValues.requiredText(staffCode, 32, "staffCode");
        this.displayName =EntityValues.requiredText(displayName, 120, "displayName");
        this.email =email ==null ? null : EntityValues.email(email);
    }

    public String getStaffCode() { return staffCode; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
}
