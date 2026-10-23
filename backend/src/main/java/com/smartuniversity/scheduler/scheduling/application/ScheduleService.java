package com.smartuniversity.scheduler.scheduling.application;

import com.smartuniversity.scheduler.catalog.application.CatalogReferences;
import com.smartuniversity.scheduler.identity.application.IdentityReferences;
import com.smartuniversity.scheduler.platform.api.*;
import com.smartuniversity.scheduler.scheduling.api.*;
import com.smartuniversity.scheduler.scheduling.domain.SavedSchedule;
import com.smartuniversity.scheduler.scheduling.infrastructure.SavedScheduleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional
public class ScheduleService {
    private final SavedScheduleRepository schedules;
    private final IdentityReferences identities;
    private final CatalogReferences catalog;
    public ScheduleService(SavedScheduleRepository schedules, IdentityReferences identities, CatalogReferences catalog) {
        this.schedules =schedules; this.identities =identities; this.catalog =catalog;
    }

    @Transactional(readOnly =true)
    public PageResponse<SavedScheduleDto> list(UUID userId, int page, int size) {
        var result =schedules.findByUserId(userId, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))));
        if (!result.isEmpty()) {
            schedules.findDetailedByIdsAndUserId(result.stream().map(SavedSchedule::getId).toList(), userId);
        }
        return PageResponse.from(result, ScheduleDtoMapper::toDto);
    }

    public SavedScheduleDto create(UUID userId, CreateScheduleRequest request) {
        SavedSchedule schedule =new SavedSchedule(identities.user(userId), catalog.semester(request.semesterId()), request.name());
        for (UUID sectionId : request.sectionIds()) schedule.addSection(catalog.section(sectionId));
        schedules.saveAndFlush(schedule);
        return ScheduleDtoMapper.toDto(schedule);
    }

    public void delete(UUID id, UUID userId) {
        SavedSchedule schedule =schedules.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Schedule"));
        schedules.delete(schedule);
        schedules.flush();
    }
}
