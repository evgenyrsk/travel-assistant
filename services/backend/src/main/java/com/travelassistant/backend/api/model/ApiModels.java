package com.travelassistant.backend.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelassistant.backend.api.error.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class ApiModels {

    private ApiModels() {
    }

    public record HealthResponse(
            String status,
            String service,
            String version,
            OffsetDateTime currentTime
    ) {
    }

    public record AssistantSession(
            String sessionId,
            AssistantSessionStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            SearchIntentSummary searchIntentSummary
    ) {
    }

    public record AssistantMessageRequest(
            @NotBlank @Size(max = 4000) String message,
            @Valid ClientContext clientContext
    ) {
    }

    public record ClientContext(
            String locale,
            String timezone
    ) {
    }

    public record AssistantMessageResponse(
            AssistantSession session,
            AssistantMessage assistantMessage,
            AssistantNextAction nextAction,
            HotelSearchRequest hotelSearchRequest
    ) {
    }

    public record AssistantMessage(
            Role role,
            String content,
            List<AssistantAssumption> assumptions,
            List<DerivedAssumption> derivedAssumptions,
            List<UnknownData> unknowns
    ) {
    }

    public record SearchIntentSummary(
            @Valid HotelSearchCriteria criteria,
            List<String> userProvidedConstraints,
            List<UserPreference> userPreferences,
            List<AssistantAssumption> assistantAssumptions,
            List<DerivedAssumption> derivedAssumptions,
            List<UnknownData> unknowns,
            List<String> missingRequiredFields
    ) {
    }

    public record HotelSearchRequest(
            @NotBlank String sessionId,
            @NotNull @Valid HotelSearchCriteria criteria,
            @Valid SearchIntentSummary searchIntentSummary
    ) {
    }

    public record HotelSearchCriteria(
            @NotBlank String destination,
            @NotNull LocalDate checkInDate,
            @NotNull LocalDate checkOutDate,
            @NotNull @Valid Guests guests,
            @Min(1) Integer rooms,
            @Valid Budget budget,
            List<UserPreference> preferences,
            List<String> requiredAmenities,
            List<AssistantAssumption> assistantAssumptions,
            List<DerivedAssumption> derivedAssumptions,
            List<UnknownData> unknowns
    ) {
    }

    public record Guests(
            @NotNull @Min(1) Integer adults,
            @Min(0) Integer children
    ) {
    }

    public record Budget(
            @Min(0) BigDecimal min,
            @Min(0) BigDecimal max,
            @Size(min = 3, max = 3) String currency
    ) {
    }

    public record HotelSearchResponse(
            String searchId,
            String sessionId,
            HotelSearchStatus status,
            HotelSearchCriteria criteria,
            SearchResultMetadata metadata,
            HotelSearchFailure failure
    ) {
    }

    public record HotelOffersResponse(
            String searchId,
            HotelSearchStatus status,
            List<HotelOffer> offers,
            SearchResultMetadata metadata,
            HotelSearchFailure failure,
            List<ProviderFact> providerFacts,
            List<UserPreference> userPreferences,
            List<AssistantAssumption> assumptions,
            List<DerivedAssumption> derivedAssumptions,
            List<UnknownData> unknowns
    ) {
    }

    public record HotelOffer(
            String offerId,
            String providerOfferRef,
            String hotelName,
            HotelLocation location,
            HotelPrice price,
            HotelRating rating,
            List<HotelAmenity> amenities,
            String cancellationPolicy,
            Availability availability,
            String source,
            Freshness freshness,
            String matchSummary,
            List<ProviderFact> providerFacts,
            List<AssistantAssumption> assistantAssumptions,
            List<DerivedAssumption> derivedAssumptions,
            List<UnknownData> unknowns
    ) {
    }

    public record HotelLocation(
            String city,
            String country,
            String address,
            @Min(-90) @Max(90) BigDecimal latitude,
            @Min(-180) @Max(180) BigDecimal longitude,
            @Min(0) BigDecimal distanceToCenterKm
    ) {
    }

    public record HotelPrice(
            BigDecimal amount,
            String currency,
            PriceBasis basis,
            YesNoUnknown includesTaxesAndFees,
            Freshness providerFreshness
    ) {
    }

    public record HotelRating(
            @Min(0) BigDecimal value,
            @Min(0) BigDecimal scale,
            @Min(0) Integer reviewCount,
            String source
    ) {
    }

    public record HotelAmenity(
            String code,
            String name,
            HotelAmenitySource source
    ) {
    }

    public record SearchResultMetadata(
            ResultCompleteness resultCompleteness,
            Freshness freshness,
            ProviderState providerState,
            OffsetDateTime refreshedAt,
            List<String> warnings
    ) {
    }

    public record HotelSearchFailure(
            HotelSearchFailureCategory category,
            String message,
            Boolean retryable,
            OffsetDateTime occurredAt
    ) {
    }

    public record ProviderFact(
            String field,
            Object value,
            String source,
            Freshness freshness
    ) {
    }

    public record UserPreference(
            String field,
            String value,
            PreferenceStrength strength,
            UserPreferenceSource source
    ) {
    }

    public record AssistantAssumption(
            String field,
            String value,
            String reason,
            Boolean needsConfirmation
    ) {
    }

    public record DerivedAssumption(
            DerivedAssumptionCategory category,
            String field,
            Object value,
            String reason,
            Boolean needsConfirmation
    ) {
    }

    public record UnknownData(
            String field,
            String reason,
            Boolean decisionCritical
    ) {
    }

    public record ShortlistItem(
            String itemId,
            String sessionId,
            String offerId,
            OffsetDateTime addedAt,
            String note,
            HotelOffer offer,
            Freshness freshness
    ) {
    }

    public record ShortlistItemRequest(
            @Size(max = 1000) String note
    ) {
    }

    public record ShortlistResponse(
            String sessionId,
            List<ShortlistItem> items
    ) {
    }

    public record AssistantExplanationRequest(
            @NotEmpty @Size(max = 5) List<String> offerIds,
            @NotNull ExplanationMode mode,
            @Size(max = 2000) String question
    ) {
    }

    public record AssistantExplanationResponse(
            ExplanationMode mode,
            String content,
            List<ProviderFact> providerFactsUsed,
            List<AssistantAssumption> assistantAssumptions,
            List<DerivedAssumption> derivedAssumptions,
            List<UnknownData> unknowns
    ) {
    }

    public record ErrorResponse(
            ErrorCode code,
            String message,
            String requestId,
            Map<String, Object> details
    ) {
    }

    public record ValidationErrorResponse(
            ErrorCode code,
            String message,
            String requestId,
            List<ValidationFieldError> fields
    ) {
    }

    public record ValidationFieldError(
            String field,
            String message,
            Object rejectedValue
    ) {
    }

    public enum AssistantSessionStatus {
        @JsonProperty("collecting_requirements")
        COLLECTING_REQUIREMENTS,
        @JsonProperty("ready_for_search")
        READY_FOR_SEARCH,
        @JsonProperty("searching")
        SEARCHING,
        @JsonProperty("results_available")
        RESULTS_AVAILABLE,
        @JsonProperty("search_failed")
        SEARCH_FAILED,
        @JsonProperty("no_offers_available")
        NO_OFFERS_AVAILABLE,
        @JsonProperty("closed")
        CLOSED
    }

    public enum AssistantNextAction {
        @JsonProperty("ask_clarification")
        ASK_CLARIFICATION,
        @JsonProperty("ready_for_hotel_search")
        READY_FOR_HOTEL_SEARCH,
        @JsonProperty("show_hotel_results")
        SHOW_HOTEL_RESULTS,
        @JsonProperty("show_boundary_message")
        SHOW_BOUNDARY_MESSAGE,
        @JsonProperty("none")
        NONE
    }

    public enum Role {
        @JsonProperty("assistant")
        ASSISTANT,
        @JsonProperty("user")
        USER
    }

    public enum HotelSearchStatus {
        @JsonProperty("accepted")
        ACCEPTED,
        @JsonProperty("searching")
        SEARCHING,
        @JsonProperty("completed_with_offers")
        COMPLETED_WITH_OFFERS,
        @JsonProperty("completed_no_offers")
        COMPLETED_NO_OFFERS,
        @JsonProperty("failed")
        FAILED
    }

    public enum Availability {
        @JsonProperty("available")
        AVAILABLE,
        @JsonProperty("limited")
        LIMITED,
        @JsonProperty("unknown")
        UNKNOWN
    }

    public enum Freshness {
        @JsonProperty("fresh")
        FRESH,
        @JsonProperty("stale")
        STALE,
        @JsonProperty("unknown")
        UNKNOWN
    }

    public enum PriceBasis {
        @JsonProperty("per_night")
        PER_NIGHT,
        @JsonProperty("total_stay")
        TOTAL_STAY,
        @JsonProperty("unknown")
        UNKNOWN
    }

    public enum YesNoUnknown {
        @JsonProperty("yes")
        YES,
        @JsonProperty("no")
        NO,
        @JsonProperty("unknown")
        UNKNOWN
    }

    public enum HotelAmenitySource {
        @JsonProperty("provider_fact")
        PROVIDER_FACT,
        @JsonProperty("assistant_assumption")
        ASSISTANT_ASSUMPTION,
        @JsonProperty("unknown")
        UNKNOWN
    }

    public enum ResultCompleteness {
        @JsonProperty("complete")
        COMPLETE,
        @JsonProperty("partial")
        PARTIAL,
        @JsonProperty("unknown")
        UNKNOWN
    }

    public enum ProviderState {
        @JsonProperty("available")
        AVAILABLE,
        @JsonProperty("degraded")
        DEGRADED,
        @JsonProperty("unavailable")
        UNAVAILABLE,
        @JsonProperty("failed")
        FAILED,
        @JsonProperty("unknown")
        UNKNOWN
    }

    public enum HotelSearchFailureCategory {
        @JsonProperty("provider_unavailable")
        PROVIDER_UNAVAILABLE,
        @JsonProperty("provider_failed")
        PROVIDER_FAILED,
        @JsonProperty("search_failed")
        SEARCH_FAILED,
        @JsonProperty("unknown")
        UNKNOWN
    }

    public enum PreferenceStrength {
        @JsonProperty("required")
        REQUIRED,
        @JsonProperty("preferred")
        PREFERRED,
        @JsonProperty("optional")
        OPTIONAL
    }

    public enum UserPreferenceSource {
        @JsonProperty("user_message")
        USER_MESSAGE,
        @JsonProperty("clarification")
        CLARIFICATION
    }

    public enum DerivedAssumptionCategory {
        @JsonProperty("room_count")
        ROOM_COUNT,
        @JsonProperty("guest_count")
        GUEST_COUNT,
        @JsonProperty("date_interpretation")
        DATE_INTERPRETATION,
        @JsonProperty("budget_tier")
        BUDGET_TIER,
        @JsonProperty("location_preference")
        LOCATION_PREFERENCE,
        @JsonProperty("other")
        OTHER
    }

    public enum ExplanationMode {
        @JsonProperty("explain")
        EXPLAIN,
        @JsonProperty("compare")
        COMPARE
    }
}

