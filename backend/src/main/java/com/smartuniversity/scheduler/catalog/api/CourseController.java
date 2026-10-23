package com.smartuniversity.scheduler.catalog.api;

import com.smartuniversity.scheduler.catalog.application.CourseService;
import com.smartuniversity.scheduler.platform.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
@Validated
@Tag(name ="Course Catalog")
public class CourseController {
    private final CourseService courses;
    public CourseController(CourseService courses) { this.courses =courses; }

    @GetMapping
    @Operation(summary ="Search courses by semester, exact code, or text with bounded pagination")
    public PageResponse<CourseDto> search(
            @RequestParam(required =false) UUID semesterId,
            @RequestParam(required =false) @Size(max =100) String q,
            @RequestParam(required =false) @Size(max =32) String code,
            @RequestParam(defaultValue ="0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue ="20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue ="code,asc") @Size(max =64) String sort) {
        return courses.search(semesterId, q, code, page, size, sort);
    }

    @GetMapping("/{id}")
    @Operation(summary ="Get a course with section groups, alternatives, and every weekly meeting")
    public CourseDetailDto details(@PathVariable UUID id) { return courses.details(id); }
}
