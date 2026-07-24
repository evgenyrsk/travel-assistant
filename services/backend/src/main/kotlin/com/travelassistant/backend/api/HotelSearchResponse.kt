package com.travelassistant.backend.api

import com.travelassistant.backend.domain.hotel.AccommodationAnalysisMetadata
import com.travelassistant.backend.domain.hotel.HotelSearch
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
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
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val analysis: AccommodationAnalysisResponse? = null,
    )

    companion object {
        fun from(search: HotelSearch): HotelSearchResponse =
            HotelSearchResponse(
                searchId = search.id.value,
                sessionId = search.sessionId.value,
                status = search.status.apiValue,
                criteria = HotelSearchCriteriaResponse.from(search.criteria),
                metadata = Metadata(
                    resultCompleteness = if (
                        search.analysis?.status == AccommodationAnalysisMetadata.Status.PARTIAL
                    ) {
                        "partial"
                    } else {
                        "complete"
                    },
                    freshness = "fresh",
                    providerState = if (search.status == HotelSearch.Status.FAILED) {
                        "failed"
                    } else {
                        "available"
                    },
                    warnings = emptyList(),
                    analysis = search.analysis?.let(AccommodationAnalysisResponse::from),
                ),
            )
    }
}
