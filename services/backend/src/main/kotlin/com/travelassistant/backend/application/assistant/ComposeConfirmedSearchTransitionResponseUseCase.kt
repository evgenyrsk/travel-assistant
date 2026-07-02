package com.travelassistant.backend.application.assistant

class ComposeConfirmedSearchTransitionResponseUseCase(
    private val executeTransition: ExecuteConfirmedSearchTransitionUseCase,
    private val mapToDirective: MapConfirmedSearchTransitionResultToResponseDirectiveUseCase =
        MapConfirmedSearchTransitionResultToResponseDirectiveUseCase(),
) {

    operator fun invoke(
        request: ComposeConfirmedSearchTransitionResponseRequest,
    ): ComposeConfirmedSearchTransitionResponseResult {
        val transitionResult = executeTransition(
            ExecuteConfirmedSearchTransitionRequest(
                sessionId = request.sessionId,
                decision = request.decision,
                pendingConfirmation = request.pendingConfirmation,
                now = request.now,
            ),
        )

        val directive = mapToDirective(transitionResult)

        return ComposeConfirmedSearchTransitionResponseResult(
            transitionResult = transitionResult,
            responseDirective = directive,
            messageText = safeMessageText(directive.messageKind),
            pendingConsumeInstruction = consumeInstruction(directive),
            hotelSearchId = directive.hotelSearchId,
        )
    }

    private fun consumeInstruction(
        directive: ConfirmedSearchTransitionResponseDirective,
    ): PendingConsumeInstruction =
        if (directive.shouldConsumePendingConfirmation) {
            PendingConsumeInstruction.CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS
        } else {
            PendingConsumeInstruction.DO_NOT_CONSUME_PENDING_CONFIRMATION
        }

    private fun safeMessageText(kind: TransitionMessageKind): String =
        when (kind) {
            TransitionMessageKind.PROCESSING ->
                PROCESSING_MESSAGE

            TransitionMessageKind.ALREADY_PROCESSING ->
                ALREADY_PROCESSING_MESSAGE

            TransitionMessageKind.CONFIRMATION_REJECTED ->
                CONFIRMATION_REJECTED_MESSAGE

            TransitionMessageKind.TEMPORARY_FAILURE ->
                TEMPORARY_FAILURE_MESSAGE

            TransitionMessageKind.RESULTS_READY ->
                RESULTS_READY_MESSAGE
        }

    private companion object {
        const val PROCESSING_MESSAGE =
            "I am preparing that search, but results are not available yet."

        const val ALREADY_PROCESSING_MESSAGE =
            "That search is already being prepared."

        const val CONFIRMATION_REJECTED_MESSAGE =
            "I could not proceed with the current confirmation state."

        const val TEMPORARY_FAILURE_MESSAGE =
            "I could not record the search transition. Please try again."

        const val RESULTS_READY_MESSAGE =
            "The search is ready. Hotel results are available."
    }
}
