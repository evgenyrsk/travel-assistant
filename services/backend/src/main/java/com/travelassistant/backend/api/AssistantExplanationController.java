package com.travelassistant.backend.api;

import com.travelassistant.backend.api.model.ApiModels.AssistantExplanationRequest;
import com.travelassistant.backend.api.model.ApiModels.AssistantExplanationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant/sessions/{sessionId}/explanations")
class AssistantExplanationController {

    @PostMapping
    AssistantExplanationResponse createExplanation(
            @PathVariable String sessionId,
            @Valid @RequestBody AssistantExplanationRequest request
    ) {
        return SkeletonResponses.explanation(request);
    }
}

