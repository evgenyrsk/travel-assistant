package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.domain.hotel.HotelOffer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HotelsApiFilteredSearchWithoutSortFixtureContractTest {

    @Test
    fun `filtered search fixture satisfies selected filters and maps provider facts`() {
        val response = HotelsApiJson.codec.decodeFromString<HotelsApiSearchResponseDto>(
            fixture("filtered-search-success.json"),
        )

        assertTrue(response.payload.hotels.isNotEmpty())
        assertFalse(response.payload.isLoadingCompleted)
        assertNotNull(response.payload.nextOffset)
        response.payload.hotels.forEach { hotel ->
            assertTrue(hotel.starRating in setOf(4, 5))
            assertTrue(hotel.rateForHotelsFeed.shownPrice.amount <= 80_000.0)
            assertEquals("RUB", hotel.rateForHotelsFeed.shownPrice.currency)
            assertTrue(assertNotNull(hotel.review).rating >= 8.0)
            assertTrue(assertNotNull(hotel.rateForHotelsFeed.freeCancellationUntil).isNotBlank())
        }

        val offers = assertIs<HotelsApiSearchResponseMapper.Result.Mapped>(
            HotelsApiSearchResponseMapper.map(response),
        ).offers

        assertEquals(response.payload.hotels.size, offers.size)
        offers.forEach { offer ->
            assertTrue(offer.providerReference.startsWith("hotel-filtered-fixture-"))
            assertEquals("RUB", offer.currency)
            assertTrue(assertNotNull(offer.rating) >= 8.0)
            assertTrue(assertNotNull(offer.starRating) in setOf(4, 5))
            assertNotNull(offer.freeCancellationUntil)
            assertNull(offer.amenities)
            assertEquals(HotelOffer.Availability.AVAILABLE, offer.availability)
        }
    }

    @Test
    fun `manifest records one successful call without sort retries or alternate payloads`() {
        val manifest = HotelsApiJson.codec.parseToJsonElement(
            fixture("fixture-manifest.json"),
        ).jsonObject

        assertTrue(manifest.getValue("sanitized").jsonPrimitive.boolean)
        assertFalse(manifest.getValue("automaticRetries").jsonPrimitive.boolean)
        assertFalse(manifest.getValue("alternatePayloadsSent").jsonPrimitive.boolean)

        val request = manifest.getValue("requestContract").jsonObject
        assertFalse("sort" in request)
        assertEquals(
            listOf("price", "stars", "review_rating", "free_cancellation_allowed"),
            request.getValue("filters").jsonArray.map {
                it.jsonObject.getValue("filterId").jsonPrimitive.content
            },
        )

        val call = manifest.getValue("call").jsonObject
        assertEquals(200, call.getValue("status").jsonPrimitive.int)
        assertEquals(1, call.getValue("attemptCount").jsonPrimitive.int)

        val observation = manifest.getValue("observation").jsonObject
        assertEquals(20, observation.getValue("originalHotelsCount").jsonPrimitive.int)
        assertEquals(2, observation.getValue("sanitizedHotelsCount").jsonPrimitive.int)
        assertEquals(0, observation.getValue("missingReviewCount").jsonPrimitive.int)
        assertEquals(
            0,
            observation.getValue("missingFreeCancellationUntilCount").jsonPrimitive.int,
        )
    }

    private fun fixture(name: String): String =
        requireNotNull(
            javaClass.getResource("/fixtures/hotels-api/stage-12-1c/$name"),
        ) {
            "Fixture not found: $name"
        }.readText()
}
