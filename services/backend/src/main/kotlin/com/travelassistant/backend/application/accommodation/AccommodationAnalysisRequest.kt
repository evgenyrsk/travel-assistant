package com.travelassistant.backend.application.accommodation

import com.travelassistant.backend.domain.hotel.AccommodationConcept

data class AccommodationAnalysisRequest(
    val concept: AccommodationConcept,
    val candidates: List<Candidate>,
) {
    data class Candidate(
        val ephemeralCandidateId: String,
        val hotelName: String,
        val descriptions: List<String> = emptyList(),
        val amenities: List<String> = emptyList(),
        val imageUrls: List<String> = emptyList(),
    )
}
