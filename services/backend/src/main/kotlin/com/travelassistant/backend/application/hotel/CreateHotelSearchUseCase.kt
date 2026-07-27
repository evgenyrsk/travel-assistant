package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.application.assistant.AssistantSessionNotFoundException
import com.travelassistant.backend.application.assistant.AssistantSessionStateStore
import com.travelassistant.backend.application.observability.OperationalComponent
import com.travelassistant.backend.application.observability.OperationalDependency
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalLevel
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.application.observability.recordSafely
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.domain.hotel.HotelOfferRanker
import com.travelassistant.backend.domain.hotel.AccommodationAnalysisMetadata
import kotlinx.coroutines.CancellationException
import kotlin.math.max

class CreateHotelSearchUseCase(
    private val assistantSessionStateStore: AssistantSessionStateStore,
    private val hotelOfferProvider: HotelOfferProviderBoundary,
    private val hotelOfferRanker: HotelOfferRanker = HotelOfferRanker(),
    private val hotelSearchStateStore: HotelSearchStateStore = InMemoryHotelSearchStateStore(),
    private val idGenerator: HotelSearchIdGenerator = LocalHotelSearchIdGenerator(),
    private val offerIdGenerator: HotelOfferIdGenerator = LocalHotelOfferIdGenerator(),
    private val semanticSearchLauncher: SemanticHotelSearchLauncher =
        SemanticHotelSearchLauncher.UNAVAILABLE,
    private val eventSink: OperationalEventSink = OperationalEventSink.NONE,
) : HotelSearchBoundary {

    override suspend fun createSearch(command: CreateHotelSearchCommand): CreateHotelSearchResult {
        val searchStartedAt = System.nanoTime()
        assistantSessionStateStore.findById(command.sessionId)
            ?: throw AssistantSessionNotFoundException(command.sessionId)

        if (command.criteria.preferences.accommodationConcept != null) {
            val result = createSemanticSearch(command)
            recordSearchOutcome(command, result, elapsedMillis(searchStartedAt))
            return result
        }

        val startedAt = System.nanoTime()
        val providerResult = try {
            hotelOfferProvider.search(command.criteria)
        } catch (error: CancellationException) {
            throw error
        } catch (error: RuntimeException) {
            recordProviderCall(
                sessionId = command.sessionId.value,
                outcome = OperationalOutcome.FAILED,
                level = OperationalLevel.ERROR,
                startedAt = startedAt,
            )
            throw error
        }
        recordProviderCall(
            sessionId = command.sessionId.value,
            outcome = providerResult.toOperationalOutcome(),
            level = providerResult.toOperationalLevel(),
            startedAt = startedAt,
        )

        val result = when (providerResult) {
            is HotelOfferProviderResult.SearchCompleted ->
                createAndSaveSearch(command, providerResult)

            is HotelOfferProviderResult.NotCompleted ->
                CreateHotelSearchResult.NotCreated(providerResult)
        }
        recordSearchOutcome(command, result, elapsedMillis(searchStartedAt))
        return result
    }

    private fun createSemanticSearch(
        command: CreateHotelSearchCommand,
    ): CreateHotelSearchResult.Created {
        val search = hotelSearchStateStore.save(
            HotelSearch(
                id = idGenerator.nextId(),
                sessionId = command.sessionId,
                criteria = command.criteria,
                status = HotelSearch.Status.SEARCHING,
                offers = emptyList(),
                analysis = AccommodationAnalysisMetadata.searching(POLL_AFTER_MILLIS),
            ),
        )
        val currentSearch = if (!semanticSearchLauncher.launch(search, command)) {
            val failedSearch = search.copy(
                status = HotelSearch.Status.FAILED,
                offers = emptyList(),
                analysis = AccommodationAnalysisMetadata.failed(),
            )
            when (val transition = hotelSearchStateStore.updateIfStatus(
                searchId = search.id,
                expectedStatus = HotelSearch.Status.SEARCHING,
            ) { current ->
                current.copy(
                    status = HotelSearch.Status.FAILED,
                    offers = emptyList(),
                    analysis = AccommodationAnalysisMetadata.failed(),
                )
            }) {
                is HotelSearchStateTransitionResult.Updated -> transition.search
                is HotelSearchStateTransitionResult.UnexpectedStatus -> transition.search
                HotelSearchStateTransitionResult.NotFound ->
                    hotelSearchStateStore.save(failedSearch)
            }
        } else {
            search
        }
        return CreateHotelSearchResult.Created(currentSearch)
    }

    private fun createAndSaveSearch(
        command: CreateHotelSearchCommand,
        providerResult: HotelOfferProviderResult.SearchCompleted,
    ): CreateHotelSearchResult.Created {
        val identifiedOffers = providerResult.offers.map { candidate ->
            candidate.identifiedBy(offerIdGenerator.nextId())
        }
        val rankedOffers = hotelOfferRanker.rank(identifiedOffers)
        val status = if (rankedOffers.isEmpty()) {
            HotelSearch.Status.COMPLETED_NO_OFFERS
        } else {
            HotelSearch.Status.COMPLETED_WITH_OFFERS
        }

        return CreateHotelSearchResult.Created(
            hotelSearchStateStore.save(
                HotelSearch(
                    id = idGenerator.nextId(),
                    sessionId = command.sessionId,
                    criteria = command.criteria,
                    status = status,
                    offers = rankedOffers,
                ),
            ),
        )
    }

    override fun getSearch(searchId: HotelSearchId): HotelSearch =
        hotelSearchStateStore.findById(searchId)
            ?: throw HotelSearchNotFoundException(searchId)

    private fun recordProviderCall(
        sessionId: String,
        outcome: OperationalOutcome,
        level: OperationalLevel,
        startedAt: Long,
    ) {
        eventSink.recordSafely(
            OperationalEvent(
                name = OperationalEventName.DEPENDENCY_CALL_COMPLETED,
                component = OperationalComponent.PROVIDER,
                level = level,
                sessionId = sessionId,
                operation = OperationalOperation.PROVIDER_HOTEL_SEARCH,
                dependency = OperationalDependency.HOTEL_PROVIDER,
                outcome = outcome,
                durationMillis = elapsedMillis(startedAt),
            ),
        )
    }

    private fun recordSearchOutcome(
        command: CreateHotelSearchCommand,
        result: CreateHotelSearchResult,
        durationMillis: Long,
    ) {
        val search = (result as? CreateHotelSearchResult.Created)?.search
        val outcome = when (result) {
            is CreateHotelSearchResult.Created -> when (result.search.status) {
                HotelSearch.Status.SEARCHING -> OperationalOutcome.STARTED
                HotelSearch.Status.COMPLETED_WITH_OFFERS -> OperationalOutcome.RESULTS
                HotelSearch.Status.COMPLETED_NO_OFFERS,
                HotelSearch.Status.COMPLETED_NO_SEMANTIC_MATCHES,
                -> OperationalOutcome.NO_OFFERS
                HotelSearch.Status.FAILED -> OperationalOutcome.FAILED
            }
            is CreateHotelSearchResult.NotCreated -> result.outcome.toOperationalOutcome()
        }
        eventSink.recordSafely(
            OperationalEvent(
                name = if (search?.status == HotelSearch.Status.SEARCHING) {
                    OperationalEventName.HOTEL_SEARCH_STARTED
                } else {
                    OperationalEventName.HOTEL_SEARCH_COMPLETED
                },
                component = OperationalComponent.HOTEL_SEARCH,
                level = when {
                    search?.status == HotelSearch.Status.FAILED -> OperationalLevel.ERROR
                    result is CreateHotelSearchResult.Created -> OperationalLevel.INFO
                    else -> OperationalLevel.WARNING
                },
                sessionId = command.sessionId.value,
                hotelSearchId = search?.id?.value,
                operation = OperationalOperation.CREATE_HOTEL_SEARCH,
                outcome = outcome,
                durationMillis = durationMillis,
                offerCount = search?.offers?.size,
            ),
        )
    }

    private fun HotelOfferProviderResult.toOperationalLevel(): OperationalLevel =
        when (this) {
            is HotelOfferProviderResult.SearchCompleted -> OperationalLevel.INFO
            HotelOfferProviderResult.LocationNotFound,
            is HotelOfferProviderResult.LocationSelectionRequired,
            is HotelOfferProviderResult.RequestRejected,
            -> OperationalLevel.WARNING
            is HotelOfferProviderResult.ProviderUnavailable,
            is HotelOfferProviderResult.ResponseRejected,
            -> OperationalLevel.ERROR
        }

    private fun HotelOfferProviderResult.toOperationalOutcome(): OperationalOutcome =
        when (this) {
            is HotelOfferProviderResult.SearchCompleted -> if (offers.isEmpty()) {
                OperationalOutcome.NO_OFFERS
            } else {
                OperationalOutcome.RESULTS
            }
            HotelOfferProviderResult.LocationNotFound -> OperationalOutcome.NOT_FOUND
            is HotelOfferProviderResult.LocationSelectionRequired ->
                OperationalOutcome.NEEDS_CLARIFICATION
            is HotelOfferProviderResult.RequestRejected -> OperationalOutcome.REQUEST_REJECTED
            is HotelOfferProviderResult.ResponseRejected -> OperationalOutcome.RESPONSE_REJECTED
            is HotelOfferProviderResult.ProviderUnavailable -> when (reason) {
                HotelOfferProviderResult.UnavailableReason.TIMEOUT -> OperationalOutcome.TIMEOUT
                HotelOfferProviderResult.UnavailableReason.RATE_LIMITED ->
                    OperationalOutcome.RATE_LIMITED
                HotelOfferProviderResult.UnavailableReason.AUTHENTICATION_FAILED ->
                    OperationalOutcome.AUTHENTICATION_FAILED
                HotelOfferProviderResult.UnavailableReason.UNAVAILABLE,
                HotelOfferProviderResult.UnavailableReason.UNKNOWN,
                -> OperationalOutcome.UNAVAILABLE
            }
        }

    private fun elapsedMillis(startedAtNanos: Long): Long =
        max(0, (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND)

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val POLL_AFTER_MILLIS = 1_000L
    }
}
