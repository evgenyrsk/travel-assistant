package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelLocationResolution
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class HotelsApiExactHotelResponseMapperTest {
    @Test
    fun `selects cheapest matching breakfast rate and maps exact hotel facts`() {
        val result = assertIs<HotelsApiExactHotelResponseMapper.Result.Mapped>(
            HotelsApiExactHotelResponseMapper.map(
                candidate = candidate(),
                criteria = criteria(
                    preferences = HotelSearchPreferences(
                        maxTotalPrice = HotelSearchPreferences.MaxTotalPrice(
                            amount = BigDecimal("20000"),
                            currency = "RUB",
                        ),
                        stars = setOf(5),
                        freeCancellationRequired = true,
                        breakfastIncludedRequired = true,
                    ),
                ),
                details = details(images = listOf("http://unsafe.test/hotel.jpg")),
                rates = rates(),
            ),
        )

        val offer = result.offers.single()
        assertEquals("provider-hotel-1", offer.providerReference)
        assertEquals("Cosmos Москва ВДНХ Отель", offer.hotelName)
        assertEquals("Москва", offer.city)
        assertEquals("Россия", offer.country)
        assertEquals(15_000.0, offer.totalPrice)
        assertEquals("RUB", offer.currency)
        assertEquals(5, offer.starRating)
        assertEquals(true, offer.breakfastIncluded)
        assertEquals("https://images.test/room-breakfast.jpg", offer.imageUrl)
        assertNull(offer.rating)
        assertNull(offer.reviewCount)
    }

    @Test
    fun `returns no offer when exact hotel does not satisfy stars`() {
        val result = assertIs<HotelsApiExactHotelResponseMapper.Result.Mapped>(
            HotelsApiExactHotelResponseMapper.map(
                candidate = candidate(),
                criteria = criteria(
                    preferences = HotelSearchPreferences(stars = setOf(4)),
                ),
                details = details(),
                rates = rates(),
            ),
        )

        assertEquals(emptyList(), result.offers)
    }

    @Test
    fun `rejects minimum guest rating because exact rates contract has no review fact`() {
        val result = assertIs<HotelsApiExactHotelResponseMapper.Result.Rejected>(
            HotelsApiExactHotelResponseMapper.map(
                candidate = candidate(),
                criteria = criteria(
                    preferences = HotelSearchPreferences(
                        minimumGuestRating = HotelSearchPreferences.MinimumGuestRating.EIGHT,
                    ),
                ),
                details = details(),
                rates = rates(),
            ),
        )

        assertEquals(HotelsApiSearchMappingError.Issue.INVALID_REVIEW, result.error.issue)
    }

    @Test
    fun `rejects provider reference mismatch and malformed rate facts`() {
        val mismatched = assertIs<HotelsApiExactHotelResponseMapper.Result.Rejected>(
            HotelsApiExactHotelResponseMapper.map(
                candidate = candidate(),
                criteria = criteria(),
                details = details(providerReference = "other-hotel"),
                rates = rates(),
            ),
        )
        val invalidPrice = assertIs<HotelsApiExactHotelResponseMapper.Result.Rejected>(
            HotelsApiExactHotelResponseMapper.map(
                candidate = candidate(),
                criteria = criteria(),
                details = details(),
                rates = rates(
                    breakfastPrice = -1.0,
                    noMealPrice = -1.0,
                ),
            ),
        )

        assertEquals(
            HotelsApiSearchMappingError.Issue.INVALID_PROVIDER_REFERENCE,
            mismatched.error.issue,
        )
        assertEquals(HotelsApiSearchMappingError.Issue.INVALID_PRICE, invalidPrice.error.issue)
    }

    private fun candidate(): HotelLocationResolution.HotelCandidate =
        HotelLocationResolution.HotelCandidate(
            providerReference = "provider-hotel-1",
            name = "Cosmos Москва ВДНХ Отель",
            signature = "Отель • Россия, Москва",
            type = HotelLocationResolution.Type(code = "hotel", name = "Отель"),
        )

    private fun criteria(
        preferences: HotelSearchPreferences = HotelSearchPreferences(),
    ): HotelSearchCriteria =
        HotelSearchCriteria(
            destination = "Cosmos ВДНХ",
            checkInDate = LocalDate.parse("2026-08-01"),
            checkOutDate = LocalDate.parse("2026-08-08"),
            guests = HotelSearchCriteria.Guests(adults = 2),
            rooms = 1,
            preferences = preferences,
        )

    private fun details(
        providerReference: String = "provider-hotel-1",
        images: List<String>? = null,
    ): HotelsApiHotelDetailsResponseDto =
        HotelsApiHotelDetailsResponseDto(
            payload = HotelsApiHotelDetailsResponseDto.Payload(
                hotelId = providerReference,
                hotelName = "Cosmos Москва ВДНХ Отель",
                starRating = 5,
                areaLocation = HotelsApiHotelDetailsResponseDto.AreaLocation(
                    countryName = "Россия",
                    destinationName = "Москва",
                ),
                images = images,
            ),
        )

    private fun rates(
        breakfastPrice: Double = 15_000.0,
        noMealPrice: Double = 10_000.0,
    ): HotelsApiHotelRatesResponseDto =
        HotelsApiHotelRatesResponseDto(
            payload = HotelsApiHotelRatesResponseDto.Payload(
                rates = listOf(
                    rate(
                        roomId = "room-no-meal",
                        mealType = "nomeal",
                        price = noMealPrice,
                        freeCancellationUntil = null,
                    ),
                    rate(
                        roomId = "room-breakfast",
                        mealType = "breakfast",
                        price = breakfastPrice,
                        freeCancellationUntil = "2026-07-31T18:00:00+03:00",
                    ),
                ),
                rooms = listOf(
                    room("room-no-meal", "https://images.test/room-no-meal.jpg"),
                    room("room-breakfast", "https://images.test/room-breakfast.jpg"),
                ),
            ),
        )

    private fun rate(
        roomId: String,
        mealType: String,
        price: Double,
        freeCancellationUntil: String?,
    ): HotelsApiHotelRatesResponseDto.Rate =
        HotelsApiHotelRatesResponseDto.Rate(
            availableRoomsCount = 2,
            cancellationPolicyRules = HotelsApiHotelRatesResponseDto.CancellationPolicyRules(
                freeCancellationUntil = freeCancellationUntil,
            ),
            mealName = mealType,
            mealType = mealType,
            paymentPlace = "hotel",
            roomId = roomId,
            shownPrice = HotelsApiHotelRatesResponseDto.Money(
                amount = price,
                currency = "RUB",
            ),
        )

    private fun room(
        roomId: String,
        imageUrl: String,
    ): HotelsApiHotelRatesResponseDto.Room =
        HotelsApiHotelRatesResponseDto.Room(
            roomId = roomId,
            roomName = "Тестовый номер",
            images = listOf(HotelsApiHotelRatesResponseDto.Image(imageUrl)),
        )
}
