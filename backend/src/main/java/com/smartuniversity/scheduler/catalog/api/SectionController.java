package com.smartuniversity.scheduler.catalog.api;
import com.smartuniversity.scheduler.catalog.application.CatalogReadService;
import com.smartuniversity.scheduler.platform.api.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sections")
@Tag(name ="Sections and Meetings")
public class SectionController {
    private final CatalogReadService sections;
    public SectionController(CatalogReadService sections) { this.sections =sections; }
    @GetMapping("/{id}")
    @Operation(summary ="Get a section, its current version, and all meetings")
    public SectionDto details(@PathVariable UUID id) {
        return sections.findSection(id).orElseThrow(() -> ApiException.notFound("Section"));
    }
}
