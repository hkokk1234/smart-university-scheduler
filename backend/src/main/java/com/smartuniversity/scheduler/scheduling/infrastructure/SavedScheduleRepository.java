package com.smartuniversity.scheduler.scheduling.infrastructure;

import com.smartuniversity.scheduler.scheduling.domain.SavedSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SavedScheduleRepository extends JpaRepository<SavedSchedule, UUID> {
    Page<SavedSchedule> findByUserId(UUID userId, Pageable pageable);

    // Page parent rows first; fetch collections in a second query to avoid in-memory pagination.
    @Query("select distinct s from SavedSchedule s where s.id in :ids and s.user.id =:userId")
    @EntityGraph(attributePaths ={"selections", "selections.section", "selections.section.meetings",
            "selections.section.meetings.instructor", "selections.section.meetings.room", "selections.section.sectionGroup.course"})
    List<SavedSchedule> findDetailedByIdsAndUserId(@Param("ids") List<UUID> ids, @Param("userId") UUID userId);

    @EntityGraph(attributePaths ={"selections", "selections.section", "selections.section.meetings",
            "selections.section.meetings.instructor", "selections.section.meetings.room", "selections.section.sectionGroup.course"})
    Optional<SavedSchedule> findByIdAndUserId(UUID id, UUID userId);
}
