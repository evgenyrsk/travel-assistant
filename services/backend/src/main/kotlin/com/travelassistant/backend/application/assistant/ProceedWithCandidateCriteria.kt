package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import java.time.LocalDate

data class ProceedWithCandidateCriteria(
    val destination: String,
    val checkInDate: LocalDate,
    val checkOutDate: LocalDate,
    val guests: Guests,
    val rooms: Int,
    val preferences: HotelSearchPreferences = HotelSearchPreferences(),
) {
    data class Guests(
        val adults: Int,
        val childrenAges: List<Int> = emptyList(),
    ) {
        val children: Int
            get() = childrenAges.size
    }
}
