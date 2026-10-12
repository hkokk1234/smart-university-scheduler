package com.smartuniversity.scheduler.catalog.domain;

import com.smartuniversity.scheduler.platform.persistence.UuidEntity;
import com.smartuniversity.scheduler.platform.persistence.EntityValues;
import jakarta.persistence.*;


@Entity
@Table(name ="rooms")
public class Room extends UuidEntity {
    @Column(nullable =false, length =120)
    private String building;
    @Column(nullable =false, length =32)
    private String code;
    @Column(nullable =false)
    private int capacity;

    protected Room() {}

    public Room(String building, String code, int capacity) {
        this.building =EntityValues.requiredText(building, 120, "building");
        this.code =EntityValues.requiredText(code, 32, "code");
        this.capacity =EntityValues.positive(capacity, "capacity");
    }

    public String getBuilding() { return building; }
    public String getCode() { return code; }
    public int getCapacity() { return capacity; }
}
