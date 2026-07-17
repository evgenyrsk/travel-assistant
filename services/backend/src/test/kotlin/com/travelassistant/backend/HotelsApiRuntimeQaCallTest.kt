package com.travelassistant.backend

import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.infrastructure.llm.FakeLlmClient
import com.travelassistant.backend.infrastructure.provider.HotelProviderConfig
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class HotelsApiRuntimeQaCallTest {

    @Test
    fun `runs real runtime smoke only with explicit opt in`() {
        val environment = System.getenv()
        if (environment[ENABLED_KEY] != "true") {
            return
        }

        val destination = environment[DESTINATION_KEY]?.trim().orEmpty()
            .ifEmpty { DEFAULT_DESTINATION }
        val checkInDate = environment[CHECK_IN_KEY]?.trim().orEmpty()
            .ifEmpty { DEFAULT_CHECK_IN }
        val checkOutDate = environment[CHECK_OUT_KEY]?.trim().orEmpty()
            .ifEmpty { DEFAULT_CHECK_OUT }
        val providerEnvironment = environment.toMutableMap().apply {
            this["HOTEL_PROVIDER_MODE"] = "REAL"
        }

        testApplication {
            application {
                moduleWithAssistantLlm(
                    llmClient = FakeLlmClient(LlmClientResponse.Empty),
                    providerConfig = HotelProviderConfig.fromEnvironment(providerEnvironment),
                )
            }

            val sessionResponse = client.post("/api/v1/assistant/sessions")
            val sessionBody = Json.parseToJsonElement(sessionResponse.bodyAsText()).jsonObject
            val sessionId = sessionBody["session"]
                ?.jsonObject
                ?.get("sessionId")
                ?.jsonPrimitive
                ?.content
                .orEmpty()
            val searchResponse = client.post("/api/v1/hotel-searches") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(searchRequest(sessionId, destination, checkInDate, checkOutDate))
            }
            val searchBody = Json.parseToJsonElement(searchResponse.bodyAsText()).jsonObject
            if (searchResponse.status != HttpStatusCode.Accepted) {
                val field = searchBody["fields"]?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("field")?.jsonPrimitive?.content
                fail(
                    "REAL runtime smoke failed safely: " +
                        "status=${searchResponse.status.value}, " +
                        "code=${searchBody["code"]?.jsonPrimitive?.content ?: "none"}, " +
                        "field=${field ?: "none"}",
                )
            }

            val searchId = searchBody["searchId"]?.jsonPrimitive?.content.orEmpty()
            assertTrue(searchId.isNotBlank())
            val offersResponse = client.get("/api/v1/hotel-searches/$searchId/offers")
            val offersBody = Json.parseToJsonElement(offersResponse.bodyAsText()).jsonObject
            val offerCount = offersBody["offers"]?.jsonArray?.size ?: 0

            assertEquals(HttpStatusCode.OK, offersResponse.status)
            println(
                "STAGE_9_18_SAFE_RESULT " +
                    "searchStatus=${searchResponse.status.value} " +
                    "searchState=${searchBody["status"]?.jsonPrimitive?.content} " +
                    "offerCount=$offerCount",
            )
        }
    }

    private fun searchRequest(
        sessionId: String,
        destination: String,
        checkInDate: String,
        checkOutDate: String,
    ): String =
        buildJsonObject {
            put("sessionId", sessionId)
            putJsonObject("criteria") {
                put("destination", destination)
                put("checkInDate", checkInDate)
                put("checkOutDate", checkOutDate)
                putJsonObject("guests") {
                    put("adults", 2)
                    put("children", 0)
                }
                put("rooms", 1)
            }
        }.toString()

    private companion object {
        const val ENABLED_KEY = "HOTELS_API_RUNTIME_QA_ENABLED"
        const val DESTINATION_KEY = "HOTELS_API_RUNTIME_QA_DESTINATION"
        const val CHECK_IN_KEY = "HOTELS_API_RUNTIME_QA_CHECKIN_DATE"
        const val CHECK_OUT_KEY = "HOTELS_API_RUNTIME_QA_CHECKOUT_DATE"
        const val DEFAULT_DESTINATION = "Иннополис"
        const val DEFAULT_CHECK_IN = "2026-08-10"
        const val DEFAULT_CHECK_OUT = "2026-08-14"
    }
}
