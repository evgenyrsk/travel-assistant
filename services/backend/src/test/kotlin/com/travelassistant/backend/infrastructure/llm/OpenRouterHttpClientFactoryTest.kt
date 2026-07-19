package com.travelassistant.backend.infrastructure.llm

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OpenRouterHttpClientFactoryTest {

    @Test
    fun `policy does not follow redirects or add authorization globally`() = runBlocking {
        var requestCount = 0
        var authorizationHeader: String? = null
        val client = HttpClient(
            MockEngine { request ->
                requestCount += 1
                authorizationHeader = request.headers[HttpHeaders.Authorization]
                respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(HttpHeaders.Location, "https://unexpected.test/target"),
                )
            },
        ) {
            applyOpenRouterHttpClientPolicy()
        }

        val response = client.get("https://openrouter.test/start")

        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals(1, requestCount)
        assertNull(authorizationHeader)
        client.close()
    }
}
