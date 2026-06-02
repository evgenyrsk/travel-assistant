package com.travelassistant.backend.api;

import com.travelassistant.backend.api.model.ApiModels.HealthResponse;
import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class HealthController {

    @GetMapping("/health")
    HealthResponse getHealth() {
        return new HealthResponse("ok", "travel-assistant-backend", "0.1.0", OffsetDateTime.now());
    }
}

