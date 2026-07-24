package com.travelassistant.backend.application.llm

class LlmCandidateValidator {

    fun validate(response: LlmClientResponse): LlmCandidateValidationResult =
        when (response) {
            is LlmClientResponse.Candidate -> validateCandidate(response.value)
            is LlmClientResponse.RetryableFailure -> rejected(response.reason.toValidationReason())
            LlmClientResponse.Empty -> rejected(LlmCandidateValidationResult.Reason.EMPTY_RESPONSE)
            LlmClientResponse.Failure -> rejected(LlmCandidateValidationResult.Reason.CLIENT_FAILURE)
        }

    private fun LlmClientRetryableFailureReason.toValidationReason():
        LlmCandidateValidationResult.Reason =
        when (this) {
            LlmClientRetryableFailureReason.EMPTY_RESPONSE ->
                LlmCandidateValidationResult.Reason.EMPTY_RESPONSE

            LlmClientRetryableFailureReason.CLIENT_FAILURE ->
                LlmCandidateValidationResult.Reason.CLIENT_FAILURE

            LlmClientRetryableFailureReason.INVALID_CANDIDATE ->
                LlmCandidateValidationResult.Reason.INVALID_CANDIDATE
        }

    private fun validateCandidate(candidate: LlmCandidate): LlmCandidateValidationResult {
        if (
            !hasValidContent(candidate) ||
            !hasValidPreferencePatch(candidate.preferencePatch) ||
            !hasConsistentOutcome(candidate)
        ) {
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

        val preferencesAreNotRequired = candidate.missingRequiredFields.none(
            PREFERENCE_FIELD_NAMES::contains,
        )

        return constraintsAreValid && listsAreValid && preferencesAreNotRequired
    }

    private fun hasValidPreferencePatch(patch: LlmHotelSearchPreferencesPatch): Boolean {
        val priceIsValid = patch.maxTotalPrice?.let { price ->
            val amount = price.amount.trim().toBigDecimalOrNull()
            val currency = price.currency?.trim()
            amount != null && amount.signum() > 0 &&
                (price.currency == null || !currency.isNullOrEmpty()) &&
                (currency == null || currency.equals(SUPPORTED_CURRENCY, ignoreCase = true))
        } ?: true
        val starsAreValid = patch.stars?.let { stars ->
            stars.isNotEmpty() && stars.all { star -> star in MIN_STARS..MAX_STARS }
        } ?: true
        val ratingIsValid = patch.minimumGuestRating?.let(SUPPORTED_GUEST_RATINGS::contains) ?: true
        val cancellationIsValid = patch.freeCancellationRequired != false
        val breakfastIsValid = patch.breakfastIncludedRequired != false
        val setFields = buildSet {
            if (patch.maxTotalPrice != null) add(LlmHotelSearchPreferencesPatch.Field.MAX_TOTAL_PRICE)
            if (patch.stars != null) add(LlmHotelSearchPreferencesPatch.Field.STARS)
            if (patch.minimumGuestRating != null) {
                add(LlmHotelSearchPreferencesPatch.Field.MINIMUM_GUEST_RATING)
            }
            if (patch.freeCancellationRequired != null) {
                add(LlmHotelSearchPreferencesPatch.Field.FREE_CANCELLATION)
            }
            if (patch.breakfastIncludedRequired != null) {
                add(LlmHotelSearchPreferencesPatch.Field.BREAKFAST_INCLUDED)
            }
        }
        val operationsDoNotConflict = setFields.none(patch.clear::contains)

        return priceIsValid && starsAreValid && ratingIsValid && cancellationIsValid &&
            breakfastIsValid && operationsDoNotConflict
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
                    candidate.preferencePatch.isEmpty &&
                    candidate.missingRequiredFields.isEmpty() &&
                    candidate.conflicts.isEmpty() &&
                    candidate.clarificationQuestion.isNullOrBlank()
        }

    private fun rejected(
        reason: LlmCandidateValidationResult.Reason,
    ): LlmCandidateValidationResult =
        LlmCandidateValidationResult.Rejected(reason)

    private companion object {
        const val SUPPORTED_CURRENCY = "RUB"
        const val MIN_STARS = 0
        const val MAX_STARS = 5
        val SUPPORTED_GUEST_RATINGS = 5..9
        val PREFERENCE_FIELD_NAMES = LlmHotelSearchPreferencesPatch.Field.entries
            .mapTo(mutableSetOf()) { field -> field.wireName }
    }
}
