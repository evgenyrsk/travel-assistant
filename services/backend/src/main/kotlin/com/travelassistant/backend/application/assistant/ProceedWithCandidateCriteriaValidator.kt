package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import java.time.LocalDate

class ProceedWithCandidateCriteriaValidator {

    operator fun invoke(
        decision: AssistantCandidateDecision.ProceedWithCandidate,
        preferences: HotelSearchPreferences = HotelSearchPreferences(),
    ): ProceedWithCandidateValidationResult {
        val candidate = decision.candidate
        val issues = linkedSetOf<ProceedWithCandidateValidationIssue>()

        if (candidate.intent != LlmCandidate.Intent.HOTEL_SEARCH) {
            issues += ProceedWithCandidateValidationIssue.UNSUPPORTED_INTENT
        }
        if (candidate.outcome != LlmCandidate.Outcome.INTERPRETED) {
            issues += ProceedWithCandidateValidationIssue.UNSUPPORTED_OUTCOME
        }
        if (candidate.missingRequiredFields.isNotEmpty()) {
            issues += ProceedWithCandidateValidationIssue.MISSING_REQUIRED_FIELDS
        }
        if (candidate.conflicts.isNotEmpty()) {
            issues += ProceedWithCandidateValidationIssue.CONFLICTS_PRESENT
        }
        if (!candidate.clarificationQuestion.isNullOrBlank()) {
            issues += ProceedWithCandidateValidationIssue.CLARIFICATION_REQUIRED
        }
        if (candidate.warnings.isNotEmpty()) {
            issues += ProceedWithCandidateValidationIssue.BLOCKING_WARNINGS
        }

        val destination = candidate.extractedConstraints[DESTINATION]?.trim()
        if (destination.isNullOrBlank()) {
            issues += ProceedWithCandidateValidationIssue.MISSING_DESTINATION
        }

        val checkInDate = candidate.dateConstraint(
            key = CHECK_IN_DATE,
            missingIssue = ProceedWithCandidateValidationIssue.MISSING_CHECK_IN_DATE,
            invalidIssue = ProceedWithCandidateValidationIssue.INVALID_CHECK_IN_DATE,
            issues = issues,
        )
        val checkOutDate = candidate.dateConstraint(
            key = CHECK_OUT_DATE,
            missingIssue = ProceedWithCandidateValidationIssue.MISSING_CHECK_OUT_DATE,
            invalidIssue = ProceedWithCandidateValidationIssue.INVALID_CHECK_OUT_DATE,
            issues = issues,
        )
        if (checkInDate != null && checkOutDate != null && !checkOutDate.isAfter(checkInDate)) {
            issues += ProceedWithCandidateValidationIssue.INVALID_DATE_RANGE
        }

        val adults = candidate.requiredIntConstraint(
            key = ADULTS,
            missingIssue = ProceedWithCandidateValidationIssue.MISSING_ADULTS,
            invalidIssue = ProceedWithCandidateValidationIssue.INVALID_ADULTS,
            issues = issues,
        )
        if (adults != null && adults < 1) {
            issues += ProceedWithCandidateValidationIssue.INVALID_ADULTS
        }

        val children = candidate.optionalIntConstraint(
            key = CHILDREN,
            invalidIssue = ProceedWithCandidateValidationIssue.INVALID_CHILDREN,
            issues = issues,
        ) ?: 0
        if (children < 0) {
            issues += ProceedWithCandidateValidationIssue.INVALID_CHILDREN
        }
        val childrenAges = candidate.childrenAgesConstraint(
            children = children,
            issues = issues,
        )

        val rooms = candidate.requiredIntConstraint(
            key = ROOMS,
            missingIssue = ProceedWithCandidateValidationIssue.MISSING_ROOMS,
            invalidIssue = ProceedWithCandidateValidationIssue.INVALID_ROOMS,
            issues = issues,
        )
        if (rooms != null && rooms < 1) {
            issues += ProceedWithCandidateValidationIssue.INVALID_ROOMS
        }

        if (issues.isNotEmpty()) {
            return ProceedWithCandidateValidationResult.Rejected(
                issues = issues,
                clarificationHint = candidate.clarificationQuestion?.trim()?.takeIf(String::isNotEmpty),
            )
        }

        return ProceedWithCandidateValidationResult.Accepted(
            ProceedWithCandidateCriteria(
                destination = checkNotNull(destination),
                checkInDate = checkNotNull(checkInDate),
                checkOutDate = checkNotNull(checkOutDate),
                guests = ProceedWithCandidateCriteria.Guests(
                    adults = checkNotNull(adults),
                    childrenAges = checkNotNull(childrenAges),
                ),
                rooms = checkNotNull(rooms),
                preferences = preferences,
            ),
        )
    }

