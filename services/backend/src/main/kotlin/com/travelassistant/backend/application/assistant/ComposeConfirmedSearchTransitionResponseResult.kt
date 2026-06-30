package com.travelassistant.backend.application.assistant

data class ComposeConfirmedSearchTransitionResponseResult(
    val transitionResult: ExecuteConfirmedSearchTransitionResult,
    val responseDirective: ConfirmedSearchTransitionResponseDirective,
    val messageText: String,
    val pendingConsumeInstruction: PendingConsumeInstruction,
)

enum class PendingConsumeInstruction {
    DO_NOT_CONSUME_PENDING_CONFIRMATION,
}
