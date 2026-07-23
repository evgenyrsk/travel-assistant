package com.travelassistant.backend.api

import com.travelassistant.backend.domain.hotel.HotelDetails
import java.time.LocalTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HotelDetailsResponseTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun `maps bounded details without provider identity`() {
        val encoded = json.encodeToString(
            HotelDetailsResponse.from(
                HotelDetails(
                    hotelName = "Отель Пример",
                    hotelChain = "Сеть Пример",
                    starRating = 4,
                    location = HotelDetails.Location(
                        address = "Пример адреса",
                        coordinates = HotelDetails.Coordinates(55.0, 49.0),
                    ),
                    descriptionSections = listOf(
                        HotelDetails.DescriptionSection(
                            title = "Об отеле",
                            paragraphs = listOf("Описание"),
                        ),
                    ),
                    imageUrls = listOf("https://example.invalid/hotel.jpg"),
                    amenityGroups = listOf(
                        HotelDetails.AmenityGroup(
                            name = "Основные",
                            amenities = listOf("Wi-Fi"),
                        ),
                    ),
                    checkInTime = LocalTime.of(15, 0),
                    checkOutTime = LocalTime.of(12, 0),
                    paymentMethods = listOf(
                        HotelDetails.PaymentMethod.CASH,
                        HotelDetails.PaymentMethod.CARD,
                    ),
                ),
            ),
        )
        val body = Json.parseToJsonElement(encoded).jsonObject

        assertEquals("Отель Пример", body.getValue("hotelName").jsonPrimitive.content)
        assertEquals("15:00", body.getValue("checkInTime").jsonPrimitive.content)
        assertEquals(
            listOf("cash", "card"),
            body.getValue("paymentMethods").jsonArray.map { it.jsonPrimitive.content },
        )
        assertEquals(
            setOf("source", "freshness"),
            body.getValue("metadata").jsonObject.keys,
        )
        assertFalse(encoded.contains("providerReference"))
        assertFalse(encoded.contains("hotelId"))
    }

    @Test
    fun `omits unknown optional facts`() {
        val encoded = json.encodeToString(
            HotelDetailsResponse.from(HotelDetails(hotelName = "Отель")),
        )
        val body = Json.parseToJsonElement(encoded).jsonObject

        assertEquals(setOf("hotelName", "metadata"), body.keys)
    }
}
