package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelLocationResolution
import com.travelassistant.backend.application.hotel.HotelLocationResolutionRequest
import com.travelassistant.backend.application.hotel.HotelOfferProviderResult
import com.travelassistant.backend.domain.hotel.HotelOffer
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class RealHotelOfferProviderAdapterTest {

    @Test
    fun `maps successful orchestration and configured language`() = runBlocking {
        var capturedRequest: HotelsApiSearchOrchestrator.Request? = null
        val offer = offer()
        val adapter = adapter(
            language = HotelLocationResolutionRequest.Language.RU,
        ) { request ->
            capturedRequest = request
            HotelsApiSearchOrchestrator.Result.Success(
                location = location(1001),
                offers = listOf(offer),
            )
        }

        val result = assertIs<HotelOfferProviderResult.SearchCompleted>(
            adapter.search(criteria()),
        )

        assertEquals(listOf(offer), result.offers)
        assertEquals(criteria(), capturedRequest?.criteria)
        assertEquals(HotelLocationResolutionRequest.Language.RU, capturedRequest?.language)
    }

    @Test
    fun `maps location outcomes without exposing destination identifiers`() = runBlocking {
        val notFound = adapter {
            HotelsApiSearchOrchestrator.Result.LocationNotFound
        }.search(criteria())
        val selection = adapter {
            HotelsApiSearchOrchestrator.Result.LocationSelectionRequired(
                candidates = listOf(location(1001), location(1002)),
            )
        }.search(criteria())

        assertIs<HotelOfferProviderResult.LocationNotFound>(notFound)
        val required = assertIs<HotelOfferProviderResult.LocationSelectionRequired>(selection)
        assertEquals(listOf("Казань", "Казань"), required.suggestions.map { it.name })
        assertEquals(listOf("city", "city"), required.suggestions.map { it.typeCode })
        assertEquals(false, required.toString().contains("1001"))
        assertEquals(false, required.toString().contains("1002"))
    }

    @Test
    fun `maps request and response rejections to application reasons`() = runBlocking {
        val requestIssues = mapOf(
            HotelsApiSearchMappingError.Issue.INVALID_DESTINATION_ID to
                HotelOfferProviderResult.RequestRejectionReason.INVALID_DESTINATION,
            HotelsApiSearchMappingError.Issue.INVALID_DATE_RANGE to
                HotelOfferProviderResult.RequestRejectionReason.INVALID_DATE_RANGE,
            HotelsApiSearchMappingError.Issue.INVALID_ROOM_COUNT to
                HotelOfferProviderResult.RequestRejectionReason.INVALID_OCCUPANCY,
            HotelsApiSearchMappingError.Issue.INVALID_CHILD_AGE to
                HotelOfferProviderResult.RequestRejectionReason.INVALID_OCCUPANCY,
            HotelsApiSearchMappingError.Issue.INVALID_MAX_TOTAL_PRICE to
                HotelOfferProviderResult.RequestRejectionReason.INVALID_PREFERENCES,
            HotelsApiSearchMappingError.Issue.UNSUPPORTED_MAX_TOTAL_PRICE_CURRENCY to
                HotelOfferProviderResult.RequestRejectionReason.INVALID_PREFERENCES,
            HotelsApiSearchMappingError.Issue.INVALID_STARS to
                HotelOfferProviderResult.RequestRejectionReason.INVALID_PREFERENCES,
        )
        requestIssues.forEach { (issue, expectedReason) ->
            val result = adapter {
                HotelsApiSearchOrchestrator.Result.RequestRejected(
                    HotelsApiSearchMappingError(issue),
                )
            }.search(criteria())

            assertEquals(
                HotelOfferProviderResult.RequestRejected(expectedReason),
                result,
            )
        }

        val responseIssues = mapOf(
            HotelsApiSearchMappingError.Issue.INVALID_PROVIDER_REFERENCE to
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_PROVIDER_REFERENCE,
            HotelsApiSearchMappingError.Issue.INVALID_HOTEL_NAME to
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_HOTEL_DATA,
            HotelsApiSearchMappingError.Issue.INVALID_LOCATION to
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_LOCATION_DATA,
            HotelsApiSearchMappingError.Issue.INVALID_PRICE to
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_PRICE,
            HotelsApiSearchMappingError.Issue.INVALID_CURRENCY to
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_CURRENCY,
            HotelsApiSearchMappingError.Issue.INVALID_REVIEW to
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_REVIEW,
            HotelsApiSearchMappingError.Issue.INVALID_AVAILABILITY to
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_AVAILABILITY,
            HotelsApiSearchMappingError.Issue.INVALID_STAR_RATING to
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_HOTEL_DATA,
            HotelsApiSearchMappingError.Issue.INVALID_CANCELLATION to
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_HOTEL_DATA,
        )
        responseIssues.forEach { (issue, expectedReason) ->
            val result = adapter {
                HotelsApiSearchOrchestrator.Result.ResponseRejected(
                    listOf(HotelsApiSearchMappingError(issue)),
                )
            }.search(criteria())

            assertEquals(
                HotelOfferProviderResult.ResponseRejected(expectedReason),
                result,
            )
        }
    }

    @Test
    fun `maps safe transport categories without leaking exception text`() = runBlocking {
        val categories = mapOf(
            HotelProviderErrorCategory.TIMEOUT to HotelOfferProviderResult.UnavailableReason.TIMEOUT,
            HotelProviderErrorCategory.RATE_LIMITED to
                HotelOfferProviderResult.UnavailableReason.RATE_LIMITED,
            HotelProviderErrorCategory.AUTHENTICATION_FAILED to
                HotelOfferProviderResult.UnavailableReason.AUTHENTICATION_FAILED,
            HotelProviderErrorCategory.UNAVAILABLE to
                HotelOfferProviderResult.UnavailableReason.UNAVAILABLE,
            HotelProviderErrorCategory.UNKNOWN to HotelOfferProviderResult.UnavailableReason.UNKNOWN,
        )

        categories.forEach { (category, expectedReason) ->
            val result = adapter {
                throw HotelProviderException(category, "provider-sensitive-message")
            }.search(criteria())

            assertEquals(HotelOfferProviderResult.ProviderUnavailable(expectedReason), result)
            assertEquals(false, result.toString().contains("provider-sensitive-message"))
        }

        val invalidResponse = adapter {
            throw HotelProviderException(
                HotelProviderErrorCategory.INVALID_RESPONSE,
                "provider-sensitive-body",
            )
        }.search(criteria())
        assertEquals(
            HotelOfferProviderResult.ResponseRejected(
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_PAYLOAD,
            ),
            invalidResponse,
        )
    }

    @Test
    fun `propagates coroutine cancellation`() {
        assertFailsWith<CancellationException> {
            runBlocking {
                adapter { throw CancellationException("cancelled") }.search(criteria())
            }
        }
    }

    private fun adapter(
        language: HotelLocationResolutionRequest.Language? = null,
        result: suspend (HotelsApiSearchOrchestrator.Request) -> HotelsApiSearchOrchestrator.Result,
    ): RealHotelOfferProviderAdapter =
        RealHotelOfferProviderAdapter(
            search = result,
            language = language,
        )

    private fun criteria(): HotelSearchCriteria =
        HotelSearchCriteria(
            destination = "Казань",
            checkInDate = LocalDate.parse("2026-07-18"),
            checkOutDate = LocalDate.parse("2026-07-19"),
            guests = HotelSearchCriteria.Guests(adults = 2),
            rooms = 1,
        )

    private fun location(destinationId: Int): HotelLocationResolution.Candidate =
        HotelLocationResolution.Candidate(
            destinationId = destinationId,
            name = "Казань",
            signature = "Казань, Россия",
            type = HotelLocationResolution.Type(code = "city", name = "Город"),
        )

    private fun offer(): HotelOffer =
        HotelOffer(
            id = "tbank-hotels-api:hotel-1",
            providerReference = "hotel-1",
            hotelName = "Тестовый отель",
            city = "Казань",
            country = "Россия",
            totalPrice = 12_000.0,
            currency = "RUB",
            rating = 8.7,
            reviewCount = 42,
            amenities = null,
            availability = HotelOffer.Availability.AVAILABLE,
            source = "tbank_hotels_api",
            freshness = HotelOffer.Freshness.UNKNOWN,
        )
}
