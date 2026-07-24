package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelDetailsProviderResult
import com.travelassistant.backend.application.hotel.HotelLocationResolutionRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HotelsApiHotelDetailsProviderAdapterTest {

    @Test
    fun `loads fixture through safe GET and maps provider-neutral details`() = runBlocking {
        var capturedRequest: HttpRequestData? = null
        val client = client { request ->
            capturedRequest = request
            fixture("hotel-details-success.json")
        }
        val adapter = adapter(
            client = client,
            language = HotelLocationResolutionRequest.Language.RU,
        )

        val result = assertIs<HotelDetailsProviderResult.Loaded>(
            adapter.load("hotel-example-001"),
        )

        assertEquals("Отель Пример", result.details.hotelName)
        assertEquals(HttpMethod.Get, capturedRequest?.method)
        assertEquals(
            "/api/v1/hotels/hotel-example-001",
            capturedRequest?.url?.encodedPath,
        )
        assertEquals("RU", capturedRequest?.headers?.get("X-User-Language"))
        assertNull(capturedRequest?.headers?.get(HttpHeaders.Authorization))
        assertNull(capturedRequest?.headers?.get(HttpHeaders.Cookie))
        assertNull(capturedRequest?.headers?.get(HttpHeaders.ContentType))
        client.close()
    }

    @Test
    fun `encodes opaque provider reference as one path segment`() = runBlocking {
        val providerReference = "opaque/provider id"
        var capturedRequest: HttpRequestData? = null
        val client = client { request ->
            capturedRequest = request
            minimalResponse(providerReference)
        }

        val result = adapter(client).load(providerReference)

        assertIs<HotelDetailsProviderResult.Loaded>(result)
        assertEquals(
            "/api/v1/hotels/opaque%2Fprovider%20id",
            capturedRequest?.url?.encodedPath,
        )
        client.close()
    }

    @Test
    fun `rejects blank provider reference without HTTP call`() = runBlocking {
        var requestCount = 0
        val client = client {
            requestCount++
            error("HTTP must not be called")
        }

        val result = adapter(client).load(" ")

        assertEquals(
            HotelDetailsProviderResult.ResponseRejected(
                HotelDetailsProviderResult.ResponseRejectionReason.INVALID_PROVIDER_REFERENCE,
            ),
            result,
        )
        assertEquals(0, requestCount)
        client.close()
    }

    @Test
    fun `maps not found response rejection and provider failures to typed outcomes`() {
        val cases = listOf(
            HttpStatusCode.NotFound to HotelDetailsProviderResult.NotFound,
            HttpStatusCode.RequestTimeout to HotelDetailsProviderResult.ProviderUnavailable(
                HotelDetailsProviderResult.UnavailableReason.TIMEOUT,
            ),
            HttpStatusCode.TooManyRequests to HotelDetailsProviderResult.ProviderUnavailable(
                HotelDetailsProviderResult.UnavailableReason.RATE_LIMITED,
            ),
            HttpStatusCode.Unauthorized to HotelDetailsProviderResult.ProviderUnavailable(
                HotelDetailsProviderResult.UnavailableReason.AUTHENTICATION_FAILED,
            ),
            HttpStatusCode.ServiceUnavailable to HotelDetailsProviderResult.ProviderUnavailable(
                HotelDetailsProviderResult.UnavailableReason.UNAVAILABLE,
            ),
            HttpStatusCode.BadRequest to HotelDetailsProviderResult.ProviderUnavailable(
                HotelDetailsProviderResult.UnavailableReason.UNKNOWN,
            ),
        )

        cases.forEach { (status, expected) ->
            val sensitiveBody = "provider-sensitive-$status"
            val client = client(status = status) { sensitiveBody }

            val result = runBlocking { adapter(client).load("hotel-1") }

            assertEquals(expected, result)
            assertTrue(result.toString().contains(sensitiveBody).not())
            client.close()
        }
    }

    @Test
    fun `rejects malformed mismatched and invalid mapped responses safely`() {
        val responses = listOf(
            "provider-sensitive-invalid-json" to
                HotelDetailsProviderResult.ResponseRejectionReason.INVALID_PAYLOAD,
            minimalResponse("different-hotel") to
                HotelDetailsProviderResult.ResponseRejectionReason.INVALID_PROVIDER_REFERENCE,
            minimalResponse("hotel-1", hotelName = " ") to
                HotelDetailsProviderResult.ResponseRejectionReason.INVALID_HOTEL_DATA,
            minimalResponse("hotel-1", address = " ") to
                HotelDetailsProviderResult.ResponseRejectionReason.INVALID_LOCATION_DATA,
        )

        responses.forEach { (body, expectedReason) ->
            val client = client { body }

            val result = runBlocking { adapter(client).load("hotel-1") }

            assertEquals(HotelDetailsProviderResult.ResponseRejected(expectedReason), result)
            assertTrue(result.toString().contains(body).not())
            client.close()
        }
    }

    @Test
    fun `propagates coroutine cancellation`() {
        val client = HttpClient(
            MockEngine { throw CancellationException("cancelled") },
        ) {
            install(HttpTimeout)
        }

        assertFailsWith<CancellationException> {
            runBlocking { adapter(client).load("hotel-1") }
        }
        client.close()
    }

    private fun adapter(
        client: HttpClient,
        language: HotelLocationResolutionRequest.Language? = null,
    ): HotelsApiHotelDetailsProviderAdapter =
        HotelsApiHotelDetailsProviderAdapter(
            transport = PublicHotelsApiHttpTransport(
                httpClient = client,
                publicTarget = HotelsApiTargetConfig.public(
                    baseUrl = "https://hotels.test/",
                    timeoutMillis = 5_000,
                ),
            ),
            language = language,
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

    private fun minimalResponse(
        hotelId: String,
        hotelName: String = "Тестовый отель",
        address: String? = null,
    ): String =
        """
        {
          "payload": {
            "hotelId": ${HotelsApiJson.codec.encodeToString(hotelId)},
            "hotelName": ${HotelsApiJson.codec.encodeToString(hotelName)}${
                address?.let {
                    ",\"hotelLocation\":{\"address\":${HotelsApiJson.codec.encodeToString(it)}}"
                }.orEmpty()
            }
          }
        }
        """.trimIndent()

    private fun fixture(name: String): String =
        requireNotNull(
            javaClass.getResource("/fixtures/hotels-api/stage-13-1/$name"),
        ) {
            "Fixture not found: $name"
        }.readText()
}
