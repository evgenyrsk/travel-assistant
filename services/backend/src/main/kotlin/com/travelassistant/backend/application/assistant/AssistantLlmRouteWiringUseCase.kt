package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.LlmCandidateRequest
import com.travelassistant.backend.domain.assistant.AssistantSession

class AssistantLlmRouteWiringUseCase(
    private val assistantSessionBoundary: AssistantSessionBoundary,
    private val planAssistantLlmDecisionUseCase: PlanAssistantLlmDecisionUseCase,
    private val planProceedWithCandidateConfirmationUseCase: PlanProceedWithCandidateConfirmationUseCase =
        PlanProceedWithCandidateConfirmationUseCase(),
    private val explicitHotelSearchMessageParser: MinimalHotelSearchMessageParser =
        MinimalHotelSearchMessageParser(),
) : AssistantSessionBoundary {

    override fun createSession(): AssistantSession =
        assistantSessionBoundary.createSession()

    override fun acceptUserMessage(command: AcceptAssistantMessageCommand): AcceptedAssistantMessage {
        val acceptedMessage = assistantSessionBoundary.acceptUserMessage(command)

        val explicitHotelSearchMessage = explicitHotelSearchMessageParser.parse(command.message)
        if (explicitHotelSearchMessage != MinimalHotelSearchMessageParser.Result.NotRequested) {
            return acceptedMessage
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
            is ProceedWithCandidateConfirmationPlan.ConfirmationRequired ->
                withClarification(plan.proposal.confirmationPromptMessage())

            is ProceedWithCandidateConfirmationPlan.ClarificationRequired ->
                withClarification(plan.question)

            is ProceedWithCandidateConfirmationPlan.Fallback ->
                withSafeBoundaryMessage()
        }

    private fun ProceedWithCandidateConfirmationProposal.confirmationPromptMessage(): String =
        "$summary $confirmationQuestion"

    private companion object {
        const val SAFE_BOUNDARY_MESSAGE =
            "I could not safely turn that message into a hotel search yet. " +
                "Please keep the request hotel-only and share destination, dates, guests, and rooms."
    }
}
