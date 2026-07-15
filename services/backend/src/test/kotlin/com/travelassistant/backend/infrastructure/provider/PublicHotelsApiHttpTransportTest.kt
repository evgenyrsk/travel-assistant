package com.travelassistant.backend.infrastructure.provider

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class PublicHotelsApiHttpTransportTest {

    @Test
    fun `posts unchanged JSON to configured public host without authorization`() = runBlocking {
        var capturedRequest: HttpRequestData? = null
        val client = HttpClient(
            MockEngine { request ->
                capturedRequest = request
                respond(
                    content = """{"status":"ok"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(HttpTimeout)
        }
        val transport = transport(client)
        val requestBody = """{"destinationId":"rome"}"""

        val response = transport.postJson(
            path = "/api/v1/hotels/search",
            body = requestBody,
            userLanguage = "ru",
        )

        assertEquals(200, response.statusCode)
        assertEquals(ContentType.Application.Json.toString(), response.contentType)
        assertEquals("""{"status":"ok"}""", response.body)
        assertEquals("https://public-hotels.test/api/v1/hotels/search", capturedRequest?.url.toString())
        assertEquals(ContentType.Application.Json.toString(), capturedRequest?.headers?.get(HttpHeaders.Accept))
        assertEquals("ru", capturedRequest?.headers?.get("X-User-Language"))
        assertNull(capturedRequest?.headers?.get(HttpHeaders.Authorization))
        val content = assertIs<TextContent>(capturedRequest?.body)
        assertEquals(ContentType.Application.Json, content.contentType)
        assertEquals(requestBody, content.text)

        client.close()
    }

    @Test
    fun `keeps absent response content type as null`() = runBlocking {
        val client = HttpClient(MockEngine { respondOk() }) {
            install(HttpTimeout)
        }

        val response = transport(client).postJson("/api/v1/hotels/search", "{}")

        assertNull(response.contentType)
        client.close()
    }

    @Test
    fun `omits optional user language and authorization headers`() = runBlocking {
        var capturedRequest: HttpRequestData? = null
        val client = HttpClient(
            MockEngine { request ->
                capturedRequest = request
                respondOk()
            },
        ) {
            install(HttpTimeout)
        }

        transport(client).postJson("/api/v1/hotels/search", "{}")

        assertNull(capturedRequest?.headers?.get("X-User-Language"))
        assertNull(capturedRequest?.headers?.get(HttpHeaders.Authorization))
        client.close()
    }

    @Test
    fun `rejects paths that can escape the configured public host`() {
        val client = HttpClient(MockEngine { respondOk() }) {
            install(HttpTimeout)
        }
        val transport = transport(client)
        val invalidPaths = listOf(
            "https://private-hotels.test/api/v1/hotels/search",
            "//private-hotels.test/api/v1/hotels/search",
            "/api/v1/hotels/search?target=private",
            "/api/v1/hotels/search#fragment",
            "api/v1/hotels/search",
        )

        invalidPaths.forEach { path ->
            val error = assertFailsWith<HotelProviderException> {
                runBlocking { transport.postJson(path, "{}") }
            }

            assertEquals(HotelProviderErrorCategory.UNKNOWN, error.category)
        }
        client.close()
    }

    @Test
    fun `maps unsuccessful HTTP statuses to safe provider categories`() {
        val cases = mapOf(
            HttpStatusCode.Unauthorized to HotelProviderErrorCategory.AUTHENTICATION_FAILED,
            HttpStatusCode.Forbidden to HotelProviderErrorCategory.AUTHENTICATION_FAILED,
            HttpStatusCode.RequestTimeout to HotelProviderErrorCategory.TIMEOUT,
            HttpStatusCode.TooManyRequests to HotelProviderErrorCategory.RATE_LIMITED,
            HttpStatusCode.InternalServerError to HotelProviderErrorCategory.UNAVAILABLE,
            HttpStatusCode.BadRequest to HotelProviderErrorCategory.UNKNOWN,
        )

        cases.forEach { (status, expectedCategory) ->
            val responseBody = "sensitive-provider-body-$status"
            val client = HttpClient(
                MockEngine {
                    respond(content = responseBody, status = status)
                },
            ) {
                install(HttpTimeout)
            }

            val error = assertFailsWith<HotelProviderException> {
                runBlocking {
                    transport(client).postJson("/api/v1/hotels/search", "{}")
                }
            }

            assertEquals(expectedCategory, error.category)
            assertFalse(error.message.orEmpty().contains(responseBody))
            client.close()
        }
    }

    @Test
    fun `maps request timeout to safe timeout category`() {
        val client = HttpClient(
            MockEngine {
                delay(100)
                respondOk()
            },
        ) {
            install(HttpTimeout)
        }
        val transport = PublicHotelsApiHttpTransport(
            httpClient = client,
            publicTarget = HotelsApiTargetConfig.public(
                baseUrl = "https://public-hotels.test/",
                timeoutMillis = 10,
            ),
        )

        val error = assertFailsWith<HotelProviderException> {
            runBlocking { transport.postJson("/api/v1/hotels/search", "{}") }
        }

        assertEquals(HotelProviderErrorCategory.TIMEOUT, error.category)
        client.close()
    }

    @Test
    fun `maps network failure without leaking cause details`() {
        val sensitiveCauseText = "network-secret-details"
        val client = HttpClient(
            MockEngine {
                throw IOException(sensitiveCauseText)
            },
        ) {
            install(HttpTimeout)
        }

        val error = assertFailsWith<HotelProviderException> {
            runBlocking {
                transport(client).postJson("/api/v1/hotels/search", "{}")
            }
        }

        assertEquals(HotelProviderErrorCategory.UNAVAILABLE, error.category)
        assertFalse(error.message.orEmpty().contains(sensitiveCauseText))
        client.close()
    }

    private fun transport(client: HttpClient): PublicHotelsApiHttpTransport =
        PublicHotelsApiHttpTransport(
            httpClient = client,
            publicTarget = HotelsApiTargetConfig.public(
                baseUrl = "https://public-hotels.test/",
                timeoutMillis = 5_000,
            ),
        )
}
