package com.travelassistant.backend.infrastructure.observability

import com.travelassistant.backend.application.observability.OperationalComponent
import com.travelassistant.backend.application.observability.OperationalError
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalLevel
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.application.observability.OperationalOutcome
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonOperationalEventSinkTest {

    @Test
    fun `writes the versioned JSON Lines schema with allowed context`() {
        val lines = mutableListOf<String>()
        val sink = JsonOperationalEventSink(
            clock = FIXED_CLOCK,
            writeLine = lines::add,
        )

        sink.record(
            OperationalEvent(
                name = OperationalEventName.HOTEL_SEARCH_COMPLETED,
                component = OperationalComponent.HOTEL_SEARCH,
                level = OperationalLevel.WARNING,
                requestId = "req-1",
                sessionId = "session-1",
                hotelSearchId = "search-1",
                operation = OperationalOperation.CREATE_HOTEL_SEARCH,
                outcome = OperationalOutcome.NO_OFFERS,
                durationMillis = 12,
                offerCount = 0,
                analyzedCount = 10,
                deepAnalyzedCount = 4,
                matchCount = 2,
                probableCount = 3,
            ),
        )

        val json = Json.parseToJsonElement(lines.single()).jsonObject
        assertEquals("1", json.getValue("schema_version").jsonPrimitive.content)
        assertEquals("2026-07-24T08:00:00Z", json.getValue("timestamp").jsonPrimitive.content)
        assertEquals("warning", json.getValue("level").jsonPrimitive.content)
        assertEquals("travel-assistant-backend", json.getValue("service").jsonPrimitive.content)
        assertEquals("0.1.0", json.getValue("version").jsonPrimitive.content)
        assertEquals("hotel.search.completed", json.getValue("event").jsonPrimitive.content)
        assertEquals("session-1", json.getValue("session_id").jsonPrimitive.content)
        assertEquals("search-1", json.getValue("hotel_search_id").jsonPrimitive.content)
        assertEquals("no_offers", json.getValue("outcome").jsonPrimitive.content)
        assertEquals("10", json.getValue("analyzed_count").jsonPrimitive.content)
        assertEquals("4", json.getValue("deep_analyzed_count").jsonPrimitive.content)
        assertEquals("2", json.getValue("match_count").jsonPrimitive.content)
        assertEquals("3", json.getValue("probable_count").jsonPrimitive.content)
    }

    @Test
    fun `does not serialize exception messages or sensitive fixtures`() {
        val lines = mutableListOf<String>()
        val sink = JsonOperationalEventSink(
            clock = FIXED_CLOCK,
            writeLine = lines::add,
        )
        val failure = IllegalStateException(
            "secret-token destination=Sensitive Hotel offerId=provider-42 model=private-model",
            IllegalArgumentException("raw provider body"),
        )

        sink.record(
            OperationalEvent(
                name = OperationalEventName.UNEXPECTED_ERROR,
                component = OperationalComponent.HTTP,
                level = OperationalLevel.ERROR,
                requestId = "req-safe",
                error = OperationalError.from(failure),
            ),
        )

        val line = lines.single()
        listOf(
            "secret-token",
            "Sensitive Hotel",
            "offerId",
            "provider-42",
            "private-model",
            "raw provider body",
        ).forEach { sensitiveFixture ->
            assertFalse(line.contains(sensitiveFixture))
        }
        assertTrue(line.contains("java.lang.IllegalStateException"))
        assertTrue(line.contains("java.lang.IllegalArgumentException"))
    }

    @Test
    fun `keeps concurrent writes isolated as complete JSON lines`() {
        val lines = Collections.synchronizedList(mutableListOf<String>())
        val sink = JsonOperationalEventSink(
            clock = FIXED_CLOCK,
            writeLine = lines::add,
        )
        val executor = Executors.newFixedThreadPool(8)

        repeat(100) { index ->
            executor.submit {
                sink.record(
                    OperationalEvent(
                        name = OperationalEventName.ASSISTANT_TURN_COMPLETED,
                        component = OperationalComponent.ASSISTANT,
                        requestId = "req-$index",
                        outcome = OperationalOutcome.CLARIFICATION,
                    ),
                )
            }
        }
        executor.shutdown()
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))

        assertEquals(100, lines.size)
        assertEquals(
            100,
            lines.map { line ->
                Json.parseToJsonElement(line).jsonObject
                    .getValue("request_id")
                    .jsonPrimitive
                    .content
            }.toSet().size,
        )
    }

    @Test
    fun `logger failures never escape into the caller flow`() {
        val sink = JsonOperationalEventSink(
            clock = FIXED_CLOCK,
            writeLine = { throw IllegalStateException("stdout unavailable") },
        )

        sink.record(
            OperationalEvent(
                name = OperationalEventName.SERVICE_LIFECYCLE,
                component = OperationalComponent.SERVICE,
                outcome = OperationalOutcome.STARTED,
            ),
        )
    }

    private companion object {
        val FIXED_CLOCK: Clock =
            Clock.fixed(Instant.parse("2026-07-24T08:00:00Z"), ZoneOffset.UTC)
    }
}
