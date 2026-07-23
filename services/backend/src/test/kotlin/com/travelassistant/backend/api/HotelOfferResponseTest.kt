package com.travelassistant.backend.api

import com.travelassistant.backend.domain.hotel.HotelOffer
import com.travelassistant.backend.domain.hotel.RankedHotelOffer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HotelOfferResponseTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun omitsUnknownOptionalProviderFactsFromJson() {
        val response = HotelOfferResponse.from(
            rankedOffer(
                rating = null,
                reviewCount = null,
                amenities = null,
                starRating = null,
                freeCancellationUntil = null,
            ),
        )

        val encoded = json.encodeToString(response)

        assertFalse(encoded.contains("\"rating\""))
        assertFalse(encoded.contains("\"amenities\""))
        assertFalse(encoded.contains("\"starRating\""))
        assertFalse(encoded.contains("\"freeCancellationUntil\""))
        assertFalse(encoded.contains("providerOfferRef"))
        assertFalse(encoded.contains("provider-1"))
    }

    @Test
    fun keepsKnownOptionalProviderFactsInJson() {
        val cancellationDeadline = Instant.parse("2026-08-09T18:00:00Z")
        val response = HotelOfferResponse.from(
            rankedOffer(
                rating = 8.7,
                reviewCount = 42,
                amenities = listOf("Wi-Fi"),
                starRating = 4,
                freeCancellationUntil = cancellationDeadline,
            ),
        )

        val encoded = json.encodeToString(response)

        assertTrue(encoded.contains("\"rating\""))
        assertTrue(encoded.contains("\"reviewCount\":42"))
        assertTrue(encoded.contains("\"amenities\""))
        assertTrue(encoded.contains("\"starRating\":4"))
        assertTrue(encoded.contains("\"freeCancellationUntil\":\"$cancellationDeadline\""))
    }

    private fun rankedOffer(
        rating: Double?,
        reviewCount: Int?,
        amenities: List<String>?,
        starRating: Int?,
        freeCancellationUntil: Instant?,
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
                starRating = starRating,
                freeCancellationUntil = freeCancellationUntil,
            ),
            matchSummary = "Test summary",
        )
}
