package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.application.observability.OperationalComponent
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalLevel
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.application.observability.recordSafely
import com.travelassistant.backend.domain.hotel.AccommodationAnalysisMetadata
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchId
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class SemanticHotelSearchScheduler(
    private val stateStore: HotelSearchStateStore,
    private val semanticJob: SemanticHotelSearchJob,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val budget: Duration = DEFAULT_SEMANTIC_BUDGET,
    private val eventSink: OperationalEventSink = OperationalEventSink.NONE,
) : SemanticHotelSearchLauncher, AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val jobs = ConcurrentHashMap<HotelSearchId, Job>()

    override fun launch(
        search: HotelSearch,
        command: CreateHotelSearchCommand,
    ): Boolean {
        if (search.status != HotelSearch.Status.SEARCHING) {
            return false
        }
        val startedAt = System.nanoTime()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                var forcedOutcome: OperationalOutcome? = null
                val result = try {
                    withTimeout(budget.toMillis()) {
                        semanticJob.execute(search.id, command)
                    }
                } catch (_: TimeoutCancellationException) {
                    forcedOutcome = OperationalOutcome.TIMEOUT
                    SemanticHotelSearchJobResult.Failed
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    SemanticHotelSearchJobResult.Failed
                }
                val transition = applyResult(search.id, result)
                if (transition is HotelSearchStateTransitionResult.Updated) {
                    recordTerminalOutcome(
                        search = transition.search,
                        durationMillis = elapsedMillis(startedAt),
                        forcedOutcome = forcedOutcome,
                    )
                }
            } finally {
                jobs.remove(search.id)
            }
        }
        val existing = jobs.putIfAbsent(search.id, job)
        if (existing != null) {
            job.cancel()
            return false
        }
        job.start()
        return true
    }

    private fun applyResult(
        searchId: HotelSearchId,
        result: SemanticHotelSearchJobResult,
    ): HotelSearchStateTransitionResult =
        stateStore.updateIfStatus(
            searchId = searchId,
            expectedStatus = HotelSearch.Status.SEARCHING,
        ) { current ->
            when (result) {
                is SemanticHotelSearchJobResult.Completed -> current.copy(
                    status = result.status,
                    offers = result.offers,
                    analysis = result.analysis,
                )

                SemanticHotelSearchJobResult.Failed -> current.copy(
                    status = HotelSearch.Status.FAILED,
                    offers = emptyList(),
                    analysis = AccommodationAnalysisMetadata.failed(),
                )
            }
        }

    private fun recordTerminalOutcome(
        search: HotelSearch,
        durationMillis: Long,
        forcedOutcome: OperationalOutcome?,
    ) {
        val outcome = forcedOutcome ?: when (search.status) {
            HotelSearch.Status.COMPLETED_WITH_OFFERS ->
                if (search.analysis?.status == AccommodationAnalysisMetadata.Status.PARTIAL) {
                    OperationalOutcome.PARTIAL
                } else {
                    OperationalOutcome.RESULTS
                }
            HotelSearch.Status.COMPLETED_NO_OFFERS -> OperationalOutcome.NO_OFFERS
            HotelSearch.Status.COMPLETED_NO_SEMANTIC_MATCHES ->
                OperationalOutcome.NO_SEMANTIC_MATCHES
            HotelSearch.Status.FAILED -> OperationalOutcome.FAILED
            HotelSearch.Status.SEARCHING -> OperationalOutcome.UNKNOWN
        }
        val metadata = search.analysis
        eventSink.recordSafely(
            OperationalEvent(
                name = OperationalEventName.HOTEL_SEARCH_COMPLETED,
                component = OperationalComponent.HOTEL_SEARCH,
                level = when (outcome) {
                    OperationalOutcome.FAILED,
                    OperationalOutcome.TIMEOUT,
                    -> OperationalLevel.ERROR
                    OperationalOutcome.PARTIAL -> OperationalLevel.WARNING
                    else -> OperationalLevel.INFO
                },
                sessionId = search.sessionId.value,
                hotelSearchId = search.id.value,
                operation = OperationalOperation.SEMANTIC_HOTEL_SEARCH,
                outcome = outcome,
                durationMillis = durationMillis,
                offerCount = search.offers.size,
                analyzedCount = metadata?.analyzedCount,
                deepAnalyzedCount = metadata?.deepAnalyzedCount,
                matchCount = metadata?.matchCount,
                probableCount = metadata?.probableCount,
            ),
        )
    }

    private fun elapsedMillis(startedAtNanos: Long): Long =
        max(0, (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND)

    override fun close() {
        scope.cancel("Semantic hotel search scheduler is stopping")
        jobs.clear()
    }

    internal fun activeJobCount(): Int = jobs.size

    companion object {
        val DEFAULT_SEMANTIC_BUDGET: Duration = Duration.ofSeconds(45)
        private const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
