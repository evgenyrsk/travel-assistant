package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelSearchId

fun interface SemanticHotelSearchJob {
    suspend fun execute(
        searchId: HotelSearchId,
        command: CreateHotelSearchCommand,
    ): SemanticHotelSearchJobResult
}
