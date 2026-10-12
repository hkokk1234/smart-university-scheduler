package com.smartuniversity.scheduler.scheduling.api;

import com.smartuniversity.scheduler.platform.api.*;
import com.smartuniversity.scheduler.scheduling.application.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
@Validated
@Tag(name ="Saved Schedules")
@SecurityRequirement(name ="bearerAuth")
public class ScheduleController {
    private final ScheduleService schedules;
    private final ScheduleReadService reader;
    public ScheduleController(ScheduleService schedules, ScheduleReadService reader) {
        this.schedules =schedules; this.reader =reader;
    }

    @GetMapping
    @Operation(summary ="List only the authenticated user's saved schedules")
    public ResponseEntity<PageResponse<SavedScheduleDto>> list(@AuthenticationPrincipal Jwt principal,
            @RequestParam(defaultValue ="0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue ="20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(schedules.list(UUID.fromString(principal.getSubject()), page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary ="Get an owned schedule; missing and foreign schedules both return 404")
    public ResponseEntity<SavedScheduleDto> details(@PathVariable UUID id, @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(reader.findSchedule(id, UUID.fromString(principal.getSubject()))
                        .orElseThrow(() -> ApiException.notFound("Schedule")));
    }

    @PostMapping
    @Operation(summary ="Save selected sections with all meetings; ownership comes from the JWT")
    public ResponseEntity<SavedScheduleDto> create(@AuthenticationPrincipal Jwt principal,
                                                   @Valid @RequestBody CreateScheduleRequest request) {
        SavedScheduleDto schedule =schedules.create(UUID.fromString(principal.getSubject()), request);
        return ResponseEntity.created(URI.create("/api/v1/schedules/" + schedule.id()))
                .cacheControl(CacheControl.noStore()).body(schedule);
    }

    @DeleteMapping("/{id}")
    @Operation(summary ="Delete an owned schedule")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt principal) {
        schedules.delete(id, UUID.fromString(principal.getSubject()));
        return ResponseEntity.noContent().build();
    }
}
