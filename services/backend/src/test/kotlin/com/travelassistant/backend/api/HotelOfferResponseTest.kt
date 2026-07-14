package com.travelassistant.backend.api

import com.travelassistant.backend.domain.hotel.HotelOffer
import com.travelassistant.backend.domain.hotel.RankedHotelOffer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HotelOfferResponseTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun omitsUnknownRatingAndAmenitiesFromJson() {
        val response = HotelOfferResponse.from(
            rankedOffer(
                rating = null,
                reviewCount = null,
                amenities = null,
            ),
        )

        val encoded = json.encodeToString(response)

        assertFalse(encoded.contains("\"rating\""))
        assertFalse(encoded.contains("\"amenities\""))
    }

    @Test
    fun keepsKnownRatingAndAmenitiesInJson() {
        val response = HotelOfferResponse.from(
            rankedOffer(
                rating = 8.7,
                reviewCount = 42,
                amenities = listOf("Wi-Fi"),
            ),
        )

        val encoded = json.encodeToString(response)

        assertTrue(encoded.contains("\"rating\""))
        assertTrue(encoded.contains("\"reviewCount\":42"))
        assertTrue(encoded.contains("\"amenities\""))
    }

    private fun rankedOffer(
        rating: Double?,
        reviewCount: Int?,
        amenities: List<String>?,
    ): RankedHotelOffer =
        RankedHotelOffer(
            offer = HotelOffer(
                id = "offer-1",
                providerReference = "provider-1",
                hotelName = "Test Hotel",
                city = "Kazan",
                country = "Russia",
                totalPrice = 12_000.0,
                currency = "RUB",
                rating = rating,
                reviewCount = reviewCount,
                amenities = amenities,
                availability = HotelOffer.Availability.AVAILABLE,
                source = "test",
                freshness = HotelOffer.Freshness.UNKNOWN,
            ),
            matchSummary = "Test summary",
        )
}
