package com.travelassistant.backend.infrastructure.provider

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HotelsApiFilterRequestFixtureContractTest {

    @Test
    fun `availability endpoint accepts selected filter shapes but returns no availability facts`() {
        val root = fixtureRoot("filter-availability-empty.json")

        assertTrue(root.getValue("payload").jsonObject.isEmpty())
    }

    @Test
    fun `filtered search fixture preserves sorting rejection`() {
        val error = fixtureRoot("filtered-search-sorting-not-allowed.json")
            .getValue("error")
            .jsonObject

        assertEquals(
            "sorting_is_not_allowed_yet",
            error.getValue("code").jsonPrimitive.content,
        )
    }

    @Test
    fun `manifest records exact filters and two calls without retries`() {
        val manifest = fixtureRoot("fixture-manifest.json")

        assertTrue(manifest.getValue("sanitized").jsonPrimitive.boolean)
        assertFalse(manifest.getValue("automaticRetries").jsonPrimitive.boolean)
        assertFalse(manifest.getValue("alternatePayloadsSent").jsonPrimitive.boolean)

        val request = manifest.getValue("requestContract").jsonObject
        val filters = request.getValue("filters").jsonArray
        assertEquals(
            listOf("price", "stars", "review_rating", "free_cancellation_allowed"),
            filters.map { it.jsonObject.getValue("filterId").jsonPrimitive.content },
        )

        assertRangeFilter(filters[0].jsonObject)
        assertStarsFilter(filters[1].jsonObject)
        assertRatingFilter(filters[2].jsonObject)
        assertCancellationFilter(filters[3].jsonObject)

        val sort = request.getValue("sort").jsonObject
        assertEquals("price", sort.getValue("field").jsonPrimitive.content)
        assertEquals("asc", sort.getValue("order").jsonPrimitive.content)

        val calls = manifest.getValue("calls").jsonArray
        assertEquals(2, calls.size)
        assertEquals(listOf(200, 400), calls.map { it.jsonObject.getValue("status").jsonPrimitive.int })
        assertTrue(calls.all { it.jsonObject.getValue("attemptCount").jsonPrimitive.int == 1 })
    }

    private fun assertRangeFilter(filter: JsonObject) {
        assertEquals("range", filter.objectType())
        assertEquals(0.0, filter.getValue("min").jsonPrimitive.double)
        assertEquals(80000.0, filter.getValue("max").jsonPrimitive.double)
    }

    private fun assertStarsFilter(filter: JsonObject) {
        assertEquals("array", filter.objectType())
        assertEquals(
            listOf("4", "5"),
            filter.getValue("values").jsonArray.map { it.jsonPrimitive.content },
        )
    }

    private fun assertRatingFilter(filter: JsonObject) {
        assertEquals("radio", filter.objectType())
        assertEquals("8", filter.getValue("value").jsonPrimitive.content)
    }

    private fun assertCancellationFilter(filter: JsonObject) {
        assertEquals("boolean", filter.objectType())
        assertTrue(filter.getValue("value").jsonPrimitive.boolean)
    }

    private fun JsonObject.objectType(): String =
        getValue("\$objectType").jsonPrimitive.content

    private fun fixtureRoot(name: String): JsonObject =
        HotelsApiJson.codec.parseToJsonElement(
            requireNotNull(
                javaClass.getResource("/fixtures/hotels-api/stage-12-1b/$name"),
            ) {
                "Fixture not found: $name"
            }.readText(),
        ).jsonObject
}
