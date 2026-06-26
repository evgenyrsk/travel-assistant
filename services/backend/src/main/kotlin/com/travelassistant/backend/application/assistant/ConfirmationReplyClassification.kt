package com.travelassistant.backend.application.assistant

sealed interface ConfirmationReplyClassification {
    val reason: Reason

    data object ExplicitPositive : ConfirmationReplyClassification {
        override val reason: Reason = Reason.EXPLICIT_POSITIVE_PHRASE
    }

    data object Ambiguous : ConfirmationReplyClassification {
        override val reason: Reason = Reason.AMBIGUOUS_SHORT_REPLY
    }

    data object Negative : ConfirmationReplyClassification {
        override val reason: Reason = Reason.NEGATIVE_PHRASE
    }

    data object Correction : ConfirmationReplyClassification {
        override val reason: Reason = Reason.CHANGED_CRITERIA_SIGNAL
    }

    data object Unknown : ConfirmationReplyClassification {
        override val reason: Reason = Reason.UNKNOWN_REPLY
    }

    enum class Reason {
        EXPLICIT_POSITIVE_PHRASE,
        AMBIGUOUS_SHORT_REPLY,
        NEGATIVE_PHRASE,
        CHANGED_CRITERIA_SIGNAL,
        UNKNOWN_REPLY,
    }
}
