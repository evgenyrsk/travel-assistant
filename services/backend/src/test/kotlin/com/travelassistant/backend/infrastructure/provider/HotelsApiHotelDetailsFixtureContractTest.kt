package com.travelassistant.backend.infrastructure.provider

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HotelsApiHotelDetailsFixtureContractTest {

    @Test
    fun `details fixture preserves observed envelope and representative field shapes`() {
        val content = fixture("hotel-details-success.json")
        val root = HotelsApiJson.codec.parseToJsonElement(content).jsonObject
        val payload = root.getValue("payload").jsonObject

        assertEquals(setOf("payload"), root.keys)
        assertEquals("hotel-example-001", payload.getValue("hotelId").jsonPrimitive.content)
        assertEquals(
            1001,
            payload.getValue("areaLocation").jsonObject
                .getValue("destinationId").jsonPrimitive.int,
        )
        assertEquals(2, payload.getValue("images").jsonArray.size)
        assertTrue(payload.getValue("images").jsonArray.all { image ->
            image.jsonPrimitive.content.startsWith("https://example.invalid/")
        })
        assertTrue(payload.getValue("facilitiesGroups").jsonArray.isNotEmpty())
        assertTrue(payload.getValue("paymentMethods").jsonArray.isNotEmpty())
        assertTrue(payload.getValue("structuredRules").jsonObject.containsKey("internet"))
        assertTrue(payload.getValue("certification").jsonObject.containsKey("roomList"))

        assertEquals("hotel@example.invalid", payload.getValue("email").jsonPrimitive.content)
        assertFalse(content.contains("hotels.tbank", ignoreCase = true))
        assertFalse(content.contains("traceid", ignoreCase = true))
    }

    @Test
    fun `manifest records one search and one anonymous details request without retries`() {
        val manifest = HotelsApiJson.codec.parseToJsonElement(
            fixture("fixture-manifest.json"),
        ).jsonObject
        val calls = manifest.getValue("calls").jsonArray
        val observation = manifest.getValue("observation").jsonObject

        assertTrue(manifest.getValue("sanitized").jsonPrimitive.boolean)
        assertFalse(manifest.getValue("automaticRetries").jsonPrimitive.boolean)
        assertFalse(manifest.getValue("alternateEndpointsProbed").jsonPrimitive.boolean)
        assertEquals(2, calls.size)
        assertEquals("POST", calls[0].jsonObject.getValue("method").jsonPrimitive.content)
        assertEquals("GET", calls[1].jsonObject.getValue("method").jsonPrimitive.content)
        assertEquals(200, calls[1].jsonObject.getValue("status").jsonPrimitive.int)
        assertFalse(
            calls[1].jsonObject.getValue("authorizationSent").jsonPrimitive.boolean,
        )
        assertTrue(
            observation.getValue("searchHotelIdAcceptedByDetailsPath").jsonPrimitive.boolean,
        )
        assertTrue(observation.getValue("anonymousRequestAccepted").jsonPrimitive.boolean)
        assertEquals(
            "a5d422acb70e79ce34090e8a16f805238feae09f4f4202963261a75512fb5ac5",
            manifest.getValue("fixture").jsonObject.getValue("sha256").jsonPrimitive.content,
        )
    }

    private fun fixture(name: String): String =
        requireNotNull(
            javaClass.getResource("/fixtures/hotels-api/stage-13-1/$name"),
        ) {
            "Fixture not found: $name"
        }.readText()
}
