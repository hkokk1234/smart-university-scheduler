package com.smartuniversity.scheduler.digitaltwin.api;

import com.smartuniversity.scheduler.digitaltwin.application.DigitalTwinService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/digital-twin")
public class DigitalTwinController {
    private final DigitalTwinService service;
    public DigitalTwinController(DigitalTwinService service) { this.service = service; }
    @PostMapping("/simulate")
    public DigitalTwinService.Comparison simulate(@Valid @RequestBody SimulationRequest request) { return service.simulate(request); }
}
