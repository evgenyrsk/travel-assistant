package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.hotel.HotelOfferProviderResult

class MapConfirmedSearchTransitionResultToResponseDirectiveUseCase {

    operator fun invoke(
        result: ExecuteConfirmedSearchTransitionResult,
    ): ConfirmedSearchTransitionResponseDirective =
        when (result) {
            is ExecuteConfirmedSearchTransitionResult.Transitioned ->
                mapTransitioned(result)

            is ExecuteConfirmedSearchTransitionResult.DuplicateDetected ->
                mapDuplicate(result)

            is ExecuteConfirmedSearchTransitionResult.SearchNotCreated ->
                mapSearchNotCreated(result)

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

    private fun mapSearchNotCreated(
        result: ExecuteConfirmedSearchTransitionResult.SearchNotCreated,
    ): ConfirmedSearchTransitionResponseDirective =
        ConfirmedSearchTransitionResponseDirective(
            nextAction = InternalTransitionNextAction.ASK_CLARIFICATION,
            messageKind = when (result.outcome) {
                HotelOfferProviderResult.LocationNotFound ->
                    TransitionMessageKind.LOCATION_NOT_FOUND

                is HotelOfferProviderResult.LocationSelectionRequired ->
                    TransitionMessageKind.LOCATION_SELECTION_REQUIRED

                is HotelOfferProviderResult.RequestRejected ->
                    TransitionMessageKind.SEARCH_REQUEST_REJECTED

                is HotelOfferProviderResult.ResponseRejected,
                is HotelOfferProviderResult.ProviderUnavailable,
                ->
                    TransitionMessageKind.TEMPORARY_FAILURE
            },
        )

    private fun mapTransitioned(
        result: ExecuteConfirmedSearchTransitionResult.Transitioned,
    ): ConfirmedSearchTransitionResponseDirective {
        val executionResult = result.executionResult
        if (executionResult is ConfirmedSearchExecutionResult.SearchCreated) {
            return ConfirmedSearchTransitionResponseDirective(
                nextAction = InternalTransitionNextAction.SHOW_HOTEL_RESULTS,
                messageKind = TransitionMessageKind.RESULTS_READY,
                hotelSearchId = executionResult.searchId,
                mayShowHotelResults = true,
                shouldConsumePendingConfirmation = true,
            )
        }
        return ConfirmedSearchTransitionResponseDirective(
            nextAction = InternalTransitionNextAction.ASK_CLARIFICATION,
            messageKind = TransitionMessageKind.PROCESSING,
        )
    }

    private fun mapDuplicate(
        result: ExecuteConfirmedSearchTransitionResult.DuplicateDetected,
    ): ConfirmedSearchTransitionResponseDirective =
        when (result.existingAttempt.status) {
            ConfirmedSearchExecutionAttemptStatus.SUCCEEDED -> {
                val searchId = result.existingAttempt.createdSearchId
                if (searchId != null) {
                    ConfirmedSearchTransitionResponseDirective(
                        nextAction = InternalTransitionNextAction.SHOW_HOTEL_RESULTS,
                        messageKind = TransitionMessageKind.RESULTS_READY,
                        hotelSearchId = searchId,
                        mayShowHotelResults = true,
                        shouldConsumePendingConfirmation = true,
                    )
                } else {
                    ConfirmedSearchTransitionResponseDirective(
                        nextAction = InternalTransitionNextAction.ASK_CLARIFICATION,
                        messageKind = TransitionMessageKind.ALREADY_PROCESSING,
                    )
                }
            }

            ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS ->
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
