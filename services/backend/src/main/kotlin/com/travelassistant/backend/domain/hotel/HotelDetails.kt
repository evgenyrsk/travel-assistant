package com.travelassistant.backend.domain.hotel

import java.time.LocalTime

data class HotelDetails(
    val hotelName: String,
    val hotelChain: String? = null,
    val starRating: Int? = null,
    val location: Location? = null,
    val descriptionSections: List<DescriptionSection>? = null,
    val imageUrls: List<String>? = null,
    val amenityGroups: List<AmenityGroup>? = null,
    val checkInTime: LocalTime? = null,
    val checkOutTime: LocalTime? = null,
    val paymentMethods: List<PaymentMethod>? = null,
    val source: Source = Source.PROVIDER,
    val freshness: Freshness = Freshness.UNKNOWN,
) {
    data class Location(
        val address: String? = null,
        val coordinates: Coordinates? = null,
    )

    data class Coordinates(
        val latitude: Double,
        val longitude: Double,
    )

    data class DescriptionSection(
        val title: String? = null,
        val paragraphs: List<String>,
    )

    data class AmenityGroup(
        val name: String? = null,
        val amenities: List<String>,
    )

    enum class PaymentMethod(val apiValue: String) {
        CASH("cash"),
        CARD("card"),
    }

    enum class Source(val apiValue: String) {
        PROVIDER("provider"),
    }

    enum class Freshness(val apiValue: String) {
        UNKNOWN("unknown"),
    }
}
