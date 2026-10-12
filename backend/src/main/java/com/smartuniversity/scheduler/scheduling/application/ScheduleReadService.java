package com.smartuniversity.scheduler.scheduling.application;

import com.smartuniversity.scheduler.scheduling.api.SavedScheduleDto;
import com.smartuniversity.scheduler.scheduling.infrastructure.SavedScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly =true)
public class ScheduleReadService {
    private final SavedScheduleRepository schedules;

    public ScheduleReadService(SavedScheduleRepository schedules) { this.schedules =schedules; }

    // The caller must obtain userId from an authenticated principal, never an untrusted request body.
    public Optional<SavedScheduleDto> findSchedule(UUID id, UUID userId) {
        Objects.requireNonNull(userId, "userId");
        return schedules.findByIdAndUserId(id, userId).map(ScheduleDtoMapper::toDto);
    }
}
