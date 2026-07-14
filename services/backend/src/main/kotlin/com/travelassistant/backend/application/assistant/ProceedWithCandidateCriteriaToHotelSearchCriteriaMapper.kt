package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchCriteria

class ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper {

    operator fun invoke(criteria: ProceedWithCandidateCriteria): HotelSearchCriteria =
        HotelSearchCriteria(
            destination = criteria.destination,
            checkInDate = criteria.checkInDate,
            checkOutDate = criteria.checkOutDate,
            guests = HotelSearchCriteria.Guests(
                adults = criteria.guests.adults,
                childrenAges = criteria.guests.childrenAges,
            ),
            rooms = criteria.rooms,
        )
}
