package com.travelassistant.backend.infrastructure.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HotelsApiSafeImageUrlPolicyTest {

    @Test
    fun `resolves confirmed provider size placeholder`() {
        assertEquals(
            "https://extranet-cdn.tinkoff.ru/b2b/extranet/1024x768/1001/image.jpg",
            HotelsApiSafeImageUrlPolicy.firstOrNull(
                listOf(
                    "https://extranet-cdn.tinkoff.ru/b2b/extranet/{size}/1001/image.jpg",
                ),
            ),
        )
    }

    @Test
    fun `does not resolve provider placeholder on another host`() {
        assertNull(
            HotelsApiSafeImageUrlPolicy.firstOrNull(
                listOf("https://images.example.test/{size}/image.jpg"),
            ),
        )
    }

    @Test
    fun `rejects repeated or unknown placeholders`() {
        assertNull(
            HotelsApiSafeImageUrlPolicy.firstOrNull(
                listOf(
                    "https://extranet-cdn.tinkoff.ru/{size}/{size}/image.jpg",
                    "https://extranet-cdn.tinkoff.ru/{width}/image.jpg",
                ),
            ),
        )
    }

    @Test
    fun `keeps complete safe https image unchanged`() {
        val imageUrl = "https://images.example.test/hotel/image.jpg"

        assertEquals(
            imageUrl,
            HotelsApiSafeImageUrlPolicy.firstOrNull(listOf(imageUrl)),
        )
    }
}
