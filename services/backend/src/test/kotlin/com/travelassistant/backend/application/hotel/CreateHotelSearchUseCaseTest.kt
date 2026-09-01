package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.application.assistant.CreateAssistantSessionUseCase
import com.travelassistant.backend.application.assistant.InMemoryAssistantSessionStateStore
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalLevel
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.AccommodationAnalysisMetadata
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import com.travelassistant.backend.domain.hotel.AccommodationConcept
import com.travelassistant.backend.infrastructure.provider.FakeHotelOfferProvider
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CreateHotelSearchUseCaseTest {

    @Test
    fun semanticSearchIsSavedAsSearchingBeforeBackgroundLaunchWithoutProviderCall() = runBlocking {
        val sessionStore = InMemoryAssistantSessionStateStore()
        val session = CreateAssistantSessionUseCase(sessionStateStore = sessionStore).createSession()
        val searchStore = InMemoryHotelSearchStateStore()
        var providerCallCount = 0
        var launchedSearch: HotelSearch? = null
        val events = mutableListOf<OperationalEvent>()
        val useCase = CreateHotelSearchUseCase(
            assistantSessionStateStore = sessionStore,
            hotelOfferProvider = HotelOfferProviderBoundary {
                providerCallCount += 1
                HotelOfferProviderResult.SearchCompleted(emptyList())
            },
            hotelSearchStateStore = searchStore,
            idGenerator = HotelSearchIdGenerator { HotelSearchId("semantic-search-000001") },
            semanticSearchLauncher = SemanticHotelSearchLauncher { search, _ ->
                launchedSearch = searchStore.findById(search.id)
                true
            },
            eventSink = OperationalEventSink(events::add),
        )

        val result = assertIs<CreateHotelSearchResult.Created>(
            useCase.createSearch(
                command(
                    session.id,
                    HotelSearchPreferences(
                        accommodationConcept = AccommodationConcept.GLAMPING,
                    ),
                ),
            ),
        )

        assertEquals(HotelSearch.Status.SEARCHING, result.search.status)
        assertEquals(HotelSearch.Status.SEARCHING, launchedSearch?.status)
        assertTrue(result.search.offers.isEmpty())
        assertEquals(1_000L, result.search.analysis?.pollAfterMillis)
        assertEquals(0, providerCallCount)
        assertEquals(OperationalEventName.HOTEL_SEARCH_STARTED, events.single().name)
        assertEquals(OperationalOutcome.STARTED, events.single().outcome)
    }

    @Test
    fun semanticSearchReturnsPersistedFailureWhenBackgroundLaunchIsRejected() = runBlocking {
        val sessionStore = InMemoryAssistantSessionStateStore()
        val session = CreateAssistantSessionUseCase(sessionStateStore = sessionStore).createSession()
        val searchStore = InMemoryHotelSearchStateStore()
        var providerCallCount = 0
        val events = mutableListOf<OperationalEvent>()
        val useCase = CreateHotelSearchUseCase(
            assistantSessionStateStore = sessionStore,
            hotelOfferProvider = HotelOfferProviderBoundary {
                providerCallCount += 1
                HotelOfferProviderResult.SearchCompleted(emptyList())
            },
            hotelSearchStateStore = searchStore,
            idGenerator = HotelSearchIdGenerator { HotelSearchId("semantic-search-failed") },
            semanticSearchLauncher = SemanticHotelSearchLauncher.UNAVAILABLE,
            eventSink = OperationalEventSink(events::add),
        )

        val result = assertIs<CreateHotelSearchResult.Created>(
            useCase.createSearch(
                command(
                    session.id,
                    HotelSearchPreferences(
                        accommodationConcept = AccommodationConcept.GLAMPING,
                    ),
                ),
            ),
        )

        assertEquals(HotelSearch.Status.FAILED, result.search.status)
        assertEquals(result.search, searchStore.findById(result.search.id))
        assertTrue(result.search.offers.isEmpty())
        assertEquals(
            AccommodationAnalysisMetadata.Status.FAILED,
            result.search.analysis?.status,
        )
        assertEquals(null, result.search.analysis?.pollAfterMillis)
        assertEquals(0, providerCallCount)
        assertEquals(1, events.size)
        assertEquals(OperationalEventName.HOTEL_SEARCH_COMPLETED, events.single().name)
        assertEquals(OperationalOutcome.FAILED, events.single().outcome)
        assertEquals(OperationalLevel.ERROR, events.single().level)
        assertEquals("semantic-search-failed", events.single().hotelSearchId)
        assertTrue(events.none { event -> event.toString().contains("Rome") })
    }

    @Test
    fun recordsBoundedNoOffersAndProviderFailureOutcomes() = runBlocking {
        val providerOutcomes = listOf(
            HotelOfferProviderResult.SearchCompleted(emptyList()) to OperationalOutcome.NO_OFFERS,
            HotelOfferProviderResult.ProviderUnavailable(
                HotelOfferProviderResult.UnavailableReason.TIMEOUT,
            ) to OperationalOutcome.TIMEOUT,
        )

        providerOutcomes.forEach { (providerOutcome, expectedOutcome) ->
            val sessionStore = InMemoryAssistantSessionStateStore()
            val session = CreateAssistantSessionUseCase(
                sessionStateStore = sessionStore,
            ).createSession()
            val events = mutableListOf<OperationalEvent>()
            val useCase = CreateHotelSearchUseCase(
                assistantSessionStateStore = sessionStore,
                hotelOfferProvider = HotelOfferProviderBoundary { providerOutcome },
                hotelSearchStateStore = InMemoryHotelSearchStateStore(),
                idGenerator = HotelSearchIdGenerator { HotelSearchId("opaque-search-id") },
                eventSink = OperationalEventSink(events::add),
            )

            useCase.createSearch(command(session.id))

            assertEquals(
                expectedOutcome,
                events.single {
                    it.name == OperationalEventName.DEPENDENCY_CALL_COMPLETED
                }.outcome,
            )
            assertEquals(
                expectedOutcome,
                events.single {
                    it.name == OperationalEventName.HOTEL_SEARCH_COMPLETED
                }.outcome,
            )
            assertTrue(events.all { it.sessionId == session.id.value })
            assertTrue(events.none { it.toString().contains("Rome") })
        }
    }

    @Test
    fun createsProcessLocalSearchFromDeterministicFakeProviderOffers() = runBlocking {
        val sessionStore = InMemoryAssistantSessionStateStore()
        val session = CreateAssistantSessionUseCase(
            sessionStateStore = sessionStore,
        ).createSession()
        val searchStore = InMemoryHotelSearchStateStore()
        var nextOfferId = 0
        val useCase = CreateHotelSearchUseCase(
            assistantSessionStateStore = sessionStore,
            hotelOfferProvider = FakeHotelOfferProvider(),
            hotelSearchStateStore = searchStore,
            idGenerator = HotelSearchIdGenerator {
                HotelSearchId("hotel-search-local-000001")
            },
            offerIdGenerator = HotelOfferIdGenerator {
                nextOfferId++
                "hotel-offer-test-${nextOfferId.toString().padStart(6, '0')}"
            },
        )

        val result = assertIs<CreateHotelSearchResult.Created>(
            useCase.createSearch(command(session.id)),
        )
        val search = result.search

        assertEquals("hotel-search-local-000001", search.id.value)
        assertEquals("completed_with_offers", search.status.apiValue)
        assertEquals(2, search.offers.size)
        assertEquals(
            setOf("hotel-offer-test-000001", "hotel-offer-test-000002"),
            search.offers.map { it.offer.id }.toSet(),
        )
        assertTrue(search.offers.none { it.offer.id.contains(it.offer.providerReference) })
        assertEquals("local-fake-rome-001", search.offers.first().offer.providerReference)
        assertEquals("local_fake_provider", search.offers.first().offer.source)
        assertEquals(
            "Доступно; выше размещены варианты с лучшим рейтингом, затем — с меньшей общей ценой за проживание.",
            search.offers.first().matchSummary,
        )
        assertEquals(search, searchStore.findById(search.id))
    }

    @Test
    fun createsCompletedNoOffersSearchForSuccessfulEmptyProviderResult() = runBlocking {
        val sessionStore = InMemoryAssistantSessionStateStore()
        val session = CreateAssistantSessionUseCase(
            sessionStateStore = sessionStore,
        ).createSession()
        val searchStore = InMemoryHotelSearchStateStore()
        var generatedOfferIdCount = 0
        val useCase = CreateHotelSearchUseCase(
            assistantSessionStateStore = sessionStore,
            hotelOfferProvider = HotelOfferProviderBoundary {
                HotelOfferProviderResult.SearchCompleted(emptyList())
            },
            hotelSearchStateStore = searchStore,
            idGenerator = HotelSearchIdGenerator {
                HotelSearchId("hotel-search-local-empty-001")
            },
            offerIdGenerator = HotelOfferIdGenerator {
                generatedOfferIdCount++
                "hotel-offer-must-not-be-created"
            },
        )

        val result = assertIs<CreateHotelSearchResult.Created>(
            useCase.createSearch(command(session.id)),
        )

        assertEquals(HotelSearch.Status.COMPLETED_NO_OFFERS, result.search.status)
        assertTrue(result.search.offers.isEmpty())
        assertEquals(0, generatedOfferIdCount)
        assertEquals(result.search, searchStore.findById(result.search.id))
    }

    @Test
    fun doesNotGenerateIdOrSaveSearchForEveryNotCompletedOutcome() = runBlocking {
        val outcomes = listOf(
            HotelOfferProviderResult.LocationNotFound,
            HotelOfferProviderResult.LocationSelectionRequired(
                suggestions = listOf(
                    HotelLocationSuggestion(
                        name = "Rome",
                        signature = "City, Italy",
                        typeCode = "city",
                        typeName = "City",
                    ),
                ),
            ),
            HotelOfferProviderResult.RequestRejected(
                HotelOfferProviderResult.RequestRejectionReason.INVALID_OCCUPANCY,
            ),
            HotelOfferProviderResult.ResponseRejected(
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_PAYLOAD,
            ),
            HotelOfferProviderResult.ProviderUnavailable(
                HotelOfferProviderResult.UnavailableReason.UNAVAILABLE,
            ),
        )

        outcomes.forEach { outcome ->
            val sessionStore = InMemoryAssistantSessionStateStore()
            val session = CreateAssistantSessionUseCase(
                sessionStateStore = sessionStore,
            ).createSession()
            val searchStore = RecordingHotelSearchStateStore()
            var generatedIdCount = 0
            var generatedOfferIdCount = 0
            val useCase = CreateHotelSearchUseCase(
                assistantSessionStateStore = sessionStore,
                hotelOfferProvider = HotelOfferProviderBoundary { outcome },
                hotelSearchStateStore = searchStore,
                idGenerator = HotelSearchIdGenerator {
                    generatedIdCount++
                    HotelSearchId("hotel-search-must-not-be-created")
                },
                offerIdGenerator = HotelOfferIdGenerator {
                    generatedOfferIdCount++
                    "hotel-offer-must-not-be-created"
                },
            )

            val result = assertIs<CreateHotelSearchResult.NotCreated>(
                useCase.createSearch(command(session.id)),
            )

            assertEquals(outcome, result.outcome)
            assertEquals(0, generatedIdCount)
            assertEquals(0, generatedOfferIdCount)
            assertTrue(searchStore.savedSearches.isEmpty())
        }
    }

    private fun command(
        sessionId: AssistantSessionId,
        preferences: HotelSearchPreferences = HotelSearchPreferences(),
    ): CreateHotelSearchCommand =
        CreateHotelSearchCommand(
            sessionId = sessionId,
            criteria = HotelSearchCriteria(
                destination = "Rome",
                checkInDate = LocalDate.parse("2026-07-01"),
                checkOutDate = LocalDate.parse("2026-07-04"),
                guests = HotelSearchCriteria.Guests(
                    adults = 2,
                    childrenAges = emptyList(),
                ),
                rooms = 1,
                preferences = preferences,
            ),
        )

    private class RecordingHotelSearchStateStore : HotelSearchStateStore {
        val savedSearches = mutableListOf<HotelSearch>()

        override fun save(search: HotelSearch): HotelSearch {
            savedSearches += search
            return search
        }

        override fun findById(searchId: HotelSearchId): HotelSearch? =
            savedSearches.firstOrNull { it.id == searchId }

        override fun updateIfStatus(
            searchId: HotelSearchId,
            expectedStatus: HotelSearch.Status,
            update: (HotelSearch) -> HotelSearch,
        ): HotelSearchStateTransitionResult {
            val index = savedSearches.indexOfFirst { search -> search.id == searchId }
            if (index < 0) return HotelSearchStateTransitionResult.NotFound
            val current = savedSearches[index]
            if (current.status != expectedStatus) {
                return HotelSearchStateTransitionResult.UnexpectedStatus(current)
            }
            val updated = update(current)
            savedSearches[index] = updated
            return HotelSearchStateTransitionResult.Updated(updated)
        }
    }
}
