package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelDetailsProviderBoundary
import com.travelassistant.backend.application.hotel.HotelDetailsProviderResult
import com.travelassistant.backend.domain.hotel.HotelDetails
import java.time.LocalTime

internal class FakeHotelDetailsProvider : HotelDetailsProviderBoundary {

    override suspend fun load(providerReference: String): HotelDetailsProviderResult {
        if (providerReference.isBlank()) {
            return HotelDetailsProviderResult.ResponseRejected(
                HotelDetailsProviderResult.ResponseRejectionReason.INVALID_PROVIDER_REFERENCE,
            )
        }
        if (!providerReference.startsWith(FAKE_REFERENCE_PREFIX)) {
            return HotelDetailsProviderResult.NotFound
        }

        val hotelName = when {
            providerReference.endsWith("-001") -> "Demo Central Hotel"
            providerReference.endsWith("-002") -> "Demo Riverside Stay"
            else -> "Demo Hotel"
        }

        return HotelDetailsProviderResult.Loaded(
            HotelDetails(
                hotelName = hotelName,
                starRating = 4,
                location = HotelDetails.Location(address = "Demo address"),
                descriptionSections = listOf(
                    HotelDetails.DescriptionSection(
                        title = "About the hotel",
                        paragraphs = listOf("Deterministic local hotel details."),
                    ),
                ),
                amenityGroups = listOf(
                    HotelDetails.AmenityGroup(
                        name = "Main amenities",
                        amenities = listOf("Wi-Fi", "Breakfast"),
                    ),
                ),
                checkInTime = LocalTime.of(15, 0),
                checkOutTime = LocalTime.of(12, 0),
                paymentMethods = listOf(
                    HotelDetails.PaymentMethod.CASH,
                    HotelDetails.PaymentMethod.CARD,
                ),
            ),
        )
    }

    private companion object {
        const val FAKE_REFERENCE_PREFIX = "local-fake-"
    }
}
