package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchId

data class ComposeConfirmedSearchTransitionResponseResult(
    val transitionResult: ExecuteConfirmedSearchTransitionResult,
    val responseDirective: ConfirmedSearchTransitionResponseDirective,
    val messageText: String,
    val pendingConsumeInstruction: PendingConsumeInstruction,
    val hotelSearchId: HotelSearchId? = null,
)

enum class PendingConsumeInstruction {
    DO_NOT_CONSUME_PENDING_CONFIRMATION,
    CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS,
}
