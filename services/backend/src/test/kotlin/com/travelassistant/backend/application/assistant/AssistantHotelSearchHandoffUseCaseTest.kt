package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.hotel.CreateHotelSearchUseCase
import com.travelassistant.backend.application.hotel.InMemoryHotelSearchStateStore
import com.travelassistant.backend.infrastructure.provider.FakeHotelOfferProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AssistantHotelSearchHandoffUseCaseTest {

    @Test
    fun createsRankedHotelSearchForCompleteExplicitMessage() {
        val sessionStore = InMemoryAssistantSessionStateStore()
        val hotelSearchBoundary = CreateHotelSearchUseCase(
            assistantSessionStateStore = sessionStore,
            hotelOfferProvider = FakeHotelOfferProvider(),
            hotelSearchStateStore = InMemoryHotelSearchStateStore(),
        )
        val useCase = AssistantHotelSearchHandoffUseCase(
            assistantSessionBoundary = fixedAssistantBoundary(sessionStore),
            hotelSearchBoundary = hotelSearchBoundary,
        )
        val session = useCase.createSession()

        val acceptedMessage = useCase.acceptUserMessage(
            AcceptAssistantMessageCommand(
                sessionId = session.id,
                message = completeSearchMessage(),
            ),
        )

        assertEquals(AssistantNextAction.SHOW_HOTEL_RESULTS, acceptedMessage.nextAction)
        assertEquals("hotel-search-local-000001", acceptedMessage.hotelSearchId?.value)
        assertEquals(
            "Hotel search created. Ranked offers are ready.",
            acceptedMessage.assistantReply.message,
        )

        val search = hotelSearchBoundary.getSearch(checkNotNull(acceptedMessage.hotelSearchId))
        assertEquals("fake-offer-rome-001", search.offers.first().offer.id)
        assertEquals(
            "Available; ranked by rating, total stay price, then offer ID.",
            search.offers.first().matchSummary,
        )
    }

    @Test
    fun doesNotCreateHotelSearchForIncompleteExplicitMessage() {
        val sessionStore = InMemoryAssistantSessionStateStore()
        val hotelSearchBoundary = CreateHotelSearchUseCase(
            assistantSessionStateStore = sessionStore,
            hotelOfferProvider = FakeHotelOfferProvider(),
            hotelSearchStateStore = InMemoryHotelSearchStateStore(),
        )
        val useCase = AssistantHotelSearchHandoffUseCase(
            assistantSessionBoundary = fixedAssistantBoundary(sessionStore),
            hotelSearchBoundary = hotelSearchBoundary,
        )
        val session = useCase.createSession()

        val incompleteMessage = useCase.acceptUserMessage(
            AcceptAssistantMessageCommand(
                sessionId = session.id,
                message = "hotel-search; destination=Rome; check-in=2026-07-01; adults=2; rooms=1",
            ),
        )
        val completeMessage = useCase.acceptUserMessage(
            AcceptAssistantMessageCommand(
                sessionId = session.id,
                message = completeSearchMessage(),
            ),
        )

        assertEquals(AssistantNextAction.ASK_CLARIFICATION, incompleteMessage.nextAction)
        assertNull(incompleteMessage.hotelSearchId)
        assertEquals(
            "I need a complete hotel-search request with destination, check-in, check-out, adults, and rooms.",
            incompleteMessage.assistantReply.message,
        )
        assertEquals("hotel-search-local-000001", completeMessage.hotelSearchId?.value)
    }

    private fun fixedAssistantBoundary(
        sessionStore: InMemoryAssistantSessionStateStore,
    ): AssistantSessionBoundary =
        CreateAssistantSessionUseCase(
            clock = Clock.fixed(
                Instant.parse("2026-06-19T00:00:00Z"),
                ZoneOffset.UTC,
            ),
            sessionStateStore = sessionStore,
        )

    private fun completeSearchMessage(): String =
        "hotel-search; destination=Rome; check-in=2026-07-01; " +
            "check-out=2026-07-04; adults=2; rooms=1"
}
