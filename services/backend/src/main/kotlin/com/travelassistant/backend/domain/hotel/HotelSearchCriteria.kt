package com.travelassistant.backend.domain.hotel

import java.time.LocalDate

data class HotelSearchCriteria(
    val destination: String,
    val checkInDate: LocalDate,
    val checkOutDate: LocalDate,
    val guests: Guests,
    val rooms: Int?,
) {
    data class Guests(
        val adults: Int,
        val childrenAges: List<Int> = emptyList(),
    ) {
        val children: Int
            get() = childrenAges.size
    }
}
