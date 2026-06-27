package com.travelassistant.backend.application.assistant

class PlanPostConfirmationDecisionUseCase(
    private val pendingConfirmationStore: PendingConfirmationStore,
    private val classifyConfirmationReplyUseCase: ClassifyConfirmationReplyUseCase =
        ClassifyConfirmationReplyUseCase(),
) {
    operator fun invoke(
        request: PlanPostConfirmationDecisionRequest,
    ): PostConfirmationDecision {
        val activePendingConfirmation = pendingConfirmationStore.findActiveBySession(
            sessionId = request.sessionId,
            now = request.now,
        ) ?: return PostConfirmationDecision.NoActivePendingConfirmation

        return when (classifyConfirmationReplyUseCase(request.replyText)) {
            ConfirmationReplyClassification.ExplicitPositive ->
                PostConfirmationDecision.Confirmed(activePendingConfirmation.criteria)

            ConfirmationReplyClassification.Ambiguous ->
                PostConfirmationDecision.NeedsClarification

            ConfirmationReplyClassification.Negative ->
                PostConfirmationDecision.Declined

            ConfirmationReplyClassification.Correction ->
                PostConfirmationDecision.NeedsReplanning

            ConfirmationReplyClassification.Unknown ->
                PostConfirmationDecision.Unknown
        }
    }
}
