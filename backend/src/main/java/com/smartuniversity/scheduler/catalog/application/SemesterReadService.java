package com.smartuniversity.scheduler.catalog.application;

import com.smartuniversity.scheduler.catalog.api.SemesterDto;
import com.smartuniversity.scheduler.catalog.infrastructure.SemesterRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly =true)
public class SemesterReadService {
    private final SemesterRepository semesters;
    public SemesterReadService(SemesterRepository semesters) { this.semesters =semesters; }
    public SemesterDto get(java.util.UUID id) {
        return CatalogDtoMapper.toDto(semesters.findById(id).orElseThrow(()->
                com.smartuniversity.scheduler.platform.api.ApiException.notFound("Semester")));
    }
    public List<SemesterDto> list() {
        return semesters.findAll(PageRequest.of(0, 100, Sort.by(Sort.Order.desc("startsOn"), Sort.Order.asc("id"))))
                .stream().map(CatalogDtoMapper::toDto).toList();
    }
}
