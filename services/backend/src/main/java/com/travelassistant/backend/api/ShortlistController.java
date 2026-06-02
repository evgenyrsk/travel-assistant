package com.travelassistant.backend.api;

import com.travelassistant.backend.api.model.ApiModels.ShortlistItem;
import com.travelassistant.backend.api.model.ApiModels.ShortlistItemRequest;
import com.travelassistant.backend.api.model.ApiModels.ShortlistResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant/sessions/{sessionId}/shortlist")
class ShortlistController {

    @GetMapping
    ShortlistResponse getShortlist(@PathVariable String sessionId) {
        return SkeletonResponses.shortlist(sessionId);
    }

    @PutMapping("/{offerId}")
    ShortlistItem upsertShortlistItem(
            @PathVariable String sessionId,
            @PathVariable String offerId,
            @Valid @RequestBody(required = false) ShortlistItemRequest request
    ) {
        return SkeletonResponses.shortlistItem(sessionId, offerId, request);
    }

    @DeleteMapping("/{offerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeShortlistItem(@PathVariable String sessionId, @PathVariable String offerId) {
        // Skeleton-only endpoint. No current-session storage exists in Stage 7.1.
    }
}

