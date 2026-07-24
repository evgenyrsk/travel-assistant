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

class PlanConfirmedSearchExecutionGuardUseCaseTest {

    private val now = Instant.parse("2026-06-25T10:00:00Z")
    private val mapper = ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper()

    @Test
    fun activeSessionBoundPendingAndMatchingCommandReturnsBlockedResult() {
        val useCase = PlanConfirmedSearchExecutionGuardUseCase()
        val pendingConfirmation = pendingConfirmation()
        val commandPlan = commandReadyPlan(
            sessionId = pendingConfirmation.sessionId,
            criteria = mapper(pendingConfirmation.criteria),
        )

        val result = assertIs<ConfirmedSearchExecutionGuardResult.AllowedButBlockedUntilIdempotencyGuard>(
            useCase(
                guardRequest(
                    sessionId = pendingConfirmation.sessionId,
                    commandPlan = commandPlan,
                    pendingConfirmation = pendingConfirmation,
                ),
            ),
        )

        assertEquals(commandPlan, result.commandPlan)
        assertEquals(pendingConfirmation, result.pendingConfirmation)
        assertEquals(
            ConfirmedSearchExecutionGuardResult.ExecutionBlocker.IDEMPOTENCY_GUARD_REQUIRED,
            result.blocker,
        )
    }

    @Test
    fun missingPendingConfirmationIsRejected() {
        val result = assertIs<ConfirmedSearchExecutionGuardResult.Rejected>(
            PlanConfirmedSearchExecutionGuardUseCase()(
                guardRequest(pendingConfirmation = null),
            ),
        )

        assertEquals(
            ConfirmedSearchExecutionGuardResult.RejectionReason.NO_ACTIVE_PENDING_CONFIRMATION,
            result.reason,
        )
    }

    @Test
    fun expiredPendingConfirmationIsRejected() {
        val pendingConfirmation = pendingConfirmation(
            expiresAt = now.minusSeconds(1),
        )

        val result = assertIs<ConfirmedSearchExecutionGuardResult.Rejected>(
            PlanConfirmedSearchExecutionGuardUseCase()(
                guardRequest(
                    pendingConfirmation = pendingConfirmation,
                    commandPlan = commandReadyPlan(
                        sessionId = pendingConfirmation.sessionId,
                        criteria = mapper(pendingConfirmation.criteria),
                    ),
                ),
            ),
        )

        assertEquals(
            ConfirmedSearchExecutionGuardResult.RejectionReason.PENDING_CONFIRMATION_EXPIRED,
            result.reason,
        )
    }

    @Test
    fun consumedPendingConfirmationIsRejected() {
        val pendingConfirmation = pendingConfirmation(
            status = PendingConfirmationStatus.CONSUMED,
        )

        val result = assertIs<ConfirmedSearchExecutionGuardResult.Rejected>(
            PlanConfirmedSearchExecutionGuardUseCase()(
                guardRequest(
                    pendingConfirmation = pendingConfirmation,
                    commandPlan = commandReadyPlan(
                        sessionId = pendingConfirmation.sessionId,
                        criteria = mapper(pendingConfirmation.criteria),
                    ),
                ),
            ),
        )

        assertEquals(
            ConfirmedSearchExecutionGuardResult.RejectionReason.PENDING_CONFIRMATION_CONSUMED,
            result.reason,
        )
    }

    @Test
    fun pendingSessionMismatchIsRejected() {
        val routeSessionId = AssistantSessionId("assistant-session-local-route")
        val pendingConfirmation = pendingConfirmation(
            sessionId = AssistantSessionId("assistant-session-local-pending"),
        )

        val result = assertIs<ConfirmedSearchExecutionGuardResult.Rejected>(
            PlanConfirmedSearchExecutionGuardUseCase()(
                guardRequest(
                    sessionId = routeSessionId,
                    commandPlan = commandReadyPlan(
                        sessionId = routeSessionId,
                        criteria = mapper(pendingConfirmation.criteria),
                    ),
                    pendingConfirmation = pendingConfirmation,
                ),
            ),
        )

        assertEquals(
            ConfirmedSearchExecutionGuardResult.RejectionReason.SESSION_MISMATCH,
            result.reason,
        )
    }

    @Test
    fun commandSessionMismatchIsRejected() {
        val routeSessionId = AssistantSessionId("assistant-session-local-route")
        val pendingConfirmation = pendingConfirmation(sessionId = routeSessionId)

        val result = assertIs<ConfirmedSearchExecutionGuardResult.Rejected>(
            PlanConfirmedSearchExecutionGuardUseCase()(
                guardRequest(
                    sessionId = routeSessionId,
                    commandPlan = commandReadyPlan(
                        sessionId = AssistantSessionId("assistant-session-local-command"),
                        criteria = mapper(pendingConfirmation.criteria),
                    ),
                    pendingConfirmation = pendingConfirmation,
                ),
            ),
        )

        assertEquals(
            ConfirmedSearchExecutionGuardResult.RejectionReason.SESSION_MISMATCH,
            result.reason,
        )
    }

