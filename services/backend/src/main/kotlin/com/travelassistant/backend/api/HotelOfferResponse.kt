package com.travelassistant.backend.api

import com.travelassistant.backend.domain.hotel.RankedHotelOffer
import kotlinx.serialization.Serializable

@Serializable
data class HotelOfferResponse(
    val offerId: String,
    val providerOfferRef: String,
    val hotelName: String,
    val location: Location,
    val price: Price,
    val rating: Rating,
    val amenities: List<Amenity>,
    val availability: String,
    val source: String,
    val freshness: String,
    val matchSummary: String,
    val providerFacts: List<ProviderFact>,
) {
    @Serializable
    data class Location(
        val city: String,
        val country: String,
    )

    @Serializable
    data class Price(
        val amount: Double,
        val currency: String,
        val basis: String,
        val includesTaxesAndFees: String,
        val providerFreshness: String,
    )

    @Serializable
    data class Rating(
        val value: Double,
        val scale: Double,
        val reviewCount: Int,
        val source: String,
    )

    @Serializable
    data class Amenity(
        val name: String,
        val source: String,
    )

    @Serializable
    data class ProviderFact(
        val field: String,
        val value: String,
        val source: String,
        val freshness: String,
    )

    companion object {
        fun from(rankedOffer: RankedHotelOffer): HotelOfferResponse {
            val offer = rankedOffer.offer

            return HotelOfferResponse(
                offerId = offer.id,
                providerOfferRef = offer.providerReference,
                hotelName = offer.hotelName,
                location = Location(
                    city = offer.city,
                    country = offer.country,
                ),
                price = Price(
                    amount = offer.totalPrice,
                    currency = offer.currency,
                    basis = "total_stay",
                    includesTaxesAndFees = "unknown",
                    providerFreshness = offer.freshness.apiValue,
                ),
                rating = Rating(
                    value = offer.rating,
                    scale = 10.0,
                    reviewCount = offer.reviewCount,
                    source = offer.source,
                ),
                amenities = offer.amenities.map {
                    Amenity(
                        name = it,
                        source = "provider_fact",
                    )
                },
                availability = offer.availability.apiValue,
                source = offer.source,
                freshness = offer.freshness.apiValue,
                matchSummary = rankedOffer.matchSummary,
                providerFacts = listOf(
                    ProviderFact(
                        field = "availability",
                        value = offer.availability.apiValue,
                        source = offer.source,
                        freshness = offer.freshness.apiValue,
                    ),
                ),
            )
        }
    }
}
