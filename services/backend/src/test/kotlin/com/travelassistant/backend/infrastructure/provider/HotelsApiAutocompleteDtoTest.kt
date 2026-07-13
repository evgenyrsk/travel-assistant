package com.travelassistant.backend.infrastructure.provider

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class HotelsApiAutocompleteDtoTest {

    @Test
    fun `serializes query with exact contract field name`() {
        val body = HotelsApiJson.codec.encodeToString(
            HotelsApiAutocompleteRequestDto(query = "Санкт-Петербург"),
        )
        val json = HotelsApiJson.codec.parseToJsonElement(body).jsonObject

        assertEquals("Санкт-Петербург", json.getValue("query").jsonPrimitive.content)
        assertEquals(setOf("query"), json.keys)
    }

    @Test
    fun `deserializes location and hotel suggestions`() {
        val response = HotelsApiJson.codec.decodeFromString<HotelsApiAutocompleteResponseDto>(
            responseFixture,
        )

        val location = response.payload.locations?.single()
        assertEquals(77, location?.id)
        assertEquals("Казань", location?.name)
        assertEquals("Город Казань, Россия", location?.signature)
        assertEquals("city", location?.type?.code)
        assertEquals("Город", location?.type?.name)

        val hotel = response.payload.hotels?.single()
        assertEquals("hotel-master-1", hotel?.id)
        assertEquals("Тестовый отель", hotel?.name)
        assertEquals("hotel", hotel?.type?.code)
    }

    @Test
    fun `supports absent optional suggestion groups`() {
        val response = HotelsApiJson.codec.decodeFromString<HotelsApiAutocompleteResponseDto>(
            """{"payload":{}}""",
        )

        assertNull(response.payload.locations)
        assertNull(response.payload.hotels)
    }

    @Test
    fun `ignores unknown provider fields`() {
        val response = HotelsApiJson.codec.decodeFromString<HotelsApiAutocompleteResponseDto>(
            responseFixture,
        )

        assertEquals(77, response.payload.locations?.single()?.id)
    }

    @Test
    fun `rejects missing required envelope and suggestion fields`() {
        val invalidResponses = listOf(
            "{}",
            responseFixture.replace("\"id\":77,", ""),
            responseFixture.replace("\"code\":\"city\",", ""),
        )

        invalidResponses.forEach { response ->
            assertFailsWith<SerializationException> {
                HotelsApiJson.codec.decodeFromString<HotelsApiAutocompleteResponseDto>(response)
            }
        }
    }

    private val responseFixture = """
        {
          "payload": {
            "locations": [
              {
                "id":77,
                "name":"Казань",
                "signature":"Город Казань, Россия",
                "type":{"code":"city","name":"Город"},
                "providerLocationField":"ignored"
              }
            ],
            "hotels": [
              {
                "id":"hotel-master-1",
                "name":"Тестовый отель",
                "signature":"Отель • Россия • Казань",
                "type":{"code":"hotel","name":"Отель"},
                "providerHotelField":"ignored"
              }
            ],
            "providerPayloadField":"ignored"
          },
          "providerEnvelopeField":"ignored"
        }
    """.trimIndent()
}
