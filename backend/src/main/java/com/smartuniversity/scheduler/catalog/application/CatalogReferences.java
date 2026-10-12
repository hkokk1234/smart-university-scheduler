package com.smartuniversity.scheduler.catalog.application;

import com.smartuniversity.scheduler.catalog.domain.Section;
import com.smartuniversity.scheduler.catalog.domain.Semester;
import com.smartuniversity.scheduler.catalog.infrastructure.SectionRepository;
import com.smartuniversity.scheduler.catalog.infrastructure.SemesterRepository;
import com.smartuniversity.scheduler.platform.api.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional(readOnly =true)
public class CatalogReferences {
    private final SemesterRepository semesters;
    private final SectionRepository sections;
    public CatalogReferences(SemesterRepository semesters, SectionRepository sections) {
        this.semesters =semesters; this.sections =sections;
    }
    public Semester semester(UUID id) { return semesters.findById(id).orElseThrow(() -> ApiException.notFound("Semester")); }
    public Section section(UUID id) { return sections.findById(id).orElseThrow(() -> ApiException.notFound("Section")); }
}
