package com.travelassistant.backend.infrastructure.provider

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HotelsApiImageTemplateFixtureContractTest {

    @Test
    fun `provider image template becomes a safe bounded card image`() {
        val response = HotelsApiJson.codec.decodeFromString<HotelsApiSearchResponseDto>(
            fixture("search-image-template.json"),
        )
        val sourceImage = response.payload.hotels.single().images?.single()

        assertEquals(
            "https://extranet-cdn.tinkoff.ru/b2b/extranet/{size}/1001/image-fixture.jpg",
            sourceImage,
        )

        val offer = assertIs<HotelsApiSearchResponseMapper.Result.Mapped>(
            HotelsApiSearchResponseMapper.map(response),
        ).offers.single()

        assertEquals(
            "https://extranet-cdn.tinkoff.ru/b2b/extranet/1024x768/1001/image-fixture.jpg",
            offer.imageUrl,
        )
    }

    @Test
    fun `fixture manifest records sanitized bounded observation`() {
        val manifest = HotelsApiJson.codec.parseToJsonElement(
            fixture("fixture-manifest.json"),
        ).jsonObject

        assertTrue(manifest.getValue("sanitized").jsonPrimitive.boolean)
        assertEquals(
            "1024x768",
            manifest.getValue("observation").jsonObject
                .getValue("resolvedTemplate").jsonObject
                .getValue("size").jsonPrimitive.content,
        )
    }

    private fun fixture(name: String): String =
        requireNotNull(
            javaClass.getResource("/fixtures/hotels-api/stage-14-6/$name"),
        ) {
            "Fixture not found: $name"
        }.readText()
}
