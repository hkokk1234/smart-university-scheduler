package com.smartuniversity.scheduler.catalog.infrastructure;

import com.smartuniversity.scheduler.catalog.domain.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, UUID> {
    Optional<Semester> findByCode(String code);
}
