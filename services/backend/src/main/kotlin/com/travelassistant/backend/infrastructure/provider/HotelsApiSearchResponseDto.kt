package com.travelassistant.backend.infrastructure.provider

import kotlinx.serialization.Serializable

@Serializable
internal data class HotelsApiSearchResponseDto(
    val payload: Payload,
) {
    @Serializable
    data class Payload(
        val filteredHotelsCount: Int,
        val hotels: List<Hotel>,
        val hotelsTotalCount: Int,
        val isLoadingCompleted: Boolean,
        val nextOffset: Int? = null,
        val hotelsMinPrice: Money? = null,
    )

    @Serializable
    data class Hotel(
        val hotelId: String,
        val hotelName: String,
        val starRating: Int,
        val areaLocation: AreaLocation,
        val hotelLocation: HotelLocation,
        val rateForHotelsFeed: Rate,
        val images: List<String>? = null,
        val review: Review? = null,
    )

    @Serializable
    data class AreaLocation(
        val countryName: String,
        val destinationId: Int,
        val destinationName: String,
        val signature: String,
    )

    @Serializable
    data class HotelLocation(
        val address: String,
        val coordinates: Coordinates? = null,
    )

    @Serializable
    data class Coordinates(
        val latitude: Double,
        val longitude: Double,
    )

    @Serializable
    data class Rate(
        val availableRoomsCount: Int,
        val isCreditCardDataRequired: Boolean,
        val paymentPlace: String,
        val shownPrice: Money,
        val freeCancellationUntil: String? = null,
        val mealName: String? = null,
        val mealType: String? = null,
    )

    @Serializable
    data class Money(
        val amount: Double,
        val currency: String,
    )

    @Serializable
    data class Review(
        val rating: Double,
        val ratingsCount: Int,
    )
}
