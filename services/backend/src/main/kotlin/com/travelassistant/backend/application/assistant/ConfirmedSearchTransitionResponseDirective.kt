package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchId

data class ConfirmedSearchTransitionResponseDirective(
    val nextAction: InternalTransitionNextAction,
    val messageKind: TransitionMessageKind,
    val hotelSearchId: HotelSearchId? = null,
    val mayShowHotelResults: Boolean = false,
    val shouldConsumePendingConfirmation: Boolean = false,
)

enum class InternalTransitionNextAction {
    ASK_CLARIFICATION,
    SHOW_HOTEL_RESULTS,
}

enum class TransitionMessageKind {
    PROCESSING,
    ALREADY_PROCESSING,
    CONFIRMATION_REJECTED,
    LOCATION_NOT_FOUND,
    LOCATION_SELECTION_REQUIRED,
    SEARCH_REQUEST_REJECTED,
    TEMPORARY_FAILURE,
    RESULTS_READY,
    NO_RESULTS,
}
