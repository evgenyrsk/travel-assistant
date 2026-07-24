package com.travelassistant.backend.infrastructure.provider

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HotelsApiQaHttpClientPolicyTest {

    @Test
    fun `test scoped CIO client can be created without a request`() {
        val client = HttpClient(CIO) {
            applyHotelsApiQaPolicy()
        }

        client.close()
    }

    @Test
    fun `does not follow redirects`() = runBlocking {
        var requestCount = 0
        val client = HttpClient(
            MockEngine {
                requestCount += 1
                respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(
                        HttpHeaders.Location,
                        "https://redirected-hotels.test/api/v1/hotels/search",
                    ),
                )
            },
        ) {
            applyHotelsApiQaPolicy()
        }
        val transport = PublicHotelsApiHttpTransport(
            httpClient = client,
            publicTarget = HotelsApiTargetConfig.public(
                baseUrl = "https://public-hotels.test/",
                timeoutMillis = 5_000,
            ),
        )

        val error = assertFailsWith<HotelProviderException> {
            transport.postJson("/api/v1/hotels/search", "{}")
        }

        assertEquals(HotelProviderErrorCategory.UNKNOWN, error.category)
        assertEquals(1, requestCount)
        client.close()
    }
}
