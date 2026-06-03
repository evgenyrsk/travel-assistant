package com.travelassistant.backend.api

import com.travelassistant.backend.module
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceholderRoutesTest {

    @Test
    fun hotelSearchPlaceholderReturnsStructuredNotImplementedError() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/v1/hotel-searches")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.NotImplemented, response.status)
        assertEquals("NOT_IMPLEMENTED", body["code"]?.jsonPrimitive?.content)
        assertEquals(
            "This hotel-only MVP backend boundary is a Stage 7.2 placeholder.",
            body["message"]?.jsonPrimitive?.content,
        )
        assertEquals(
            "hotel.search.create",
            body["details"]?.jsonObject?.get("boundary")?.jsonPrimitive?.content,
        )
    }

    @Test
    fun unknownRouteReturnsStructuredNotFoundError() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/v1/unknown")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("NOT_FOUND", body["code"]?.jsonPrimitive?.content)
        assertEquals("Requested backend route was not found.", body["message"]?.jsonPrimitive?.content)
    }
}
