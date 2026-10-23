package com.smartuniversity.scheduler.catalog.infrastructure;

import com.smartuniversity.scheduler.catalog.domain.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;

public interface SectionRepository extends JpaRepository<Section, UUID> {
    @Override
    @EntityGraph(attributePaths ={"meetings", "meetings.instructor", "meetings.room", "sectionGroup.course"})
    Optional<Section> findById(UUID id);

    @EntityGraph(attributePaths ={"meetings", "meetings.instructor", "meetings.room", "sectionGroup.course"})
    List<Section> findBySectionGroupIdOrderByCodeAsc(UUID sectionGroupId);
}
