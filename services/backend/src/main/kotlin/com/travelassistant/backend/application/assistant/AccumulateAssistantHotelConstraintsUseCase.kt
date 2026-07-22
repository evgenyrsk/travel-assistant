package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.time.LocalDate

data class AccumulateAssistantHotelConstraintsCommand(
    val sessionId: AssistantSessionId,
    val extractedConstraints: Map<String, String>,
)

data class AssistantHotelConstraintsAccumulationResult(
    val constraints: AssistantHotelConstraints,
    val issues: Set<AssistantHotelConstraintsAccumulationIssue>,
)

enum class AssistantHotelConstraintsAccumulationIssue(
    val field: AssistantHotelConstraintField,
) {
    INVALID_DESTINATION(AssistantHotelConstraintField.DESTINATION),
    INVALID_CHECK_IN_DATE(AssistantHotelConstraintField.CHECK_IN),
    INVALID_CHECK_OUT_DATE(AssistantHotelConstraintField.CHECK_OUT),
    INVALID_DATE_RANGE(AssistantHotelConstraintField.CHECK_OUT),
    INVALID_ADULTS(AssistantHotelConstraintField.ADULTS),
    INVALID_CHILDREN(AssistantHotelConstraintField.CHILDREN),
    INVALID_CHILDREN_AGES(AssistantHotelConstraintField.CHILDREN_AGES),
    CHILDREN_AGES_COUNT_MISMATCH(AssistantHotelConstraintField.CHILDREN_AGES),
    INVALID_ROOMS(AssistantHotelConstraintField.ROOMS),
}

