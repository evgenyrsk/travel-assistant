package com.travelassistant.backend.api

import com.travelassistant.backend.module
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRoutesTest {

    @Test
    fun healthReturnsOk() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/v1/health")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok", body["status"]?.jsonPrimitive?.content)
        assertEquals("travel-assistant-backend", body["service"]?.jsonPrimitive?.content)
        assertEquals("0.1.0", body["version"]?.jsonPrimitive?.content)
    }
}
