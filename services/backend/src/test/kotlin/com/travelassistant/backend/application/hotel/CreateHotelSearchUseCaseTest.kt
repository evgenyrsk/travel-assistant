package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.application.assistant.CreateAssistantSessionUseCase
import com.travelassistant.backend.application.assistant.InMemoryAssistantSessionStateStore
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
    fun createsProcessLocalSearchFromDeterministicFakeProviderOffers() = runBlocking {
        val sessionStore = InMemoryAssistantSessionStateStore()
        val session = CreateAssistantSessionUseCase(
            sessionStateStore = sessionStore,
        ).createSession()
        val searchStore = InMemoryHotelSearchStateStore()
        val useCase = CreateHotelSearchUseCase(
            assistantSessionStateStore = sessionStore,
            hotelOfferProvider = FakeHotelOfferProvider(),
            hotelSearchStateStore = searchStore,
            idGenerator = HotelSearchIdGenerator {
                HotelSearchId("hotel-search-local-000001")
            },
        )

        val result = assertIs<CreateHotelSearchResult.Created>(
            useCase.createSearch(command(session.id)),
        )
        val search = result.search

        assertEquals("hotel-search-local-000001", search.id.value)
        assertEquals("completed_with_offers", search.status.apiValue)
        assertEquals(2, search.offers.size)
        assertEquals("fake-offer-rome-001", search.offers.first().offer.id)
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
        val useCase = CreateHotelSearchUseCase(
            assistantSessionStateStore = sessionStore,
            hotelOfferProvider = HotelOfferProviderBoundary {
                HotelOfferProviderResult.SearchCompleted(emptyList())
            },
            hotelSearchStateStore = searchStore,
            idGenerator = HotelSearchIdGenerator {
                HotelSearchId("hotel-search-local-empty-001")
            },
        )

        val result = assertIs<CreateHotelSearchResult.Created>(
            useCase.createSearch(command(session.id)),
        )

        assertEquals(HotelSearch.Status.COMPLETED_NO_OFFERS, result.search.status)
        assertTrue(result.search.offers.isEmpty())
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
            val useCase = CreateHotelSearchUseCase(
                assistantSessionStateStore = sessionStore,
                hotelOfferProvider = HotelOfferProviderBoundary { outcome },
                hotelSearchStateStore = searchStore,
                idGenerator = HotelSearchIdGenerator {
                    generatedIdCount++
                    HotelSearchId("hotel-search-must-not-be-created")
                },
            )

            val result = assertIs<CreateHotelSearchResult.NotCreated>(
                useCase.createSearch(command(session.id)),
            )

            assertEquals(outcome, result.outcome)
            assertEquals(0, generatedIdCount)
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
