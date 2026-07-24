package com.travelassistant.backend.domain.hotel

import com.travelassistant.backend.domain.assistant.AssistantSessionId

data class HotelSearch(
    val id: HotelSearchId,
    val sessionId: AssistantSessionId,
    val criteria: HotelSearchCriteria,
    val status: Status,
    val offers: List<RankedHotelOffer>,
    val analysis: AccommodationAnalysisMetadata? = null,
) {
    enum class Status(val apiValue: String) {
        SEARCHING("searching"),
        COMPLETED_WITH_OFFERS("completed_with_offers"),
        COMPLETED_NO_OFFERS("completed_no_offers"),
        COMPLETED_NO_SEMANTIC_MATCHES("completed_no_semantic_matches"),
        FAILED("failed"),
    }
}
