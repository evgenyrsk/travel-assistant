package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.application.assistant.CreateAssistantSessionUseCase
import com.travelassistant.backend.application.assistant.InMemoryAssistantSessionStateStore
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.infrastructure.provider.FakeHotelOfferProvider
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CreateHotelSearchUseCaseTest {

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
    }
}
