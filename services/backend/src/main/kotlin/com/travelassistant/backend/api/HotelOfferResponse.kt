package com.travelassistant.backend.api

import com.travelassistant.backend.domain.hotel.RankedHotelOffer
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class HotelOfferResponse(
    val offerId: String,
    val hotelName: String,
    val location: Location,
    val price: Price,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val rating: Rating? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val amenities: List<Amenity>? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val starRating: Int? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val freeCancellationUntil: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val imageUrl: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val breakfastIncluded: Boolean? = null,
    val availability: String,
    val source: String,
    val freshness: String,
    val matchSummary: String,
    val providerFacts: List<ProviderFact>,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val semanticMatch: SemanticMatch? = null,
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

    @Serializable
    data class SemanticMatch(
        val concept: String,
        val verdict: String,
        val evidenceSources: List<String>,
    )

    companion object {
        fun from(rankedOffer: RankedHotelOffer): HotelOfferResponse {
            val offer = rankedOffer.offer

            return HotelOfferResponse(
                offerId = offer.id,
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
                rating = offer.rating?.let { rating ->
                    offer.reviewCount?.let { reviewCount ->
                        Rating(
                            value = rating,
                            scale = 10.0,
                            reviewCount = reviewCount,
                            source = offer.source,
                        )
                    }
                },
                amenities = offer.amenities?.map {
                    Amenity(
                        name = it,
                        source = "provider_fact",
                    )
                },
                starRating = offer.starRating,
                freeCancellationUntil = offer.freeCancellationUntil?.toString(),
                imageUrl = offer.imageUrl,
                breakfastIncluded = offer.breakfastIncluded,
                availability = offer.availability.apiValue,
                source = offer.source,
                freshness = offer.freshness.apiValue,
                matchSummary = rankedOffer.matchSummary,
                providerFacts = buildList {
                    add(
                        ProviderFact(
                            field = "availability",
                            value = offer.availability.apiValue,
                            source = offer.source,
                            freshness = offer.freshness.apiValue,
                        ),
                    )
                    offer.breakfastIncluded?.let { value ->
                        add(
                            ProviderFact(
                                field = "breakfastIncluded",
                                value = value.toString(),
                                source = offer.source,
                                freshness = offer.freshness.apiValue,
                            ),
                        )
                    }
                },
                semanticMatch = rankedOffer.semanticMatch?.let { semanticMatch ->
                    SemanticMatch(
                        concept = semanticMatch.concept.code,
                        verdict = semanticMatch.verdict.apiValue,
                        evidenceSources = semanticMatch.evidenceSources
                            .sortedBy { evidence -> evidence.ordinal }
                            .map { evidence -> evidence.apiValue },
                    )
                },
            )
        }
    }
}
