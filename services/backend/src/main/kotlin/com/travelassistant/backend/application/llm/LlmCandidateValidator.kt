package com.travelassistant.backend.application.llm

class LlmCandidateValidator {

    fun validate(response: LlmClientResponse): LlmCandidateValidationResult =
        when (response) {
            is LlmClientResponse.Candidate -> validateCandidate(response.value)
            LlmClientResponse.Empty -> rejected(LlmCandidateValidationResult.Reason.EMPTY_RESPONSE)
            LlmClientResponse.Failure -> rejected(LlmCandidateValidationResult.Reason.CLIENT_FAILURE)
        }

    private fun validateCandidate(candidate: LlmCandidate): LlmCandidateValidationResult {
        if (!hasValidContent(candidate) || !hasConsistentOutcome(candidate)) {
            return rejected(LlmCandidateValidationResult.Reason.INVALID_CANDIDATE)
        }

        return LlmCandidateValidationResult.Accepted(candidate)
    }

    private fun hasValidContent(candidate: LlmCandidate): Boolean {
        val constraintsAreValid = candidate.extractedConstraints.all { (key, value) ->
            key.isNotBlank() && value.isNotBlank()
        }
        val listsAreValid = listOf(
            candidate.missingRequiredFields,
            candidate.conflicts,
            candidate.warnings,
        ).all { values -> values.none(String::isBlank) }

        return constraintsAreValid && listsAreValid
    }

    private fun hasConsistentOutcome(candidate: LlmCandidate): Boolean =
        when (candidate.outcome) {
            LlmCandidate.Outcome.INTERPRETED ->
                candidate.intent == LlmCandidate.Intent.HOTEL_SEARCH &&
                    candidate.missingRequiredFields.isEmpty() &&
                    candidate.conflicts.isEmpty() &&
                    candidate.clarificationQuestion.isNullOrBlank()

            LlmCandidate.Outcome.NEEDS_CLARIFICATION ->
                candidate.intent != LlmCandidate.Intent.UNSUPPORTED &&
                    !candidate.clarificationQuestion.isNullOrBlank()

            LlmCandidate.Outcome.AMBIGUOUS ->
                candidate.intent != LlmCandidate.Intent.UNSUPPORTED &&
                    !candidate.clarificationQuestion.isNullOrBlank()

            LlmCandidate.Outcome.UNSUPPORTED ->
                candidate.intent == LlmCandidate.Intent.UNSUPPORTED &&
                    candidate.extractedConstraints.isEmpty() &&
                    candidate.missingRequiredFields.isEmpty() &&
                    candidate.conflicts.isEmpty() &&
                    candidate.clarificationQuestion.isNullOrBlank()
        }

    private fun rejected(
        reason: LlmCandidateValidationResult.Reason,
    ): LlmCandidateValidationResult =
        LlmCandidateValidationResult.Rejected(reason)
}
