package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.AccommodationAnalysisMetadata
import com.travelassistant.backend.domain.hotel.AccommodationConcept
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import java.time.Duration
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SemanticHotelSearchSchedulerTest {

    @Test
    fun `launches one job and atomically stores terminal result`() = runBlocking {
        val store = InMemoryHotelSearchStateStore()
        val search = store.save(search())
        val started = CompletableDeferred<Unit>()
        val result = CompletableDeferred<SemanticHotelSearchJobResult>()
        val events = mutableListOf<OperationalEvent>()
        var executionCount = 0
        val scheduler = SemanticHotelSearchScheduler(
            stateStore = store,
            semanticJob = SemanticHotelSearchJob { _, _ ->
                executionCount += 1
                started.complete(Unit)
                result.await()
            },
            eventSink = OperationalEventSink(events::add),
        )

        assertTrue(scheduler.launch(search, command()))
        started.await()
        assertFalse(scheduler.launch(search, command()))
        result.complete(
            SemanticHotelSearchJobResult.Completed(
                status = HotelSearch.Status.COMPLETED_NO_SEMANTIC_MATCHES,
                offers = emptyList(),
                analysis = completedAnalysis(),
            ),
        )
        awaitStatus(store, HotelSearch.Status.COMPLETED_NO_SEMANTIC_MATCHES)
        awaitIdle(scheduler)

        assertEquals(1, executionCount)
        assertEquals(0, scheduler.activeJobCount())
        val event = events.single()
        assertEquals(OperationalEventName.HOTEL_SEARCH_COMPLETED, event.name)
        assertEquals(OperationalOperation.SEMANTIC_HOTEL_SEARCH, event.operation)
        assertEquals(OperationalOutcome.NO_SEMANTIC_MATCHES, event.outcome)
        assertEquals(2, event.analyzedCount)
        assertEquals("semantic-search-000001", event.hotelSearchId)
        scheduler.close()
    }

    @Test
    fun `timeout moves search to failed without retry`() = runBlocking {
        val store = InMemoryHotelSearchStateStore()
        val search = store.save(search())
        var executionCount = 0
        val events = mutableListOf<OperationalEvent>()
        val scheduler = SemanticHotelSearchScheduler(
            stateStore = store,
            semanticJob = SemanticHotelSearchJob { _, _ ->
                executionCount += 1
                awaitCancellation()
            },
            budget = Duration.ofMillis(20),
            eventSink = OperationalEventSink(events::add),
        )

        assertTrue(scheduler.launch(search, command()))
        awaitStatus(store, HotelSearch.Status.FAILED)
        awaitIdle(scheduler)

        assertEquals(1, executionCount)
        assertEquals(AccommodationAnalysisMetadata.Status.FAILED, store.current().analysis?.status)
        assertEquals(OperationalOutcome.TIMEOUT, events.single().outcome)
        scheduler.close()
    }

    @Test
    fun `shutdown cancels active job without publishing a late terminal state`() = runBlocking {
        val store = InMemoryHotelSearchStateStore()
        val search = store.save(search())
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val scheduler = SemanticHotelSearchScheduler(
            stateStore = store,
            semanticJob = SemanticHotelSearchJob { _, _ ->
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            },
        )

        assertTrue(scheduler.launch(search, command()))
        started.await()
        scheduler.close()
        withTimeout(1_000) { cancelled.await() }

        assertEquals(HotelSearch.Status.SEARCHING, store.current().status)
    }

    @Test
    fun `late job result cannot overwrite an existing terminal state`() = runBlocking {
        val store = InMemoryHotelSearchStateStore()
        val search = store.save(search())
        val result = CompletableDeferred<SemanticHotelSearchJobResult>()
        val scheduler = SemanticHotelSearchScheduler(
            stateStore = store,
            semanticJob = SemanticHotelSearchJob { _, _ -> result.await() },
        )
        assertTrue(scheduler.launch(search, command()))
        store.updateIfStatus(search.id, HotelSearch.Status.SEARCHING) { current ->
            current.copy(
                status = HotelSearch.Status.FAILED,
                analysis = AccommodationAnalysisMetadata.failed(),
            )
        }
        result.complete(
            SemanticHotelSearchJobResult.Completed(
                HotelSearch.Status.COMPLETED_NO_SEMANTIC_MATCHES,
                emptyList(),
                completedAnalysis(),
            ),
        )
        withTimeout(1_000) {
            while (scheduler.activeJobCount() != 0) yield()
        }

        assertEquals(HotelSearch.Status.FAILED, store.current().status)
        scheduler.close()
    }

    private suspend fun awaitStatus(
        store: InMemoryHotelSearchStateStore,
        status: HotelSearch.Status,
    ) {
        withTimeout(1_000) {
            while (store.current().status != status) yield()
        }
    }

    private suspend fun awaitIdle(scheduler: SemanticHotelSearchScheduler) {
        withTimeout(1_000) {
            while (scheduler.activeJobCount() != 0) yield()
        }
    }

    private fun InMemoryHotelSearchStateStore.current(): HotelSearch =
        checkNotNull(findById(HotelSearchId("semantic-search-000001")))

    private fun completedAnalysis() = AccommodationAnalysisMetadata(
        status = AccommodationAnalysisMetadata.Status.COMPLETED,
        analyzedCount = 2,
        deepAnalyzedCount = 0,
        matchCount = 0,
        probableCount = 0,
    )

    private fun search() = HotelSearch(
        id = HotelSearchId("semantic-search-000001"),
        sessionId = AssistantSessionId("assistant-session-000001"),
        criteria = command().criteria,
        status = HotelSearch.Status.SEARCHING,
        offers = emptyList(),
        analysis = AccommodationAnalysisMetadata.searching(1_000),
    )

    private fun command() = CreateHotelSearchCommand(
        sessionId = AssistantSessionId("assistant-session-000001"),
        criteria = HotelSearchCriteria(
            destination = "Synthetic destination",
            checkInDate = LocalDate.parse("2026-08-10"),
            checkOutDate = LocalDate.parse("2026-08-14"),
            guests = HotelSearchCriteria.Guests(adults = 2),
            rooms = 1,
            preferences = HotelSearchPreferences(
                accommodationConcept = AccommodationConcept.GLAMPING,
            ),
        ),
    )
}
