package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateRequest

class AssistantDateInterpretationPolicy {

    operator fun invoke(
        decision: AssistantCandidateDecision,
        request: LlmCandidateRequest,
    ): AssistantCandidateDecision {
        if (request.referenceDate != null || decision is AssistantCandidateDecision.Fallback) {
            return decision
        }

        val candidate = decision.candidateOrNull()
        if (candidate == null || !candidate.isSafeHotelCandidate()) {
            return decision
        }

        val explicitYearIsMissing = !EXPLICIT_FOUR_DIGIT_YEAR.containsMatchIn(request.userMessage)
        val containsRelativeDate = RELATIVE_DATE_MARKER.containsMatchIn(request.userMessage)
        val containsYearlessDate = explicitYearIsMissing &&
            (
                MONTH_NAME_MARKER.containsMatchIn(request.userMessage) ||
                    NUMERIC_DAY_MONTH_MARKER.containsMatchIn(request.userMessage)
                )

        if (!containsRelativeDate && !containsYearlessDate) {
            return decision
        }

        return AssistantCandidateDecision.AskClarification(
            question = DATE_WITH_YEAR_CLARIFICATION_MESSAGE,
            candidate = candidate.withoutUnanchoredDates(),
        )
    }

    private fun AssistantCandidateDecision.candidateOrNull(): LlmCandidate? =
        when (this) {
            is AssistantCandidateDecision.AskClarification -> candidate
            is AssistantCandidateDecision.ProceedWithCandidate -> candidate
            is AssistantCandidateDecision.Fallback -> null
        }

    private fun LlmCandidate.withoutUnanchoredDates(): LlmCandidate =
        copy(
            outcome = LlmCandidate.Outcome.NEEDS_CLARIFICATION,
            extractedConstraints = extractedConstraints - DATE_KEYS,
            missingRequiredFields = (missingRequiredFields + DATE_KEYS).distinct(),
            clarificationQuestion = DATE_WITH_YEAR_CLARIFICATION_MESSAGE,
        )

    private fun LlmCandidate.isSafeHotelCandidate(): Boolean =
        intent == LlmCandidate.Intent.HOTEL_SEARCH && conflicts.isEmpty() && warnings.isEmpty()

    companion object {
        const val DATE_WITH_YEAR_CLARIFICATION_MESSAGE =
            "Уточните даты поездки, указав день, месяц и год."

        private val DATE_KEYS = setOf("check-in", "check-out")
        private val EXPLICIT_FOUR_DIGIT_YEAR = Regex("(?<!\\d)\\d{4}(?!\\d)")
        private val RELATIVE_DATE_MARKER = Regex(
            pattern = "(?iu)(?<![\\p{L}\\p{N}_])(сегодня|завтра|послезавтра|today|tomorrow)(?![\\p{L}\\p{N}_])",
        )
        private val MONTH_NAME_MARKER = Regex(
            pattern = "(?iu)(январ[ьяе]|феврал[ьяе]|март[ае]?|апрел[ьяе]|ма[йяе]|июн[ьяе]|" +
                "июл[ьяе]|август[ае]?|сентябр[ьяе]|октябр[ьяе]|ноябр[ьяе]|декабр[ьяе]|" +
                "january|february|march|april|may|june|july|august|september|october|november|december)",
        )
        private val NUMERIC_DAY_MONTH_MARKER = Regex(
            pattern = "(?<!\\d)\\d{1,2}[./-]\\d{1,2}(?![./-]\\d)",
        )
    }
}
