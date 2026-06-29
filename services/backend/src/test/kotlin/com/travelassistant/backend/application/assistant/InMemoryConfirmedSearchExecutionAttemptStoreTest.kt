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

class InMemoryConfirmedSearchExecutionAttemptStoreTest {

    private val now = Instant.parse("2026-06-25T10:00:00Z")

    @Test
    fun savesAndFindsPreparedAttemptByIdempotencyKey() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val attempt = preparedAttempt()

        val result = assertIs<ConfirmedSearchExecutionAttemptStoreResult.Stored>(
            store.savePrepared(attempt),
        )

        assertEquals(attempt, result.attempt)
        assertEquals(attempt, store.findByIdempotencyKey(attempt.idempotencyKey))
    }

    @Test
    fun savingSamePreparedAttemptReturnsDuplicateWithoutReplacingOriginal() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val originalAttempt = preparedAttempt()
        val duplicateAttempt = originalAttempt.copy(updatedAt = now.plusSeconds(30))

        store.savePrepared(originalAttempt)

        val result = assertIs<ConfirmedSearchExecutionAttemptStoreResult.Duplicate>(
            store.savePrepared(duplicateAttempt),
        )

        assertEquals(originalAttempt, result.existingAttempt)
        assertEquals(originalAttempt, store.findByIdempotencyKey(originalAttempt.idempotencyKey))
    }

    @Test
    fun preparedAttemptCanBeMarkedInProgress() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val attempt = preparedAttempt()
        store.savePrepared(attempt)

        val result = assertIs<ConfirmedSearchExecutionAttemptStoreResult.Stored>(
            store.markInProgress(
                idempotencyKey = attempt.idempotencyKey,
                now = now.plusSeconds(1),
            ),
        )

        assertEquals(ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS, result.attempt.status)
        assertEquals(now.plusSeconds(1), result.attempt.updatedAt)
        assertNull(result.attempt.createdSearchId)
        assertEquals(result.attempt, store.findByIdempotencyKey(attempt.idempotencyKey))
    }

    @Test
    fun inProgressDuplicateIsDetectedWithoutCreatingNewAttempt() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val inProgressAttempt = savedInProgressAttempt(store)

        val result = assertIs<ConfirmedSearchExecutionAttemptStoreResult.Duplicate>(
            store.markInProgress(
                idempotencyKey = inProgressAttempt.idempotencyKey,
                now = now.plusSeconds(2),
            ),
        )

        assertEquals(inProgressAttempt, result.existingAttempt)
        assertEquals(
            inProgressAttempt,
            store.findByIdempotencyKey(inProgressAttempt.idempotencyKey),
        )
    }

    @Test
    fun inProgressAttemptCanBeMarkedSucceededWithModeledSearchReference() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val inProgressAttempt = savedInProgressAttempt(store)
        val createdSearchId = HotelSearchId("future-modeled-search-id")

        val result = assertIs<ConfirmedSearchExecutionAttemptStoreResult.Stored>(
            store.markSucceeded(
                idempotencyKey = inProgressAttempt.idempotencyKey,
                createdSearchId = createdSearchId,
                now = now.plusSeconds(2),
            ),
        )

        assertEquals(ConfirmedSearchExecutionAttemptStatus.SUCCEEDED, result.attempt.status)
        assertEquals(createdSearchId, result.attempt.createdSearchId)
        assertNull(result.attempt.failureReason)
        assertEquals(result.attempt, store.findByIdempotencyKey(inProgressAttempt.idempotencyKey))
    }

    @Test
    fun duplicateSucceededAttemptReturnsSameStoredSuccessReference() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val inProgressAttempt = savedInProgressAttempt(store)
        val createdSearchId = HotelSearchId("future-modeled-search-id")
        val succeededAttempt = assertIs<ConfirmedSearchExecutionAttemptStoreResult.Stored>(
            store.markSucceeded(
                idempotencyKey = inProgressAttempt.idempotencyKey,
                createdSearchId = createdSearchId,
                now = now.plusSeconds(2),
            ),
        ).attempt

        val result = assertIs<ConfirmedSearchExecutionAttemptStoreResult.Duplicate>(
            store.savePrepared(preparedAttempt()),
        )

        assertEquals(ConfirmedSearchExecutionAttemptStatus.SUCCEEDED, result.existingAttempt.status)
        assertEquals(succeededAttempt.createdSearchId, result.existingAttempt.createdSearchId)
    }

    @Test
    fun inProgressAttemptCanBeMarkedFailed() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val inProgressAttempt = savedInProgressAttempt(store)

        val result = assertIs<ConfirmedSearchExecutionAttemptStoreResult.Stored>(
            store.markFailed(
                idempotencyKey = inProgressAttempt.idempotencyKey,
                reason = ConfirmedSearchExecutionAttemptFailureReason.SEARCH_CREATION_FAILED,
                now = now.plusSeconds(2),
            ),
        )

        assertEquals(ConfirmedSearchExecutionAttemptStatus.FAILED, result.attempt.status)
        assertEquals(
            ConfirmedSearchExecutionAttemptFailureReason.SEARCH_CREATION_FAILED,
            result.attempt.failureReason,
        )
        assertNull(result.attempt.createdSearchId)
        assertEquals(result.attempt, store.findByIdempotencyKey(inProgressAttempt.idempotencyKey))
    }

    @Test
    fun duplicateFailedAttemptReturnsStoredFailedState() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val inProgressAttempt = savedInProgressAttempt(store)
        val failedAttempt = assertIs<ConfirmedSearchExecutionAttemptStoreResult.Stored>(
            store.markFailed(
                idempotencyKey = inProgressAttempt.idempotencyKey,
                reason = ConfirmedSearchExecutionAttemptFailureReason.EXECUTION_STATE_UNKNOWN,
                now = now.plusSeconds(2),
            ),
        ).attempt

        val result = assertIs<ConfirmedSearchExecutionAttemptStoreResult.Duplicate>(
            store.savePrepared(preparedAttempt()),
        )

        assertEquals(failedAttempt, result.existingAttempt)
        assertEquals(
            ConfirmedSearchExecutionAttemptFailureReason.EXECUTION_STATE_UNKNOWN,
            result.existingAttempt.failureReason,
        )
    }

    @Test
    fun invalidTransitionsAreRejectedWithoutMutatingAttempt() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val attempt = preparedAttempt()
        store.savePrepared(attempt)

        val result = assertIs<ConfirmedSearchExecutionAttemptStoreResult.Rejected>(
            store.markSucceeded(
                idempotencyKey = attempt.idempotencyKey,
                createdSearchId = HotelSearchId("future-modeled-search-id"),
                now = now.plusSeconds(1),
            ),
        )

        assertEquals(
            ConfirmedSearchExecutionAttemptStoreResult.RejectionReason.ATTEMPT_NOT_IN_PROGRESS,
            result.reason,
        )
        assertEquals(attempt, store.findByIdempotencyKey(attempt.idempotencyKey))
    }

    @Test
    fun missingAttemptTransitionIsRejected() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()

        val result = assertIs<ConfirmedSearchExecutionAttemptStoreResult.Rejected>(
            store.markInProgress(
                idempotencyKey = ConfirmedSearchExecutionIdempotencyKey.from(commandReadyPlan()),
                now = now.plusSeconds(1),
            ),
        )

        assertEquals(
            ConfirmedSearchExecutionAttemptStoreResult.RejectionReason.ATTEMPT_NOT_FOUND,
            result.reason,
        )
    }

    @Test
    fun storeIsProcessLocal() {
        val firstStore = InMemoryConfirmedSearchExecutionAttemptStore()
        val secondStore = InMemoryConfirmedSearchExecutionAttemptStore()
        val attempt = preparedAttempt()

        firstStore.savePrepared(attempt)

        assertEquals(attempt, firstStore.findByIdempotencyKey(attempt.idempotencyKey))
        assertNull(secondStore.findByIdempotencyKey(attempt.idempotencyKey))
    }

    @Test
    fun storeDoesNotMutatePendingConfirmationOrExposeSearchExecutionMarkers() {
        val pendingStore = InMemoryPendingConfirmationStore()
        val pendingConfirmation = pendingStore.save(pendingConfirmation())
        val attemptStore = InMemoryConfirmedSearchExecutionAttemptStore()
        val attempt = preparedAttempt(sessionId = pendingConfirmation.sessionId)

        val result = attemptStore.savePrepared(attempt)
        val resultText = result.toString()

        assertEquals(
            pendingConfirmation,
            pendingStore.findActiveBySession(
                sessionId = pendingConfirmation.sessionId,
                now = now.plusSeconds(1),
            ),
        )
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
                "Confirmed search execution attempt store result must not expose $forbidden",
            )
        }
    }

    private fun savedInProgressAttempt(
        store: InMemoryConfirmedSearchExecutionAttemptStore,
    ): ConfirmedSearchExecutionAttempt {
        val attempt = preparedAttempt()
        store.savePrepared(attempt)

        return assertIs<ConfirmedSearchExecutionAttemptStoreResult.Stored>(
            store.markInProgress(
                idempotencyKey = attempt.idempotencyKey,
                now = now.plusSeconds(1),
            ),
        ).attempt
    }

    private fun preparedAttempt(
        sessionId: AssistantSessionId = AssistantSessionId("assistant-session-local-000123"),
        commandPlan: ConfirmedSearchCreationCommandPlan.CommandReady =
            commandReadyPlan(sessionId = sessionId),
        createdAt: Instant = now,
        updatedAt: Instant = now,
    ): ConfirmedSearchExecutionAttempt =
        ConfirmedSearchExecutionAttempt(
            idempotencyKey = ConfirmedSearchExecutionIdempotencyKey.from(commandPlan),
            sessionId = sessionId,
            commandPlan = commandPlan,
            status = ConfirmedSearchExecutionAttemptStatus.PREPARED,
            createdAt = createdAt,
            updatedAt = updatedAt,
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
