package com.travelassistant.backend.infrastructure.provider

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HotelsApiSearchFilterCatalogFixtureContractTest {

    @Test
    fun `catalog fixture confirms selected filter shapes and rating contract drift`() {
        val payload = fixtureRoot("search-filters-catalog.json")
            .getValue("payload")
            .jsonObject
        val filters = payload.getValue("filters").jsonArray

        assertEquals("range", filter(filters, "price").objectType())

        val stars = filter(filters, "stars")
        assertEquals("array", stars.objectType())
        assertEquals(listOf("5", "4", "3", "2", "1", "0"), stars.stringValues())

        val rating = filter(filters, "review_rating")
        assertEquals("radio", rating.objectType())
        assertFalse(rating.objectType() == "range")
        assertEquals(listOf("9", "8", "7", "6", "5"), rating.stringValues())

        assertEquals(
            "boolean",
            filter(filters, "free_cancellation_allowed").objectType(),
        )

        val mealTypes = filter(filters, "meal_types")
        assertEquals("array", mealTypes.objectType())
        assertTrue("breakfast" in mealTypes.stringValues())

        val popularFilters = payload.getValue("popularFilters").jsonArray
        val popularRating = filter(popularFilters, "review_rating")
        assertEquals("radio", popularRating.objectType())
        assertEquals(listOf("8"), popularRating.stringValues())

        val popularCancellation = filter(popularFilters, "free_cancellation_allowed")
        assertEquals("boolean", popularCancellation.objectType())
        assertTrue(popularCancellation.getValue("value").jsonPrimitive.boolean)

        val popularMealTypes = filter(popularFilters, "meal_types")
        assertEquals("array", popularMealTypes.objectType())
        assertEquals(listOf("breakfast"), popularMealTypes.stringValues())
    }

    @Test
    fun `fixture manifest records one call and fail closed skipped calls`() {
        val manifest = fixtureRoot("fixture-manifest.json")

        assertTrue(manifest.getValue("sanitized").jsonPrimitive.boolean)
        assertTrue(manifest.getValue("stoppedOnContractDrift").jsonPrimitive.boolean)
        assertEquals(1, manifest.getValue("fixtures").jsonArray.size)

        val skippedCalls = manifest.getValue("skippedCalls").jsonArray
        assertEquals(2, skippedCalls.size)
        assertEquals(
            setOf(
                "/api/v1/hotels/search-filters-availability",
                "/api/v1/hotels/search",
            ),
            skippedCalls.map {
                it.jsonObject.getValue("path").jsonPrimitive.content
            }.toSet(),
        )
    }

    private fun filter(filters: JsonArray, filterId: String): JsonObject =
        assertNotNull(
            filters
                .map { it.jsonObject }
                .singleOrNull { it.getValue("filterId").jsonPrimitive.content == filterId },
        )

    private fun JsonObject.objectType(): String =
        getValue("\$objectType").jsonPrimitive.content

    private fun JsonObject.stringValues(): List<String> =
        getValue("values").jsonArray.map { it.jsonPrimitive.content }

    private fun fixtureRoot(name: String): JsonObject =
        HotelsApiJson.codec.parseToJsonElement(
            requireNotNull(
                javaClass.getResource("/fixtures/hotels-api/stage-12-1/$name"),
            ) {
                "Fixture not found: $name"
            }.readText(),
        ).jsonObject
}
