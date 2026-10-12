package com.smartuniversity.scheduler.copilot.api;

import com.smartuniversity.scheduler.copilot.application.CopilotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/copilot")
@SecurityRequirement(name="bearerAuth")
public class CopilotController {
    private final CopilotService copilot;
    public CopilotController(CopilotService copilot) { this.copilot=copilot; }
    public record Status(boolean available) {}
    @GetMapping("/status")
    public ResponseEntity<Status> status() { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Status(copilot.available())); }
    @PostMapping("/chat")
    @Operation(summary="Academic advice from the live catalog and Java CSP solver")
    public ResponseEntity<CopilotResponse> chat(@Valid @RequestBody CopilotRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(copilot.chat(request));
    }
}
