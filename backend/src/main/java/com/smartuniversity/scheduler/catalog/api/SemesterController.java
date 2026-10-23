package com.smartuniversity.scheduler.catalog.api;

import com.smartuniversity.scheduler.catalog.application.SemesterReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/semesters")
@Tag(name ="Course Catalog")
public class SemesterController {
    private final SemesterReadService semesters;
    public SemesterController(SemesterReadService semesters) { this.semesters =semesters; }
    @GetMapping
    @Operation(summary ="List up to 100 semesters, newest first")
    public List<SemesterDto> list() {
        return semesters.list();
    }
}
