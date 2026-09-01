package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchId

interface HotelSearchStateStore {
    fun save(search: HotelSearch): HotelSearch

    fun findById(searchId: HotelSearchId): HotelSearch?

    fun updateIfStatus(
        searchId: HotelSearchId,
        expectedStatus: HotelSearch.Status,
        update: (HotelSearch) -> HotelSearch,
    ): HotelSearchStateTransitionResult
}
