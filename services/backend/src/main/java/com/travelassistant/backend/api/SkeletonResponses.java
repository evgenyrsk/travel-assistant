package com.travelassistant.backend.api;

import com.travelassistant.backend.api.model.ApiModels.AssistantMessage;
import com.travelassistant.backend.api.model.ApiModels.AssistantMessageRequest;
import com.travelassistant.backend.api.model.ApiModels.AssistantMessageResponse;
import com.travelassistant.backend.api.model.ApiModels.AssistantExplanationRequest;
import com.travelassistant.backend.api.model.ApiModels.AssistantExplanationResponse;
import com.travelassistant.backend.api.model.ApiModels.AssistantNextAction;
import com.travelassistant.backend.api.model.ApiModels.AssistantSession;
import com.travelassistant.backend.api.model.ApiModels.AssistantSessionStatus;
import com.travelassistant.backend.api.model.ApiModels.HotelOffersResponse;
import com.travelassistant.backend.api.model.ApiModels.HotelSearchRequest;
import com.travelassistant.backend.api.model.ApiModels.HotelSearchResponse;
import com.travelassistant.backend.api.model.ApiModels.HotelSearchStatus;
import com.travelassistant.backend.api.model.ApiModels.Role;
import com.travelassistant.backend.api.model.ApiModels.ShortlistItem;
import com.travelassistant.backend.api.model.ApiModels.ShortlistItemRequest;
import com.travelassistant.backend.api.model.ApiModels.ShortlistResponse;
import java.time.OffsetDateTime;
import java.util.List;

final class SkeletonResponses {

    private SkeletonResponses() {
    }

    static AssistantMessageResponse assistantMessage(String sessionId, AssistantMessageRequest request) {
        var now = OffsetDateTime.now();
        var session = new AssistantSession(
                sessionId,
                AssistantSessionStatus.COLLECTING_REQUIREMENTS,
                now,
                now,
                null
        );
        var content = "Skeleton assistant response. Stage 7.1 does not implement orchestration.";
        var message = new AssistantMessage(Role.ASSISTANT, content, List.of(), List.of(), List.of());
        return new AssistantMessageResponse(session, message, AssistantNextAction.NONE, null);
    }

    static HotelSearchResponse hotelSearch(HotelSearchRequest request) {
        return new HotelSearchResponse(
                "search-skeleton",
                request.sessionId(),
                HotelSearchStatus.ACCEPTED,
                request.criteria(),
                null,
                null
        );
    }

    static HotelOffersResponse hotelOffers(String searchId) {
        return new HotelOffersResponse(
                searchId,
                HotelSearchStatus.SEARCHING,
                List.of(),
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    static ShortlistResponse shortlist(String sessionId) {
        return new ShortlistResponse(sessionId, List.of());
    }

    static ShortlistItem shortlistItem(String sessionId, String offerId, ShortlistItemRequest request) {
        return new ShortlistItem(
                "shortlist-item-skeleton",
                sessionId,
                offerId,
                OffsetDateTime.now(),
                request == null ? null : request.note(),
                null,
                null
        );
    }

    static AssistantExplanationResponse explanation(AssistantExplanationRequest request) {
        var content = "Skeleton explanation response. Stage 7.1 does not implement LLM reasoning.";
        return new AssistantExplanationResponse(
                request.mode(),
                content,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}

