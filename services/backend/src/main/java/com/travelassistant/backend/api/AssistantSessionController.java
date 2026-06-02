package com.travelassistant.backend.api;

import com.travelassistant.backend.api.model.ApiModels.AssistantMessageRequest;
import com.travelassistant.backend.api.model.ApiModels.AssistantMessageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant/sessions")
class AssistantSessionController {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AssistantMessageResponse createSession(@Valid @RequestBody(required = false) AssistantMessageRequest request) {
        return SkeletonResponses.assistantMessage("session-skeleton", request);
    }

    @PostMapping("/{sessionId}/messages")
    AssistantMessageResponse continueSession(
            @PathVariable String sessionId,
            @Valid @RequestBody AssistantMessageRequest request
    ) {
        return SkeletonResponses.assistantMessage(sessionId, request);
    }
}

