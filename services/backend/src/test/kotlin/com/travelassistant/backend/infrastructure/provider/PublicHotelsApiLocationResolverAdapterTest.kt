package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelLocationResolutionRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PublicHotelsApiLocationResolverAdapterTest {

    @Test
    fun `posts public input contract and maps every location without hotel identifiers`() =
        runBlocking {
            var capturedRequest: HttpRequestData? = null
            val client = client { request ->
                capturedRequest = request
                fixture("autocomplete-success.json")
            }
            val resolver = resolver(client)

            val resolution = resolver.resolve(
                HotelLocationResolutionRequest(
                    query = "Казань",
                    language = HotelLocationResolutionRequest.Language.RU,
                ),
            )

            assertEquals(
                "https://hotels.test/search-api/search/autocomplete",
                capturedRequest?.url.toString(),
            )
            assertEquals("RU", capturedRequest?.headers?.get("X-User-Language"))
            assertNull(capturedRequest?.headers?.get(HttpHeaders.Authorization))
            val body = HotelsApiJson.codec.parseToJsonElement(
                assertIs<TextContent>(capturedRequest?.body).text,
            ).jsonObject
            assertEquals(setOf("input"), body.keys)
            assertEquals("Казань", body.getValue("input").jsonPrimitive.content)

            assertEquals(
                listOf(1001, 1002, 1003, 1004, 1005),
                resolution.candidates.map { it.destinationId },
            )
            assertEquals(
                listOf("city", "airport", "railway_station", "city", "railway_station"),
                resolution.candidates.map { it.type.code },
            )
            assertEquals("Тестовая локация 1", resolution.candidates.first().name)
            assertEquals("Тестовая подпись локации 1", resolution.candidates.first().signature)

            client.close()
        }

    @Test
    fun `keeps absent language header and empty location result`() = runBlocking {
        var capturedRequest: HttpRequestData? = null
        val client = client { request ->
            capturedRequest = request
            """
                {
                  "payload": {
                    "hotels": [
                      {
                        "id": "9999",
                        "name": "Отель не является направлением",
                        "signature": "Подпись отеля",
                        "type": {
                          "code": "hotel",
                          "name": "Отель"
                        }
                      }
                    ]
                  }
                }
            """.trimIndent()
        }
        val resolver = resolver(client)

        val resolution = resolver.resolve(
            HotelLocationResolutionRequest(query = "Неизвестное направление"),
        )

        assertTrue(resolution.candidates.isEmpty())
        assertNull(capturedRequest?.headers?.get("X-User-Language"))
        assertNull(capturedRequest?.headers?.get(HttpHeaders.Authorization))
        client.close()
    }

    @Test
    fun `maps malformed response to safe invalid response error`() = runBlocking {
        val sensitiveBody = "provider-sensitive-autocomplete-body"
        val client = client { sensitiveBody }
        val resolver = resolver(client)

        val error = assertFailsWith<HotelProviderException> {
            resolver.resolve(HotelLocationResolutionRequest(query = "Казань"))
        }

        assertEquals(HotelProviderErrorCategory.INVALID_RESPONSE, error.category)
        assertEquals("Hotels API autocomplete response is invalid", error.message)
        assertNull(error.cause)
        assertTrue(error.message.orEmpty().contains(sensitiveBody).not())
        client.close()
    }

    @Test
    fun `preserves safe transport failure category`() = runBlocking {
        val client = client(
            status = HttpStatusCode.TooManyRequests,
        ) { """{"error":"provider-sensitive-rate-limit"}""" }
        val resolver = resolver(client)

        val error = assertFailsWith<HotelProviderException> {
            resolver.resolve(HotelLocationResolutionRequest(query = "Казань"))
        }

        assertEquals(HotelProviderErrorCategory.RATE_LIMITED, error.category)
        assertEquals("Hotels API rate limit was reached", error.message)
        client.close()
    }

    private fun resolver(client: HttpClient): PublicHotelsApiLocationResolverAdapter =
        PublicHotelsApiLocationResolverAdapter(
            transport = PublicHotelsApiHttpTransport(
                httpClient = client,
                publicTarget = HotelsApiTargetConfig.public(
                    baseUrl = "https://hotels.test/",
                    timeoutMillis = 5_000,
                ),
            ),
        )

    private fun client(
        status: HttpStatusCode = HttpStatusCode.OK,
        responseBody: (HttpRequestData) -> String,
    ): HttpClient =
        HttpClient(
            MockEngine { request ->
                respond(
                    content = responseBody(request),
                    status = status,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString(),
                    ),
                )
            },
        ) {
            install(HttpTimeout)
        }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/hotels-api/$name")) {
            "Fixture not found: $name"
        }.readText()
}
