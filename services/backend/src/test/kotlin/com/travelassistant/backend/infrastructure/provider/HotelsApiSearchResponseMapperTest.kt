package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.domain.hotel.HotelOffer
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class HotelsApiSearchResponseMapperTest {

    @Test
    fun `maps opaque provider reference total-stay price review and availability`() {
        val response = response(
            hotel(
                hotelId = "opaque/provider:hotel-1",
                starRating = 5,
                shownPrice = money(amount = 18_900.0, currency = "RUB"),
                review = HotelsApiSearchResponseDto.Review(
                    rating = 8.7,
                    ratingsCount = 321,
                ),
                availableRoomsCount = 2,
                freeCancellationUntil = "2026-07-17T21:00:00+00:00",
            ),
        )

        val offer = assertIs<HotelsApiSearchResponseMapper.Result.Mapped>(
            HotelsApiSearchResponseMapper.map(response),
        ).offers.single()

        assertEquals("opaque/provider:hotel-1", offer.providerReference)
        assertEquals(18_900.0, offer.totalPrice)
        assertEquals("RUB", offer.currency)
        assertEquals(8.7, offer.rating)
        assertEquals(321, offer.reviewCount)
        assertNull(offer.amenities)
        assertEquals(5, offer.starRating)
        assertEquals(Instant.parse("2026-07-17T21:00:00Z"), offer.freeCancellationUntil)
        assertEquals(HotelOffer.Availability.AVAILABLE, offer.availability)
        assertEquals("tbank_hotels_api", offer.source)
        assertEquals(HotelOffer.Freshness.UNKNOWN, offer.freshness)
    }

    @Test
    fun `does not replace absent guest review with star rating`() {
        val offer = assertIs<HotelsApiSearchResponseMapper.Result.Mapped>(
            HotelsApiSearchResponseMapper.map(
                response(
                    hotel(
                        starRating = 5,
                        review = null,
                        availableRoomsCount = 0,
                    ),
                ),
            ),
        ).offers.single()

        assertNull(offer.rating)
        assertNull(offer.reviewCount)
        assertEquals(5, offer.starRating)
        assertNull(offer.freeCancellationUntil)
        assertEquals(HotelOffer.Availability.UNKNOWN, offer.availability)
    }

    @Test
    fun `preserves a short non-blank provider hotel name without inventing a replacement`() {
        val offer = assertIs<HotelsApiSearchResponseMapper.Result.Mapped>(
            HotelsApiSearchResponseMapper.map(
                response(
                    hotel(hotelName = "МА"),
                ),
            ),
        ).offers.single()

        assertEquals("МА", offer.hotelName)
    }

    @Test
    fun `deduplicates provider references and ignores pagination fields`() {
        val first = hotel(hotelId = "hotel-1", hotelName = "First")
        val duplicate = hotel(hotelId = "hotel-1", hotelName = "Duplicate")
        val response = HotelsApiSearchResponseDto(
            payload = HotelsApiSearchResponseDto.Payload(
                filteredHotelsCount = 2,
                hotels = listOf(first, duplicate),
                hotelsTotalCount = 50,
                isLoadingCompleted = false,
                nextOffset = 50,
            ),
        )

        val offers = assertIs<HotelsApiSearchResponseMapper.Result.Mapped>(
            HotelsApiSearchResponseMapper.map(response),
        ).offers

        assertEquals(1, offers.size)
        assertEquals("First", offers.single().hotelName)
    }

    @Test
    fun `returns typed errors for invalid provider facts`() {
        val invalidHotels = listOf(
            hotel(shownPrice = money(amount = -1.0)) to
                HotelsApiSearchMappingError.Issue.INVALID_PRICE,
            hotel(shownPrice = money(currency = " ")) to
                HotelsApiSearchMappingError.Issue.INVALID_CURRENCY,
            hotel(
                review = HotelsApiSearchResponseDto.Review(rating = 10.1, ratingsCount = 1),
            ) to HotelsApiSearchMappingError.Issue.INVALID_REVIEW,
            hotel(
                review = HotelsApiSearchResponseDto.Review(rating = 8.0, ratingsCount = -1),
            ) to HotelsApiSearchMappingError.Issue.INVALID_REVIEW,
            hotel(availableRoomsCount = -1) to
                HotelsApiSearchMappingError.Issue.INVALID_AVAILABILITY,
            hotel(starRating = 6) to
                HotelsApiSearchMappingError.Issue.INVALID_STAR_RATING,
            hotel(freeCancellationUntil = "not-a-date") to
                HotelsApiSearchMappingError.Issue.INVALID_CANCELLATION,
        )

        invalidHotels.forEach { (hotel, expectedIssue) ->
            val result = assertIs<HotelsApiSearchResponseMapper.Result.Rejected>(
                HotelsApiSearchResponseMapper.map(response(hotel)),
            )

            assertEquals(expectedIssue, result.errors.single().issue)
        }
    }

    private fun response(vararg hotels: HotelsApiSearchResponseDto.Hotel): HotelsApiSearchResponseDto =
        HotelsApiSearchResponseDto(
            payload = HotelsApiSearchResponseDto.Payload(
                filteredHotelsCount = hotels.size,
                hotels = hotels.toList(),
                hotelsTotalCount = hotels.size,
                isLoadingCompleted = true,
            ),
        )

    private fun hotel(
        hotelId: String = "hotel-1",
        hotelName: String = "Тестовый отель",
        starRating: Int = 4,
        shownPrice: HotelsApiSearchResponseDto.Money = money(),
        review: HotelsApiSearchResponseDto.Review? = HotelsApiSearchResponseDto.Review(
            rating = 9.1,
            ratingsCount = 10,
        ),
        availableRoomsCount: Int = 1,
        freeCancellationUntil: String? = null,
    ): HotelsApiSearchResponseDto.Hotel =
        HotelsApiSearchResponseDto.Hotel(
            hotelId = hotelId,
            hotelName = hotelName,
            starRating = starRating,
            areaLocation = HotelsApiSearchResponseDto.AreaLocation(
                countryName = "Россия",
                destinationId = 77,
                destinationName = "Казань",
                signature = "Казань, Россия",
            ),
            hotelLocation = HotelsApiSearchResponseDto.HotelLocation(
                address = "Тестовая улица, 1",
            ),
            rateForHotelsFeed = HotelsApiSearchResponseDto.Rate(
                availableRoomsCount = availableRoomsCount,
                isCreditCardDataRequired = false,
                paymentPlace = "online",
                shownPrice = shownPrice,
                freeCancellationUntil = freeCancellationUntil,
            ),
            review = review,
        )

    private fun money(
        amount: Double = 12_000.0,
        currency: String = "RUB",
    ): HotelsApiSearchResponseDto.Money =
        HotelsApiSearchResponseDto.Money(
            amount = amount,
            currency = currency,
        )
}
