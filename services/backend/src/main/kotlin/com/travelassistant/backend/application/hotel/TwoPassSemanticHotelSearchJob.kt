package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.application.accommodation.AccommodationAnalysisClient
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisRequest
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisResult
import com.travelassistant.backend.application.accommodation.MergeAccommodationAnalysisUseCase
import com.travelassistant.backend.application.accommodation.SelectSemanticHotelOffersUseCase
import com.travelassistant.backend.application.accommodation.ValidateAccommodationAnalysisResultUseCase
import com.travelassistant.backend.domain.hotel.AccommodationAnalysisMetadata
import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict
import com.travelassistant.backend.domain.hotel.AccommodationSemanticMatch
import com.travelassistant.backend.domain.hotel.HotelDetails
import com.travelassistant.backend.domain.hotel.HotelOfferRanker
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.domain.hotel.RankedHotelOffer
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
) : SemanticHotelSearchJob {

    override suspend fun execute(
        searchId: HotelSearchId,
        command: CreateHotelSearchCommand,
    ): SemanticHotelSearchJobResult {
        val concept = command.criteria.preferences.accommodationConcept
            ?: return SemanticHotelSearchJobResult.Failed
        val providerResult = hotelOfferProvider.search(command.criteria)
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
        val coarseMatches = analyzeAndValidate(coarseRequest)
            ?: return SemanticHotelSearchJobResult.Failed

        val deepContexts = selectDeepContexts(contexts, coarseMatches)
        val deepCandidates = loadDeepCandidates(deepContexts)
        val deepMatches = if (deepCandidates.isEmpty()) {
            emptyMap()
        } else {
            val deepRequest = AccommodationAnalysisRequest(
                concept = concept,
                candidates = deepCandidates.map { candidate -> candidate.toAnalysisCandidate() },
            )
            analyzeAndValidate(deepRequest).orEmpty()
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
    ): Map<String, AccommodationSemanticMatch>? {
        val result = try {
            analysisSemaphore.withPermit { analysisClient.analyze(request) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return null
        }
        if (result !is AccommodationAnalysisResult.Completed) return null
        return when (val validation = validateAnalysis(request, result)) {
            is ValidateAccommodationAnalysisResultUseCase.ValidationResult.Accepted ->
                validation.matchesByCandidateId
            is ValidateAccommodationAnalysisResultUseCase.ValidationResult.Rejected -> null
        }
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
    ): List<DeepCandidate> = coroutineScope {
        contexts.map { context ->
            async {
                val reference = context.rankedOffer.offer.providerReference
                val cached = detailsCache.find(reference)
                val details = cached ?: detailsSemaphore.withPermit {
                    when (val result = hotelDetailsProvider.load(reference)) {
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
    }
}
