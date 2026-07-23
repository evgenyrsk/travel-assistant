package com.travelassistant.backend.infrastructure.provider

import kotlinx.serialization.Serializable

@Serializable
internal data class HotelsApiHotelDetailsResponseDto(
    val payload: Payload,
) {
    @Serializable
    data class Payload(
        val hotelId: String,
        val hotelName: String,
        val hotelChain: String? = null,
        val starRating: Int? = null,
        val hotelLocation: HotelLocation? = null,
        val description: List<DescriptionSection>? = null,
        val images: List<String>? = null,
        val facilitiesGroups: List<FacilitiesGroup>? = null,
        val checkInTime: String? = null,
        val checkOutTime: String? = null,
        val paymentMethods: List<String>? = null,
    )

    @Serializable
    data class HotelLocation(
        val address: String? = null,
        val coordinates: Coordinates? = null,
    )

    @Serializable
    data class Coordinates(
        val latitude: Double,
        val longitude: Double,
    )

    @Serializable
    data class DescriptionSection(
        val title: String? = null,
        val paragraphs: List<String> = emptyList(),
    )

    @Serializable
    data class FacilitiesGroup(
        val groupName: String? = null,
        val facilities: List<Facility> = emptyList(),
    )

    @Serializable
    data class Facility(
        val name: String? = null,
    )
}
