package com.travelassistant.backend.application.hotel

internal fun interface HotelCandidateSelectionPolicy {
    fun select(
        query: String,
        candidates: List<HotelLocationResolution.HotelCandidate>,
        hasLocationCandidates: Boolean,
    ): HotelCandidateSelectionResult
}
