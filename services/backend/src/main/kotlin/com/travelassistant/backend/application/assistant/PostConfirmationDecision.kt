package com.travelassistant.backend.application.assistant

sealed interface PostConfirmationDecision {
    val reason: Reason

    data class Confirmed(
        val criteria: ProceedWithCandidateCriteria,
    ) : PostConfirmationDecision {
        override val reason: Reason = Reason.EXPLICITLY_CONFIRMED
    }

    data object NeedsClarification : PostConfirmationDecision {
        override val reason: Reason = Reason.AMBIGUOUS_REPLY
    }

    data object Declined : PostConfirmationDecision {
        override val reason: Reason = Reason.NEGATIVE_REPLY
    }

    data object NeedsReplanning : PostConfirmationDecision {
        override val reason: Reason = Reason.CORRECTION_REPLY
    }

    data object NoActivePendingConfirmation : PostConfirmationDecision {
        override val reason: Reason = Reason.NO_ACTIVE_PENDING_CONFIRMATION
    }

    data object Unknown : PostConfirmationDecision {
        override val reason: Reason = Reason.UNKNOWN_REPLY
    }

    enum class Reason {
        EXPLICITLY_CONFIRMED,
        AMBIGUOUS_REPLY,
        NEGATIVE_REPLY,
        CORRECTION_REPLY,
        NO_ACTIVE_PENDING_CONFIRMATION,
        UNKNOWN_REPLY,
    }
}
