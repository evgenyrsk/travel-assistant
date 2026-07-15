package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.domain.hotel.HotelOffer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HotelsApiProviderFixtureContractTest {

    @Test
    fun `autocomplete fixture preserves provider identifier types and maps only locations`() {
        val response = HotelsApiJson.codec.decodeFromString<HotelsApiAutocompleteResponseDto>(
            fixture("autocomplete-success.json"),
        )
        val locations = assertNotNull(response.payload.locations)
        val hotels = assertNotNull(response.payload.hotels)

        assertTrue(locations.isNotEmpty())
        assertTrue(hotels.isNotEmpty())
        assertTrue(
            locations.all {
                it.id > 0 && it.type.code.isNotBlank() && it.type.name.isNotBlank()
            },
        )
        assertTrue(
            hotels.all {
                it.id.isNotBlank() && it.type.code.isNotBlank() && it.type.name.isNotBlank()
            },
        )

        val resolution = HotelsApiAutocompleteLocationMapper.map(response)

        assertEquals(locations.map { it.id }, resolution.candidates.map { it.destinationId })
        assertEquals(locations.map { it.type.code }, resolution.candidates.map { it.type.code })
        assertTrue(
            hotels.none { hotel ->
                resolution.candidates.any { candidate -> hotel.id == candidate.destinationId.toString() }
            },
        )
    }

    @Test
    fun `search fixture maps provider facts without pagination or invented values`() {
        val response = HotelsApiJson.codec.decodeFromString<HotelsApiSearchResponseDto>(
            fixture("search-success.json"),
        )

        assertFalse(response.payload.isLoadingCompleted)
        assertNotNull(response.payload.nextOffset)

        val offers = assertIs<HotelsApiSearchResponseMapper.Result.Mapped>(
            HotelsApiSearchResponseMapper.map(response),
        ).offers
        val hotelsById = response.payload.hotels.associateBy { it.hotelId }

        assertEquals(response.payload.hotels.size, offers.size)
        offers.forEach { offer ->
            val hotel = assertNotNull(hotelsById[offer.providerReference])
            val review = assertNotNull(hotel.review)

            assertEquals(hotel.rateForHotelsFeed.shownPrice.amount, offer.totalPrice)
            assertEquals(hotel.rateForHotelsFeed.shownPrice.currency, offer.currency)
            assertEquals(review.rating, offer.rating)
            assertEquals(review.ratingsCount, offer.reviewCount)
            assertNull(offer.amenities)
            assertNotEquals(HotelOffer.Availability.LIMITED, offer.availability)
        }

        val differentRatings = response.payload.hotels.first { hotel ->
            hotel.review?.rating != hotel.starRating.toDouble()
        }
        val mappedOffer = offers.single { it.providerReference == differentRatings.hotelId }
        assertEquals(differentRatings.review?.rating, mappedOffer.rating)
        assertNotEquals(differentRatings.starRating.toDouble(), mappedOffer.rating)
    }

    @Test
    fun `invalid rooms fixture preserves typed provider error`() {
        val root = HotelsApiJson.codec.parseToJsonElement(
            fixture("search-invalid-rooms.json"),
        ).jsonObject
        val error = root.getValue("error").jsonObject

        assertEquals("invalid_rooms_count", error.getValue("code").jsonPrimitive.content)
        assertIs<JsonObject>(error.getValue("details"))
    }

    @Test
    fun `fixture manifest confirms sanitized provider observations`() {
        val manifest = HotelsApiJson.codec.parseToJsonElement(
            fixture("fixture-manifest.json"),
        ).jsonObject

        assertTrue(manifest.getValue("sanitized").jsonPrimitive.boolean)
        assertEquals(3, manifest.getValue("fixtures").jsonArray.size)
    }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/hotels-api/$name")) {
            "Fixture not found: $name"
        }.readText()
}
