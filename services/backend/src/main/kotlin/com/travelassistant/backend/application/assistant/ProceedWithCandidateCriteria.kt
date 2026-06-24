package com.travelassistant.backend.application.assistant

import java.time.LocalDate

data class ProceedWithCandidateCriteria(
    val destination: String,
    val checkInDate: LocalDate,
    val checkOutDate: LocalDate,
    val guests: Guests,
    val rooms: Int,
) {
    data class Guests(
        val adults: Int,
        val children: Int,
    )
}
