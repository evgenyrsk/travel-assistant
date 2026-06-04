package com.travelassistant.backend.api

import com.travelassistant.backend.module
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssistantSessionRoutesTest {

    @Test
    fun createAssistantSessionReturnsCreatedSessionMetadata() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/assistant/sessions")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val createdAt = body["createdAt"]?.jsonPrimitive?.content.orEmpty()

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("assistant-session-local-000001", body["sessionId"]?.jsonPrimitive?.content)
        assertEquals("collecting_requirements", body["status"]?.jsonPrimitive?.content)
        assertTrue(createdAt.isNotBlank())
        Instant.parse(createdAt)
    }
}
