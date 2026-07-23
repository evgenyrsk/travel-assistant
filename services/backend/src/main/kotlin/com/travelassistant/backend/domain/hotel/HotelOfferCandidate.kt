package com.travelassistant.backend.domain.hotel

import java.time.Instant

data class HotelOfferCandidate(
    val providerReference: String,
    val hotelName: String,
    val city: String,
    val country: String,
    val totalPrice: Double,
    val currency: String,
    val rating: Double?,
    val reviewCount: Int?,
    val amenities: List<String>?,
    val availability: HotelOffer.Availability,
    val source: String,
    val freshness: HotelOffer.Freshness,
    val starRating: Int? = null,
    val freeCancellationUntil: Instant? = null,
    val imageUrl: String? = null,
    val breakfastIncluded: Boolean? = null,
) {
    fun identifiedBy(id: String): HotelOffer =
        HotelOffer(
            id = id,
            providerReference = providerReference,
            hotelName = hotelName,
            city = city,
            country = country,
            totalPrice = totalPrice,
            currency = currency,
            rating = rating,
            reviewCount = reviewCount,
            amenities = amenities,
            availability = availability,
            source = source,
            freshness = freshness,
            starRating = starRating,
            freeCancellationUntil = freeCancellationUntil,
            imageUrl = imageUrl,
            breakfastIncluded = breakfastIncluded,
        )
}
