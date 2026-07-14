package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelLocationResolution
import com.travelassistant.backend.application.hotel.HotelLocationResolutionRequest
import com.travelassistant.backend.application.hotel.HotelLocationResolverBoundary
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
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
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class HotelsApiSearchOrchestratorTest {

    @Test
    fun `resolves one location performs one search and maps offers`() = runBlocking {
        var resolverRequest: HotelLocationResolutionRequest? = null
        var capturedRequest: HttpRequestData? = null
        var requestCount = 0
        val client = client { request ->
            requestCount += 1
            capturedRequest = request
            searchResponse(
                isLoadingCompleted = false,
                nextOffset = 50,
            )
        }
        val orchestrator = orchestrator(
            client = client,
            resolver = HotelLocationResolverBoundary { request ->
                resolverRequest = request
                HotelLocationResolution(candidates = listOf(location(77)))
            },
        )

        val result = assertIs<HotelsApiSearchOrchestrator.Result.Success>(
            orchestrator.search(
                HotelsApiSearchOrchestrator.Request(
                    criteria = criteria(childrenAges = listOf(0, 17)),
                    language = HotelLocationResolutionRequest.Language.RU,
                ),
            ),
        )

        assertEquals("Казань", resolverRequest?.query)
        assertEquals(HotelLocationResolutionRequest.Language.RU, resolverRequest?.language)
        assertEquals(1, requestCount)
        assertEquals("https://hotels.test/api/v1/hotels/search", capturedRequest?.url.toString())
        assertEquals("RU", capturedRequest?.headers?.get("X-User-Language"))
        assertNull(capturedRequest?.headers?.get(HttpHeaders.Authorization))

        val body = HotelsApiJson.codec.parseToJsonElement(
            assertIs<TextContent>(capturedRequest?.body).text,
        ).jsonObject
        assertEquals(77, body.getValue("destinationId").jsonPrimitive.content.toInt())
        assertEquals("2026-07-18", body.getValue("checkinDate").jsonPrimitive.content)
        assertEquals("2026-07-19", body.getValue("checkoutDate").jsonPrimitive.content)
        assertEquals(0, body.getValue("offset").jsonPrimitive.content.toInt())
        assertEquals(20, body.getValue("limit").jsonPrimitive.content.toInt())
        assertEquals(
            listOf("0", "17"),
            body.getValue("guests").jsonArray.single().jsonObject
                .getValue("childrenAge").jsonArray.map { it.jsonPrimitive.content },
        )
        assertEquals(77, result.location.destinationId)
        assertEquals(listOf("hotel-1"), result.offers.map { it.providerReference })

        client.close()
    }

    @Test
    fun `keeps first twenty unique candidates from one provider response`() = runBlocking {
        var requestCount = 0
        val hotelIds = listOf("hotel-1", "hotel-1") + (2..25).map { "hotel-$it" }
        val client = client {
            requestCount += 1
            searchResponse(
                hotelIds = hotelIds,
                isLoadingCompleted = false,
                nextOffset = 50,
            )
        }
        val orchestrator = orchestrator(
            client = client,
            resolver = HotelLocationResolverBoundary {
                HotelLocationResolution(candidates = listOf(location(77)))
            },
        )

        val result = assertIs<HotelsApiSearchOrchestrator.Result.Success>(
            orchestrator.search(
                HotelsApiSearchOrchestrator.Request(criteria = criteria()),
            ),
        )

        assertEquals(1, requestCount)
        assertEquals(20, result.offers.size)
        assertEquals(
            (1..20).map { "hotel-$it" },
            result.offers.map { it.providerReference },
        )
        client.close()
    }

    @Test
    fun `does not call search when location is absent or ambiguous`() = runBlocking {
        listOf(
            emptyList(),
            listOf(location(1), location(2)),
        ).forEach { candidates ->
            var requestCount = 0
            val client = client {
                requestCount += 1
                searchResponse()
            }
            val orchestrator = orchestrator(
                client = client,
                resolver = HotelLocationResolverBoundary {
                    HotelLocationResolution(candidates = candidates)
                },
            )

            val result = orchestrator.search(
                HotelsApiSearchOrchestrator.Request(criteria = criteria()),
            )

            if (candidates.isEmpty()) {
                assertIs<HotelsApiSearchOrchestrator.Result.LocationNotFound>(result)
            } else {
                val selection = assertIs<
                    HotelsApiSearchOrchestrator.Result.LocationSelectionRequired,
                >(result)
                assertEquals(candidates, selection.candidates)
            }
            assertEquals(0, requestCount)
            client.close()
        }
    }

    @Test
    fun `does not call search when request mapping is rejected`() = runBlocking {
        var requestCount = 0
        val client = client {
            requestCount += 1
            searchResponse()
        }
        val orchestrator = orchestrator(
            client = client,
            resolver = HotelLocationResolverBoundary {
                HotelLocationResolution(candidates = listOf(location(77)))
            },
        )

        val result = assertIs<HotelsApiSearchOrchestrator.Result.RequestRejected>(
            orchestrator.search(
                HotelsApiSearchOrchestrator.Request(criteria = criteria(rooms = 2)),
            ),
        )

        assertEquals(HotelsApiSearchMappingError.Issue.INVALID_ROOM_COUNT, result.error.issue)
        assertEquals(0, requestCount)
        client.close()
    }

    @Test
    fun `returns typed response mapping rejection without pagination retry`() = runBlocking {
        var requestCount = 0
        val client = client {
            requestCount += 1
            searchResponse(priceAmount = -1.0, isLoadingCompleted = false, nextOffset = 50)
        }
        val orchestrator = orchestrator(
            client = client,
            resolver = HotelLocationResolverBoundary {
                HotelLocationResolution(candidates = listOf(location(77)))
            },
        )

        val result = assertIs<HotelsApiSearchOrchestrator.Result.ResponseRejected>(
            orchestrator.search(
                HotelsApiSearchOrchestrator.Request(criteria = criteria()),
            ),
        )

        assertEquals(HotelsApiSearchMappingError.Issue.INVALID_PRICE, result.errors.single().issue)
        assertEquals(1, requestCount)
        client.close()
    }

    @Test
    fun `maps malformed JSON to safe invalid response error`() = runBlocking {
        val sensitiveBody = "provider-sensitive-invalid-body"
        val client = client { sensitiveBody }
        val orchestrator = orchestrator(
            client = client,
            resolver = HotelLocationResolverBoundary {
                HotelLocationResolution(candidates = listOf(location(77)))
            },
        )

        val error = assertFailsWith<HotelProviderException> {
            orchestrator.search(
                HotelsApiSearchOrchestrator.Request(criteria = criteria()),
            )
        }

        assertEquals(HotelProviderErrorCategory.INVALID_RESPONSE, error.category)
        assertEquals("Hotels API response is invalid", error.message)
        assertNull(error.cause)
        client.close()
    }

    private fun orchestrator(
        client: HttpClient,
        resolver: HotelLocationResolverBoundary,
    ): HotelsApiSearchOrchestrator =
        HotelsApiSearchOrchestrator(
            locationResolver = resolver,
            transport = PublicHotelsApiHttpTransport(
                httpClient = client,
                publicTarget = HotelsApiTargetConfig.public(
                    baseUrl = "https://hotels.test/",
                    timeoutMillis = 5_000,
                ),
            ),
        )

    private fun client(responseBody: (HttpRequestData) -> String): HttpClient =
        HttpClient(
            MockEngine { request ->
                respond(
                    content = responseBody(request),
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString(),
                    ),
                )
            },
        ) {
            install(HttpTimeout)
        }

    private fun location(destinationId: Int): HotelLocationResolution.Candidate =
        HotelLocationResolution.Candidate(
            destinationId = destinationId,
            name = "Казань",
            signature = "Казань, Россия",
            type = HotelLocationResolution.Type(code = "city", name = "Город"),
        )

    private fun criteria(
        childrenAges: List<Int> = emptyList(),
        rooms: Int? = 1,
    ): HotelSearchCriteria =
        HotelSearchCriteria(
            destination = "Казань",
            checkInDate = LocalDate.parse("2026-07-18"),
            checkOutDate = LocalDate.parse("2026-07-19"),
            guests = HotelSearchCriteria.Guests(
                adults = 2,
                childrenAges = childrenAges,
            ),
            rooms = rooms,
        )

    private fun searchResponse(
        priceAmount: Double = 12_000.0,
        isLoadingCompleted: Boolean = true,
        nextOffset: Int? = null,
        hotelIds: List<String> = listOf("hotel-1"),
    ): String {
        val nextOffsetField = nextOffset?.let { ",\"nextOffset\":$it" }.orEmpty()
        val hotels = hotelIds.mapIndexed { index, hotelId ->
            hotelJson(
                hotelId = hotelId,
                hotelIndex = index,
                priceAmount = priceAmount,
            )
        }.joinToString(separator = ",")
        return """
            {
              "payload": {
                "filteredHotelsCount": ${hotelIds.size},
                "hotels": [$hotels],
                "hotelsTotalCount": ${hotelIds.size},
                "isLoadingCompleted": $isLoadingCompleted$nextOffsetField
              }
            }
        """.trimIndent()
    }

    private fun hotelJson(
        hotelId: String,
        hotelIndex: Int,
        priceAmount: Double,
    ): String =
        """
            {
              "hotelId": "$hotelId",
              "hotelName": "Тестовый отель $hotelIndex",
              "starRating": 4,
              "areaLocation": {
                "countryName": "Россия",
                "destinationId": 77,
                "destinationName": "Казань",
                "signature": "Казань, Россия"
              },
              "hotelLocation": {"address": "Тестовая улица, $hotelIndex"},
              "rateForHotelsFeed": {
                "availableRoomsCount": 1,
                "isCreditCardDataRequired": false,
                "paymentPlace": "online",
                "shownPrice": {"amount": $priceAmount, "currency": "RUB"}
              },
              "review": {"rating": 8.7, "ratingsCount": 42}
            }
        """.trimIndent()
}
