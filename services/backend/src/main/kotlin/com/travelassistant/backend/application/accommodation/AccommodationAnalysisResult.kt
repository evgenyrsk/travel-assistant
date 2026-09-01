package com.travelassistant.backend.application.accommodation

import com.travelassistant.backend.domain.hotel.AccommodationEvidenceSource
import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict

sealed interface AccommodationAnalysisResult {
    data class Completed(
        val decisions: List<Decision>,
    ) : AccommodationAnalysisResult

    data class Failed(
        val reason: FailureReason,
    ) : AccommodationAnalysisResult

    data class Decision(
        val ephemeralCandidateId: String,
        val verdict: AccommodationMatchVerdict,
        val evidence: Set<Evidence>,
    )

    data class Evidence(
        val source: AccommodationEvidenceSource,
        val signal: Signal,
    )

    enum class Signal(
        val positive: Boolean,
    ) {
        EXPLICIT_GLAMPING_LABEL(true),
        GLAMPING_STRUCTURE(true),
        NATURE_SETTING(true),
        GLAMPING_AMENITY(true),
        IMAGE_GLAMPING_STRUCTURE(true),
        STANDARD_HOTEL_FORMAT(false),
        APARTMENT_BLOCK_FORMAT(false),
        EMPTY_CAMPING_PITCH(false),
        ORDINARY_COTTAGE(false),
    }

    enum class FailureReason {
        TIMEOUT,
        RATE_LIMITED,
        UNAVAILABLE,
        AUTHENTICATION_FAILED,
        REQUEST_REJECTED,
        INVALID_RESPONSE,
    }
}
