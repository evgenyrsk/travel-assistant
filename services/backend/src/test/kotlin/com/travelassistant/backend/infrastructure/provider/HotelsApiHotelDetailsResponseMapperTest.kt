package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.domain.hotel.HotelDetails
import java.time.LocalTime
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class HotelsApiHotelDetailsResponseMapperTest {

    @Test
    fun `maps sanitized provider fixture into bounded provider-neutral details`() {
        val response = HotelsApiJson.codec.decodeFromString<HotelsApiHotelDetailsResponseDto>(
            fixture("hotel-details-success.json"),
        )

        val mapped = assertIs<HotelsApiHotelDetailsResponseMapper.Result.Mapped>(
            HotelsApiHotelDetailsResponseMapper.map(response),
        )
        val details = mapped.details

        assertEquals("hotel-example-001", mapped.providerReference)
        assertEquals("Отель Пример", details.hotelName)
        assertEquals("Сеть Пример", details.hotelChain)
        assertEquals(4, details.starRating)
        assertEquals("Пример адреса", details.location?.address)
        assertEquals(0.0, details.location?.coordinates?.latitude)
        assertEquals(0.0, details.location?.coordinates?.longitude)
        assertEquals(1, details.descriptionSections?.size)
        assertEquals(2, details.imageUrls?.size)
        assertEquals(2, details.amenityGroups?.size)
        assertEquals(LocalTime.of(15, 0), details.checkInTime)
        assertEquals(LocalTime.of(12, 0), details.checkOutTime)
        assertEquals(
            listOf(HotelDetails.PaymentMethod.CARD, HotelDetails.PaymentMethod.CASH),
            details.paymentMethods,
        )
        assertEquals(HotelDetails.Source.PROVIDER, details.source)
        assertEquals(HotelDetails.Freshness.UNKNOWN, details.freshness)
    }

    @Test
    fun `preserves unknown optional facts instead of inventing values`() {
        val mapped = assertIs<HotelsApiHotelDetailsResponseMapper.Result.Mapped>(
            HotelsApiHotelDetailsResponseMapper.map(
                response(
                    payload = HotelsApiHotelDetailsResponseDto.Payload(
                        hotelId = "hotel-1",
                        hotelName = "Отель",
                    ),
                ),
            ),
        ).details

        assertNull(mapped.hotelChain)
        assertNull(mapped.starRating)
        assertNull(mapped.location)
        assertNull(mapped.descriptionSections)
        assertNull(mapped.imageUrls)
        assertNull(mapped.amenityGroups)
        assertNull(mapped.checkInTime)
        assertNull(mapped.checkOutTime)
        assertNull(mapped.paymentMethods)
    }

    @Test
    fun `keeps only safe unique https images and limits their count`() {
        val images = (1..12).map { "https://example.invalid/image-$it.jpg" } + listOf(
            "http://example.invalid/insecure.jpg",
            "https://user:password@example.invalid/credentials.jpg",
            "https://example.invalid/fragment.jpg#unsafe",
            "not-a-url",
            "https://example.invalid/image-1.jpg",
        )
        val details = assertIs<HotelsApiHotelDetailsResponseMapper.Result.Mapped>(
            HotelsApiHotelDetailsResponseMapper.map(
                response(payload().copy(images = images)),
            ),
        ).details

        assertEquals(10, details.imageUrls?.size)
        assertEquals("https://example.invalid/image-1.jpg", details.imageUrls?.first())
        assertEquals("https://example.invalid/image-10.jpg", details.imageUrls?.last())
    }

    @Test
    fun `keeps allowlisted descriptions and removes service and contact data`() {
        val details = assertIs<HotelsApiHotelDetailsResponseMapper.Result.Mapped>(
            HotelsApiHotelDetailsResponseMapper.map(
                response(
                    payload().copy(
                        description = listOf(
                            description(
                                title = null,
                                "Тихий отель рядом с центром.",
                                "Телефон: +7 999 123-45-67",
                            ),
                            description(
                                title = "Об отеле",
                                "В отеле есть круглосуточная стойка регистрации.",
                                "Подробнее: https://registry.example.invalid/hotel",
                            ),
                            description(
                                title = "Сертификация",
                                "ИНН 1234567890, ОГРН 1234567890123, владелец — пример.",
                            ),
                            description(
                                title = "Служебный раздел",
                                "Этот текст не предназначен для пользователя.",
                            ),
                            description(
                                title = "Important information",
                                "Заселение проводится по документу, удостоверяющему личность.",
                                "Contact: hotel@example.invalid",
                            ),
                        ),
                    ),
                ),
            ),
        ).details

        assertEquals(
            listOf(
                HotelDetails.DescriptionSection(
                    title = null,
                    paragraphs = listOf("Тихий отель рядом с центром."),
                ),
                HotelDetails.DescriptionSection(
                    title = "Об отеле",
                    paragraphs = listOf("В отеле есть круглосуточная стойка регистрации."),
                ),
                HotelDetails.DescriptionSection(
                    title = "Important information",
                    paragraphs = listOf(
                        "Заселение проводится по документу, удостоверяющему личность.",
                    ),
                ),
            ),
            details.descriptionSections,
        )
        val publicText = details.descriptionSections.toString()
        listOf("ИНН", "ОГРН", "registry", "владелец", "hotel@example.invalid").forEach { forbidden ->
            assertEquals(false, publicText.contains(forbidden, ignoreCase = true))
        }
    }

    @Test
    fun `returns typed errors for invalid identity location star rating and times`() {
        val invalidPayloads = listOf(
            payload().copy(hotelId = " ") to
                HotelsApiHotelDetailsMappingError.Issue.INVALID_PROVIDER_REFERENCE,
            payload().copy(hotelName = " ") to
                HotelsApiHotelDetailsMappingError.Issue.INVALID_HOTEL_NAME,
            payload().copy(starRating = 6) to
                HotelsApiHotelDetailsMappingError.Issue.INVALID_STAR_RATING,
            payload().copy(
                hotelLocation = HotelsApiHotelDetailsResponseDto.HotelLocation(address = " "),
            ) to HotelsApiHotelDetailsMappingError.Issue.INVALID_LOCATION,
            payload().copy(
                hotelLocation = HotelsApiHotelDetailsResponseDto.HotelLocation(
                    coordinates = HotelsApiHotelDetailsResponseDto.Coordinates(
                        latitude = 91.0,
                        longitude = 0.0,
                    ),
                ),
            ) to HotelsApiHotelDetailsMappingError.Issue.INVALID_LOCATION,
            payload().copy(checkInTime = "invalid") to
                HotelsApiHotelDetailsMappingError.Issue.INVALID_CHECK_IN_TIME,
            payload().copy(checkOutTime = "invalid") to
                HotelsApiHotelDetailsMappingError.Issue.INVALID_CHECK_OUT_TIME,
        )

        invalidPayloads.forEach { (payload, issue) ->
            val result = assertIs<HotelsApiHotelDetailsResponseMapper.Result.Rejected>(
                HotelsApiHotelDetailsResponseMapper.map(response(payload)),
            )
            assertEquals(issue, result.error.issue)
        }
    }

    @Test
    fun `normalizes known payment methods and ignores unknown provider values`() {
        val details = assertIs<HotelsApiHotelDetailsResponseMapper.Result.Mapped>(
            HotelsApiHotelDetailsResponseMapper.map(
                response(
                    payload().copy(
                        paymentMethods = listOf(
                            "cash",
                            "visa",
                            "master_card",
                            "mir",
                            "future_method",
                        ),
                    ),
                ),
            ),
        ).details

        assertEquals(
            listOf(HotelDetails.PaymentMethod.CASH, HotelDetails.PaymentMethod.CARD),
            details.paymentMethods,
        )
    }

    private fun response(
        payload: HotelsApiHotelDetailsResponseDto.Payload,
    ): HotelsApiHotelDetailsResponseDto = HotelsApiHotelDetailsResponseDto(payload)

    private fun payload(): HotelsApiHotelDetailsResponseDto.Payload =
        HotelsApiHotelDetailsResponseDto.Payload(
            hotelId = "hotel-1",
            hotelName = "Отель",
        )

    private fun description(
        title: String?,
        vararg paragraphs: String,
    ): HotelsApiHotelDetailsResponseDto.DescriptionSection =
        HotelsApiHotelDetailsResponseDto.DescriptionSection(
            title = title,
            paragraphs = paragraphs.toList(),
        )

    private fun fixture(name: String): String =
        requireNotNull(
            javaClass.getResource("/fixtures/hotels-api/stage-13-1/$name"),
        ) {
            "Fixture not found: $name"
        }.readText()
}
