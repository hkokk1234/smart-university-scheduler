package com.smartuniversity.scheduler.catalog.infrastructure;

import com.smartuniversity.scheduler.catalog.domain.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {
    @EntityGraph(attributePaths ={"instructor", "room"})
    List<Meeting> findBySectionIdOrderByWeekdayAscStartTimeAsc(UUID sectionId);
}
