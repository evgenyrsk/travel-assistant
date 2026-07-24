package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.domain.hotel.HotelSearchCriteria

internal object HotelsApiHotelRatesRequestMapper {
    fun map(criteria: HotelSearchCriteria): Result {
        if (!criteria.checkOutDate.isAfter(criteria.checkInDate)) {
            return rejected(HotelsApiSearchMappingError.Issue.INVALID_DATE_RANGE)
        }
        if (criteria.rooms != SUPPORTED_ROOM_COUNT) {
            return rejected(HotelsApiSearchMappingError.Issue.INVALID_ROOM_COUNT)
        }
        if (criteria.guests.adults < 1) {
            return rejected(HotelsApiSearchMappingError.Issue.INVALID_ADULTS_COUNT)
        }
        if (criteria.guests.childrenAges.any { age -> age !in MIN_CHILD_AGE..MAX_CHILD_AGE }) {
            return rejected(HotelsApiSearchMappingError.Issue.INVALID_CHILD_AGE)
        }
        when (val preferences = HotelsApiSearchFilterMapper.map(criteria.preferences)) {
            is HotelsApiSearchFilterMapper.Result.Rejected ->
                return Result.Rejected(preferences.error)
            is HotelsApiSearchFilterMapper.Result.Mapped -> Unit
        }

        return Result.Mapped(
            HotelsApiHotelRatesRequestDto(
                checkinDate = criteria.checkInDate.toString(),
                checkoutDate = criteria.checkOutDate.toString(),
                guests = listOf(
                    HotelsApiHotelRatesRequestDto.Guest(
                        adultsCount = criteria.guests.adults,
                        childrenAge = criteria.guests.childrenAges,
                    ),
                ),
                filters = emptyList(),
            ),
        )
    }

    sealed interface Result {
        data class Mapped(val request: HotelsApiHotelRatesRequestDto) : Result

        data class Rejected(val error: HotelsApiSearchMappingError) : Result
    }

    private fun rejected(issue: HotelsApiSearchMappingError.Issue): Result.Rejected =
        Result.Rejected(HotelsApiSearchMappingError(issue))

    private const val SUPPORTED_ROOM_COUNT = 1
    private const val MIN_CHILD_AGE = 0
    private const val MAX_CHILD_AGE = 17
}
