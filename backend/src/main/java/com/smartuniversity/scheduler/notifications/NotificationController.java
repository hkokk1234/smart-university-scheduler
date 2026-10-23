package com.smartuniversity.scheduler.notifications;

import com.smartuniversity.scheduler.platform.api.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.time.Instant;
import java.util.UUID;
import static com.smartuniversity.scheduler.notifications.ChangeModels.*;

@RestController
@Validated
@SecurityRequirement(name ="bearerAuth")
@Tag(name ="Notifications and change history")
public class NotificationController {
    private final ChangeService changes;
    private final NotificationStream streams;
    public NotificationController(ChangeService changes, NotificationStream streams) { this.changes =changes; this.streams =streams; }
    @GetMapping("/api/v1/notifications")
    public ResponseEntity<Inbox> inbox(@AuthenticationPrincipal Jwt principal,
            @RequestParam(defaultValue ="0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue ="20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(changes.inbox(user(principal), page, size));
    }
    @PostMapping("/api/v1/notifications/{id}/read")
    public ResponseEntity<Void> read(@PathVariable UUID id, @AuthenticationPrincipal Jwt principal) {
        changes.markRead(id, user(principal)); return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    @PostMapping("/api/v1/notifications/read-all")
    public ResponseEntity<Void> readAll(@AuthenticationPrincipal Jwt principal) {
        changes.markAllRead(user(principal)); return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    @GetMapping(value ="/api/v1/notifications/stream", produces =MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(@AuthenticationPrincipal Jwt principal) {
        Instant expires =principal.getExpiresAt() ==null ? Instant.now().plusSeconds(60) : principal.getExpiresAt();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Accel-Buffering", "no")
                .body(streams.subscribe(user(principal), expires));
    }
    @GetMapping("/api/v1/admin/sections/{id}/history")
    public ResponseEntity<PageResponse<Change>> history(@PathVariable UUID id,
            @RequestParam(defaultValue ="0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue ="20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(changes.history(id, page, size));
    }
    private UUID user(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
