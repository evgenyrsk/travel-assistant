package com.travelassistant.backend

import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationHostTest {
    @Test
    fun `uses loopback when backend host is not explicitly configured`() {
        assertEquals(DEFAULT_BACKEND_HOST, resolveBackendHost(emptyMap()))
        assertEquals(DEFAULT_BACKEND_HOST, resolveBackendHost(mapOf("HOST" to "  ")))
    }

    @Test
    fun `allows an explicit backend host for non-demo deployments`() {
        assertEquals("0.0.0.0", resolveBackendHost(mapOf("HOST" to " 0.0.0.0 ")))
    }
}
