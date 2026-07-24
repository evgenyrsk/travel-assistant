package com.travelassistant.backend.api

import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.infrastructure.observability.JsonOperationalEventSink
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RequestCorrelationAndHttpEventsTest {

    @Test
    fun `reuses safe request id in response error body and structured log`() = testApplication {
        val events = mutableListOf<OperationalEvent>()
        val jsonLines = mutableListOf<String>()
        application {
            testOperationalModule(events, jsonLines)
        }

        val response = client.get("/missing") {
            header(REQUEST_ID_HEADER, SAFE_REQUEST_ID)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(SAFE_REQUEST_ID, response.headers[REQUEST_ID_HEADER])
        assertEquals(SAFE_REQUEST_ID, response.errorRequestId())
        assertEquals(
            SAFE_REQUEST_ID,
            events.single { it.name == OperationalEventName.HTTP_REQUEST_COMPLETED }.requestId,
        )
        assertTrue(jsonLines.any { line -> line.contains("\"request_id\":\"$SAFE_REQUEST_ID\"") })
    }

    @Test
    fun `generates request id when header is absent`() = testApplication {
        val events = mutableListOf<OperationalEvent>()
        application {
            testOperationalModule(events)
        }

        val response = client.get("/missing")
        val requestId = checkNotNull(response.headers[REQUEST_ID_HEADER])

        assertTrue(isSafeRequestId(requestId))
        assertEquals(requestId, response.errorRequestId())
        assertEquals(
            requestId,
            events.single { it.name == OperationalEventName.HTTP_REQUEST_COMPLETED }.requestId,
        )
    }

    @Test
    fun `replaces malformed and oversized request ids`() = testApplication {
        application {
            testOperationalModule(mutableListOf())
        }

        listOf("contains whitespace", "x".repeat(129)).forEach { unsafeRequestId ->
            val response = client.get("/missing") {
                header(REQUEST_ID_HEADER, unsafeRequestId)
            }
            val replacement = checkNotNull(response.headers[REQUEST_ID_HEADER])

            assertNotEquals(unsafeRequestId, replacement)
            assertTrue(isSafeRequestId(replacement))
            assertEquals(replacement, response.errorRequestId())
        }
    }

    @Test
    fun `records fixed operation and expected status outcomes without raw paths`() = testApplication {
        val events = mutableListOf<OperationalEvent>()
        application {
            testOperationalModule(events)
        }

        assertEquals(HttpStatusCode.OK, client.get("/ok").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/status/400").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/missing/private-value").status)
        assertEquals(HttpStatusCode.InternalServerError, client.get("/boom").status)
        assertEquals(HttpStatusCode.BadGateway, client.get("/status/502").status)
        assertEquals(HttpStatusCode.ServiceUnavailable, client.get("/status/503").status)

        val httpEvents = events.filter { it.name == OperationalEventName.HTTP_REQUEST_COMPLETED }
        assertEquals(listOf(200, 400, 404, 500, 502, 503), httpEvents.map { it.statusCode })
        assertTrue(httpEvents.all { it.operation?.wireValue == "unmatched" })
        assertTrue(httpEvents.all { (it.durationMillis ?: -1) >= 0 })
        assertTrue(events.any { it.name == OperationalEventName.UNEXPECTED_ERROR })
        assertTrue(events.none { event -> event.toString().contains("private-value") })
    }

    private fun io.ktor.server.application.Application.testOperationalModule(
        events: MutableList<OperationalEvent>,
        jsonLines: MutableList<String>? = null,
    ) {
        val collectingSink = OperationalEventSink { event -> events += event }
        val eventSink = if (jsonLines == null) {
            collectingSink
        } else {
            OperationalEventSink { event ->
                collectingSink.record(event)
                JsonOperationalEventSink(
                    clock = FIXED_CLOCK,
                    writeLine = jsonLines::add,
                ).record(event)
            }
        }

        configureRequestCorrelation()
        configureOperationalHttpEvents(eventSink)
        configureSerialization()
        configureErrorHandling(eventSink)
        routing {
            get("/ok") {
                call.respondText("ok")
            }
            get("/status/{code}") {
                val status = HttpStatusCode.fromValue(
                    checkNotNull(call.parameters["code"]).toInt(),
                )
                call.respond(
                    status,
                    ErrorResponse(
                        code = ErrorCode.INTERNAL_ERROR,
                        message = "Safe test error.",
                        requestId = call.requestId(),
                    ),
                )
            }
            get("/boom") {
                error("raw-sensitive-exception-message")
            }
        }
    }

    private suspend fun io.ktor.client.statement.HttpResponse.errorRequestId(): String =
        ApiJson.parseToJsonElement(bodyAsText())
            .jsonObject
            .getValue("requestId")
            .jsonPrimitive
            .content

    private companion object {
        const val SAFE_REQUEST_ID = "req_Stage15.safe-123"
        val FIXED_CLOCK: Clock =
            Clock.fixed(Instant.parse("2026-07-24T08:00:00Z"), ZoneOffset.UTC)
    }
}
