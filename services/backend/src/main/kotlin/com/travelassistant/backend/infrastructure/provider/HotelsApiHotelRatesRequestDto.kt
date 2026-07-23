package com.travelassistant.backend.infrastructure.provider

import kotlinx.serialization.Serializable

@Serializable
internal data class HotelsApiHotelRatesRequestDto(
    val checkinDate: String,
    val checkoutDate: String,
    val guests: List<Guest>,
    val filters: List<HotelsApiSearchFilterDto>,
) {
    @Serializable
    data class Guest(
        val adultsCount: Int,
        val childrenAge: List<Int> = emptyList(),
    )
}
