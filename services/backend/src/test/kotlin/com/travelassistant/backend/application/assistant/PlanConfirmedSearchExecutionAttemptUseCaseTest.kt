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
import kotlin.test.assertIs
import kotlin.test.assertNull

class PlanConfirmedSearchExecutionAttemptUseCaseTest {

    private val now = Instant.parse("2026-06-25T10:00:00Z")

    @Test
    fun allowedGuardResultProducesPreparedAttemptButBlocksExecution() {
        val guardResult = allowedGuardResult()

        val result = assertIs<ConfirmedSearchExecutionAttemptResult.AttemptPreparedButExecutionBlocked>(
            PlanConfirmedSearchExecutionAttemptUseCase()(
                guardResult = guardResult,
                now = now,
            ),
        )

        assertEquals(guardResult.commandPlan.command.sessionId, result.attempt.sessionId)
        assertEquals(guardResult.commandPlan, result.attempt.commandPlan)
        assertEquals(
            ConfirmedSearchExecutionAttemptStatus.PREPARED,
            result.attempt.status,
        )
        assertEquals(
            ConfirmedSearchExecutionAttemptResult.ExecutionBlocker
                .ATTEMPT_STORE_REQUIRED_BEFORE_EXECUTION,
            result.blocker,
        )
        assertNull(result.attempt.createdSearchId)
    }

    @Test
    fun idempotencyBasisIsDeterministicAndInternalOnly() {
        val guardResult = allowedGuardResult()
        val useCase = PlanConfirmedSearchExecutionAttemptUseCase()

        val firstResult = assertIs<ConfirmedSearchExecutionAttemptResult.AttemptPreparedButExecutionBlocked>(
            useCase(
                guardResult = guardResult,
                now = now,
            ),
        )
        val secondResult = assertIs<ConfirmedSearchExecutionAttemptResult.AttemptPreparedButExecutionBlocked>(
            useCase(
                guardResult = guardResult,
                now = now.plusSeconds(1),
            ),
        )

        assertEquals(firstResult.attempt.idempotencyKey, secondResult.attempt.idempotencyKey)
        assertFalse(
            firstResult.attempt.idempotencyKey.value.contains(
                guardResult.commandPlan.command.sessionId.value,
            ),
        )
        assertFalse(
            firstResult.attempt.idempotencyKey.value.contains(
                guardResult.commandPlan.command.criteria.destination,
            ),
        )
    }

    @Test
    fun idempotencyTreatsPermutedChildAgesAsEquivalent() {
        val sessionId = AssistantSessionId("assistant-session-local-000123")
        val firstPlan = commandReadyPlan(
            sessionId = sessionId,
            criteria = hotelSearchCriteria(
                guests = HotelSearchCriteria.Guests(
                    adults = 2,
                    childrenAges = listOf(0, 17),
                ),
            ),
        )
        val secondPlan = commandReadyPlan(
            sessionId = sessionId,
            criteria = hotelSearchCriteria(
                guests = HotelSearchCriteria.Guests(
                    adults = 2,
                    childrenAges = listOf(17, 0),
                ),
            ),
        )

        assertEquals(
            ConfirmedSearchExecutionIdempotencyKey.from(firstPlan),
            ConfirmedSearchExecutionIdempotencyKey.from(secondPlan),
        )
    }

    @Test
    fun duplicateInProgressAttemptIsDetectedAndBlocked() {
        val guardResult = allowedGuardResult()
        val existingAttempt = attempt(
            guardResult = guardResult,
            status = ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS,
        )

        val result = assertIs<ConfirmedSearchExecutionAttemptResult.DuplicateDetected>(
            PlanConfirmedSearchExecutionAttemptUseCase()(
                guardResult = guardResult,
                now = now.plusSeconds(5),
                existingAttempt = existingAttempt,
            ),
        )

        assertEquals(existingAttempt, result.originalAttempt)
        assertEquals(
            ConfirmedSearchExecutionAttemptStatus.DUPLICATE_BLOCKED,
            result.duplicateAttempt.status,
        )
        assertEquals(
            ConfirmedSearchExecutionAttemptResult.DuplicateReason.IN_PROGRESS,
            result.reason,
        )
        assertEquals(
            ConfirmedSearchExecutionAttemptResult.ExecutionBlocker.ACTUAL_EXECUTION_NOT_CONNECTED,
            result.blocker,
        )
    }

    @Test
    fun duplicateSucceededAttemptKeepsModeledSearchReferenceButDoesNotCreateSearch() {
        val guardResult = allowedGuardResult()
        val existingAttempt = attempt(
            guardResult = guardResult,
            status = ConfirmedSearchExecutionAttemptStatus.SUCCEEDED,
            createdSearchId = HotelSearchId("future-modeled-search-id"),
        )

        val result = assertIs<ConfirmedSearchExecutionAttemptResult.DuplicateDetected>(
            PlanConfirmedSearchExecutionAttemptUseCase()(
                guardResult = guardResult,
                now = now.plusSeconds(5),
                existingAttempt = existingAttempt,
            ),
        )

        assertEquals(
            ConfirmedSearchExecutionAttemptResult.DuplicateReason.SUCCEEDED,
            result.reason,
        )
        assertEquals(existingAttempt.createdSearchId, result.duplicateAttempt.createdSearchId)
    }

