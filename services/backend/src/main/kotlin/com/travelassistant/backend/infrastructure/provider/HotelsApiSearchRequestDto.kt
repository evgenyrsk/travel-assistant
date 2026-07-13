package com.travelassistant.backend.infrastructure.provider

import kotlinx.serialization.Serializable

@Serializable
internal data class HotelsApiSearchRequestDto(
    val destinationId: Int,
    val checkinDate: String,
    val checkoutDate: String,
    val guests: List<Guest>,
    val offset: Int? = null,
    val limit: Int? = null,
) {
    @Serializable
    data class Guest(
        val adultsCount: Int,
        val childrenAge: List<Int>? = null,
    )
}
