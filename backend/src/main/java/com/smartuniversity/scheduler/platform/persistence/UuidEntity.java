package com.smartuniversity.scheduler.platform.persistence;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

@MappedSuperclass
public abstract class UuidEntity {
    @Id
    @GeneratedValue(strategy =GenerationType.UUID)
    private UUID id;

    protected UuidEntity() {}

    public UUID getId() { return id; }

    public boolean hasSameIdentity(UuidEntity other) {
        return this ==other || (other !=null && getId() !=null && getId().equals(other.getId()));
    }
}
