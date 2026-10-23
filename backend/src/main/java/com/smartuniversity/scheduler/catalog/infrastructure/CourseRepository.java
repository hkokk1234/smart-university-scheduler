package com.smartuniversity.scheduler.catalog.infrastructure;

import com.smartuniversity.scheduler.catalog.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, UUID>, JpaSpecificationExecutor<Course> {
    List<Course> findBySemesterIdOrderByCodeAsc(UUID semesterId);
    Optional<Course> findBySemesterIdAndCode(UUID semesterId, String code);
}