class AccumulateAssistantHotelConstraintsUseCase(
    private val store: AssistantHotelConstraintsStore,
) {
    operator fun invoke(
        command: AccumulateAssistantHotelConstraintsCommand,
    ): AssistantHotelConstraintsAccumulationResult {
        val current = store.findBySession(command.sessionId) ?: AssistantHotelConstraints()
        val issues = linkedSetOf<AssistantHotelConstraintsAccumulationIssue>()
        val unresolved = current.unresolvedFields.toMutableSet()

        var destination = current.destination
        var checkInDate = current.checkInDate
        var checkOutDate = current.checkOutDate
        var adults = current.adults
        var childrenCount = current.childrenCount
        var childrenAges = current.childrenAges
        var rooms = current.rooms

        val destinationValue = command.valueFor(AssistantHotelConstraintField.DESTINATION)
        if (destinationValue.wasProvided) {
            unresolved -= AssistantHotelConstraintField.DESTINATION
            destination = destinationValue.rawValue?.trim()?.takeIf(String::isNotEmpty)
            if (destination == null) {
                unresolved += AssistantHotelConstraintField.DESTINATION
                issues += AssistantHotelConstraintsAccumulationIssue.INVALID_DESTINATION
            }
        }

        val checkInValue = command.valueFor(AssistantHotelConstraintField.CHECK_IN)
        if (checkInValue.wasProvided) {
            unresolved -= AssistantHotelConstraintField.CHECK_IN
            checkInDate = checkInValue.rawValue.parseDateOrNull()
            if (checkInDate == null) {
                unresolved += AssistantHotelConstraintField.CHECK_IN
                issues += AssistantHotelConstraintsAccumulationIssue.INVALID_CHECK_IN_DATE
            }
        }

        val checkOutValue = command.valueFor(AssistantHotelConstraintField.CHECK_OUT)
        if (checkOutValue.wasProvided) {
            unresolved -= AssistantHotelConstraintField.CHECK_OUT
            checkOutDate = checkOutValue.rawValue.parseDateOrNull()
            if (checkOutDate == null) {
                unresolved += AssistantHotelConstraintField.CHECK_OUT
                issues += AssistantHotelConstraintsAccumulationIssue.INVALID_CHECK_OUT_DATE
            }
        }

        val adultsValue = command.valueFor(AssistantHotelConstraintField.ADULTS)
        if (adultsValue.wasProvided) {
            unresolved -= AssistantHotelConstraintField.ADULTS
            adults = adultsValue.rawValue.parseIntOrNull()?.takeIf { it >= 1 }
            if (adults == null) {
                unresolved += AssistantHotelConstraintField.ADULTS
                issues += AssistantHotelConstraintsAccumulationIssue.INVALID_ADULTS
            }
        }

        val childrenValue = command.valueFor(AssistantHotelConstraintField.CHILDREN)
        val parsedChildrenCount = if (childrenValue.wasProvided) {
            unresolved -= AssistantHotelConstraintField.CHILDREN
            childrenValue.rawValue.parseIntOrNull()?.takeIf { it >= 0 }
        } else {
            null
        }
        if (childrenValue.wasProvided) {
            if (parsedChildrenCount == null) {
                childrenCount = null
                unresolved += AssistantHotelConstraintField.CHILDREN
                issues += AssistantHotelConstraintsAccumulationIssue.INVALID_CHILDREN
            } else {
                val previousChildrenCount = childrenCount ?: childrenAges?.size
                childrenCount = parsedChildrenCount
                when {
                    parsedChildrenCount == 0 -> {
                        childrenAges = emptyList()
                        unresolved -= AssistantHotelConstraintField.CHILDREN_AGES
                    }

                    previousChildrenCount != parsedChildrenCount -> childrenAges = null
                }
            }
        }

        val childrenAgesValue = command.valueFor(AssistantHotelConstraintField.CHILDREN_AGES)
        if (childrenAgesValue.wasProvided) {
            unresolved -= AssistantHotelConstraintField.CHILDREN_AGES
            val parsedAges = childrenAgesValue.rawValue.parseChildrenAgesOrNull()
            if (parsedAges == null) {
                childrenAges = null
                unresolved += AssistantHotelConstraintField.CHILDREN_AGES
                issues += AssistantHotelConstraintsAccumulationIssue.INVALID_CHILDREN_AGES
            } else {
                val expectedChildrenCount = parsedChildrenCount ?: childrenCount
                if (expectedChildrenCount != null && expectedChildrenCount != parsedAges.size) {
                    childrenAges = null
                    unresolved += AssistantHotelConstraintField.CHILDREN_AGES
                    issues += AssistantHotelConstraintsAccumulationIssue.CHILDREN_AGES_COUNT_MISMATCH
                } else {
                    childrenAges = parsedAges
                    childrenCount = parsedAges.size
                    unresolved -= AssistantHotelConstraintField.CHILDREN
                }
            }
        }

        val roomsValue = command.valueFor(AssistantHotelConstraintField.ROOMS)
        if (roomsValue.wasProvided) {
            unresolved -= AssistantHotelConstraintField.ROOMS
            rooms = roomsValue.rawValue.parseIntOrNull()?.takeIf { it >= 1 }
            if (rooms == null) {
                unresolved += AssistantHotelConstraintField.ROOMS
                issues += AssistantHotelConstraintsAccumulationIssue.INVALID_ROOMS
            }
        }

        if (checkInDate != null && checkOutDate != null && !checkOutDate.isAfter(checkInDate)) {
            issues += AssistantHotelConstraintsAccumulationIssue.INVALID_DATE_RANGE
            when {
                checkInValue.wasProvided && checkOutValue.wasProvided -> {
                    checkInDate = null
                    checkOutDate = null
                    unresolved += AssistantHotelConstraintField.CHECK_IN
                    unresolved += AssistantHotelConstraintField.CHECK_OUT
                }

                checkInValue.wasProvided -> {
                    checkInDate = null
                    unresolved += AssistantHotelConstraintField.CHECK_IN
                }

                else -> {
                    checkOutDate = null
                    unresolved += AssistantHotelConstraintField.CHECK_OUT
                }
            }
        }

        val updated = AssistantHotelConstraints(
            destination = destination,
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            adults = adults,
            childrenCount = childrenCount,
            childrenAges = childrenAges,
            rooms = rooms,
            preferences = current.preferences,
            unresolvedFields = unresolved,
        )

        return AssistantHotelConstraintsAccumulationResult(
            constraints = store.save(command.sessionId, updated),
            issues = issues,
        )
    }

    private fun AccumulateAssistantHotelConstraintsCommand.valueFor(
        field: AssistantHotelConstraintField,
    ): ProvidedConstraintValue =
        ProvidedConstraintValue(
            wasProvided = extractedConstraints.containsKey(field.key),
            rawValue = extractedConstraints[field.key],
        )

    private fun String?.parseDateOrNull(): LocalDate? =
        this?.trim()?.takeIf(String::isNotEmpty)?.let { value ->
            runCatching { LocalDate.parse(value) }.getOrNull()
        }

    private fun String?.parseIntOrNull(): Int? =
        this?.trim()?.takeIf(String::isNotEmpty)?.toIntOrNull()

    private fun String?.parseChildrenAgesOrNull(): List<Int>? {
        val value = this?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val ages = value.split(',').map { rawAge ->
            rawAge.trim().toIntOrNull() ?: return null
        }
        return ages.takeIf { values -> values.all { age -> age in MIN_CHILD_AGE..MAX_CHILD_AGE } }
    }

    private data class ProvidedConstraintValue(
        val wasProvided: Boolean,
        val rawValue: String?,
    )

    private companion object {
        const val MIN_CHILD_AGE = 0
        const val MAX_CHILD_AGE = 17
    }
}
