package com.smartuniversity.scheduler.scheduling.infrastructure;

import com.smartuniversity.scheduler.scheduling.domain.ScheduleSection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface ScheduleSectionRepository extends JpaRepository<ScheduleSection, UUID> {
    List<ScheduleSection> findBySavedScheduleId(UUID savedScheduleId);
}
