package com.smartuniversity.scheduler.catalog.infrastructure;

import com.smartuniversity.scheduler.catalog.domain.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface InstructorRepository extends JpaRepository<Instructor, UUID> {
    Optional<Instructor> findByStaffCode(String staffCode);
}
