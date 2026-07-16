package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelOfferProviderBoundary
import com.travelassistant.backend.application.hotel.HotelOfferProviderResult
import com.travelassistant.backend.domain.hotel.HotelOffer
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria

/**
 * Deterministic local provider for Stage 7 development and tests.
 *
 * This adapter performs no external I/O and must not be treated as a real
 * travel provider integration.
 */
class FakeHotelOfferProvider : HotelOfferProviderBoundary {

    override suspend fun search(criteria: HotelSearchCriteria): HotelOfferProviderResult {
        val destination = criteria.destination.trim()
        val slug = destination
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "destination" }

        return HotelOfferProviderResult.SearchCompleted(
            listOf(
                HotelOffer(
                    id = "fake-offer-$slug-002",
                    providerReference = "local-fake-$slug-002",
                    hotelName = "$destination Riverside Stay",
                    city = destination,
                    country = countryFor(destination),
                    totalPrice = 510.0,
                    currency = "EUR",
                    rating = 9.0,
                    reviewCount = 860,
                    amenities = listOf("Wi-Fi", "Gym"),
                    availability = HotelOffer.Availability.LIMITED,
                    source = SOURCE,
                    freshness = HotelOffer.Freshness.FRESH,
                ),
                HotelOffer(
                    id = "fake-offer-$slug-001",
                    providerReference = "local-fake-$slug-001",
                    hotelName = "$destination Central Hotel",
                    city = destination,
                    country = countryFor(destination),
                    totalPrice = 420.0,
                    currency = "EUR",
                    rating = 8.6,
                    reviewCount = 1240,
                    amenities = listOf("Wi-Fi", "Breakfast"),
                    availability = HotelOffer.Availability.AVAILABLE,
                    source = SOURCE,
                    freshness = HotelOffer.Freshness.FRESH,
                ),
            ),
        )
    }

    private fun countryFor(destination: String): String =
        when (destination.lowercase()) {
            "rome" -> "Italy"
            "paris" -> "France"
            "berlin" -> "Germany"
            else -> "Demo"
        }

    private companion object {
        const val SOURCE = "local_fake_provider"
    }
}
