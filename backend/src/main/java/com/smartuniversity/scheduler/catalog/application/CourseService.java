package com.smartuniversity.scheduler.catalog.application;

import com.smartuniversity.scheduler.catalog.api.*;
import com.smartuniversity.scheduler.catalog.domain.Course;
import com.smartuniversity.scheduler.catalog.infrastructure.*;
import com.smartuniversity.scheduler.platform.api.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly =true)
public class CourseService {
    private static final Set<String> SORT_FIELDS =Set.of("code", "title", "credits", "id");
    private final CourseRepository courses;
    private final SectionGroupRepository groups;
    private final SectionRepository sections;
    public CourseService(CourseRepository courses, SectionGroupRepository groups, SectionRepository sections) {
        this.courses =courses; this.groups =groups; this.sections =sections;
    }

    public PageResponse<CourseDto> search(UUID semesterId, String q, String code, int page, int size, String sort) {
        String[] parts =sort.split(",", -1);
        if (parts.length !=2 || !SORT_FIELDS.contains(parts[0])
                || !(parts[1].equals("asc") || parts[1].equals("desc"))) {
            throw new IllegalArgumentException("Unsupported sort");
        }
        Sort ordering =Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
        if (!parts[0].equals("id")) ordering =ordering.and(Sort.by("id"));
        Specification<Course> filter =(root, query, cb) -> {
            var predicates =new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (semesterId !=null) predicates.add(cb.equal(root.get("semester").get("id"), semesterId));
            if (code !=null && !code.isBlank()) predicates.add(cb.equal(cb.lower(root.get("code")), code.strip().toLowerCase(Locale.ROOT)));
            if (q !=null && !q.isBlank()) {
                String pattern ="%" + escapeLike(q.strip().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("code")), pattern, '\\'),
                        cb.like(cb.lower(root.get("title")), pattern, '\\')));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return PageResponse.from(courses.findAll(filter, PageRequest.of(page, size, ordering)), CatalogDtoMapper::toDto);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public CourseDetailDto details(UUID id) {
        Course course =courses.findById(id).orElseThrow(() -> ApiException.notFound("Course"));
        var choices =groups.findByCourseIdOrderByCodeAsc(id).stream().map(group ->
                new CourseGroupDto(CatalogDtoMapper.toDto(group),
                        sections.findBySectionGroupIdOrderByCodeAsc(group.getId()).stream()
                                .map(CatalogDtoMapper::toDto).toList())).toList();
        return new CourseDetailDto(CatalogDtoMapper.toDto(course), choices);
    }
}
