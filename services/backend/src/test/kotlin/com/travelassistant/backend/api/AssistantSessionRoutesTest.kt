package com.travelassistant.backend.api

import com.travelassistant.backend.module
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
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

    @Test
    fun acceptAssistantMessageReturnsIntakeMetadata() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/assistant/sessions/assistant-session-local-000001/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"I want a hotel in Rome for two adults next weekend"}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val receivedAt = body["receivedAt"]?.jsonPrimitive?.content.orEmpty()

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("assistant-session-local-000001", body["sessionId"]?.jsonPrimitive?.content)
        assertEquals("collecting_requirements", body["status"]?.jsonPrimitive?.content)
        assertTrue(receivedAt.isNotBlank())
        Instant.parse(receivedAt)
    }

    @Test
    fun blankAssistantMessageReturnsValidationError() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/assistant/sessions/assistant-session-local-000001/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"message":"   "}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("VALIDATION_ERROR", body["code"]?.jsonPrimitive?.content)
        assertEquals("Request validation failed.", body["message"]?.jsonPrimitive?.content)
        assertEquals("message", body["details"]?.jsonObject?.get("field")?.jsonPrimitive?.content)
    }

    @Test
    fun missingAssistantMessageReturnsValidationError() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/assistant/sessions/assistant-session-local-000001/messages") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("VALIDATION_ERROR", body["code"]?.jsonPrimitive?.content)
        assertEquals("Request validation failed.", body["message"]?.jsonPrimitive?.content)
        assertEquals("message", body["details"]?.jsonObject?.get("field")?.jsonPrimitive?.content)
    }

    @Test
    fun missingAssistantMessageBodyReturnsValidationError() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/assistant/sessions/assistant-session-local-000001/messages")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("VALIDATION_ERROR", body["code"]?.jsonPrimitive?.content)
        assertEquals("Request validation failed.", body["message"]?.jsonPrimitive?.content)
        assertEquals("message", body["details"]?.jsonObject?.get("field")?.jsonPrimitive?.content)
    }
}