    @Test
    fun criteriaMismatchIsRejected() {
        val pendingConfirmation = pendingConfirmation()

        val result = assertIs<ConfirmedSearchExecutionGuardResult.Rejected>(
            PlanConfirmedSearchExecutionGuardUseCase()(
                guardRequest(
                    sessionId = pendingConfirmation.sessionId,
                    commandPlan = commandReadyPlan(
                        sessionId = pendingConfirmation.sessionId,
                        criteria = hotelSearchCriteria(destination = "Paris"),
                    ),
                    pendingConfirmation = pendingConfirmation,
                ),
            ),
        )

        assertEquals(
            ConfirmedSearchExecutionGuardResult.RejectionReason.CRITERIA_MISMATCH,
            result.reason,
        )
    }

    @Test
    fun idempotencyGuardIsRequiredBeforeExecution() {
        val useCase = PlanConfirmedSearchExecutionGuardUseCase()
        val pendingConfirmation = pendingConfirmation()

        val result = assertIs<ConfirmedSearchExecutionGuardResult.AllowedButBlockedUntilIdempotencyGuard>(
            useCase(
                guardRequest(
                    sessionId = pendingConfirmation.sessionId,
                    commandPlan = commandReadyPlan(
                        sessionId = pendingConfirmation.sessionId,
                        criteria = mapper(pendingConfirmation.criteria),
                    ),
                    pendingConfirmation = pendingConfirmation,
                ),
            ),
        )

        assertEquals(
            ConfirmedSearchExecutionPolicy.DuplicateHandling
                .REQUIRE_IDEMPOTENCY_GUARD_BEFORE_EXECUTION,
            result.executionPolicy.duplicateHandling,
        )
        assertEquals(
            ConfirmedSearchExecutionGuardResult.ExecutionBlocker.IDEMPOTENCY_GUARD_REQUIRED,
            result.blocker,
        )
    }

    @Test
    fun guardIsReadOnlyAndDoesNotConsumePendingConfirmation() {
        val store = InMemoryPendingConfirmationStore()
        val pendingConfirmation = store.save(pendingConfirmation())
        val snapshot = store.findActiveBySession(
            sessionId = pendingConfirmation.sessionId,
            now = now.plusSeconds(60),
        )

        val result = assertIs<ConfirmedSearchExecutionGuardResult.AllowedButBlockedUntilIdempotencyGuard>(
            PlanConfirmedSearchExecutionGuardUseCase()(
                guardRequest(
                    sessionId = pendingConfirmation.sessionId,
                    commandPlan = commandReadyPlan(
                        sessionId = pendingConfirmation.sessionId,
                        criteria = mapper(pendingConfirmation.criteria),
                    ),
                    pendingConfirmation = snapshot,
                    now = now.plusSeconds(60),
                ),
            ),
        )

        assertEquals(pendingConfirmation, result.pendingConfirmation)
        assertEquals(
            pendingConfirmation,
            store.findActiveBySession(
                sessionId = pendingConfirmation.sessionId,
                now = now.plusSeconds(61),
            ),
        )
    }

    @Test
    fun guardResultDoesNotExposeSearchExecutionOrInternalCandidateData() {
        val pendingConfirmation = pendingConfirmation()

        val result = PlanConfirmedSearchExecutionGuardUseCase()(
            guardRequest(
                sessionId = pendingConfirmation.sessionId,
                commandPlan = commandReadyPlan(
                    sessionId = pendingConfirmation.sessionId,
                    criteria = mapper(pendingConfirmation.criteria),
                ),
                pendingConfirmation = pendingConfirmation,
            ),
        )
        val resultText = result.toString()

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
                "Confirmed search guard result must not expose $forbidden",
            )
        }
    }

    @Test
    fun remainsDeterministicForSameGuardRequest() {
        val useCase = PlanConfirmedSearchExecutionGuardUseCase()
        val pendingConfirmation = pendingConfirmation()
        val request = guardRequest(
            sessionId = pendingConfirmation.sessionId,
            commandPlan = commandReadyPlan(
                sessionId = pendingConfirmation.sessionId,
                criteria = mapper(pendingConfirmation.criteria),
            ),
            pendingConfirmation = pendingConfirmation,
        )

        val firstResult = useCase(request)
        val secondResult = useCase(request)

        assertEquals(firstResult, secondResult)
    }

    private fun guardRequest(
        sessionId: AssistantSessionId = AssistantSessionId("assistant-session-local-000123"),
        commandPlan: ConfirmedSearchCreationCommandPlan.CommandReady =
            commandReadyPlan(sessionId = sessionId),
        pendingConfirmation: PendingProceedWithCandidateConfirmation? = pendingConfirmation(
            sessionId = sessionId,
        ),
        now: Instant = this.now,
    ): ConfirmedSearchExecutionGuardRequest =
        ConfirmedSearchExecutionGuardRequest(
            sessionId = sessionId,
            commandPlan = commandPlan,
            pendingConfirmation = pendingConfirmation,
            now = now,
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
