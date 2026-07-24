package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.hotel.CreateHotelSearchCommand
import com.travelassistant.backend.application.hotel.CreateHotelSearchResult
import com.travelassistant.backend.application.hotel.CreateHotelSearchUseCase
import com.travelassistant.backend.application.hotel.HotelOfferProviderResult
import com.travelassistant.backend.application.hotel.HotelSearchBoundary
import com.travelassistant.backend.application.hotel.InMemoryHotelSearchStateStore
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.infrastructure.provider.FakeHotelOfferProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AssistantHotelSearchHandoffUseCaseTest {

    @Test
    fun createsRankedHotelSearchForCompleteExplicitMessage() = runBlocking {
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
            "Поиск завершён. Результат готов.",
            acceptedMessage.assistantReply.message,
        )

        val search = hotelSearchBoundary.getSearch(checkNotNull(acceptedMessage.hotelSearchId))
        assertEquals("hotel-offer-local-000002", search.offers.first().offer.id)
        assertEquals(
            "Доступно; выше размещены варианты с лучшим рейтингом, затем — с меньшей общей ценой за проживание.",
            search.offers.first().matchSummary,
        )
    }

    @Test
    fun doesNotCreateHotelSearchForIncompleteExplicitMessage() = runBlocking {
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
            "Укажите направление, даты заезда и выезда и количество взрослых.",
            incompleteMessage.assistantReply.message,
        )
        assertEquals("hotel-search-local-000001", completeMessage.hotelSearchId?.value)
    }

    @Test
    fun providerNotCompletedOutcomesReturnClarificationWithoutSearchId() = runBlocking {
        val cases = listOf(
            HotelOfferProviderResult.LocationNotFound to
                "Не удалось определить направление. Уточните город или место.",
            HotelOfferProviderResult.RequestRejected(
                HotelOfferProviderResult.RequestRejectionReason.INVALID_OCCUPANCY,
            ) to
                "Не удалось безопасно подготовить поиск. Проверьте направление, даты и состав гостей.",
            HotelOfferProviderResult.ProviderUnavailable(
                HotelOfferProviderResult.UnavailableReason.UNAVAILABLE,
            ) to
                "Сейчас не удалось завершить поиск отелей. Попробуйте ещё раз.",
        )

        cases.forEach { (outcome, expectedMessage) ->
            val sessionStore = InMemoryAssistantSessionStateStore()
            val useCase = AssistantHotelSearchHandoffUseCase(
                assistantSessionBoundary = fixedAssistantBoundary(sessionStore),
                hotelSearchBoundary = NotCreatedHotelSearchBoundary(outcome),
            )
            val session = useCase.createSession()

            val acceptedMessage = useCase.acceptUserMessage(
                AcceptAssistantMessageCommand(
                    sessionId = session.id,
                    message = completeSearchMessage(),
                ),
            )

            assertEquals(AssistantNextAction.ASK_CLARIFICATION, acceptedMessage.nextAction)
            assertNull(acceptedMessage.hotelSearchId)
            assertEquals(expectedMessage, acceptedMessage.assistantReply.message)
        }
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

    private class NotCreatedHotelSearchBoundary(
        private val outcome: HotelOfferProviderResult.NotCompleted,
    ) : HotelSearchBoundary {
        override suspend fun createSearch(
            command: CreateHotelSearchCommand,
        ): CreateHotelSearchResult =
            CreateHotelSearchResult.NotCreated(outcome)

        override fun getSearch(searchId: HotelSearchId): HotelSearch =
            throw RuntimeException("Not expected in tests")
    }
}
