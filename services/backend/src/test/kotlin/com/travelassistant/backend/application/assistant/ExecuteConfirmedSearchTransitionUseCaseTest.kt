package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.hotel.CreateHotelSearchCommand
import com.travelassistant.backend.application.hotel.HotelSearchBoundary
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchId
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class ExecuteConfirmedSearchTransitionUseCaseTest {

    private val now = Instant.parse("2026-06-25T10:00:00Z")

    @Test
    fun successfulTransitionCreatesSearchAndRecordsSucceeded() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val fakeSearch = fakeHotelSearch("hotel-search-local-test-001")
        val boundary = StubHotelSearchBoundary(fakeSearch)
        val useCase = ExecuteConfirmedSearchTransitionUseCase(
            attemptStore = store,
            hotelSearchBoundary = boundary,
        )

        val result = assertIs<ExecuteConfirmedSearchTransitionResult.Transitioned>(
            useCase(transitionRequest()),
        )

        assertEquals(ConfirmedSearchExecutionAttemptStatus.SUCCEEDED, result.attempt.status)
        assertEquals(fakeSearch.id, result.attempt.createdSearchId)
        assertNull(result.attempt.failureReason)
        assertIs<ConfirmedSearchExecutionResult.SearchCreated>(result.executionResult)
        assertEquals(fakeSearch.id, (result.executionResult as ConfirmedSearchExecutionResult.SearchCreated).searchId)
        assertEquals(1, boundary.createSearchCallCount)
    }

    @Test
    fun duplicateAfterSuccessDoesNotCreateSecondSearch() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val fakeSearch = fakeHotelSearch("hotel-search-local-test-001")
        val boundary = StubHotelSearchBoundary(fakeSearch)
        val useCase = ExecuteConfirmedSearchTransitionUseCase(
            attemptStore = store,
            hotelSearchBoundary = boundary,
        )
        val request = transitionRequest()

        val firstResult = assertIs<ExecuteConfirmedSearchTransitionResult.Transitioned>(
            useCase(request),
        )

        val secondResult = assertIs<ExecuteConfirmedSearchTransitionResult.DuplicateDetected>(
            useCase(request),
        )

        assertEquals(1, boundary.createSearchCallCount)
        assertEquals(
            ConfirmedSearchExecutionAttemptResult.DuplicateReason.SUCCEEDED,
            secondResult.duplicateReason,
        )
        assertEquals(firstResult.attempt.createdSearchId, secondResult.existingAttempt.createdSearchId)
    }

    @Test
    fun duplicateAfterSuccessReusesExistingHotelSearchId() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val fakeSearch = fakeHotelSearch("hotel-search-local-reuse-001")
        val boundary = StubHotelSearchBoundary(fakeSearch)
        val useCase = ExecuteConfirmedSearchTransitionUseCase(
            attemptStore = store,
            hotelSearchBoundary = boundary,
        )
        val request = transitionRequest()

        val firstResult = assertIs<ExecuteConfirmedSearchTransitionResult.Transitioned>(
            useCase(request),
        )

        val secondResult = assertIs<ExecuteConfirmedSearchTransitionResult.DuplicateDetected>(
            useCase(request),
        )

        assertNotNull(firstResult.attempt.createdSearchId)
        assertEquals(firstResult.attempt.createdSearchId, secondResult.existingAttempt.createdSearchId)
    }

    @Test
    fun failedSearchCreationRecordsFailedWithSearchCreationFailedReason() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val boundary = FailingHotelSearchBoundary()
        val useCase = ExecuteConfirmedSearchTransitionUseCase(
            attemptStore = store,
            hotelSearchBoundary = boundary,
        )

        val result = assertIs<ExecuteConfirmedSearchTransitionResult.StoreRejected>(
            useCase(transitionRequest()),
        )

        assertEquals(1, boundary.createSearchCallCount)

        val storedAttempt = store.findByIdempotencyKey(
            ConfirmedSearchExecutionIdempotencyKey.from(commandReadyPlan()),
            now,
        )
        assertNotNull(storedAttempt)
        assertEquals(ConfirmedSearchExecutionAttemptStatus.FAILED, storedAttempt.status)
        assertEquals(
            ConfirmedSearchExecutionAttemptFailureReason.SEARCH_CREATION_FAILED,
            storedAttempt.failureReason,
        )
    }

    @Test
    fun failedSearchCreationDoesNotConsumePendingConfirmation() {
        val pendingStore = InMemoryPendingConfirmationStore()
        val pendingConfirmation = pendingStore.save(pendingConfirmation())
        val attemptStore = InMemoryConfirmedSearchExecutionAttemptStore()
        val boundary = FailingHotelSearchBoundary()
        val useCase = ExecuteConfirmedSearchTransitionUseCase(
            attemptStore = attemptStore,
            hotelSearchBoundary = boundary,
        )

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

    @Test
    fun guardRejectedReturnsGuardRejectedWithoutCallingSearchCreation() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val boundary = StubHotelSearchBoundary(fakeHotelSearch("hotel-search-local-guard-001"))
        val useCase = ExecuteConfirmedSearchTransitionUseCase(
            attemptStore = store,
            hotelSearchBoundary = boundary,
        )

        val result = assertIs<ExecuteConfirmedSearchTransitionResult.GuardRejected>(
            useCase(transitionRequest(pendingConfirmation = null)),
        )

        assertEquals(
            ConfirmedSearchExecutionAttemptResult.RejectionReason.GUARD_REJECTED,
            result.attemptRejectionReason,
        )
        assertEquals(0, boundary.createSearchCallCount)
    }

    @Test
    fun retryAfterStaleInProgressCreatesNewSearch() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val retrySearch = fakeHotelSearch("hotel-search-local-retry-001")
        val boundary = SequenceHotelSearchBoundary(listOf(retrySearch))
        val useCase = ExecuteConfirmedSearchTransitionUseCase(
            attemptStore = store,
            hotelSearchBoundary = boundary,
        )
        val commandPlan = commandReadyPlan()
        val idempotencyKey = ConfirmedSearchExecutionIdempotencyKey.from(commandPlan)

        val staleAttempt = ConfirmedSearchExecutionAttempt(
            idempotencyKey = idempotencyKey,
            sessionId = commandPlan.command.sessionId,
            commandPlan = commandPlan,
            status = ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS,
            createdAt = now,
            updatedAt = now,
            expiresAt = now.plusSeconds(60),
        )
        store.savePrepared(staleAttempt)
        store.markInProgress(idempotencyKey, now.plusSeconds(1))

        val retryRequest = transitionRequest(
            now = now.plusSeconds(61),
            pendingConfirmation = pendingConfirmation(expiresAt = now.plusSeconds(3600)),
        )
        val retryResult = assertIs<ExecuteConfirmedSearchTransitionResult.Transitioned>(
            useCase(retryRequest),
        )

        assertEquals(ConfirmedSearchExecutionAttemptStatus.SUCCEEDED, retryResult.attempt.status)
        assertEquals(retrySearch.id, retryResult.attempt.createdSearchId)
        assertEquals(1, boundary.createSearchCallCount)
    }

    @Test
    fun retryBlockedAfterExecutionStateUnknown() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val boundary = StubHotelSearchBoundary(fakeHotelSearch("hotel-search-local-blocked-001"))
        val useCase = ExecuteConfirmedSearchTransitionUseCase(
            attemptStore = store,
            hotelSearchBoundary = boundary,
        )
        val commandPlan = commandReadyPlan()
        val idempotencyKey = ConfirmedSearchExecutionIdempotencyKey.from(commandPlan)

        val manualAttempt = ConfirmedSearchExecutionAttempt(
            idempotencyKey = idempotencyKey,
            sessionId = commandPlan.command.sessionId,
            commandPlan = commandPlan,
            status = ConfirmedSearchExecutionAttemptStatus.PREPARED,
            createdAt = now,
            updatedAt = now,
            expiresAt = now.plusSeconds(900),
        )
        store.savePrepared(manualAttempt)
        store.markInProgress(idempotencyKey, now.plusSeconds(1))
        store.markFailed(
            idempotencyKey,
            ConfirmedSearchExecutionAttemptFailureReason.EXECUTION_STATE_UNKNOWN,
            now.plusSeconds(2),
        )

        val retryResult = assertIs<ExecuteConfirmedSearchTransitionResult.DuplicateDetected>(
            useCase(transitionRequest()),
        )

        assertEquals(
            ConfirmedSearchExecutionAttemptResult.DuplicateReason.FAILED,
            retryResult.duplicateReason,
        )
        assertEquals(0, boundary.createSearchCallCount)
    }

    @Test
    fun doesNotMutatePendingConfirmationStore() {
        val pendingStore = InMemoryPendingConfirmationStore()
        val pendingConfirmation = pendingStore.save(pendingConfirmation())
        val attemptStore = InMemoryConfirmedSearchExecutionAttemptStore()
        val boundary = StubHotelSearchBoundary(fakeHotelSearch("hotel-search-local-pending-001"))
        val useCase = ExecuteConfirmedSearchTransitionUseCase(
            attemptStore = attemptStore,
            hotelSearchBoundary = boundary,
        )

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

    private fun fakeHotelSearch(id: String): HotelSearch =
        HotelSearch(
            id = HotelSearchId(id),
            sessionId = AssistantSessionId("assistant-session-local-000123"),
            criteria = HotelSearchCriteria(
                destination = "Rome",
                checkInDate = LocalDate.parse("2026-07-01"),
                checkOutDate = LocalDate.parse("2026-07-04"),
                guests = HotelSearchCriteria.Guests(adults = 2),
                rooms = 1,
            ),
            status = HotelSearch.Status.COMPLETED_WITH_OFFERS,
            offers = emptyList(),
        )

    private class StubHotelSearchBoundary(
        private val search: HotelSearch,
    ) : HotelSearchBoundary {
        var createSearchCallCount = 0
            private set

        override fun createSearch(command: CreateHotelSearchCommand): HotelSearch {
            createSearchCallCount++
            return search
        }

        override fun getSearch(searchId: HotelSearchId): HotelSearch = search
    }

    private class FailingHotelSearchBoundary : HotelSearchBoundary {
        var createSearchCallCount = 0
            private set

        override fun createSearch(command: CreateHotelSearchCommand): HotelSearch {
            createSearchCallCount++
            throw RuntimeException("Simulated search creation failure")
        }

        override fun getSearch(searchId: HotelSearchId): HotelSearch =
            throw RuntimeException("Not expected in tests")
    }

    private class SequenceHotelSearchBoundary(
        private val searches: List<HotelSearch>,
    ) : HotelSearchBoundary {
        var createSearchCallCount = 0
            private set

        override fun createSearch(command: CreateHotelSearchCommand): HotelSearch {
            val index = createSearchCallCount
            createSearchCallCount++
            return searches[index]
        }

        override fun getSearch(searchId: HotelSearchId): HotelSearch =
            searches.first { it.id == searchId }
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
        expiresAt: Instant = now.plusSeconds(900),
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
