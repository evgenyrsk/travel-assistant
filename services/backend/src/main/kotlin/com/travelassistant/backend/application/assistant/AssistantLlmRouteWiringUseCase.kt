package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.LlmCandidateRequest
import com.travelassistant.backend.domain.assistant.AssistantSession
import java.time.Clock
import java.time.Duration
import java.time.Instant

class AssistantLlmRouteWiringUseCase(
    private val assistantSessionBoundary: AssistantSessionBoundary,
    private val planAssistantLlmDecisionUseCase: PlanAssistantLlmDecisionUseCase,
    private val planProceedWithCandidateConfirmationUseCase: PlanProceedWithCandidateConfirmationUseCase =
        PlanProceedWithCandidateConfirmationUseCase(),
    private val pendingConfirmationStore: PendingConfirmationStore = InMemoryPendingConfirmationStore(),
    private val planPostConfirmationDecisionUseCase: PlanPostConfirmationDecisionUseCase =
        PlanPostConfirmationDecisionUseCase(pendingConfirmationStore),
    private val composeTransitionResponse: ComposeConfirmedSearchTransitionResponseUseCase,
    private val clock: Clock = Clock.systemUTC(),
    private val pendingConfirmationTtl: Duration = DEFAULT_PENDING_CONFIRMATION_TTL,
    private val explicitHotelSearchMessageParser: MinimalHotelSearchMessageParser =
        MinimalHotelSearchMessageParser(),
) : AssistantSessionBoundary {

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
            return acceptedMessage.withPostConfirmationDecision(
                planPostConfirmationDecisionUseCase(
                    PlanPostConfirmationDecisionRequest(
                        sessionId = command.sessionId,
                        replyText = command.message,
                        now = now,
                    ),
                ),
                decidedAt = now,
                activePendingConfirmation = activePendingConfirmation,
            )
        }

        return when (val decision = planAssistantLlmDecisionUseCase(requestFor(command, acceptedMessage))) {
            is AssistantCandidateDecision.AskClarification ->
                acceptedMessage.withClarification(decision.question)

            is AssistantCandidateDecision.Fallback ->
                acceptedMessage.withSafeBoundaryMessage()

            is AssistantCandidateDecision.ProceedWithCandidate ->
                acceptedMessage.withConfirmationPlan(
                    planProceedWithCandidateConfirmationUseCase(decision),
                )
        }
    }

    private fun requestFor(
        command: AcceptAssistantMessageCommand,
        acceptedMessage: AcceptedAssistantMessage,
    ): LlmCandidateRequest =
        LlmCandidateRequest(
            userMessage = command.message,
            missingRequiredFields = acceptedMessage.hotelRequirementsCoveragePlan
                .missingRequiredSlotKeys
                .map { it.value },
        )

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
            "I could not safely turn that message into a hotel search yet. " +
                "Please keep the request hotel-only and share destination, dates, guests, and rooms."

        const val CONFIRMATION_NEEDS_CLARIFICATION_MESSAGE =
            "Please confirm clearly, cancel, or share corrected hotel search criteria."

        const val CONFIRMATION_DECLINED_MESSAGE =
            "Okay, I will not start a hotel search. You can share new hotel criteria when ready."

        const val CONFIRMATION_REPLANNING_MESSAGE =
            "Please share the corrected destination, dates, guests, and rooms before I continue."

        const val NO_ACTIVE_CONFIRMATION_MESSAGE =
            "I do not have an active hotel search confirmation to apply. Please share your hotel request again."

        const val CONFIRMATION_UNKNOWN_REPLY_MESSAGE =
            "I could not match that reply to the pending confirmation. Please confirm, cancel, or share corrected criteria."
    }
}
