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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ComposeConfirmedSearchTransitionResponseUseCaseTest {

    private val now = Instant.parse("2026-06-25T10:00:00Z")

    @Test
    fun successfulTransitionComposesShowHotelResultsDirective() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = store,
                hotelSearchBoundary = TestHotelSearchBoundary(),
            ),
        )

        val result = useCase(compositionRequest())

        assertIs<ExecuteConfirmedSearchTransitionResult.Transitioned>(result.transitionResult)
        assertEquals(InternalTransitionNextAction.SHOW_HOTEL_RESULTS, result.responseDirective.nextAction)
        assertEquals(TransitionMessageKind.RESULTS_READY, result.responseDirective.messageKind)
        assertEquals(
            "The search is ready. Hotel results are available.",
            result.messageText,
        )
        assertEquals(
            PendingConsumeInstruction.CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS,
            result.pendingConsumeInstruction,
        )
        assertNotNull(result.hotelSearchId)
    }

    @Test
    fun successfulTransitionIncludesHotelSearchId() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = store,
                hotelSearchBoundary = TestHotelSearchBoundary(),
            ),
        )

        val result = useCase(compositionRequest())

        assertNotNull(result.responseDirective.hotelSearchId)
        assertNotNull(result.hotelSearchId)
        assertEquals(result.responseDirective.hotelSearchId, result.hotelSearchId)
    }

    @Test
    fun successfulTransitionRequestsHotelResults() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = store,
                hotelSearchBoundary = TestHotelSearchBoundary(),
            ),
        )

        val result = useCase(compositionRequest())

        assertEquals(true, result.responseDirective.mayShowHotelResults)
        assertEquals(InternalTransitionNextAction.SHOW_HOTEL_RESULTS, result.responseDirective.nextAction)
    }

    @Test
    fun successfulTransitionRequestsPendingConsume() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = store,
                hotelSearchBoundary = TestHotelSearchBoundary(),
            ),
        )

        val result = useCase(compositionRequest())

        assertEquals(
            PendingConsumeInstruction.CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS,
            result.pendingConsumeInstruction,
        )
        assertEquals(true, result.responseDirective.shouldConsumePendingConfirmation)
    }

    @Test
    fun duplicateSucceededComposesShowHotelResultsWithExistingSearchId() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = store,
                hotelSearchBoundary = TestHotelSearchBoundary(),
            ),
        )
        val request = compositionRequest(
            pendingConfirmation = pendingConfirmation(expiresAt = now.plusSeconds(3600)),
        )

        val firstResult = useCase(request)

        val secondResult = useCase(
            compositionRequest(
                now = now.plusSeconds(1),
                pendingConfirmation = pendingConfirmation(expiresAt = now.plusSeconds(3600)),
            ),
        )

        assertIs<ExecuteConfirmedSearchTransitionResult.DuplicateDetected>(secondResult.transitionResult)
        assertEquals(InternalTransitionNextAction.SHOW_HOTEL_RESULTS, secondResult.responseDirective.nextAction)
        assertEquals(TransitionMessageKind.RESULTS_READY, secondResult.responseDirective.messageKind)
        assertNotNull(secondResult.hotelSearchId)
        assertEquals(firstResult.hotelSearchId, secondResult.hotelSearchId)
    }

    @Test
    fun guardRejectedComposesConfirmationRejectedMessageWithoutConsume() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = store,
                hotelSearchBoundary = TestHotelSearchBoundary(),
            ),
        )

        val result = useCase(
            compositionRequest(pendingConfirmation = null),
        )

        assertIs<ExecuteConfirmedSearchTransitionResult.GuardRejected>(result.transitionResult)
        assertEquals(TransitionMessageKind.CONFIRMATION_REJECTED, result.responseDirective.messageKind)
        assertEquals(
            "I could not proceed with the current confirmation state.",
            result.messageText,
        )
        assertEquals(
            PendingConsumeInstruction.DO_NOT_CONSUME_PENDING_CONFIRMATION,
            result.pendingConsumeInstruction,
        )
    }

    @Test
    fun compositionDoesNotRequireCreateHotelSearchUseCase() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = store,
                hotelSearchBoundary = TestHotelSearchBoundary(),
            ),
        )

        val result = useCase(compositionRequest())

        assertIs<ComposeConfirmedSearchTransitionResponseResult>(result)
    }

    @Test
    fun compositionDoesNotMutatePendingConfirmationStore() {
        val pendingStore = InMemoryPendingConfirmationStore()
        val pendingConfirmation = pendingStore.save(pendingConfirmation())
        val attemptStore = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = attemptStore,
                hotelSearchBoundary = TestHotelSearchBoundary(),
            ),
        )

        useCase(
            compositionRequest(
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
    fun composedResultDoesNotExposeForbiddenTokens() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val useCase = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = store,
                hotelSearchBoundary = TestHotelSearchBoundary(),
            ),
        )

        val result = useCase(compositionRequest())
        val resultText = result.toString()

        listOf(
            "CreateHotelSearchUseCase",
            "markConsumed",
            "provider",
        ).forEach { forbidden ->
            assertFalse(
                resultText.contains(forbidden),
                "Composed result must not expose $forbidden",
            )
        }
    }

    @Test
    fun duplicateSucceededWithSearchIdComposesShowHotelResultsDirective() {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val criteria = proceedCriteria()
        val commandPlan = ConfirmedSearchCreationCommandPlan.CommandReady(
            command = CreateHotelSearchCommand(
                sessionId = AssistantSessionId("assistant-session-local-000123"),
                criteria = HotelSearchCriteria(
                    destination = criteria.destination,
                    checkInDate = criteria.checkInDate,
                    checkOutDate = criteria.checkOutDate,
                    guests = HotelSearchCriteria.Guests(
                        adults = criteria.guests.adults,
                        children = criteria.guests.children,
                    ),
                    rooms = criteria.rooms,
                ),
            ),
            lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
        )
        val attempt = ConfirmedSearchExecutionAttempt(
            idempotencyKey = ConfirmedSearchExecutionIdempotencyKey.from(commandPlan),
            sessionId = AssistantSessionId("assistant-session-local-000123"),
            commandPlan = commandPlan,
            status = ConfirmedSearchExecutionAttemptStatus.PREPARED,
            createdAt = now,
            updatedAt = now,
            expiresAt = now.plusSeconds(900),
        )
        store.savePrepared(attempt)
        store.markInProgress(attempt.idempotencyKey, now.plusSeconds(1))
        val searchId = HotelSearchId("hotel-search-local-composed-001")
        store.markSucceeded(attempt.idempotencyKey, searchId, now.plusSeconds(2))

        val useCase = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = store,
                hotelSearchBoundary = TestHotelSearchBoundary(),
            ),
        )

        val result = useCase(
            compositionRequest(
                pendingConfirmation = pendingConfirmation(
                    expiresAt = now.plusSeconds(3600),
                ),
            ),
        )

        assertIs<ExecuteConfirmedSearchTransitionResult.DuplicateDetected>(result.transitionResult)
        assertEquals(InternalTransitionNextAction.SHOW_HOTEL_RESULTS, result.responseDirective.nextAction)
        assertEquals(TransitionMessageKind.RESULTS_READY, result.responseDirective.messageKind)
        assertEquals(searchId, result.responseDirective.hotelSearchId)
        assertEquals(searchId, result.hotelSearchId)
        assertEquals(true, result.responseDirective.mayShowHotelResults)
        assertEquals(true, result.responseDirective.shouldConsumePendingConfirmation)
        assertEquals(
            PendingConsumeInstruction.CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS,
            result.pendingConsumeInstruction,
        )
        assertEquals(
            "The search is ready. Hotel results are available.",
            result.messageText,
        )
    }

    private fun compositionRequest(
        sessionId: AssistantSessionId = AssistantSessionId("assistant-session-local-000123"),
        criteria: ProceedWithCandidateCriteria = proceedCriteria(),
        pendingConfirmation: PendingProceedWithCandidateConfirmation? =
            pendingConfirmation(sessionId = sessionId, criteria = criteria),
        now: Instant = this.now,
    ): ComposeConfirmedSearchTransitionResponseRequest =
        ComposeConfirmedSearchTransitionResponseRequest(
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
}

private class TestHotelSearchBoundary : HotelSearchBoundary {
    override fun createSearch(command: CreateHotelSearchCommand): HotelSearch =
        HotelSearch(
            id = HotelSearchId("hotel-search-local-test-compose-001"),
            sessionId = command.sessionId,
            criteria = command.criteria,
            status = HotelSearch.Status.COMPLETED_WITH_OFFERS,
            offers = emptyList(),
        )

    override fun getSearch(searchId: HotelSearchId): HotelSearch =
        throw RuntimeException("Not expected in tests")
}
