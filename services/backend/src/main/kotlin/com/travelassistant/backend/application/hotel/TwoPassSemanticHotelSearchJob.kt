package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.application.accommodation.AccommodationAnalysisClient
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisRequest
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisResult
import com.travelassistant.backend.application.accommodation.MergeAccommodationAnalysisUseCase
import com.travelassistant.backend.application.accommodation.SelectSemanticHotelOffersUseCase
import com.travelassistant.backend.application.accommodation.ValidateAccommodationAnalysisResultUseCase
import com.travelassistant.backend.application.observability.OperationalComponent
import com.travelassistant.backend.application.observability.OperationalDependency
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalLevel
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.application.observability.recordSafely
import com.travelassistant.backend.domain.hotel.AccommodationAnalysisMetadata
import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict
import com.travelassistant.backend.domain.hotel.AccommodationSemanticMatch
import com.travelassistant.backend.domain.hotel.HotelDetails
import com.travelassistant.backend.domain.hotel.HotelOfferRanker
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.domain.hotel.RankedHotelOffer
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class TwoPassSemanticHotelSearchJob(
    private val hotelOfferProvider: HotelOfferProviderBoundary,
    private val hotelDetailsProvider: HotelDetailsProviderBoundary,
    private val analysisClient: AccommodationAnalysisClient,
    private val detailsCache: HotelDetailsCache,
    private val offerRanker: HotelOfferRanker = HotelOfferRanker(),
    private val offerIdGenerator: HotelOfferIdGenerator = LocalHotelOfferIdGenerator(),
    private val validateAnalysis: ValidateAccommodationAnalysisResultUseCase =
        ValidateAccommodationAnalysisResultUseCase(),
    private val mergeAnalysis: MergeAccommodationAnalysisUseCase =
        MergeAccommodationAnalysisUseCase(),
    private val selectOffers: SelectSemanticHotelOffersUseCase =
        SelectSemanticHotelOffersUseCase(),
    private val detailsSemaphore: Semaphore = Semaphore(MAX_PARALLEL_DETAILS_CALLS),
    private val analysisSemaphore: Semaphore = Semaphore(MAX_PARALLEL_ANALYSIS_CALLS),
    private val eventSink: OperationalEventSink = OperationalEventSink.NONE,
) : SemanticHotelSearchJob {

    override suspend fun execute(
        searchId: HotelSearchId,
        command: CreateHotelSearchCommand,
    ): SemanticHotelSearchJobResult {
        val concept = command.criteria.preferences.accommodationConcept
            ?: return SemanticHotelSearchJobResult.Failed
        val providerStartedAt = System.nanoTime()
        val providerResult = try {
            hotelOfferProvider.search(command.criteria)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            recordDependencyCall(
                searchId = searchId,
                sessionId = command.sessionId.value,
                component = OperationalComponent.PROVIDER,
                dependency = OperationalDependency.HOTEL_PROVIDER,
                operation = OperationalOperation.PROVIDER_HOTEL_SEARCH,
                outcome = OperationalOutcome.FAILED,
                durationMillis = elapsedMillis(providerStartedAt),
            )
            throw error
        }
        recordDependencyCall(
            searchId = searchId,
            sessionId = command.sessionId.value,
            component = OperationalComponent.PROVIDER,
            dependency = OperationalDependency.HOTEL_PROVIDER,
            operation = OperationalOperation.PROVIDER_HOTEL_SEARCH,
            outcome = providerResult.toOperationalOutcome(),
            durationMillis = elapsedMillis(providerStartedAt),
        )
        if (providerResult !is HotelOfferProviderResult.SearchCompleted) {
            return SemanticHotelSearchJobResult.Failed
        }
        if (providerResult.offers.isEmpty()) {
            return SemanticHotelSearchJobResult.Completed(
                status = HotelSearch.Status.COMPLETED_NO_OFFERS,
                offers = emptyList(),
                analysis = completedMetadata(0, 0, emptyList(), partial = false),
            )
        }

        val rankedOffers = offerRanker.rank(
            providerResult.offers
                .take(MAX_COARSE_CANDIDATES)
                .map { candidate -> candidate.identifiedBy(offerIdGenerator.nextId()) },
        )
        val contexts = rankedOffers.mapIndexed { index, rankedOffer ->
            CandidateContext(
                ephemeralCandidateId = "candidate-${(index + 1).toString().padStart(3, '0')}",
                rankedOffer = rankedOffer,
            )
        }
        val coarseRequest = AccommodationAnalysisRequest(
            concept = concept,
            candidates = contexts.map { context ->
                AccommodationAnalysisRequest.Candidate(
                    ephemeralCandidateId = context.ephemeralCandidateId,
                    hotelName = context.rankedOffer.offer.hotelName,
                    imageUrls = listOfNotNull(context.rankedOffer.offer.imageUrl).take(1),
                )
            },
        )
        val coarseMatches = analyzeAndValidate(
            request = coarseRequest,
            searchId = searchId,
            sessionId = command.sessionId.value,
            operation = OperationalOperation.ACCOMMODATION_COARSE_ANALYSIS,
        )
            ?: return SemanticHotelSearchJobResult.Failed

        val deepContexts = selectDeepContexts(contexts, coarseMatches)
        val deepCandidates = loadDeepCandidates(
            contexts = deepContexts,
            searchId = searchId,
            sessionId = command.sessionId.value,
        )
        val deepMatches = if (deepCandidates.isEmpty()) {
            emptyMap()
        } else {
            val deepRequest = AccommodationAnalysisRequest(
                concept = concept,
                candidates = deepCandidates.map { candidate -> candidate.toAnalysisCandidate() },
            )
            analyzeAndValidate(
                request = deepRequest,
                searchId = searchId,
                sessionId = command.sessionId.value,
                operation = OperationalOperation.ACCOMMODATION_DEEP_ANALYSIS,
            ).orEmpty()
        }
        val merged = mergeAnalysis(
            coarseMatches = coarseMatches,
            deepMatches = deepMatches,
            expectedDeepCandidateIds = deepContexts
                .map { context -> context.ephemeralCandidateId }
                .toSet(),
        )
        val selected = selectOffers(
            contexts.mapNotNull { context ->
                merged.matchesByCandidateId[context.ephemeralCandidateId]?.let { match ->
                    SelectSemanticHotelOffersUseCase.Candidate(context.rankedOffer, match)
                }
            },
        )
        val status = if (selected.isEmpty()) {
            HotelSearch.Status.COMPLETED_NO_SEMANTIC_MATCHES
        } else {
            HotelSearch.Status.COMPLETED_WITH_OFFERS
        }
        return SemanticHotelSearchJobResult.Completed(
            status = status,
            offers = selected,
            analysis = completedMetadata(
                analyzedCount = coarseMatches.size,
                deepAnalyzedCount = deepMatches.size,
                selectedOffers = selected,
                partial = merged.partial,
            ),
        )
    }

    private suspend fun analyzeAndValidate(
        request: AccommodationAnalysisRequest,
        searchId: HotelSearchId,
        sessionId: String,
        operation: OperationalOperation,
    ): Map<String, AccommodationSemanticMatch>? {
        val startedAt = System.nanoTime()
        val result = try {
            analysisSemaphore.withPermit { analysisClient.analyze(request) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            recordDependencyCall(
                searchId = searchId,
                sessionId = sessionId,
                component = OperationalComponent.ACCOMMODATION_ANALYSIS,
                dependency = OperationalDependency.ACCOMMODATION_ANALYZER,
                operation = operation,
                outcome = OperationalOutcome.FAILED,
                durationMillis = elapsedMillis(startedAt),
            )
            return null
        }
        if (result is AccommodationAnalysisResult.Failed) {
            recordDependencyCall(
                searchId = searchId,
                sessionId = sessionId,
                component = OperationalComponent.ACCOMMODATION_ANALYSIS,
                dependency = OperationalDependency.ACCOMMODATION_ANALYZER,
                operation = operation,
                outcome = result.reason.toOperationalOutcome(),
                durationMillis = elapsedMillis(startedAt),
            )
            return null
        }
        val completed = result as AccommodationAnalysisResult.Completed
        val matches = when (val validation = validateAnalysis(request, completed)) {
            is ValidateAccommodationAnalysisResultUseCase.ValidationResult.Accepted ->
                validation.matchesByCandidateId
            is ValidateAccommodationAnalysisResultUseCase.ValidationResult.Rejected -> null
        }
        recordDependencyCall(
            searchId = searchId,
            sessionId = sessionId,
            component = OperationalComponent.ACCOMMODATION_ANALYSIS,
            dependency = OperationalDependency.ACCOMMODATION_ANALYZER,
            operation = operation,
            outcome = if (matches == null) {
                OperationalOutcome.RESPONSE_REJECTED
            } else {
                OperationalOutcome.SUCCEEDED
            },
            durationMillis = elapsedMillis(startedAt),
        )
        return matches
    }

    private fun selectDeepContexts(
        contexts: List<CandidateContext>,
        coarseMatches: Map<String, AccommodationSemanticMatch>,
    ): List<CandidateContext> =
        contexts
            .withIndex()
            .sortedWith(
                compareBy<IndexedValue<CandidateContext>> { indexed ->
                    deepPriority(coarseMatches[indexed.value.ephemeralCandidateId])
                }.thenBy { indexed -> indexed.index },
            )
            .take(MAX_DEEP_CANDIDATES)
            .map(IndexedValue<CandidateContext>::value)

    private fun deepPriority(match: AccommodationSemanticMatch?): Int =
        when {
            match?.verdict == AccommodationMatchVerdict.PROBABLE -> 0
            match?.verdict == AccommodationMatchVerdict.MATCH && match.evidenceSources.size <= 1 -> 1
            match?.verdict == AccommodationMatchVerdict.UNKNOWN -> 2
            else -> 3
        }

    private suspend fun loadDeepCandidates(
        contexts: List<CandidateContext>,
        searchId: HotelSearchId,
        sessionId: String,
    ): List<DeepCandidate> = coroutineScope {
        contexts.map { context ->
            async {
                val reference = context.rankedOffer.offer.providerReference
                val cached = detailsCache.find(reference)
                val details = cached ?: detailsSemaphore.withPermit {
                    val startedAt = System.nanoTime()
                    val result = try {
                        hotelDetailsProvider.load(reference)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        recordDependencyCall(
                            searchId = searchId,
                            sessionId = sessionId,
                            component = OperationalComponent.PROVIDER,
                            dependency = OperationalDependency.HOTEL_PROVIDER,
                            operation = OperationalOperation.PROVIDER_HOTEL_DETAILS,
                            outcome = OperationalOutcome.FAILED,
                            durationMillis = elapsedMillis(startedAt),
                        )
                        return@withPermit null
                    }
                    recordDependencyCall(
                        searchId = searchId,
                        sessionId = sessionId,
                        component = OperationalComponent.PROVIDER,
                        dependency = OperationalDependency.HOTEL_PROVIDER,
                        operation = OperationalOperation.PROVIDER_HOTEL_DETAILS,
                        outcome = result.toOperationalOutcome(),
                        durationMillis = elapsedMillis(startedAt),
                    )
                    when (result) {
                        is HotelDetailsProviderResult.Loaded -> result.details.also { loaded ->
                            detailsCache.save(reference, loaded)
                        }
                        HotelDetailsProviderResult.NotFound,
                        is HotelDetailsProviderResult.ProviderUnavailable,
                        is HotelDetailsProviderResult.ResponseRejected,
                        -> null
                    }
                }
                details?.let { loaded -> DeepCandidate(context, loaded) }
            }
        }.awaitAll().filterNotNull()
    }

    private fun recordDependencyCall(
        searchId: HotelSearchId,
        sessionId: String,
        component: OperationalComponent,
        dependency: OperationalDependency,
        operation: OperationalOperation,
        outcome: OperationalOutcome,
        durationMillis: Long,
    ) {
        eventSink.recordSafely(
            OperationalEvent(
                name = OperationalEventName.DEPENDENCY_CALL_COMPLETED,
                component = component,
                level = when (outcome) {
                    OperationalOutcome.SUCCEEDED,
                    OperationalOutcome.RESULTS,
                    OperationalOutcome.NO_OFFERS,
                    -> OperationalLevel.INFO
                    OperationalOutcome.NOT_FOUND,
                    OperationalOutcome.REQUEST_REJECTED,
                    OperationalOutcome.RESPONSE_REJECTED,
                    -> OperationalLevel.WARNING
                    else -> OperationalLevel.ERROR
                },
                sessionId = sessionId,
                hotelSearchId = searchId.value,
                operation = operation,
                dependency = dependency,
                outcome = outcome,
                durationMillis = durationMillis,
            ),
        )
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

    private fun HotelDetailsProviderResult.toOperationalOutcome(): OperationalOutcome =
        when (this) {
            is HotelDetailsProviderResult.Loaded -> OperationalOutcome.SUCCEEDED
            HotelDetailsProviderResult.NotFound -> OperationalOutcome.NOT_FOUND
            is HotelDetailsProviderResult.ResponseRejected -> OperationalOutcome.RESPONSE_REJECTED
            is HotelDetailsProviderResult.ProviderUnavailable -> when (reason) {
                HotelDetailsProviderResult.UnavailableReason.TIMEOUT -> OperationalOutcome.TIMEOUT
                HotelDetailsProviderResult.UnavailableReason.RATE_LIMITED ->
                    OperationalOutcome.RATE_LIMITED
                HotelDetailsProviderResult.UnavailableReason.AUTHENTICATION_FAILED ->
                    OperationalOutcome.AUTHENTICATION_FAILED
                HotelDetailsProviderResult.UnavailableReason.UNAVAILABLE,
                HotelDetailsProviderResult.UnavailableReason.UNKNOWN,
                -> OperationalOutcome.UNAVAILABLE
            }
        }

    private fun AccommodationAnalysisResult.FailureReason.toOperationalOutcome(): OperationalOutcome =
        when (this) {
            AccommodationAnalysisResult.FailureReason.TIMEOUT -> OperationalOutcome.TIMEOUT
            AccommodationAnalysisResult.FailureReason.RATE_LIMITED -> OperationalOutcome.RATE_LIMITED
            AccommodationAnalysisResult.FailureReason.UNAVAILABLE -> OperationalOutcome.UNAVAILABLE
            AccommodationAnalysisResult.FailureReason.AUTHENTICATION_FAILED ->
                OperationalOutcome.AUTHENTICATION_FAILED
            AccommodationAnalysisResult.FailureReason.REQUEST_REJECTED ->
                OperationalOutcome.REQUEST_REJECTED
            AccommodationAnalysisResult.FailureReason.INVALID_RESPONSE ->
                OperationalOutcome.RESPONSE_REJECTED
        }

    private fun elapsedMillis(startedAtNanos: Long): Long =
        max(0, (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND)

    private fun DeepCandidate.toAnalysisCandidate(): AccommodationAnalysisRequest.Candidate =
        AccommodationAnalysisRequest.Candidate(
            ephemeralCandidateId = context.ephemeralCandidateId,
            hotelName = context.rankedOffer.offer.hotelName,
            descriptions = details.descriptionSections
                .orEmpty()
                .flatMap { section -> section.paragraphs }
                .filter(String::isNotBlank),
            amenities = details.amenityGroups
                .orEmpty()
                .flatMap { group -> group.amenities }
                .filter(String::isNotBlank),
            imageUrls = details.imageUrls.orEmpty().take(MAX_DEEP_IMAGES),
        )

    private fun completedMetadata(
        analyzedCount: Int,
        deepAnalyzedCount: Int,
        selectedOffers: List<RankedHotelOffer>,
        partial: Boolean,
    ): AccommodationAnalysisMetadata =
        AccommodationAnalysisMetadata(
            status = if (partial) {
                AccommodationAnalysisMetadata.Status.PARTIAL
            } else {
                AccommodationAnalysisMetadata.Status.COMPLETED
            },
            analyzedCount = analyzedCount,
            deepAnalyzedCount = deepAnalyzedCount,
            matchCount = selectedOffers.count { offer ->
                offer.semanticMatch?.verdict == AccommodationMatchVerdict.MATCH
            },
            probableCount = selectedOffers.count { offer ->
                offer.semanticMatch?.verdict == AccommodationMatchVerdict.PROBABLE
            },
        )

    private data class CandidateContext(
        val ephemeralCandidateId: String,
        val rankedOffer: RankedHotelOffer,
    )

    private data class DeepCandidate(
        val context: CandidateContext,
        val details: HotelDetails,
    )

    companion object {
        const val MAX_COARSE_CANDIDATES = 20
        const val MAX_DEEP_CANDIDATES = 6
        const val MAX_DEEP_IMAGES = 3
        const val MAX_PARALLEL_DETAILS_CALLS = 3
        const val MAX_PARALLEL_ANALYSIS_CALLS = 2
        private const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
