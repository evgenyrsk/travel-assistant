package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.AccommodationAnalysisMetadata
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchId
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
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
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val result = try {
                    withTimeout(budget.toMillis()) {
                        semanticJob.execute(search.id, command)
                    }
                } catch (_: TimeoutCancellationException) {
                    SemanticHotelSearchJobResult.Failed
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    SemanticHotelSearchJobResult.Failed
                }
                applyResult(search.id, result)
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

    override fun close() {
        scope.cancel("Semantic hotel search scheduler is stopping")
        jobs.clear()
    }

    internal fun activeJobCount(): Int = jobs.size

    companion object {
        val DEFAULT_SEMANTIC_BUDGET: Duration = Duration.ofSeconds(45)
    }
}
