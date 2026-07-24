package com.travelassistant.backend.application.hotel

internal data class HotelLocationResolution(
    val candidates: List<Candidate>,
    val hotelCandidates: List<HotelCandidate> = emptyList(),
) {
    data class Candidate(
        val destinationId: Int,
        val name: String,
        val signature: String,
        val type: Type,
    )

    data class Type(
        val code: String,
        val name: String,
    )

    data class HotelCandidate(
        val providerReference: String,
        val name: String,
        val signature: String,
        val type: Type,
    )
}