    private fun LlmCandidate.dateConstraint(
        key: String,
        missingIssue: ProceedWithCandidateValidationIssue,
        invalidIssue: ProceedWithCandidateValidationIssue,
        issues: MutableSet<ProceedWithCandidateValidationIssue>,
    ): LocalDate? {
        val rawValue = extractedConstraints[key]?.trim()
        if (rawValue.isNullOrBlank()) {
            issues += missingIssue
            return null
        }

        return runCatching {
            LocalDate.parse(rawValue)
        }.getOrElse {
            issues += invalidIssue
            null
        }
    }

    private fun LlmCandidate.requiredIntConstraint(
        key: String,
        missingIssue: ProceedWithCandidateValidationIssue,
        invalidIssue: ProceedWithCandidateValidationIssue,
        issues: MutableSet<ProceedWithCandidateValidationIssue>,
    ): Int? {
        val rawValue = extractedConstraints[key]?.trim()
        if (rawValue.isNullOrBlank()) {
            issues += missingIssue
            return null
        }

        return rawValue.toIntOrNull() ?: run {
            issues += invalidIssue
            null
        }
    }

    private fun LlmCandidate.optionalIntConstraint(
        key: String,
        invalidIssue: ProceedWithCandidateValidationIssue,
        issues: MutableSet<ProceedWithCandidateValidationIssue>,
    ): Int? {
        val rawValue = extractedConstraints[key]?.trim() ?: return null
        if (rawValue.isBlank()) {
            issues += invalidIssue
            return null
        }

        return rawValue.toIntOrNull() ?: run {
            issues += invalidIssue
            null
        }
    }

    private fun LlmCandidate.childrenAgesConstraint(
        children: Int,
        issues: MutableSet<ProceedWithCandidateValidationIssue>,
    ): List<Int>? {
        val rawValue = extractedConstraints[CHILDREN_AGES]?.trim()
        if (rawValue.isNullOrBlank()) {
            if (children > 0) {
                issues += ProceedWithCandidateValidationIssue.MISSING_CHILDREN_AGES
                return null
            }
            return emptyList()
        }

        val ages = rawValue.split(',').map { value ->
            value.trim().toIntOrNull() ?: run {
                issues += ProceedWithCandidateValidationIssue.INVALID_CHILDREN_AGES
                return null
            }
        }
        if (ages.any { it !in MIN_CHILD_AGE..MAX_CHILD_AGE }) {
            issues += ProceedWithCandidateValidationIssue.INVALID_CHILDREN_AGES
        }
        if (ages.size != children) {
            issues += ProceedWithCandidateValidationIssue.CHILDREN_AGES_COUNT_MISMATCH
        }

        return ages
    }

    private companion object {
        const val DESTINATION = "destination"
        const val CHECK_IN_DATE = "check-in"
        const val CHECK_OUT_DATE = "check-out"
        const val ADULTS = "adults"
        const val CHILDREN = "children"
        const val CHILDREN_AGES = "children-ages"
        const val ROOMS = "rooms"
        const val MIN_CHILD_AGE = 0
        const val MAX_CHILD_AGE = 17
    }
}
