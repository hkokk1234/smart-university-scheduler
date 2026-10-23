package com.smartuniversity.scheduler.identity.domain;

import com.smartuniversity.scheduler.platform.persistence.UuidEntity;
import com.smartuniversity.scheduler.platform.persistence.EntityValues;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name ="app_users")
public class User extends UuidEntity {
    @Column(nullable =false, length =254, unique =true)
    private String email;
    @Column(name ="display_name", nullable =false, length =120)
    private String displayName;
    @Column(name ="created_at", nullable =false, updatable =false)
    private Instant createdAt;
    @Column(name ="password_hash", length =100)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable =false, length =16)
    private Role role =Role.STUDENT;

    protected User() {}

    public User(String email, String displayName) {
        this.email =EntityValues.email(email);
        this.displayName =EntityValues.requiredText(displayName, 120, "displayName");
        this.createdAt =Instant.now();
    }

    public String getEmail() { return email; }
    public User(String email, String displayName, String passwordHash) {
        this(email, displayName);
        this.passwordHash =java.util.Objects.requireNonNull(passwordHash, "passwordHash");
    }

    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public String getDisplayName() { return displayName; }
    public Instant getCreatedAt() { return createdAt; }
}
