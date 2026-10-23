package com.smartuniversity.scheduler.scheduling.api;

import com.smartuniversity.scheduler.scheduling.application.ScheduleGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/schedules")
@SecurityRequirement(name ="bearerAuth")
public class ScheduleGenerationController {
    private final ScheduleGenerationService generator;
    public ScheduleGenerationController(ScheduleGenerationService generator) { this.generator =generator; }
    @PostMapping("/generate")
    @Operation(summary ="Generate up to five ranked schedules; TIMEOUT can include partial valid results")
    public ResponseEntity<GenerationResponse> generate(@Valid @RequestBody GenerateScheduleRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(generator.generate(request));
    }
}
