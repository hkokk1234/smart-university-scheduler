package com.smartuniversity.scheduler.catalog.application;

import com.smartuniversity.scheduler.catalog.api.SectionDto;
import com.smartuniversity.scheduler.catalog.infrastructure.SectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly =true)
public class CatalogReadService {
    private final SectionRepository sections;

    public CatalogReadService(SectionRepository sections) { this.sections =sections; }

    public Optional<SectionDto> findSection(UUID id) {
        return sections.findById(id).map(CatalogDtoMapper::toDto);
    }

    public List<SectionDto> findSectionAlternatives(UUID sectionGroupId) {
        return sections.findBySectionGroupIdOrderByCodeAsc(sectionGroupId)
                .stream().map(CatalogDtoMapper::toDto).toList();
    }
}
