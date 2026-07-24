package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.application.accommodation.AccommodationAnalysisClient
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisRequest
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisResult
import com.travelassistant.backend.application.observability.OperationalDependency
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.AccommodationAnalysisMetadata
import com.travelassistant.backend.domain.hotel.AccommodationConcept
import com.travelassistant.backend.domain.hotel.AccommodationEvidenceSource
import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict
import com.travelassistant.backend.domain.hotel.HotelDetails
import com.travelassistant.backend.domain.hotel.HotelOffer
import com.travelassistant.backend.domain.hotel.HotelOfferCandidate
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TwoPassSemanticHotelSearchJobTest {

    @Test
    fun `limits analysis calls across concurrent semantic jobs`() = runBlocking {
        val active = AtomicInteger()
        val maxActive = AtomicInteger()
        val job = job(
            offers = offers(1),
            analysisClient = AccommodationAnalysisClient { request ->
                val current = active.incrementAndGet()
                maxActive.accumulateAndGet(current, ::maxOf)
                try {
                    delay(20)
                    AccommodationAnalysisResult.Completed(
                        request.candidates.map { candidate ->
                            probable(candidate.ephemeralCandidateId)
                        },
                    )
                } finally {
                    active.decrementAndGet()
                }
            },
        )

        coroutineScope {
            (1..3).map { index ->
                async {
                    job.execute(HotelSearchId("semantic-$index"), command())
                }
            }.awaitAll()
        }

        assertTrue(maxActive.get() <= 2)
    }

    @Test
    fun `caps coarse and deep candidates and details concurrency`() = runBlocking {
        val analysisSizes = mutableListOf<Int>()
        val activeDetails = AtomicInteger()
        val maxActiveDetails = AtomicInteger()
        val detailsCache = InMemoryHotelDetailsCache()
        var detailsCalls = 0
        val events = mutableListOf<OperationalEvent>()
        val detailsProvider = HotelDetailsProviderBoundary {
            detailsCalls += 1
            val active = activeDetails.incrementAndGet()
            maxActiveDetails.accumulateAndGet(active, ::maxOf)
            try {
                delay(20)
                HotelDetailsProviderResult.Loaded(details())
            } finally {
                activeDetails.decrementAndGet()
            }
        }
        val job = job(
            offers = offers(25),
            detailsProvider = detailsProvider,
            detailsCache = detailsCache,
            analysisClient = AccommodationAnalysisClient { request ->
                analysisSizes += request.candidates.size
                val deep = request.candidates.any { candidate -> candidate.descriptions.isNotEmpty() }
                AccommodationAnalysisResult.Completed(
                    request.candidates.map { candidate ->
                        if (deep) explicitMatch(candidate.ephemeralCandidateId) else probable(
                            candidate.ephemeralCandidateId,
                        )
                    },
                )
            },
            eventSink = OperationalEventSink(events::add),
        )

        val completed = assertIs<SemanticHotelSearchJobResult.Completed>(
            job.execute(HotelSearchId("semantic-search"), command()),
        )

        assertEquals(listOf(20, 6), analysisSizes)
        assertEquals(6, detailsCalls)
        assertTrue(maxActiveDetails.get() <= 3)
        assertEquals(20, completed.analysis.analyzedCount)
        assertEquals(6, completed.analysis.deepAnalyzedCount)
        assertEquals(6, completed.analysis.matchCount)
        assertEquals(14, completed.analysis.probableCount)
        assertEquals(HotelSearch.Status.COMPLETED_WITH_OFFERS, completed.status)
        assertEquals(
            1,
            events.count {
                it.name == OperationalEventName.DEPENDENCY_CALL_COMPLETED &&
                    it.operation == OperationalOperation.PROVIDER_HOTEL_SEARCH
            },
        )
        assertEquals(
            6,
            events.count { it.operation == OperationalOperation.PROVIDER_HOTEL_DETAILS },
        )
        assertEquals(
            setOf(
                OperationalOperation.ACCOMMODATION_COARSE_ANALYSIS,
                OperationalOperation.ACCOMMODATION_DEEP_ANALYSIS,
            ),
            events.filter { it.dependency == OperationalDependency.ACCOMMODATION_ANALYZER }
                .mapNotNull { event -> event.operation }
                .toSet(),
        )
        assertTrue(events.none { event -> event.toString().contains("Synthetic candidate") })

        val searchStore = InMemoryHotelSearchStateStore()
        searchStore.save(
            HotelSearch(
                id = HotelSearchId("semantic-search"),
                sessionId = command().sessionId,
                criteria = command().criteria,
                status = completed.status,
                offers = completed.offers,
                analysis = completed.analysis,
            ),
        )
        val selectedDetails = LoadSelectedHotelDetailsUseCase(
            resolveSelectedOffer = ResolveSelectedHotelOfferUseCase(searchStore),
            hotelDetailsProvider = detailsProvider,
            detailsCache = detailsCache,
        )(
            ResolveSelectedHotelOfferRequest(
                searchId = HotelSearchId("semantic-search"),
                offerId = completed.offers.first().offer.id,
            ),
        )
        assertIs<LoadSelectedHotelDetailsResult.Loaded>(selectedDetails)
        assertEquals(6, detailsCalls)
    }

    @Test
    fun `details failure keeps coarse verdict and marks partial`() = runBlocking {
        var calls = 0
        val job = job(
            offers = offers(3),
            detailsProvider = HotelDetailsProviderBoundary {
                calls += 1
                if (calls == 1) {
                    HotelDetailsProviderResult.Loaded(details())
                } else {
                    HotelDetailsProviderResult.ProviderUnavailable(
                        HotelDetailsProviderResult.UnavailableReason.UNAVAILABLE,
                    )
                }
            },
            analysisClient = AccommodationAnalysisClient { request ->
                val deep = request.candidates.any { candidate -> candidate.descriptions.isNotEmpty() }
                AccommodationAnalysisResult.Completed(
                    request.candidates.map { candidate ->
                        if (deep) explicitMatch(candidate.ephemeralCandidateId) else probable(
                            candidate.ephemeralCandidateId,
                        )
                    },
                )
            },
        )

        val completed = assertIs<SemanticHotelSearchJobResult.Completed>(
            job.execute(HotelSearchId("semantic-search"), command()),
        )

        assertEquals(AccommodationAnalysisMetadata.Status.PARTIAL, completed.analysis.status)
        assertEquals(1, completed.analysis.deepAnalyzedCount)
        assertEquals(3, completed.offers.size)
    }

    @Test
    fun `distinguishes zero provider offers zero semantic matches and classifier outage`() =
        runBlocking {
            var analysisCalls = 0
            val emptyProviderJob = job(
                offers = emptyList(),
                analysisClient = AccommodationAnalysisClient {
                    analysisCalls += 1
                    AccommodationAnalysisResult.Completed(emptyList())
                },
            )
            val noOffers = assertIs<SemanticHotelSearchJobResult.Completed>(
                emptyProviderJob.execute(HotelSearchId("empty"), command()),
            )
            assertEquals(HotelSearch.Status.COMPLETED_NO_OFFERS, noOffers.status)
            assertEquals(0, analysisCalls)

            val noMatchesJob = job(
                offers = offers(2),
                analysisClient = AccommodationAnalysisClient { request ->
                    AccommodationAnalysisResult.Completed(
                        request.candidates.map { candidate ->
                            AccommodationAnalysisResult.Decision(
                                candidate.ephemeralCandidateId,
                                AccommodationMatchVerdict.NO_MATCH,
                                emptySet(),
                            )
                        },
                    )
                },
            )
            val noMatches = assertIs<SemanticHotelSearchJobResult.Completed>(
                noMatchesJob.execute(HotelSearchId("no-matches"), command()),
            )
            assertEquals(HotelSearch.Status.COMPLETED_NO_SEMANTIC_MATCHES, noMatches.status)

            val unavailableJob = job(
                offers = offers(2),
                analysisClient = AccommodationAnalysisClient {
                    AccommodationAnalysisResult.Failed(
                        AccommodationAnalysisResult.FailureReason.UNAVAILABLE,
                    )
                },
            )
            assertEquals(
                SemanticHotelSearchJobResult.Failed,
                unavailableJob.execute(HotelSearchId("failed"), command()),
            )
        }

    private fun job(
        offers: List<HotelOfferCandidate>,
        detailsProvider: HotelDetailsProviderBoundary = HotelDetailsProviderBoundary {
            HotelDetailsProviderResult.Loaded(details())
        },
        detailsCache: HotelDetailsCache = InMemoryHotelDetailsCache(),
        analysisClient: AccommodationAnalysisClient,
        eventSink: OperationalEventSink = OperationalEventSink.NONE,
    ) = TwoPassSemanticHotelSearchJob(
        hotelOfferProvider = HotelOfferProviderBoundary {
            HotelOfferProviderResult.SearchCompleted(offers)
        },
        hotelDetailsProvider = detailsProvider,
        analysisClient = analysisClient,
        detailsCache = detailsCache,
        eventSink = eventSink,
    )

    private fun probable(candidateId: String) = AccommodationAnalysisResult.Decision(
        candidateId,
        AccommodationMatchVerdict.PROBABLE,
        setOf(
            AccommodationAnalysisResult.Evidence(
                AccommodationEvidenceSource.NAME,
                AccommodationAnalysisResult.Signal.NATURE_SETTING,
            ),
        ),
    )

    private fun explicitMatch(candidateId: String) = AccommodationAnalysisResult.Decision(
        candidateId,
        AccommodationMatchVerdict.MATCH,
        setOf(
            AccommodationAnalysisResult.Evidence(
                AccommodationEvidenceSource.DESCRIPTION,
                AccommodationAnalysisResult.Signal.EXPLICIT_GLAMPING_LABEL,
            ),
        ),
    )

    private fun offers(count: Int): List<HotelOfferCandidate> =
        (1..count).map { index ->
            HotelOfferCandidate(
                providerReference = "provider-$index",
                hotelName = "Synthetic candidate $index",
                city = "Synthetic city",
                country = "Synthetic country",
                totalPrice = 100.0 + index,
                currency = "RUB",
                rating = 9.0 - index.toDouble() / 100,
                reviewCount = 10,
                amenities = null,
                availability = HotelOffer.Availability.AVAILABLE,
                source = "synthetic",
                freshness = HotelOffer.Freshness.FRESH,
            )
        }

    private fun details() = HotelDetails(
        hotelName = "Synthetic details",
        descriptionSections = listOf(
            HotelDetails.DescriptionSection(paragraphs = listOf("Synthetic glamping description")),
        ),
        amenityGroups = listOf(
            HotelDetails.AmenityGroup(amenities = listOf("Fire pit")),
        ),
        imageUrls = listOf(
            "https://images.example.test/1.jpg",
            "https://images.example.test/2.jpg",
            "https://images.example.test/3.jpg",
            "https://images.example.test/4.jpg",
        ),
    )

    private fun command() = CreateHotelSearchCommand(
        sessionId = AssistantSessionId("assistant-session"),
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
