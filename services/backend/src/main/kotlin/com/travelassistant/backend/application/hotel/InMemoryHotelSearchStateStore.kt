package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchId
import java.util.concurrent.ConcurrentHashMap

class InMemoryHotelSearchStateStore : HotelSearchStateStore {
    private val searches = ConcurrentHashMap<HotelSearchId, HotelSearch>()

    override fun save(search: HotelSearch): HotelSearch {
        searches[search.id] = search
        return search
    }

    override fun findById(searchId: HotelSearchId): HotelSearch? = searches[searchId]

    override fun updateIfStatus(
        searchId: HotelSearchId,
        expectedStatus: HotelSearch.Status,
        update: (HotelSearch) -> HotelSearch,
    ): HotelSearchStateTransitionResult {
        var result: HotelSearchStateTransitionResult = HotelSearchStateTransitionResult.NotFound
        searches.compute(searchId) { _, current ->
            when {
                current == null -> null
                current.status != expectedStatus -> {
                    result = HotelSearchStateTransitionResult.UnexpectedStatus(current)
                    current
                }

                else -> update(current).also { updated ->
                    result = HotelSearchStateTransitionResult.Updated(updated)
                }
            }
        }
        return result
    }
}
