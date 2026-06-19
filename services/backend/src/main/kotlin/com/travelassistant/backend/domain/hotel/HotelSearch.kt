package com.travelassistant.backend.domain.hotel

import com.travelassistant.backend.domain.assistant.AssistantSessionId

data class HotelSearch(
    val id: HotelSearchId,
    val sessionId: AssistantSessionId,
    val criteria: HotelSearchCriteria,
    val status: Status,
    val offers: List<RankedHotelOffer>,
) {
    enum class Status(val apiValue: String) {
        COMPLETED_WITH_OFFERS("completed_with_offers"),
        COMPLETED_NO_OFFERS("completed_no_offers"),
    }
}
