package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.hotel.CreateHotelSearchCommand
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class ExecuteConfirmedSearchTransitionUseCaseTest {

    private val now = Instant.parse("2026-06-25T10:00:00Z")

    @Test
    fun newAttemptIsStoredAndTransitionedToInProgressWithNoOpExecution() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ExecuteConfirmedSearchTransitionUseCase(attemptStore = store)
        val request = transitionRequest()

        val result = assertIs<ExecuteConfirmedSearchTransitionResult.Transitioned>(
            useCase(request),
        )

        assertEquals(ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS, result.attempt.status)
        assertNull(result.attempt.createdSearchId)
        assertNull(result.attempt.failureReason)
        assertEquals(request.sessionId, result.attempt.sessionId)
        assertIs<ConfirmedSearchExecutionResult.PreparedButNotExecuted>(result.executionResult)
        assertEquals(
            ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision
                .CONSUME_AFTER_SUCCESSFUL_RECORDING,
            result.pendingConsumptionDecision,
        )

        val storedAttempt = store.findByIdempotencyKey(result.attempt.idempotencyKey)
        assertEquals(result.attempt, storedAttempt)
    }

    @Test
    fun duplicateCallReturnsExistingInProgressAttemptWithoutCreatingSecondAttempt() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ExecuteConfirmedSearchTransitionUseCase(attemptStore = store)
        val request = transitionRequest()

        val firstResult = assertIs<ExecuteConfirmedSearchTransitionResult.Transitioned>(
            useCase(request),
        )
        val secondResult = assertIs<ExecuteConfirmedSearchTransitionResult.DuplicateDetected>(
            useCase(request),
        )

        assertEquals(ConfirmedSearchExecutionAttemptResult.DuplicateReason.IN_PROGRESS, secondResult.duplicateReason)
        assertEquals(firstResult.attempt.idempotencyKey, secondResult.existingAttempt.idempotencyKey)
        assertEquals(
            ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision.DO_NOT_CONSUME,
            secondResult.pendingConsumptionDecision,
        )
    }

    @Test
    fun guardRejectedReturnsGuardRejectedWithoutStoringAttempt() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ExecuteConfirmedSearchTransitionUseCase(attemptStore = store)
        val sessionId = AssistantSessionId("assistant-session-local-000123")
        val request = transitionRequest(
            sessionId = sessionId,
            pendingConfirmation = null,
        )

        val result = assertIs<ExecuteConfirmedSearchTransitionResult.GuardRejected>(
            useCase(request),
        )

        assertEquals(
            ConfirmedSearchExecutionAttemptResult.RejectionReason.GUARD_REJECTED,
            result.attemptRejectionReason,
        )

        val idempotencyKey = ConfirmedSearchExecutionIdempotencyKey.from(
            commandReadyPlan(sessionId = sessionId),
        )
        assertNull(store.findByIdempotencyKey(idempotencyKey))
    }

    @Test
    fun transitionedResultDoesNotContainRealHotelSearchId() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ExecuteConfirmedSearchTransitionUseCase(attemptStore = store)
        val request = transitionRequest()

        val result = assertIs<ExecuteConfirmedSearchTransitionResult.Transitioned>(
            useCase(request),
        )

        assertNull(result.attempt.createdSearchId)
        val resultText = result.toString()
        assertFalse(resultText.contains("show_hotel_results"))
        assertFalse(resultText.contains("CreateHotelSearchUseCase"))
        assertFalse(resultText.contains("provider"))
    }

    @Test
    fun useCaseDoesNotRequireCreateHotelSearchUseCase() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ExecuteConfirmedSearchTransitionUseCase(attemptStore = store)

        val result = useCase(transitionRequest())

        assertIs<ExecuteConfirmedSearchTransitionResult.Transitioned>(result)
    }

    @Test
    fun orderingIsGuardThenLookupThenPlanThenPersistThenTransition() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ExecuteConfirmedSearchTransitionUseCase(attemptStore = store)
        val request = transitionRequest()

        val result = assertIs<ExecuteConfirmedSearchTransitionResult.Transitioned>(
            useCase(request),
        )

        assertEquals(ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS, result.attempt.status)
        assertEquals(request.sessionId, result.attempt.sessionId)
        val storedAttempt = store.findByIdempotencyKey(result.attempt.idempotencyKey)
        assertEquals(result.attempt, storedAttempt)
    }

    @Test
    fun transitionResultDoesNotLeakSearchExecutionOrInternalCandidateData() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ExecuteConfirmedSearchTransitionUseCase(attemptStore = store)

        val result = useCase(transitionRequest())
        val resultText = result.toString()

        listOf(
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
                "Transition result must not expose $forbidden",
            )
        }
    }

    @Test
    fun doesNotMutatePendingConfirmationStore() {
        val pendingStore = InMemoryPendingConfirmationStore()
        val pendingConfirmation = pendingStore.save(pendingConfirmation())
        val attemptStore = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ExecuteConfirmedSearchTransitionUseCase(attemptStore = attemptStore)

        useCase(
            transitionRequest(
                sessionId = pendingConfirmation.sessionId,
                pendingConfirmation = pendingConfirmation,
            ),
        )

        assertEquals(
            pendingConfirmation,
            pendingStore.findActiveBySession(
                sessionId = pendingConfirmation.sessionId,
                now = now.plusSeconds(1),
            ),
        )
    }

    private fun transitionRequest(
        sessionId: AssistantSessionId = AssistantSessionId("assistant-session-local-000123"),
        criteria: ProceedWithCandidateCriteria = proceedCriteria(),
        pendingConfirmation: PendingProceedWithCandidateConfirmation? =
            pendingConfirmation(sessionId = sessionId, criteria = criteria),
        now: Instant = this.now,
    ): ExecuteConfirmedSearchTransitionRequest =
        ExecuteConfirmedSearchTransitionRequest(
            sessionId = sessionId,
            decision = PostConfirmationDecision.Confirmed(criteria),
            pendingConfirmation = pendingConfirmation,
            now = now,
        )

    private fun pendingConfirmation(
        sessionId: AssistantSessionId = AssistantSessionId("assistant-session-local-000123"),
        criteria: ProceedWithCandidateCriteria = proceedCriteria(),
        createdAt: Instant = now,
        updatedAt: Instant = now,
        expiresAt: Instant = now.plusSeconds(300),
        status: PendingConfirmationStatus = PendingConfirmationStatus.PENDING,
    ): PendingProceedWithCandidateConfirmation =
        PendingProceedWithCandidateConfirmation(
            sessionId = sessionId,
            criteria = criteria,
            proposal = ProceedWithCandidateConfirmationProposal(
                summary = "Параметры hotel search: направление: ${criteria.destination}; " +
                    "заезд: ${criteria.checkInDate}; выезд: ${criteria.checkOutDate}; " +
                    "взрослые: ${criteria.guests.adults}; дети: ${criteria.guests.children}; " +
                    "номера: ${criteria.rooms}.",
                confirmationQuestion = "Проверить отели по этим параметрам?",
                displayFields = listOf(
                    ProceedWithCandidateConfirmationField(
                        key = "destination",
                        label = "направление",
                        value = criteria.destination,
                    ),
                ),
            ),
            createdAt = createdAt,
            updatedAt = updatedAt,
            expiresAt = expiresAt,
            status = status,
        )

    private fun commandReadyPlan(
        sessionId: AssistantSessionId = AssistantSessionId("assistant-session-local-000123"),
        criteria: HotelSearchCriteria = hotelSearchCriteria(),
        lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy =
            ConfirmedSearchCreationLifecyclePolicy(),
    ): ConfirmedSearchCreationCommandPlan.CommandReady =
        ConfirmedSearchCreationCommandPlan.CommandReady(
            command = CreateHotelSearchCommand(
                sessionId = sessionId,
                criteria = criteria,
            ),
            lifecyclePolicy = lifecyclePolicy,
        )

    private fun proceedCriteria(
        destination: String = "Rome",
        checkInDate: LocalDate = LocalDate.parse("2026-07-01"),
        checkOutDate: LocalDate = LocalDate.parse("2026-07-04"),
        guests: ProceedWithCandidateCriteria.Guests = ProceedWithCandidateCriteria.Guests(
            adults = 2,
            children = 0,
        ),
        rooms: Int = 1,
    ): ProceedWithCandidateCriteria =
        ProceedWithCandidateCriteria(
            destination = destination,
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            guests = guests,
            rooms = rooms,
        )

    private fun hotelSearchCriteria(
        destination: String = "Rome",
        checkInDate: LocalDate = LocalDate.parse("2026-07-01"),
        checkOutDate: LocalDate = LocalDate.parse("2026-07-04"),
        guests: HotelSearchCriteria.Guests = HotelSearchCriteria.Guests(
            adults = 2,
            children = 0,
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
