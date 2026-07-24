package com.travelassistant.backend.api

import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class HotelSearchCriteriaResponse(
    val destination: String,
    val checkInDate: String,
    val checkOutDate: String,
    val guests: Guests,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val rooms: Int? = null,
) {
    @Serializable
    data class Guests(
        val adults: Int,
        val children: Int,
        val childrenAges: List<Int>,
    )

    companion object {
        fun from(criteria: HotelSearchCriteria): HotelSearchCriteriaResponse =
            HotelSearchCriteriaResponse(
                destination = criteria.destination,
                checkInDate = criteria.checkInDate.toString(),
                checkOutDate = criteria.checkOutDate.toString(),
                guests = Guests(
                    adults = criteria.guests.adults,
                    children = criteria.guests.children,
                    childrenAges = criteria.guests.childrenAges,
                ),
                rooms = criteria.rooms,
            )
    }
}
