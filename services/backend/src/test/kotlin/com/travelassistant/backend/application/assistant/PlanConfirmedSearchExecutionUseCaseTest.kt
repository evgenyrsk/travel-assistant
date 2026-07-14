package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.hotel.CreateHotelSearchCommand
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class PlanConfirmedSearchExecutionUseCaseTest {

    @Test
    fun returnsPreparedResultWithoutExecutingSearch() {
        val useCase = PlanConfirmedSearchExecutionUseCase()
        val commandPlan = commandReadyPlan()

        val result = assertIs<ConfirmedSearchExecutionResult.PreparedButNotExecuted>(
            useCase(commandPlan),
        )

        assertEquals(commandPlan, result.commandPlan)
        assertEquals(
            ConfirmedSearchExecutionResult.NotExecutedReason.IDEMPOTENCY_GUARD_REQUIRED,
            result.reason,
        )
    }

    @Test
    fun preservesLifecyclePolicyFromCommandPlan() {
        val useCase = PlanConfirmedSearchExecutionUseCase()
        val lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy()

        val result = assertIs<ConfirmedSearchExecutionResult.PreparedButNotExecuted>(
            useCase(commandReadyPlan(lifecyclePolicy = lifecyclePolicy)),
        )

        assertEquals(lifecyclePolicy, result.lifecyclePolicy)
    }

    @Test
    fun requiresIdempotencyGuardBeforeActualExecution() {
        val useCase = PlanConfirmedSearchExecutionUseCase()

        val result = assertIs<ConfirmedSearchExecutionResult.PreparedButNotExecuted>(
            useCase(commandReadyPlan()),
        )

        assertEquals(
            ConfirmedSearchExecutionPolicy.DuplicateHandling
                .REQUIRE_IDEMPOTENCY_GUARD_BEFORE_EXECUTION,
            result.executionPolicy.duplicateHandling,
        )
        assertEquals(
            ConfirmedSearchExecutionResult.NotExecutedReason.IDEMPOTENCY_GUARD_REQUIRED,
            result.reason,
        )
    }

    @Test
    fun keepsFailureResponsesWithoutSearchId() {
        val useCase = PlanConfirmedSearchExecutionUseCase()

        val result = assertIs<ConfirmedSearchExecutionResult.PreparedButNotExecuted>(
            useCase(commandReadyPlan()),
        )

        assertEquals(
            ConfirmedSearchExecutionPolicy.FailureResponse.OMIT_SEARCH_ID_ON_FAILURE,
            result.executionPolicy.failureResponse,
        )
    }

    @Test
    fun keepsConsumePolicyAfterFutureSuccessOnly() {
        val useCase = PlanConfirmedSearchExecutionUseCase()

        val result = assertIs<ConfirmedSearchExecutionResult.PreparedButNotExecuted>(
            useCase(commandReadyPlan()),
        )

        assertEquals(
            ConfirmedSearchCreationLifecyclePolicy.PendingConsumption
                .CONSUME_AFTER_SEARCH_SUCCESS,
            result.lifecyclePolicy.pendingConsumption,
        )
        assertEquals(
            ConfirmedSearchExecutionPolicy.PendingConsumption
                .CONSUME_AFTER_FUTURE_SEARCH_SUCCESS,
            result.executionPolicy.pendingConsumption,
        )
    }

    @Test
    fun requiresActivePendingConfirmationFromFutureRouteContext() {
        val useCase = PlanConfirmedSearchExecutionUseCase()

        val result = assertIs<ConfirmedSearchExecutionResult.PreparedButNotExecuted>(
            useCase(commandReadyPlan()),
        )

        assertEquals(
            ConfirmedSearchExecutionPolicy.RouteContext.REQUIRE_ACTIVE_PENDING_CONFIRMATION,
            result.executionPolicy.routeContext,
        )
    }

    @Test
    fun doesNotCreateSearchResultOrPublicAction() {
        val useCase = PlanConfirmedSearchExecutionUseCase()

        val result = useCase(commandReadyPlan())
        val resultText = result.toString()

        assertFalse(result is ConfirmedSearchExecutionResult.SearchCreated)
        listOf(
            "hotelSearchId",
            "show_hotel_results",
            "CreateHotelSearchUseCase",
            "Hotel search created",
            "markConsumed",
            "LlmCandidate",
            "candidatePayload",
            "modelResponse",
        ).forEach { forbidden ->
            assertFalse(
                resultText.contains(forbidden),
                "Confirmed search execution result must not expose $forbidden",
            )
        }
    }

    @Test
    fun remainsDeterministicForSameCommandPlan() {
        val useCase = PlanConfirmedSearchExecutionUseCase()
        val commandPlan = commandReadyPlan()

        val firstResult = useCase(commandPlan)
        val secondResult = useCase(commandPlan)

        assertEquals(firstResult, secondResult)
    }

    private fun commandReadyPlan(
        lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy =
            ConfirmedSearchCreationLifecyclePolicy(),
    ): ConfirmedSearchCreationCommandPlan.CommandReady =
        ConfirmedSearchCreationCommandPlan.CommandReady(
            command = CreateHotelSearchCommand(
                sessionId = AssistantSessionId("assistant-session-local-000123"),
                criteria = HotelSearchCriteria(
                    destination = "Rome",
                    checkInDate = LocalDate.parse("2026-07-01"),
                    checkOutDate = LocalDate.parse("2026-07-04"),
                    guests = HotelSearchCriteria.Guests(
                        adults = 2,
                        childrenAges = listOf(7),
                    ),
                    rooms = 1,
                ),
            ),
            lifecyclePolicy = lifecyclePolicy,
        )
}
