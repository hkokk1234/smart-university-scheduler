package com.smartuniversity.scheduler.catalog.api;

import com.smartuniversity.scheduler.catalog.application.CatalogAdministrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
<<<<<<< HEAD
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
=======
>>>>>>> origin/main
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name ="Administration")
@SecurityRequirement(name ="bearerAuth")
public class AdminController {
    private final CatalogAdministrationService administration;
    public AdminController(CatalogAdministrationService administration) { this.administration =administration; }

    @GetMapping("/rooms")
    @Operation(summary ="ADMIN: list available rooms")
    public java.util.List<RoomDto> rooms() { return administration.rooms(); }

    @GetMapping("/instructors")
    @Operation(summary ="ADMIN: list available instructors")
    public java.util.List<InstructorDto> instructors() { return administration.instructors(); }

    @PostMapping("/courses")
    @Operation(summary ="ADMIN: create a course in an existing semester")
    public ResponseEntity<CourseDto> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        CourseDto course =administration.createCourse(request);
        return ResponseEntity.created(URI.create("/api/v1/courses/" + course.id())).body(course);
    }

    @PutMapping("/sections/{id}")
    @Operation(summary ="ADMIN: replace section details and its complete meeting list using the current version")
<<<<<<< HEAD
    public SectionDto updateSection(@PathVariable UUID id, @Valid @RequestBody UpdateSectionRequest request,
                                    @AuthenticationPrincipal Jwt principal) {
        return administration.updateSection(id, request, UUID.fromString(principal.getSubject()));
=======
    public SectionDto updateSection(@PathVariable UUID id, @Valid @RequestBody UpdateSectionRequest request) {
        return administration.updateSection(id, request);
>>>>>>> origin/main
    }
}