    @Test
    fun duplicateFailedAttemptIsDetectedAndBlocked() {
        val guardResult = allowedGuardResult()
        val existingAttempt = attempt(
            guardResult = guardResult,
            status = ConfirmedSearchExecutionAttemptStatus.FAILED,
        )

        val result = assertIs<ConfirmedSearchExecutionAttemptResult.DuplicateDetected>(
            PlanConfirmedSearchExecutionAttemptUseCase()(
                guardResult = guardResult,
                now = now.plusSeconds(5),
                existingAttempt = existingAttempt,
            ),
        )

        assertEquals(
            ConfirmedSearchExecutionAttemptResult.DuplicateReason.FAILED,
            result.reason,
        )
    }

    @Test
    fun rejectedGuardResultRejectsAttemptPlanning() {
        val guardResult = ConfirmedSearchExecutionGuardResult.Rejected(
            reason = ConfirmedSearchExecutionGuardResult.RejectionReason
                .NO_ACTIVE_PENDING_CONFIRMATION,
            lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
            executionPolicy = ConfirmedSearchExecutionPolicy(),
        )

        val result = assertIs<ConfirmedSearchExecutionAttemptResult.Rejected>(
            PlanConfirmedSearchExecutionAttemptUseCase()(
                guardResult = guardResult,
                now = now,
            ),
        )

        assertEquals(
            ConfirmedSearchExecutionAttemptResult.RejectionReason.GUARD_REJECTED,
            result.reason,
        )
    }

    @Test
    fun mismatchedExistingAttemptKeyIsRejected() {
        val guardResult = allowedGuardResult()
        val existingAttempt = attempt(
            guardResult = allowedGuardResult(
                sessionId = AssistantSessionId("assistant-session-local-other"),
            ),
        )

        val result = assertIs<ConfirmedSearchExecutionAttemptResult.Rejected>(
            PlanConfirmedSearchExecutionAttemptUseCase()(
                guardResult = guardResult,
                now = now.plusSeconds(5),
                existingAttempt = existingAttempt,
            ),
        )

        assertEquals(
            ConfirmedSearchExecutionAttemptResult.RejectionReason.ATTEMPT_KEY_MISMATCH,
            result.reason,
        )
    }

    @Test
    fun attemptPlanningIsReadOnlyAndDoesNotConsumePendingConfirmation() {
        val store = InMemoryPendingConfirmationStore()
        val pendingConfirmation = store.save(pendingConfirmation())
        val guardResult = allowedGuardResult(
            sessionId = pendingConfirmation.sessionId,
            pendingConfirmation = pendingConfirmation,
        )

        val result = assertIs<ConfirmedSearchExecutionAttemptResult.AttemptPreparedButExecutionBlocked>(
            PlanConfirmedSearchExecutionAttemptUseCase()(
                guardResult = guardResult,
                now = now.plusSeconds(1),
            ),
        )

        assertEquals(pendingConfirmation.sessionId, result.attempt.sessionId)
        assertEquals(
            pendingConfirmation,
            store.findActiveBySession(
                sessionId = pendingConfirmation.sessionId,
                now = now.plusSeconds(2),
            ),
        )
    }

    @Test
    fun attemptResultDoesNotExposeRuntimeSearchExecutionOrInternalCandidateData() {
        val result = PlanConfirmedSearchExecutionAttemptUseCase()(
            guardResult = allowedGuardResult(),
            now = now,
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
                "Confirmed search execution attempt result must not expose $forbidden",
            )
        }
    }

    private fun allowedGuardResult(
        sessionId: AssistantSessionId = AssistantSessionId("assistant-session-local-000123"),
        criteria: HotelSearchCriteria = hotelSearchCriteria(),
        pendingConfirmation: PendingProceedWithCandidateConfirmation = pendingConfirmation(
            sessionId = sessionId,
        ),
    ): ConfirmedSearchExecutionGuardResult.AllowedButBlockedUntilIdempotencyGuard =
        ConfirmedSearchExecutionGuardResult.AllowedButBlockedUntilIdempotencyGuard(
            commandPlan = commandReadyPlan(
                sessionId = sessionId,
                criteria = criteria,
            ),
            pendingConfirmation = pendingConfirmation,
            lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
            executionPolicy = ConfirmedSearchExecutionPolicy(),
        )

    private fun attempt(
        guardResult: ConfirmedSearchExecutionGuardResult.AllowedButBlockedUntilIdempotencyGuard,
        status: ConfirmedSearchExecutionAttemptStatus =
            ConfirmedSearchExecutionAttemptStatus.PREPARED,
        createdSearchId: HotelSearchId? = null,
        expiresAt: Instant = now.plusSeconds(900),
    ): ConfirmedSearchExecutionAttempt =
        ConfirmedSearchExecutionAttempt(
            idempotencyKey = ConfirmedSearchExecutionIdempotencyKey.from(
                guardResult.commandPlan,
            ),
            sessionId = guardResult.commandPlan.command.sessionId,
            commandPlan = guardResult.commandPlan,
            status = status,
            createdSearchId = createdSearchId,
            createdAt = now,
            updatedAt = now,
            expiresAt = expiresAt,
        )

    private fun commandReadyPlan(
        sessionId: AssistantSessionId,
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

    private fun proceedCriteria(
        destination: String = "Rome",
        checkInDate: LocalDate = LocalDate.parse("2026-07-01"),
        checkOutDate: LocalDate = LocalDate.parse("2026-07-04"),
        guests: ProceedWithCandidateCriteria.Guests = ProceedWithCandidateCriteria.Guests(
            adults = 2,
            childrenAges = emptyList(),
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
            childrenAges = emptyList(),
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
