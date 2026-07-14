package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelLocationResolution
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria

internal object HotelsApiSearchRequestMapper {

    fun map(
        location: HotelLocationResolution.Candidate,
        criteria: HotelSearchCriteria,
    ): Result {
        if (location.destinationId <= 0) {
            return Result.Rejected(error(HotelsApiSearchMappingError.Issue.INVALID_DESTINATION_ID))
        }
        if (!criteria.checkOutDate.isAfter(criteria.checkInDate)) {
            return Result.Rejected(error(HotelsApiSearchMappingError.Issue.INVALID_DATE_RANGE))
        }
        if (criteria.rooms != SUPPORTED_ROOM_COUNT) {
            return Result.Rejected(error(HotelsApiSearchMappingError.Issue.INVALID_ROOM_COUNT))
        }
        if (criteria.guests.adults < 1) {
            return Result.Rejected(error(HotelsApiSearchMappingError.Issue.INVALID_ADULTS_COUNT))
        }
        if (criteria.guests.childrenAges.any { it !in MIN_CHILD_AGE..MAX_CHILD_AGE }) {
            return Result.Rejected(error(HotelsApiSearchMappingError.Issue.INVALID_CHILD_AGE))
        }

        return Result.Mapped(
            request = HotelsApiSearchRequestDto(
                destinationId = location.destinationId,
                checkinDate = criteria.checkInDate.toString(),
                checkoutDate = criteria.checkOutDate.toString(),
                guests = listOf(
                    HotelsApiSearchRequestDto.Guest(
                        adultsCount = criteria.guests.adults,
                        childrenAge = criteria.guests.childrenAges,
                    ),
                ),
                offset = null,
                limit = null,
            ),
        )
    }

    sealed interface Result {
        data class Mapped(val request: HotelsApiSearchRequestDto) : Result

        data class Rejected(val error: HotelsApiSearchMappingError) : Result
    }

    private fun error(issue: HotelsApiSearchMappingError.Issue): HotelsApiSearchMappingError =
        HotelsApiSearchMappingError(issue = issue)

    private const val SUPPORTED_ROOM_COUNT = 1
    private const val MIN_CHILD_AGE = 0
    private const val MAX_CHILD_AGE = 17
}
