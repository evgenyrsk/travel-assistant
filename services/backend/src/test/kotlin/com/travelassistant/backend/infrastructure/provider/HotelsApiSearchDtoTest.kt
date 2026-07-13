package com.travelassistant.backend.infrastructure.provider

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class HotelsApiSearchDtoTest {

    @Test
    fun `serializes request with exact Swagger field names`() {
        val request = HotelsApiSearchRequestDto(
            destinationId = 77,
            checkinDate = "2026-08-10T00:00:00Z",
            checkoutDate = "2026-08-14T00:00:00Z",
            guests = listOf(
                HotelsApiSearchRequestDto.Guest(
                    adultsCount = 2,
                    childrenAge = listOf(5, 11),
                ),
                HotelsApiSearchRequestDto.Guest(adultsCount = 1),
            ),
            offset = 20,
            limit = 20,
        )

        val json = HotelsApiJson.codec.parseToJsonElement(
            HotelsApiJson.codec.encodeToString(request),
        ).jsonObject

        assertEquals(77, json.getValue("destinationId").jsonPrimitive.content.toInt())
        assertEquals("2026-08-10T00:00:00Z", json.getValue("checkinDate").jsonPrimitive.content)
        assertEquals("2026-08-14T00:00:00Z", json.getValue("checkoutDate").jsonPrimitive.content)
        assertEquals(20, json.getValue("offset").jsonPrimitive.content.toInt())
        assertEquals(20, json.getValue("limit").jsonPrimitive.content.toInt())

        val guests = json.getValue("guests").jsonArray
        assertEquals(2, guests.size)
        assertEquals(2, guests[0].jsonObject.getValue("adultsCount").jsonPrimitive.content.toInt())
        assertEquals(
            listOf("5", "11"),
            guests[0].jsonObject.getValue("childrenAge").jsonArray.map { it.jsonPrimitive.content },
        )
        assertEquals(1, guests[1].jsonObject.getValue("adultsCount").jsonPrimitive.content.toInt())
        assertFalse(guests[1].jsonObject.containsKey("childrenAge"))
    }

    @Test
    fun `omits nullable request fields when they are absent`() {
        val request = HotelsApiSearchRequestDto(
            destinationId = 77,
            checkinDate = "2026-08-10T00:00:00Z",
            checkoutDate = "2026-08-14T00:00:00Z",
            guests = listOf(HotelsApiSearchRequestDto.Guest(adultsCount = 2)),
        )

        val json = HotelsApiJson.codec.parseToJsonElement(
            HotelsApiJson.codec.encodeToString(request),
        ).jsonObject

        assertFalse(json.containsKey("offset"))
        assertFalse(json.containsKey("limit"))
        assertFalse(json.getValue("guests").jsonArray[0].jsonObject.containsKey("childrenAge"))
    }

    @Test
    fun `deserializes mapping-ready response fields and ignores deferred fields`() {
        val response = HotelsApiJson.codec.decodeFromString<HotelsApiSearchResponseDto>(
            responseFixture(review = """{"rating":9.1,"ratingsCount":321}"""),
        )

        assertEquals(1, response.payload.filteredHotelsCount)
        assertEquals(3, response.payload.hotelsTotalCount)
        assertFalse(response.payload.isLoadingCompleted)
        assertEquals(20, response.payload.nextOffset)
        assertEquals(14500.0, response.payload.hotelsMinPrice?.amount)

        val hotel = response.payload.hotels.single()
        assertEquals("hotel-1", hotel.hotelId)
        assertEquals("Тестовый отель", hotel.hotelName)
        assertEquals(4, hotel.starRating)
        assertEquals("Россия", hotel.areaLocation.countryName)
        assertEquals(77, hotel.areaLocation.destinationId)
        assertEquals("Казань", hotel.areaLocation.destinationName)
        assertEquals("Казань, Россия", hotel.areaLocation.signature)
        assertEquals("Тестовая улица, 1", hotel.hotelLocation.address)
        assertEquals(55.796, hotel.hotelLocation.coordinates?.latitude)
        assertEquals(49.106, hotel.hotelLocation.coordinates?.longitude)
        assertEquals(listOf("https://images.test/hotel-1.jpg"), hotel.images)
        assertEquals(2, hotel.rateForHotelsFeed.availableRoomsCount)
        assertEquals("future_payment_place", hotel.rateForHotelsFeed.paymentPlace)
        assertEquals(18900.0, hotel.rateForHotelsFeed.shownPrice.amount)
        assertEquals("FUTURE_CURRENCY", hotel.rateForHotelsFeed.shownPrice.currency)
        assertEquals("2026-08-08T18:00:00Z", hotel.rateForHotelsFeed.freeCancellationUntil)
        assertEquals("Завтрак", hotel.rateForHotelsFeed.mealName)
        assertEquals("breakfast", hotel.rateForHotelsFeed.mealType)
        assertEquals(9.1, hotel.review?.rating)
        assertEquals(321, hotel.review?.ratingsCount)
    }

    @Test
    fun `supports explicit null and absent review`() {
        val explicitNull = HotelsApiJson.codec.decodeFromString<HotelsApiSearchResponseDto>(
            responseFixture(review = "null"),
        )
        val absent = HotelsApiJson.codec.decodeFromString<HotelsApiSearchResponseDto>(
            responseFixture(review = null),
        )

        assertNull(explicitNull.payload.hotels.single().review)
        assertNull(absent.payload.hotels.single().review)
    }

    @Test
    fun `rejects response without required modeled fields`() {
        val validResponse = responseFixture(review = null)
        val invalidResponses = listOf(
            "{}",
            validResponse.replace("\"hotelId\": \"hotel-1\",", ""),
            validResponse.replace(
                "\"shownPrice\": {\"amount\": 18900.0, \"currency\": \"FUTURE_CURRENCY\"},",
                "",
            ),
        )

        invalidResponses.forEach { response ->
            assertFailsWith<SerializationException> {
                HotelsApiJson.codec.decodeFromString<HotelsApiSearchResponseDto>(response)
            }
        }
    }

    private fun responseFixture(review: String?): String {
        val reviewField = review?.let { ",\"review\":$it" }.orEmpty()
        return """
            {
              "payload": {
                "filteredHotelsCount": 1,
                "filters": {"deferredFilter": true},
                "hotels": [
                  {
                    "hotelId": "hotel-1",
                    "hotelName": "Тестовый отель",
                    "starRating": 4,
                    "areaLocation": {
                      "countryName": "Россия",
                      "destinationId": 77,
                      "destinationName": "Казань",
                      "signature": "Казань, Россия",
                      "type": {"code": "city", "name": "Город"}
                    },
                    "hotelLocation": {
                      "address": "Тестовая улица, 1",
                      "coordinates": {"latitude": 55.796, "longitude": 49.106}
                    },
                    "images": ["https://images.test/hotel-1.jpg"],
                    "rateForHotelsFeed": {
                      "availableRoomsCount": 2,
                      "isCreditCardDataRequired": false,
                      "paymentPlace": "future_payment_place",
                      "shownPrice": {"amount": 18900.0, "currency": "FUTURE_CURRENCY"},
                      "freeCancellationUntil": "2026-08-08T18:00:00Z",
                      "mealName": "Завтрак",
                      "mealType": "breakfast",
                      "providerRateField": "ignored"
                    },
                    "cashback": {"sum": {"amount": 500.0, "currency": "RUB"}},
                    "providerHotelField": "ignored"$reviewField
                  }
                ],
                "hotelsMinPrice": {"amount": 14500.0, "currency": "RUB"},
                "hotelsTotalCount": 3,
                "isLoadingCompleted": false,
                "nextOffset": 20,
                "providerPayloadField": "ignored"
              },
              "providerEnvelopeField": "ignored"
            }
        """.trimIndent()
    }
}
