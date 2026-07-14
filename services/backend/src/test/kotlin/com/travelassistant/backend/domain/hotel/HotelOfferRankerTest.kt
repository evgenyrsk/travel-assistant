package com.travelassistant.backend.domain.hotel

import kotlin.test.Test
import kotlin.test.assertEquals

class HotelOfferRankerTest {

    @Test
    fun ranksByAvailabilityThenRatingThenPriceThenOfferId() {
        val offers = listOf(
            offer(
                id = "limited-high-rating",
                availability = HotelOffer.Availability.LIMITED,
                rating = 9.8,
                totalPrice = 300.0,
            ),
            offer(
                id = "available-low-rating",
                availability = HotelOffer.Availability.AVAILABLE,
                rating = 8.0,
                totalPrice = 250.0,
            ),
            offer(
                id = "available-high-expensive",
                availability = HotelOffer.Availability.AVAILABLE,
                rating = 9.0,
                totalPrice = 500.0,
            ),
            offer(
                id = "available-high-cheap-b",
                availability = HotelOffer.Availability.AVAILABLE,
                rating = 9.0,
                totalPrice = 400.0,
            ),
            offer(
                id = "available-high-cheap-a",
                availability = HotelOffer.Availability.AVAILABLE,
                rating = 9.0,
                totalPrice = 400.0,
            ),
        )

        val ranked = HotelOfferRanker().rank(offers)

        assertEquals(
            listOf(
                "available-high-cheap-a",
                "available-high-cheap-b",
                "available-high-expensive",
                "available-low-rating",
                "limited-high-rating",
            ),
            ranked.map { it.offer.id },
        )
        assertEquals(
            "Available; ranked by rating, total stay price, then offer ID.",
            ranked.first().matchSummary,
        )
        assertEquals(
            "Limited availability; ranked after available offers, then by rating and total stay price.",
            ranked.last().matchSummary,
        )
    }

    @Test
    fun ranksKnownRatingBeforeUnknownRatingWithinSameAvailability() {
        val ranked = HotelOfferRanker().rank(
            listOf(
                offer(
                    id = "unknown-cheaper",
                    availability = HotelOffer.Availability.AVAILABLE,
                    rating = null,
                    totalPrice = 100.0,
                ),
                offer(
                    id = "known-expensive",
                    availability = HotelOffer.Availability.AVAILABLE,
                    rating = 7.5,
                    totalPrice = 200.0,
                ),
            ),
        )

        assertEquals(
            listOf("known-expensive", "unknown-cheaper"),
            ranked.map { it.offer.id },
        )
        assertEquals(
            "Available; rating unavailable, ranked by total stay price, then offer ID.",
            ranked.last().matchSummary,
        )
    }

    private fun offer(
        id: String,
        availability: HotelOffer.Availability,
        rating: Double?,
        totalPrice: Double,
    ): HotelOffer =
        HotelOffer(
            id = id,
            providerReference = "provider-$id",
            hotelName = id,
            city = "Rome",
            country = "Italy",
            totalPrice = totalPrice,
            currency = "EUR",
            rating = rating,
            reviewCount = 1,
            amenities = emptyList(),
            availability = availability,
            source = "test",
            freshness = HotelOffer.Freshness.FRESH,
        )
}
