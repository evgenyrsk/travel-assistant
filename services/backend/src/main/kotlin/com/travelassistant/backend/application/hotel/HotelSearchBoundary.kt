package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchId

interface HotelSearchBoundary {
    suspend fun createSearch(command: CreateHotelSearchCommand): CreateHotelSearchResult

    fun getSearch(searchId: HotelSearchId): HotelSearch
}
