package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.application.assistant.CreateAssistantSessionUseCase
import com.travelassistant.backend.application.assistant.InMemoryAssistantSessionStateStore
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.infrastructure.provider.FakeHotelOfferProvider
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateHotelSearchUseCaseTest {

    @Test
    fun createsProcessLocalSearchFromDeterministicFakeProviderOffers() {
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

        val search = useCase.createSearch(
            CreateHotelSearchCommand(
                sessionId = session.id,
                criteria = HotelSearchCriteria(
                    destination = "Rome",
                    checkInDate = LocalDate.parse("2026-07-01"),
                    checkOutDate = LocalDate.parse("2026-07-04"),
                    guests = HotelSearchCriteria.Guests(
                        adults = 2,
                        children = 0,
                    ),
                    rooms = 1,
                ),
            ),
        )

        assertEquals("hotel-search-local-000001", search.id.value)
        assertEquals("completed_with_offers", search.status.apiValue)
        assertEquals(2, search.offers.size)
        assertEquals("fake-offer-rome-001", search.offers.first().id)
        assertEquals("local_fake_provider", search.offers.first().source)
        assertEquals(search, searchStore.findById(search.id))
    }
}
