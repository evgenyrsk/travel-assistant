package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelLocationResolution

internal object HotelsApiAutocompleteLocationMapper {
    fun map(response: HotelsApiAutocompleteResponseDto): HotelLocationResolution =
        HotelLocationResolution(
            candidates = response.payload.locations.orEmpty().map { location ->
                HotelLocationResolution.Candidate(
                    destinationId = location.id,
                    name = location.name,
                    signature = location.signature,
                    type = HotelLocationResolution.Type(
                        code = location.type.code,
                        name = location.type.name,
                    ),
                )
            },
        )
}
