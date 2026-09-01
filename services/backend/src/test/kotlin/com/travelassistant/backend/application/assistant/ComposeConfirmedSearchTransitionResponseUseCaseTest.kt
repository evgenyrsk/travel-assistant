package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.hotel.CreateHotelSearchCommand
import com.travelassistant.backend.application.hotel.CreateHotelSearchResult
import com.travelassistant.backend.application.hotel.HotelOfferProviderResult
import com.travelassistant.backend.application.hotel.HotelSearchBoundary
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchId
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ComposeConfirmedSearchTransitionResponseUseCaseTest {

    private val now = Instant.parse("2026-06-25T10:00:00Z")

    @Test
    fun searchingTransitionUsesLaunchMessageWithoutDuplicateOrStaleReadinessCopy() = runBlocking {
        val searchingSearch = HotelSearch(
            id = HotelSearchId("semantic-search-local-compose-001"),
            sessionId = AssistantSessionId("assistant-session-local-000123"),
            criteria = HotelSearchCriteria(
                destination = "Rome",
                checkInDate = LocalDate.parse("2026-07-01"),
                checkOutDate = LocalDate.parse("2026-07-04"),
                guests = HotelSearchCriteria.Guests(
                    adults = 2,
                    childrenAges = emptyList(),
                ),
                rooms = 1,
            ),
            status = HotelSearch.Status.SEARCHING,
            offers = emptyList(),
        )
        val useCase = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = InMemoryConfirmedSearchExecutionAttemptStore(),
                hotelSearchBoundary = TestHotelSearchBoundary(
                    result = CreateHotelSearchResult.Created(searchingSearch),
                ),
            ),
        )

        val result = useCase(compositionRequest())

        assertEquals(TransitionMessageKind.PROCESSING, result.responseDirective.messageKind)
        assertEquals("Проверка типа размещения запущена.", result.messageText)
        assertFalse(result.messageText.contains("уже"))
        assertFalse(result.messageText.contains("не готовы"))
        assertEquals(searchingSearch.id, result.hotelSearchId)
    }

    @Test
    fun successfulTransitionComposesShowHotelResultsDirective() = runBlocking {
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
            "Поиск завершён. Результат готов.",
            result.messageText,
        )
        assertEquals(
            PendingConsumeInstruction.CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS,
            result.pendingConsumeInstruction,
        )
        assertNotNull(result.hotelSearchId)
    }

    @Test
    fun successfulTransitionIncludesHotelSearchId() = runBlocking {
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
    fun successfulTransitionRequestsHotelResults() = runBlocking {
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
    fun successfulTransitionRequestsPendingConsume() = runBlocking {
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
    fun emptySuccessfulSearchUsesClearNoResultsMessage() = runBlocking {
        val emptySearch = HotelSearch(
            id = HotelSearchId("hotel-search-local-empty-compose-001"),
            sessionId = AssistantSessionId("assistant-session-local-000123"),
            criteria = HotelSearchCriteria(
                destination = "Rome",
                checkInDate = LocalDate.parse("2026-07-01"),
                checkOutDate = LocalDate.parse("2026-07-04"),
                guests = HotelSearchCriteria.Guests(
                    adults = 2,
                    childrenAges = emptyList(),
                ),
                rooms = 1,
            ),
            status = HotelSearch.Status.COMPLETED_NO_OFFERS,
            offers = emptyList(),
        )
        val useCase = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = InMemoryConfirmedSearchExecutionAttemptStore(),
                hotelSearchBoundary = TestHotelSearchBoundary(
                    result = CreateHotelSearchResult.Created(emptySearch),
                ),
            ),
        )

        val result = useCase(compositionRequest())

        assertEquals(TransitionMessageKind.NO_RESULTS, result.responseDirective.messageKind)
        assertEquals(
            "Поиск завершён, но подходящих вариантов не найдено.",
            result.messageText,
        )
        assertNotNull(result.hotelSearchId)
    }

    @Test
    fun duplicateSucceededComposesShowHotelResultsWithExistingSearchId() = runBlocking {
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
    fun duplicateInProgressUsesSeparateAlreadyProcessingMessage() = runBlocking {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val criteria = proceedCriteria()
        val commandPlan = commandReadyPlan(criteria)
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
        val useCase = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = store,
                hotelSearchBoundary = TestHotelSearchBoundary(),
            ),
        )

        val result = useCase(
            compositionRequest(
                criteria = criteria,
                pendingConfirmation = pendingConfirmation(
                    criteria = criteria,
                    expiresAt = now.plusSeconds(3600),
                ),
                now = now.plusSeconds(2),
            ),
        )

        assertIs<ExecuteConfirmedSearchTransitionResult.DuplicateDetected>(result.transitionResult)
        assertEquals(TransitionMessageKind.ALREADY_PROCESSING, result.responseDirective.messageKind)
        assertEquals("Этот поиск уже выполняется.", result.messageText)
        assertEquals(null, result.hotelSearchId)
    }

    @Test
    fun guardRejectedComposesConfirmationRejectedMessageWithoutConsume() = runBlocking {
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
            "Не удалось продолжить поиск с текущим подтверждением.",
            result.messageText,
        )
        assertEquals(
            PendingConsumeInstruction.DO_NOT_CONSUME_PENDING_CONFIRMATION,
            result.pendingConsumeInstruction,
        )
    }

    @Test
    fun compositionDoesNotRequireCreateHotelSearchUseCase() = runBlocking {
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
    fun compositionDoesNotMutatePendingConfirmationStore() = runBlocking {
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
    fun composedResultDoesNotExposeForbiddenTokens() = runBlocking {
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
    fun duplicateSucceededWithSearchIdComposesShowHotelResultsDirective() = runBlocking {
        val store = InMemoryConfirmedSearchExecutionAttemptStore()
        val criteria = proceedCriteria()
        val commandPlan = commandReadyPlan(criteria)
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
            "Поиск завершён. Результат готов.",
            result.messageText,
        )
    }

    private fun commandReadyPlan(
        criteria: ProceedWithCandidateCriteria,
    ): ConfirmedSearchCreationCommandPlan.CommandReady =
        ConfirmedSearchCreationCommandPlan.CommandReady(
            command = CreateHotelSearchCommand(
                sessionId = AssistantSessionId("assistant-session-local-000123"),
                criteria = HotelSearchCriteria(
                    destination = criteria.destination,
                    checkInDate = criteria.checkInDate,
                    checkOutDate = criteria.checkOutDate,
                    guests = HotelSearchCriteria.Guests(
                        adults = criteria.guests.adults,
                        childrenAges = criteria.guests.childrenAges,
                    ),
                    rooms = criteria.rooms,
                ),
            ),
            lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
        )

    @Test
    fun providerUnavailableComposesSafeClarificationWithoutSearchIdOrConsume() = runBlocking {
        val outcome = HotelOfferProviderResult.ProviderUnavailable(
            HotelOfferProviderResult.UnavailableReason.UNAVAILABLE,
        )
        val useCase = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = InMemoryConfirmedSearchExecutionAttemptStore(),
                hotelSearchBoundary = TestHotelSearchBoundary(
                    result = CreateHotelSearchResult.NotCreated(outcome),
                ),
            ),
        )

        val result = useCase(compositionRequest())

        assertIs<ExecuteConfirmedSearchTransitionResult.SearchNotCreated>(result.transitionResult)
        assertEquals(InternalTransitionNextAction.ASK_CLARIFICATION, result.responseDirective.nextAction)
        assertEquals(TransitionMessageKind.TEMPORARY_FAILURE, result.responseDirective.messageKind)
        assertEquals(
            "Сейчас не удалось завершить поиск отелей. Попробуйте ещё раз.",
            result.messageText,
        )
        assertEquals(
            PendingConsumeInstruction.DO_NOT_CONSUME_PENDING_CONFIRMATION,
            result.pendingConsumeInstruction,
        )
        assertEquals(null, result.hotelSearchId)
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
}

private class TestHotelSearchBoundary(
    private val result: CreateHotelSearchResult? = null,
) : HotelSearchBoundary {
    override suspend fun createSearch(command: CreateHotelSearchCommand): CreateHotelSearchResult =
        result ?: CreateHotelSearchResult.Created(
            HotelSearch(
                id = HotelSearchId("hotel-search-local-test-compose-001"),
                sessionId = command.sessionId,
                criteria = command.criteria,
                status = HotelSearch.Status.COMPLETED_WITH_OFFERS,
                offers = emptyList(),
            ),
        )

    override fun getSearch(searchId: HotelSearchId): HotelSearch =
        throw RuntimeException("Not expected in tests")
}
