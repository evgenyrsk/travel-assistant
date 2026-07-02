package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.hotel.CreateHotelSearchCommand
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchId
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class MapConfirmedSearchTransitionResultToResponseDirectiveUseCaseTest {

    private val now = Instant.parse("2026-06-25T10:00:00Z")
    private val mapper = MapConfirmedSearchTransitionResultToResponseDirectiveUseCase()

    @Test
    fun transitionedMapsToProcessingWithoutHotelResultsOrSearchId() {
        val result = ExecuteConfirmedSearchTransitionResult.Transitioned(
            attempt = attempt(status = ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS),
            executionResult = ConfirmedSearchExecutionResult.PreparedButNotExecuted(
                commandPlan = commandReadyPlan(),
                reason = ConfirmedSearchExecutionResult.NotExecutedReason.IDEMPOTENCY_GUARD_REQUIRED,
                lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
                executionPolicy = ConfirmedSearchExecutionPolicy(),
            ),
            pendingConsumptionDecision =
                ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision
                    .CONSUME_AFTER_SUCCESSFUL_RECORDING,
            lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
            executionPolicy = ConfirmedSearchExecutionPolicy(),
        )

        val directive = mapper(result)

        assertEquals(InternalTransitionNextAction.ASK_CLARIFICATION, directive.nextAction)
        assertEquals(TransitionMessageKind.PROCESSING, directive.messageKind)
        assertNull(directive.hotelSearchId)
        assertFalse(directive.mayShowHotelResults)
        assertFalse(directive.shouldConsumePendingConfirmation)
    }

    @Test
    fun transitionedDoesNotRequestPendingConsume() {
        val result = ExecuteConfirmedSearchTransitionResult.Transitioned(
            attempt = attempt(status = ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS),
            executionResult = ConfirmedSearchExecutionResult.PreparedButNotExecuted(
                commandPlan = commandReadyPlan(),
                reason = ConfirmedSearchExecutionResult.NotExecutedReason.IDEMPOTENCY_GUARD_REQUIRED,
                lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
                executionPolicy = ConfirmedSearchExecutionPolicy(),
            ),
            pendingConsumptionDecision =
                ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision
                    .CONSUME_AFTER_SUCCESSFUL_RECORDING,
            lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
            executionPolicy = ConfirmedSearchExecutionPolicy(),
        )

        val directive = mapper(result)

        assertFalse(directive.shouldConsumePendingConfirmation)
    }

    @Test
    fun duplicateInProgressMapsToAlreadyProcessingWithoutHotelResults() {
        val result = ExecuteConfirmedSearchTransitionResult.DuplicateDetected(
            existingAttempt = attempt(status = ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS),
            duplicateReason = ConfirmedSearchExecutionAttemptResult.DuplicateReason.IN_PROGRESS,
            pendingConsumptionDecision =
                ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision.DO_NOT_CONSUME,
            lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
            executionPolicy = ConfirmedSearchExecutionPolicy(),
        )

        val directive = mapper(result)

        assertEquals(InternalTransitionNextAction.ASK_CLARIFICATION, directive.nextAction)
        assertEquals(TransitionMessageKind.ALREADY_PROCESSING, directive.messageKind)
        assertNull(directive.hotelSearchId)
        assertFalse(directive.mayShowHotelResults)
        assertFalse(directive.shouldConsumePendingConfirmation)
    }

    @Test
    fun guardRejectedMapsToConfirmationRejectedWithoutConsumeOrResults() {
        val result = ExecuteConfirmedSearchTransitionResult.GuardRejected(
            attemptRejectionReason =
                ConfirmedSearchExecutionAttemptResult.RejectionReason.GUARD_REJECTED,
            lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
            executionPolicy = ConfirmedSearchExecutionPolicy(),
        )

        val directive = mapper(result)

        assertEquals(InternalTransitionNextAction.ASK_CLARIFICATION, directive.nextAction)
        assertEquals(TransitionMessageKind.CONFIRMATION_REJECTED, directive.messageKind)
        assertNull(directive.hotelSearchId)
        assertFalse(directive.mayShowHotelResults)
        assertFalse(directive.shouldConsumePendingConfirmation)
    }

    @Test
    fun storeRejectedMapsToTemporaryFailureWithoutConsumeOrResults() {
        val result = ExecuteConfirmedSearchTransitionResult.StoreRejected(
            reason = ConfirmedSearchExecutionAttemptStoreResult.RejectionReason.ATTEMPT_NOT_FOUND,
            lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
            executionPolicy = ConfirmedSearchExecutionPolicy(),
        )

        val directive = mapper(result)

        assertEquals(InternalTransitionNextAction.ASK_CLARIFICATION, directive.nextAction)
        assertEquals(TransitionMessageKind.TEMPORARY_FAILURE, directive.messageKind)
        assertNull(directive.hotelSearchId)
        assertFalse(directive.mayShowHotelResults)
        assertFalse(directive.shouldConsumePendingConfirmation)
    }

    @Test
    fun noMapperOutputContainsRawShowHotelResults() {
        val results = listOf(
            ExecuteConfirmedSearchTransitionResult.Transitioned(
                attempt = attempt(status = ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS),
                executionResult = ConfirmedSearchExecutionResult.PreparedButNotExecuted(
                    commandPlan = commandReadyPlan(),
                    reason = ConfirmedSearchExecutionResult.NotExecutedReason.IDEMPOTENCY_GUARD_REQUIRED,
                    lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
                    executionPolicy = ConfirmedSearchExecutionPolicy(),
                ),
                pendingConsumptionDecision =
                    ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision
                        .CONSUME_AFTER_SUCCESSFUL_RECORDING,
                lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
                executionPolicy = ConfirmedSearchExecutionPolicy(),
            ),
            ExecuteConfirmedSearchTransitionResult.DuplicateDetected(
                existingAttempt = attempt(status = ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS),
                duplicateReason = ConfirmedSearchExecutionAttemptResult.DuplicateReason.IN_PROGRESS,
                pendingConsumptionDecision =
                    ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision.DO_NOT_CONSUME,
                lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
                executionPolicy = ConfirmedSearchExecutionPolicy(),
            ),
            ExecuteConfirmedSearchTransitionResult.GuardRejected(
                attemptRejectionReason =
                    ConfirmedSearchExecutionAttemptResult.RejectionReason.GUARD_REJECTED,
                lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
                executionPolicy = ConfirmedSearchExecutionPolicy(),
            ),
            ExecuteConfirmedSearchTransitionResult.StoreRejected(
                reason = ConfirmedSearchExecutionAttemptStoreResult.RejectionReason.ATTEMPT_NOT_FOUND,
                lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
                executionPolicy = ConfirmedSearchExecutionPolicy(),
            ),
        )

        results.forEach { result ->
            val directive = mapper(result)
            val directiveText = directive.toString()
            assertFalse(
                directiveText.contains("show_hotel_results"),
                "Directive must not expose show_hotel_results for ${result::class.simpleName}",
            )
            assertFalse(
                directiveText.contains("SHOW_HOTEL_RESULTS"),
                "Directive must not use SHOW_HOTEL_RESULTS nextAction for ${result::class.simpleName}",
            )
        }
    }

    @Test
    fun mapperDoesNotCreateOrFakeRealHotelSearchId() {
        val result = ExecuteConfirmedSearchTransitionResult.Transitioned(
            attempt = attempt(status = ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS),
            executionResult = ConfirmedSearchExecutionResult.PreparedButNotExecuted(
                commandPlan = commandReadyPlan(),
                reason = ConfirmedSearchExecutionResult.NotExecutedReason.IDEMPOTENCY_GUARD_REQUIRED,
                lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
                executionPolicy = ConfirmedSearchExecutionPolicy(),
            ),
            pendingConsumptionDecision =
                ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision
                    .CONSUME_AFTER_SUCCESSFUL_RECORDING,
            lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
            executionPolicy = ConfirmedSearchExecutionPolicy(),
        )

        val directive = mapper(result)

        assertNull(directive.hotelSearchId)
        assertFalse(directive.mayShowHotelResults)
    }

    @Test
    fun mapperDoesNotRequireCreateHotelSearchUseCase() {
        val useCase = MapConfirmedSearchTransitionResultToResponseDirectiveUseCase()

        val directive = useCase(
            ExecuteConfirmedSearchTransitionResult.GuardRejected(
                attemptRejectionReason =
                    ConfirmedSearchExecutionAttemptResult.RejectionReason.GUARD_REJECTED,
                lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
                executionPolicy = ConfirmedSearchExecutionPolicy(),
            ),
        )

        assertEquals(InternalTransitionNextAction.ASK_CLARIFICATION, directive.nextAction)
    }

    @Test
    fun transitionedWithSearchCreatedMapsToShowHotelResultsDirective() {
        val searchId = HotelSearchId("hotel-search-local-success-001")
        val result = ExecuteConfirmedSearchTransitionResult.Transitioned(
            attempt = attempt(status = ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS),
            executionResult = ConfirmedSearchExecutionResult.SearchCreated(
                searchId = searchId,
                lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
                executionPolicy = ConfirmedSearchExecutionPolicy(),
            ),
            pendingConsumptionDecision =
                ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision
                    .CONSUME_AFTER_SUCCESSFUL_RECORDING,
            lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
            executionPolicy = ConfirmedSearchExecutionPolicy(),
        )

        val directive = mapper(result)

        assertEquals(InternalTransitionNextAction.SHOW_HOTEL_RESULTS, directive.nextAction)
        assertEquals(TransitionMessageKind.RESULTS_READY, directive.messageKind)
        assertEquals(searchId, directive.hotelSearchId)
        assertEquals(true, directive.mayShowHotelResults)
        assertEquals(true, directive.shouldConsumePendingConfirmation)
    }

    @Test
    fun duplicateSucceededWithSearchIdMapsToShowHotelResultsDirective() {
        val searchId = HotelSearchId("hotel-search-local-dup-001")
        val result = ExecuteConfirmedSearchTransitionResult.DuplicateDetected(
            existingAttempt = attempt(
                status = ConfirmedSearchExecutionAttemptStatus.SUCCEEDED,
                createdSearchId = searchId,
            ),
            duplicateReason = ConfirmedSearchExecutionAttemptResult.DuplicateReason.SUCCEEDED,
            pendingConsumptionDecision =
                ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision
                    .CONSUME_AFTER_SUCCESSFUL_RECORDING,
            lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
            executionPolicy = ConfirmedSearchExecutionPolicy(),
        )

        val directive = mapper(result)

        assertEquals(InternalTransitionNextAction.SHOW_HOTEL_RESULTS, directive.nextAction)
        assertEquals(TransitionMessageKind.RESULTS_READY, directive.messageKind)
        assertEquals(searchId, directive.hotelSearchId)
        assertEquals(true, directive.mayShowHotelResults)
        assertEquals(true, directive.shouldConsumePendingConfirmation)
    }

    @Test
    fun duplicateSucceededWithoutSearchIdMapsToAlreadyProcessing() {
        val result = ExecuteConfirmedSearchTransitionResult.DuplicateDetected(
            existingAttempt = attempt(
                status = ConfirmedSearchExecutionAttemptStatus.SUCCEEDED,
                createdSearchId = null,
            ),
            duplicateReason = ConfirmedSearchExecutionAttemptResult.DuplicateReason.SUCCEEDED,
            pendingConsumptionDecision =
                ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision.DO_NOT_CONSUME,
            lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
            executionPolicy = ConfirmedSearchExecutionPolicy(),
        )

        val directive = mapper(result)

        assertEquals(InternalTransitionNextAction.ASK_CLARIFICATION, directive.nextAction)
        assertEquals(TransitionMessageKind.ALREADY_PROCESSING, directive.messageKind)
        assertNull(directive.hotelSearchId)
        assertFalse(directive.mayShowHotelResults)
        assertFalse(directive.shouldConsumePendingConfirmation)
    }

    private fun attempt(
        status: ConfirmedSearchExecutionAttemptStatus,
        createdSearchId: HotelSearchId? = null,
    ): ConfirmedSearchExecutionAttempt {
        val commandPlan = commandReadyPlan()
        return ConfirmedSearchExecutionAttempt(
            idempotencyKey = ConfirmedSearchExecutionIdempotencyKey.from(commandPlan),
            sessionId = AssistantSessionId("assistant-session-local-000123"),
            commandPlan = commandPlan,
            status = status,
            createdSearchId = createdSearchId,
            createdAt = now,
            updatedAt = now,
            expiresAt = now.plusSeconds(900),
        )
    }

    private fun commandReadyPlan(): ConfirmedSearchCreationCommandPlan.CommandReady =
        ConfirmedSearchCreationCommandPlan.CommandReady(
            command = CreateHotelSearchCommand(
                sessionId = AssistantSessionId("assistant-session-local-000123"),
                criteria = HotelSearchCriteria(
                    destination = "Rome",
                    checkInDate = LocalDate.parse("2026-07-01"),
                    checkOutDate = LocalDate.parse("2026-07-04"),
                    guests = HotelSearchCriteria.Guests(adults = 2, children = 0),
                    rooms = 1,
                ),
            ),
            lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
        )
}
