package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.AccommodationAnalysisMetadata
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.RankedHotelOffer

sealed interface SemanticHotelSearchJobResult {
    data class Completed(
        val status: HotelSearch.Status,
        val offers: List<RankedHotelOffer>,
        val analysis: AccommodationAnalysisMetadata,
    ) : SemanticHotelSearchJobResult {
        init {
            require(status in TERMINAL_SEMANTIC_STATUSES) {
                "Semantic hotel search job must return a terminal semantic status"
            }
            require(analysis.status != AccommodationAnalysisMetadata.Status.SEARCHING) {
                "Terminal semantic hotel search analysis cannot remain searching"
            }
        }
    }

    data object Failed : SemanticHotelSearchJobResult

    companion object {
        private val TERMINAL_SEMANTIC_STATUSES = setOf(
            HotelSearch.Status.COMPLETED_WITH_OFFERS,
            HotelSearch.Status.COMPLETED_NO_OFFERS,
            HotelSearch.Status.COMPLETED_NO_SEMANTIC_MATCHES,
            HotelSearch.Status.FAILED,
        )
    }
}
