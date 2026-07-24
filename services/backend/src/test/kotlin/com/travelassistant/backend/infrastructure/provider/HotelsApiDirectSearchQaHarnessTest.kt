package com.travelassistant.backend.infrastructure.provider

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class HotelsApiDirectSearchQaHarnessTest {

    @Test
    fun `does not create a client without explicit opt in`() = runBlocking {
        var clientCreated = false
        val harness = HotelsApiDirectSearchQaHarness(
            environment = completeEnvironment() - HotelsApiDirectSearchQaHarness.ENABLED_KEY,
            clientFactory = {
                clientCreated = true
                error("Client must not be created")
            },
        )

        val result = harness.run()

        assertIs<HotelsApiDirectSearchQaHarness.Result.Disabled>(result)
        assertFalse(clientCreated)
    }

    @Test
    fun `rejects missing or invalid input before creating a client`() = runBlocking {
        val cases = listOf(
            mapOf(HotelsApiDirectSearchQaHarness.ENABLED_KEY to "true") to
                HotelsApiDirectSearchQaHarness.Issue.INVALID_DESTINATION_ID,
            mapOf(
                HotelsApiDirectSearchQaHarness.ENABLED_KEY to "true",
                HotelsApiDirectSearchQaHarness.DESTINATION_ID_KEY to "17039",
            ) to HotelsApiDirectSearchQaHarness.Issue.INVALID_CHECK_IN_DATE,
            completeEnvironment() - HotelsApiDirectSearchQaHarness.CHECK_OUT_DATE_KEY to
                HotelsApiDirectSearchQaHarness.Issue.INVALID_CHECK_OUT_DATE,
            completeEnvironment().toMutableMap().apply {
                this[HotelsApiDirectSearchQaHarness.CHECK_OUT_DATE_KEY] = "2026-08-09"
            } to HotelsApiDirectSearchQaHarness.Issue.INVALID_DATE_RANGE,
        )

        cases.forEach { (environment, expectedIssue) ->
            var clientCreated = false
            val result = HotelsApiDirectSearchQaHarness(
                environment = environment,
                clientFactory = {
                    clientCreated = true
                    error("Client must not be created")
                },
            ).run()

            assertEquals(
                expectedIssue,
                assertIs<HotelsApiDirectSearchQaHarness.Result.Rejected>(result).issue,
            )
            assertFalse(clientCreated)
        }
    }

    @Test
    fun `performs one mock request and returns only safe summary`() = runBlocking {
        var requestCount = 0
        var capturedRequest: HttpRequestData? = null
        val client = HttpClient(
            MockEngine { request ->
                requestCount += 1
                capturedRequest = request
                respond(
                    content = fixture("search-success.json"),
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString(),
                    ),
                )
            },
        ) {
            applyHotelsApiQaPolicy()
        }
        val result = assertIs<HotelsApiDirectSearchQaHarness.Result.Success>(
            HotelsApiDirectSearchQaHarness(
                environment = completeEnvironment(),
                clientFactory = { client },
            ).run(),
        )

        assertEquals(1, requestCount)
        assertEquals(200, result.statusCode)
        assertEquals(ContentType.Application.Json.toString(), result.contentType)
        assertEquals(20, result.hotelCount)
        assertEquals(20, result.offerCount)
        assertFalse(result.isLoadingCompleted)
        assertEquals(true, result.hasNextOffset)
        assertEquals(
            "https://hotels.tbank.ru/api/v1/hotels/search",
            capturedRequest?.url.toString(),
        )
        assertNull(capturedRequest?.headers?.get(HttpHeaders.Authorization))

        val body = HotelsApiJson.codec.parseToJsonElement(
            assertIs<TextContent>(capturedRequest?.body).text,
        ).jsonObject
        assertEquals(
            "17039",
            body.getValue("destinationId").jsonPrimitive.content,
        )
        assertEquals(
            "2026-08-10",
            body.getValue("checkinDate").jsonPrimitive.content,
        )
        assertEquals(
            "2026-08-14",
            body.getValue("checkoutDate").jsonPrimitive.content,
        )
        assertEquals("0", body.getValue("offset").jsonPrimitive.content)
        assertEquals("20", body.getValue("limit").jsonPrimitive.content)
        assertEquals(
            "2",
            body.getValue("guests").jsonArray.single().jsonObject
                .getValue("adultsCount").jsonPrimitive.content,
        )
    }

    @Test
    fun `rejects unexpected content type without exposing the body`() = runBlocking {
        val sensitiveBody = "provider-sensitive-body"
        val client = HttpClient(
            MockEngine {
                respond(
                    content = sensitiveBody,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString()),
                )
            },
        ) {
            applyHotelsApiQaPolicy()
        }

        val result = HotelsApiDirectSearchQaHarness(
            environment = completeEnvironment(),
            clientFactory = { client },
        ).run()

        assertEquals(
            HotelsApiDirectSearchQaHarness.Issue.UNEXPECTED_CONTENT_TYPE,
            assertIs<HotelsApiDirectSearchQaHarness.Result.Rejected>(result).issue,
        )
        assertFalse(result.toString().contains(sensitiveBody))
    }

    private fun completeEnvironment(): Map<String, String> =
        mapOf(
            HotelsApiDirectSearchQaHarness.ENABLED_KEY to "true",
            HotelsApiDirectSearchQaHarness.DESTINATION_ID_KEY to "17039",
            HotelsApiDirectSearchQaHarness.CHECK_IN_DATE_KEY to "2026-08-10",
            HotelsApiDirectSearchQaHarness.CHECK_OUT_DATE_KEY to "2026-08-14",
        )

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/hotels-api/$name")) {
            "Fixture not found: $name"
        }.readText()
}
