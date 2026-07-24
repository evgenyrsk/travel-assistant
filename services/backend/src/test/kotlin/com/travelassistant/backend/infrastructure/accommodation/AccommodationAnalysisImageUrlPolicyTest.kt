package com.travelassistant.backend.infrastructure.accommodation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccommodationAnalysisImageUrlPolicyTest {
    private val policy = AccommodationAnalysisImageUrlPolicy(setOf("images.example.test"))

    @Test
    fun `allows only exact HTTPS host without credentials query fragment or port`() {
        val allowed = "https://images.example.test/hotel/photo.jpg"
        assertEquals(allowed, policy.allowedOrNull(allowed))
        assertNull(policy.allowedOrNull("http://images.example.test/hotel/photo.jpg"))
        assertNull(policy.allowedOrNull("https://other.example.test/hotel/photo.jpg"))
        assertNull(policy.allowedOrNull("https://sub.images.example.test/hotel/photo.jpg"))
        assertNull(policy.allowedOrNull("https://user@images.example.test/hotel/photo.jpg"))
        assertNull(policy.allowedOrNull("https://images.example.test/hotel/photo.jpg?size=640"))
        assertNull(policy.allowedOrNull("https://images.example.test/hotel/photo.jpg#fragment"))
        assertNull(policy.allowedOrNull("https://images.example.test:443/hotel/photo.jpg"))
    }
}
