package com.travelassistant.backend.domain.hotel

import java.time.Instant

data class HotelOffer(
    val id: String,
    val providerReference: String,
    val hotelName: String,
    val city: String,
    val country: String,
    val totalPrice: Double,
    val currency: String,
    val rating: Double?,
    val reviewCount: Int?,
    val amenities: List<String>?,
    val availability: Availability,
    val source: String,
    val freshness: Freshness,
    val starRating: Int? = null,
    val freeCancellationUntil: Instant? = null,
    val imageUrl: String? = null,
) {
    enum class Availability(val apiValue: String) {
        AVAILABLE("available"),
        LIMITED("limited"),
        UNKNOWN("unknown"),
    }

    enum class Freshness(val apiValue: String) {
        FRESH("fresh"),
        STALE("stale"),
        UNKNOWN("unknown"),
    }
}
