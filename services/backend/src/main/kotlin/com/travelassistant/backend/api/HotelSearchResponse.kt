package com.travelassistant.backend.api

import com.travelassistant.backend.domain.hotel.HotelSearch
import kotlinx.serialization.Serializable

@Serializable
data class HotelSearchResponse(
    val searchId: String,
    val sessionId: String,
    val status: String,
    val criteria: HotelSearchCriteriaResponse,
    val metadata: Metadata,
) {
    @Serializable
    data class Metadata(
        val resultCompleteness: String,
        val freshness: String,
        val providerState: String,
        val warnings: List<String>,
    )

    companion object {
        fun from(search: HotelSearch): HotelSearchResponse =
            HotelSearchResponse(
                searchId = search.id.value,
                sessionId = search.sessionId.value,
                status = search.status.apiValue,
                criteria = HotelSearchCriteriaResponse.from(search.criteria),
                metadata = Metadata(
                    resultCompleteness = "complete",
                    freshness = "fresh",
                    providerState = "available",
                    warnings = listOf(
                        "Results are deterministic local fake-provider data.",
                    ),
                ),
            )
    }
}
