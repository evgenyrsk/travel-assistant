package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class PlanPostConfirmationDecisionUseCaseTest {

    private val now = Instant.parse("2026-06-26T10:00:00Z")
    private val sessionId = AssistantSessionId("assistant-session-local-000001")

    @Test
    fun returnsConfirmedCriteriaForExplicitPositiveReplyWithActivePendingConfirmation() {
        val store = InMemoryPendingConfirmationStore()
        val pendingConfirmation = pendingConfirmation()
        store.save(pendingConfirmation)
        val useCase = PlanPostConfirmationDecisionUseCase(store)

        val decision = useCase(request(replyText = "да, ищи"))

        val confirmed = assertIs<PostConfirmationDecision.Confirmed>(decision)
        assertEquals(pendingConfirmation.criteria, confirmed.criteria)
        assertEquals(PostConfirmationDecision.Reason.EXPLICITLY_CONFIRMED, confirmed.reason)
    }

    @Test
    fun returnsNoActivePendingConfirmationForExplicitPositiveReplyWithoutActiveState() {
        val useCase = PlanPostConfirmationDecisionUseCase(InMemoryPendingConfirmationStore())

        val decision = useCase(request(replyText = "yes"))

        assertEquals(PostConfirmationDecision.NoActivePendingConfirmation, decision)
    }

    @Test
    fun doesNotConfirmExpiredPendingConfirmation() {
        val store = InMemoryPendingConfirmationStore()
        store.save(
            pendingConfirmation(
                expiresAt = now.minusSeconds(1),
            ),
        )
        val useCase = PlanPostConfirmationDecisionUseCase(store)

        val decision = useCase(request(replyText = "confirm"))

        assertEquals(PostConfirmationDecision.NoActivePendingConfirmation, decision)
    }

    @Test
    fun doesNotConfirmConsumedPendingConfirmation() {
        val store = InMemoryPendingConfirmationStore()
        store.save(pendingConfirmation())
        store.markConsumed(
            sessionId = sessionId,
            consumedAt = now.minusSeconds(1),
        )
        val useCase = PlanPostConfirmationDecisionUseCase(store)

        val decision = useCase(request(replyText = "confirm"))

        assertEquals(PostConfirmationDecision.NoActivePendingConfirmation, decision)
    }

    @Test
    fun returnsNeedsClarificationForAmbiguousReplyWithActivePendingConfirmation() {
        val useCase = useCaseWithActivePendingConfirmation()

        val decision = useCase(request(replyText = "ок"))

        assertEquals(PostConfirmationDecision.NeedsClarification, decision)
    }

    @Test
    fun returnsDeclinedForNegativeReplyWithActivePendingConfirmation() {
        val useCase = useCaseWithActivePendingConfirmation()

        val decision = useCase(request(replyText = "не надо"))

        assertEquals(PostConfirmationDecision.Declined, decision)
    }

    @Test
    fun returnsNeedsReplanningForCorrectionReplyWithActivePendingConfirmation() {
        val useCase = useCaseWithActivePendingConfirmation()

        val decision = useCase(request(replyText = "лучше Париж"))

        assertEquals(PostConfirmationDecision.NeedsReplanning, decision)
    }

    @Test
    fun returnsUnknownForUnrelatedReplyWithActivePendingConfirmation() {
        val useCase = useCaseWithActivePendingConfirmation()

        val decision = useCase(request(replyText = "расскажи про музеи рядом"))

        assertEquals(PostConfirmationDecision.Unknown, decision)
    }

    @Test
    fun confirmedDecisionDoesNotExposeRawCandidateOrCreateSearch() {
        val useCase = useCaseWithActivePendingConfirmation()

        val decision = useCase(request(replyText = "да"))
        val decisionText = decision.toString()

        listOf(
            "LlmCandidate",
            "raw candidate",
            "candidatePayload",
            "modelResponse",
            "hotelSearchId",
            "show_hotel_results",
            "Hotel search created",
        ).forEach { forbidden ->
            assertFalse(
                decisionText.contains(forbidden),
                "Post-confirmation decision must not expose $forbidden",
            )
        }
    }

    @Test
    fun remainsDeterministicForSameRequestAndPendingState() {
        val store = InMemoryPendingConfirmationStore()
        store.save(pendingConfirmation())
        val useCase = PlanPostConfirmationDecisionUseCase(store)
        val request = request(replyText = "да")

        val firstDecision = useCase(request)
        val secondDecision = useCase(request)

        assertEquals(firstDecision, secondDecision)
    }

    @Test
    fun doesNotRequireRouteApiOpenApiOrFrontendDependency() {
        val store = InMemoryPendingConfirmationStore()
        store.save(pendingConfirmation())
        val useCase = PlanPostConfirmationDecisionUseCase(store)

        val decision = useCase(request(replyText = "confirm"))

        assertIs<PostConfirmationDecision.Confirmed>(decision)
    }

    private fun useCaseWithActivePendingConfirmation(): PlanPostConfirmationDecisionUseCase {
        val store = InMemoryPendingConfirmationStore()
        store.save(pendingConfirmation())
        return PlanPostConfirmationDecisionUseCase(store)
    }

    private fun request(
        replyText: String,
        requestSessionId: AssistantSessionId = sessionId,
        requestTime: Instant = now,
    ): PlanPostConfirmationDecisionRequest =
        PlanPostConfirmationDecisionRequest(
            sessionId = requestSessionId,
            replyText = replyText,
            now = requestTime,
        )

    private fun pendingConfirmation(
        expiresAt: Instant = now.plusSeconds(300),
    ): PendingProceedWithCandidateConfirmation {
        val criteria = ProceedWithCandidateCriteria(
            destination = "Rome",
            checkInDate = LocalDate.parse("2026-07-01"),
            checkOutDate = LocalDate.parse("2026-07-04"),
            guests = ProceedWithCandidateCriteria.Guests(
                adults = 2,
                children = 0,
            ),
            rooms = 1,
        )

        return PendingProceedWithCandidateConfirmation(
            sessionId = sessionId,
            criteria = criteria,
            proposal = ProceedWithCandidateConfirmationProposal(
                summary = "Параметры hotel search: направление: Rome; заезд: 2026-07-01; " +
                    "выезд: 2026-07-04; взрослые: 2; дети: 0; номера: 1.",
                confirmationQuestion = "Проверить отели по этим параметрам?",
                displayFields = listOf(
                    ProceedWithCandidateConfirmationField(
                        key = "destination",
                        label = "направление",
                        value = "Rome",
                    ),
                ),
            ),
            createdAt = now,
            updatedAt = now,
            expiresAt = expiresAt,
        )
    }
}
