package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelSearch

fun interface SemanticHotelSearchLauncher {
    fun launch(
        search: HotelSearch,
        command: CreateHotelSearchCommand,
    ): Boolean

    companion object {
        val UNAVAILABLE = SemanticHotelSearchLauncher { _, _ -> false }
    }
}
