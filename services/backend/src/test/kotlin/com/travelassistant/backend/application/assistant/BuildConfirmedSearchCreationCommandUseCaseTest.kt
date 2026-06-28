package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class BuildConfirmedSearchCreationCommandUseCaseTest {

    @Test
    fun copiesSessionIdFromExplicitInput() {
        val useCase = BuildConfirmedSearchCreationCommandUseCase()
        val sessionId = AssistantSessionId("assistant-session-local-000123")

        val result = assertIs<ConfirmedSearchCreationCommandPlan.CommandReady>(
            useCase(
                sessionId = sessionId,
                plan = readyPlan(),
            ),
        )

        assertEquals(sessionId, result.command.sessionId)
    }

    @Test
    fun copiesHotelCriteriaFromReadyPlan() {
        val useCase = BuildConfirmedSearchCreationCommandUseCase()
        val criteria = hotelSearchCriteria(
            destination = "Rome Centro",
            checkInDate = LocalDate.parse("2026-12-30"),
            checkOutDate = LocalDate.parse("2027-01-03"),
            guests = HotelSearchCriteria.Guests(
                adults = 3,
                children = 0,
            ),
            rooms = 2,
        )

        val result = assertIs<ConfirmedSearchCreationCommandPlan.CommandReady>(
            useCase(
                sessionId = AssistantSessionId("assistant-session-local-000123"),
                plan = readyPlan(criteria = criteria),
            ),
        )

        assertEquals(criteria, result.command.criteria)
    }

    @Test
    fun keepsLifecyclePolicyAvailableWithCommand() {
        val useCase = BuildConfirmedSearchCreationCommandUseCase()
        val lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy()

        val result = assertIs<ConfirmedSearchCreationCommandPlan.CommandReady>(
            useCase(
                sessionId = AssistantSessionId("assistant-session-local-000123"),
                plan = readyPlan(lifecyclePolicy = lifecyclePolicy),
            ),
        )

        assertEquals(lifecyclePolicy, result.lifecyclePolicy)
    }

    @Test
    fun doesNotInferSessionFromFreeFormCriteriaText() {
        val useCase = BuildConfirmedSearchCreationCommandUseCase()
        val explicitSessionId = AssistantSessionId("assistant-session-local-explicit")
        val criteria = hotelSearchCriteria(
            destination = "Use assistant-session-local-from-text for Paris",
        )

        val result = assertIs<ConfirmedSearchCreationCommandPlan.CommandReady>(
            useCase(
                sessionId = explicitSessionId,
                plan = readyPlan(criteria = criteria),
            ),
        )

        assertEquals(explicitSessionId, result.command.sessionId)
        assertEquals(criteria, result.command.criteria)
    }

    @Test
    fun commandPlanDoesNotExposeSearchExecutionOrRuntimeSideEffects() {
        val useCase = BuildConfirmedSearchCreationCommandUseCase()

        val result = useCase(
            sessionId = AssistantSessionId("assistant-session-local-000123"),
            plan = readyPlan(),
        )
        val resultText = result.toString()

        listOf(
            "hotelSearchId",
            "show_hotel_results",
            "CreateHotelSearchUseCase",
            "Hotel search created",
            "provider",
            "markConsumed",
            "LlmCandidate",
            "candidatePayload",
            "modelResponse",
        ).forEach { forbidden ->
            assertFalse(
                resultText.contains(forbidden),
                "Confirmed search command plan must not expose $forbidden",
            )
        }
    }

    @Test
    fun remainsDeterministicForSameSessionAndPlan() {
        val useCase = BuildConfirmedSearchCreationCommandUseCase()
        val sessionId = AssistantSessionId("assistant-session-local-000123")
        val plan = readyPlan()

        val firstResult = useCase(sessionId, plan)
        val secondResult = useCase(sessionId, plan)

        assertEquals(firstResult, secondResult)
    }

    private fun readyPlan(
        criteria: HotelSearchCriteria = hotelSearchCriteria(),
        lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy =
            ConfirmedSearchCreationLifecyclePolicy(),
    ): ConfirmedSearchCreationPlan.ReadyToCreateSearch =
        ConfirmedSearchCreationPlan.ReadyToCreateSearch(
            criteria = criteria,
            lifecyclePolicy = lifecyclePolicy,
        )

    private fun hotelSearchCriteria(
        destination: String = "Rome",
        checkInDate: LocalDate = LocalDate.parse("2026-07-01"),
        checkOutDate: LocalDate = LocalDate.parse("2026-07-04"),
        guests: HotelSearchCriteria.Guests = HotelSearchCriteria.Guests(
            adults = 2,
            children = 1,
        ),
        rooms: Int = 1,
    ): HotelSearchCriteria =
        HotelSearchCriteria(
            destination = destination,
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            guests = guests,
            rooms = rooms,
        )
}
