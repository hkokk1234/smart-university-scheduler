package com.smartuniversity.scheduler.catalog.infrastructure;

import com.smartuniversity.scheduler.catalog.domain.SectionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface SectionGroupRepository extends JpaRepository<SectionGroup, UUID> {
    List<SectionGroup> findByCourseIdOrderByCodeAsc(UUID courseId);
}
