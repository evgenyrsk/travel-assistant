package com.travelassistant.backend.application.assistant

class MapConfirmedSearchTransitionResultToResponseDirectiveUseCase {

    operator fun invoke(
        result: ExecuteConfirmedSearchTransitionResult,
    ): ConfirmedSearchTransitionResponseDirective =
        when (result) {
            is ExecuteConfirmedSearchTransitionResult.Transitioned ->
                ConfirmedSearchTransitionResponseDirective(
                    nextAction = InternalTransitionNextAction.ASK_CLARIFICATION,
                    messageKind = TransitionMessageKind.PROCESSING,
                )

            is ExecuteConfirmedSearchTransitionResult.DuplicateDetected ->
                mapDuplicate(result)

            is ExecuteConfirmedSearchTransitionResult.GuardRejected ->
                ConfirmedSearchTransitionResponseDirective(
                    nextAction = InternalTransitionNextAction.ASK_CLARIFICATION,
                    messageKind = TransitionMessageKind.CONFIRMATION_REJECTED,
                )

            is ExecuteConfirmedSearchTransitionResult.StoreRejected ->
                ConfirmedSearchTransitionResponseDirective(
                    nextAction = InternalTransitionNextAction.ASK_CLARIFICATION,
                    messageKind = TransitionMessageKind.TEMPORARY_FAILURE,
                )
        }

    private fun mapDuplicate(
        result: ExecuteConfirmedSearchTransitionResult.DuplicateDetected,
    ): ConfirmedSearchTransitionResponseDirective =
        when (result.existingAttempt.status) {
            ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS ->
                ConfirmedSearchTransitionResponseDirective(
                    nextAction = InternalTransitionNextAction.ASK_CLARIFICATION,
                    messageKind = TransitionMessageKind.ALREADY_PROCESSING,
                )

            ConfirmedSearchExecutionAttemptStatus.SUCCEEDED ->
                ConfirmedSearchTransitionResponseDirective(
                    nextAction = InternalTransitionNextAction.ASK_CLARIFICATION,
                    messageKind = TransitionMessageKind.ALREADY_PROCESSING,
                )

            ConfirmedSearchExecutionAttemptStatus.PREPARED,
            ConfirmedSearchExecutionAttemptStatus.FAILED,
            ConfirmedSearchExecutionAttemptStatus.DUPLICATE_BLOCKED ->
                ConfirmedSearchTransitionResponseDirective(
                    nextAction = InternalTransitionNextAction.ASK_CLARIFICATION,
                    messageKind = TransitionMessageKind.ALREADY_PROCESSING,
                )
        }
}
