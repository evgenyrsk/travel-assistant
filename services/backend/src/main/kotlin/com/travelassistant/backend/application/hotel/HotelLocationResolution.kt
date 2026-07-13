package com.travelassistant.backend.application.hotel

internal data class HotelLocationResolution(
    val candidates: List<Candidate>,
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
}
