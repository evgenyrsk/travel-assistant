package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateRequest
import com.travelassistant.backend.domain.assistant.AssistantSession
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.time.Clock
import java.time.Duration
import java.time.Instant

class AssistantLlmRouteWiringUseCase(
    private val assistantSessionBoundary: AssistantSessionBoundary,
    private val planAssistantLlmDecisionUseCase: PlanAssistantLlmDecisionUseCase,
    private val planProceedWithCandidateConfirmationUseCase: PlanProceedWithCandidateConfirmationUseCase =
        PlanProceedWithCandidateConfirmationUseCase(),
    private val pendingConfirmationStore: PendingConfirmationStore = InMemoryPendingConfirmationStore(),
    private val hotelConstraintsStore: AssistantHotelConstraintsStore =
        InMemoryAssistantHotelConstraintsStore(),
    private val planPostConfirmationDecisionUseCase: PlanPostConfirmationDecisionUseCase =
        PlanPostConfirmationDecisionUseCase(pendingConfirmationStore),
    private val composeTransitionResponse: ComposeConfirmedSearchTransitionResponseUseCase,
    private val clock: Clock = Clock.systemUTC(),
    private val pendingConfirmationTtl: Duration = DEFAULT_PENDING_CONFIRMATION_TTL,
    private val explicitHotelSearchMessageParser: MinimalHotelSearchMessageParser =
        MinimalHotelSearchMessageParser(),
) : AssistantSessionBoundary {
    private val accumulateHotelConstraints =
        AccumulateAssistantHotelConstraintsUseCase(hotelConstraintsStore)

    override fun createSession(): AssistantSession =
        assistantSessionBoundary.createSession()

    override suspend fun acceptUserMessage(command: AcceptAssistantMessageCommand): AcceptedAssistantMessage {
        val acceptedMessage = assistantSessionBoundary.acceptUserMessage(command)

        val explicitHotelSearchMessage = explicitHotelSearchMessageParser.parse(command.message)
        if (explicitHotelSearchMessage != MinimalHotelSearchMessageParser.Result.NotRequested) {
            return acceptedMessage
        }

        val now = clock.instant()
        val activePendingConfirmation = pendingConfirmationStore.findActiveBySession(
            sessionId = command.sessionId,
            now = now,
        )
        if (activePendingConfirmation != null) {
            val postConfirmationDecision = planPostConfirmationDecisionUseCase(
                PlanPostConfirmationDecisionRequest(
                    sessionId = command.sessionId,
                    replyText = command.message,
                    now = now,
                ),
            )
            if (postConfirmationDecision != PostConfirmationDecision.NeedsReplanning) {
                return acceptedMessage.withPostConfirmationDecision(
                    decision = postConfirmationDecision,
                    decidedAt = now,
                    activePendingConfirmation = activePendingConfirmation,
                )
            }

            acceptedMessage.consumePendingConfirmation(now)
        }

        return acceptedMessage.withLlmDecision(command)
    }

    private suspend fun AcceptedAssistantMessage.withLlmDecision(
        command: AcceptAssistantMessageCommand,
    ): AcceptedAssistantMessage =
        when (val decision = planAssistantLlmDecisionUseCase(requestFor(command))) {
            is AssistantCandidateDecision.AskClarification -> {
                decision.candidate
                    ?.takeIf { candidate -> candidate.isSafeForContextAccumulation() }
                    ?.let { candidate -> accumulateConstraints(sessionId, candidate) }
                withClarification(decision.question)
            }

            is AssistantCandidateDecision.Fallback ->
                withSafeBoundaryMessage()

            is AssistantCandidateDecision.ProceedWithCandidate ->
                withConfirmationPlan(
                    planProceedWithCandidateConfirmationUseCase(
                        decision.withAccumulatedConstraints(sessionId),
                    ),
                )
        }

    private fun requestFor(command: AcceptAssistantMessageCommand): LlmCandidateRequest {
        val constraints = hotelConstraintsStore.findBySession(command.sessionId)
            ?: AssistantHotelConstraints()

        return LlmCandidateRequest(
            userMessage = command.message,
            confirmedConstraints = constraints.toConfirmedConstraints(),
            missingRequiredFields = constraints.missingRequiredFields(),
        )
    }

    private fun AssistantCandidateDecision.ProceedWithCandidate.withAccumulatedConstraints(
        sessionId: AssistantSessionId,
    ): AssistantCandidateDecision.ProceedWithCandidate {
        if (!candidate.isSafeForContextAccumulation()) {
            return this
        }

        val accumulation = accumulateConstraints(sessionId, candidate)
        val missingFields = (
            accumulation.constraints.missingRequiredFields() +
                accumulation.issues.map { issue -> issue.field.key }
            ).distinct()

        return AssistantCandidateDecision.ProceedWithCandidate(
            candidate.copy(
                extractedConstraints = accumulation.constraints.toConfirmedConstraints(),
                missingRequiredFields = missingFields,
                clarificationQuestion = when {
                    AssistantHotelConstraintField.CHILDREN.key in missingFields ->
                        CHILDREN_COUNT_CLARIFICATION_MESSAGE

                    AssistantHotelConstraintField.CHILDREN_AGES.key in missingFields ->
                        CHILDREN_AGES_CLARIFICATION_MESSAGE

                    else -> candidate.clarificationQuestion
                },
            ),
        )
    }

    private fun accumulateConstraints(
        sessionId: AssistantSessionId,
        candidate: LlmCandidate,
    ): AssistantHotelConstraintsAccumulationResult =
        accumulateHotelConstraints(
            AccumulateAssistantHotelConstraintsCommand(
                sessionId = sessionId,
                extractedConstraints = candidate.extractedConstraints,
            ),
        )

    private fun LlmCandidate.isSafeForContextAccumulation(): Boolean =
        intent == LlmCandidate.Intent.HOTEL_SEARCH &&
            outcome in setOf(
                LlmCandidate.Outcome.INTERPRETED,
                LlmCandidate.Outcome.NEEDS_CLARIFICATION,
            ) &&
            conflicts.isEmpty() &&
            warnings.isEmpty()

    private fun AcceptedAssistantMessage.withClarification(question: String): AcceptedAssistantMessage =
        copy(
            assistantReply = AssistantReply(
                type = AssistantReplyType.CLARIFICATION,
                message = question,
            ),
            nextAction = AssistantNextAction.ASK_CLARIFICATION,
            hotelSearchId = null,
        )

    private fun AcceptedAssistantMessage.withSafeBoundaryMessage(): AcceptedAssistantMessage =
        copy(
            assistantReply = AssistantReply(
                type = AssistantReplyType.CLARIFICATION,
                message = SAFE_BOUNDARY_MESSAGE,
            ),
            nextAction = AssistantNextAction.SHOW_BOUNDARY_MESSAGE,
            hotelSearchId = null,
        )

    private fun AcceptedAssistantMessage.withConfirmationPlan(
        plan: ProceedWithCandidateConfirmationPlan,
    ): AcceptedAssistantMessage =
        when (plan) {
            is ProceedWithCandidateConfirmationPlan.ConfirmationRequired -> {
                savePendingConfirmation(plan)
                withClarification(plan.proposal.confirmationPromptMessage())
            }

            is ProceedWithCandidateConfirmationPlan.ClarificationRequired ->
                withClarification(plan.question)

            is ProceedWithCandidateConfirmationPlan.Fallback ->
                withSafeBoundaryMessage()
        }

    private suspend fun AcceptedAssistantMessage.withPostConfirmationDecision(
        decision: PostConfirmationDecision,
        decidedAt: Instant,
        activePendingConfirmation: PendingProceedWithCandidateConfirmation? = null,
    ): AcceptedAssistantMessage =
        when (decision) {
            is PostConfirmationDecision.Confirmed -> {
                val composedResult = composeTransitionResponse(
                    ComposeConfirmedSearchTransitionResponseRequest(
                        sessionId = sessionId,
                        decision = decision,
                        pendingConfirmation = activePendingConfirmation,
                        now = decidedAt,
                    ),
                )
                if (composedResult.pendingConsumeInstruction ==
                    PendingConsumeInstruction.CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS
                ) {
                    consumePendingConfirmation(decidedAt)
                }
                when (composedResult.responseDirective.nextAction) {
                    InternalTransitionNextAction.SHOW_HOTEL_RESULTS ->
                        copy(
                            assistantReply = AssistantReply(
                                type = AssistantReplyType.CLARIFICATION,
                                message = composedResult.messageText,
                            ),
                            nextAction = AssistantNextAction.SHOW_HOTEL_RESULTS,
                            hotelSearchId = composedResult.hotelSearchId,
                        )

                    InternalTransitionNextAction.ASK_CLARIFICATION ->
                        withClarification(composedResult.messageText)
                }
            }

            PostConfirmationDecision.NeedsClarification ->
                withClarification(CONFIRMATION_NEEDS_CLARIFICATION_MESSAGE)

            PostConfirmationDecision.Declined -> {
                consumePendingConfirmation(decidedAt)
                withClarification(CONFIRMATION_DECLINED_MESSAGE)
            }

            PostConfirmationDecision.NeedsReplanning -> {
                consumePendingConfirmation(decidedAt)
                withClarification(CONFIRMATION_REPLANNING_MESSAGE)
            }

            PostConfirmationDecision.NoActivePendingConfirmation ->
                withClarification(NO_ACTIVE_CONFIRMATION_MESSAGE)

            PostConfirmationDecision.Unknown ->
                withClarification(CONFIRMATION_UNKNOWN_REPLY_MESSAGE)
        }

    private fun ProceedWithCandidateConfirmationProposal.confirmationPromptMessage(): String =
        "$summary $confirmationQuestion"

    private fun AcceptedAssistantMessage.consumePendingConfirmation(
        consumedAt: Instant,
    ) {
        pendingConfirmationStore.markConsumed(
            sessionId = sessionId,
            consumedAt = consumedAt,
        )
    }

    private fun AcceptedAssistantMessage.savePendingConfirmation(
        plan: ProceedWithCandidateConfirmationPlan.ConfirmationRequired,
    ) {
        val createdAt = clock.instant()
        pendingConfirmationStore.save(
            PendingProceedWithCandidateConfirmation(
                sessionId = sessionId,
                criteria = plan.criteria,
                proposal = plan.proposal,
                createdAt = createdAt,
                updatedAt = createdAt,
                expiresAt = createdAt.plus(pendingConfirmationTtl),
            ),
        )
    }

    private companion object {
        val DEFAULT_PENDING_CONFIRMATION_TTL: Duration = Duration.ofMinutes(15)

        const val SAFE_BOUNDARY_MESSAGE =
            "Пока не удалось безопасно преобразовать сообщение в запрос на поиск отелей. " +
                "Уточните направление, даты, состав гостей и количество номеров."

        const val CONFIRMATION_NEEDS_CLARIFICATION_MESSAGE =
            "Подтвердите параметры, отмените поиск или пришлите исправленные условия."

        const val CONFIRMATION_DECLINED_MESSAGE =
            "Хорошо, поиск отелей не запущен. Когда будете готовы, сообщите новые параметры."

        const val CONFIRMATION_REPLANNING_MESSAGE =
            "Уточните исправленные направление, даты, состав гостей и количество номеров."

        const val NO_ACTIVE_CONFIRMATION_MESSAGE =
            "Нет активного запроса, ожидающего подтверждения. Отправьте параметры поиска отелей ещё раз."

        const val CONFIRMATION_UNKNOWN_REPLY_MESSAGE =
            "Не удалось распознать ответ на подтверждение. Подтвердите параметры, отмените поиск или пришлите исправленные условия."

        const val CHILDREN_COUNT_CLARIFICATION_MESSAGE =
            "Укажите количество детей."

        const val CHILDREN_AGES_CLARIFICATION_MESSAGE =
            "Укажите возраст каждого ребёнка (от 0 до 17 лет)."
    }
}
