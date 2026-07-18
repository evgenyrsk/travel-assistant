package com.travelassistant.backend.application.assistant

import java.time.LocalDate

data class AssistantHotelConstraints(
    val destination: String? = null,
    val checkInDate: LocalDate? = null,
    val checkOutDate: LocalDate? = null,
    val adults: Int? = null,
    val childrenCount: Int? = null,
    val childrenAges: List<Int>? = null,
    val rooms: Int? = null,
    val unresolvedFields: Set<AssistantHotelConstraintField> = emptySet(),
) {
    fun toConfirmedConstraints(): Map<String, String> =
        buildMap {
            destination
                ?.takeIf { AssistantHotelConstraintField.DESTINATION !in unresolvedFields }
                ?.let { put(AssistantHotelConstraintField.DESTINATION.key, it) }
            checkInDate
                ?.takeIf { AssistantHotelConstraintField.CHECK_IN !in unresolvedFields }
                ?.let { put(AssistantHotelConstraintField.CHECK_IN.key, it.toString()) }
            checkOutDate
                ?.takeIf { AssistantHotelConstraintField.CHECK_OUT !in unresolvedFields }
                ?.let { put(AssistantHotelConstraintField.CHECK_OUT.key, it.toString()) }
            adults
                ?.takeIf { AssistantHotelConstraintField.ADULTS !in unresolvedFields }
                ?.let { put(AssistantHotelConstraintField.ADULTS.key, it.toString()) }

            val resolvedChildrenCount = childrenCount ?: childrenAges?.size
            resolvedChildrenCount
                ?.takeIf { AssistantHotelConstraintField.CHILDREN !in unresolvedFields }
                ?.let { put(AssistantHotelConstraintField.CHILDREN.key, it.toString()) }
            childrenAges
                ?.takeIf(List<Int>::isNotEmpty)
                ?.takeIf { AssistantHotelConstraintField.CHILDREN_AGES !in unresolvedFields }
                ?.let { ages ->
                    put(
                        AssistantHotelConstraintField.CHILDREN_AGES.key,
                        ages.joinToString(","),
                    )
                }

            rooms
                ?.takeIf { AssistantHotelConstraintField.ROOMS !in unresolvedFields }
                ?.let { put(AssistantHotelConstraintField.ROOMS.key, it.toString()) }
        }

    fun missingRequiredFields(): List<String> {
        val missingFields = linkedSetOf<AssistantHotelConstraintField>()
        if (destination == null) missingFields += AssistantHotelConstraintField.DESTINATION
        if (checkInDate == null) missingFields += AssistantHotelConstraintField.CHECK_IN
        if (checkOutDate == null) missingFields += AssistantHotelConstraintField.CHECK_OUT
        if (adults == null) missingFields += AssistantHotelConstraintField.ADULTS

        val resolvedChildrenCount = childrenCount ?: childrenAges?.size
        if (
            resolvedChildrenCount != null &&
            resolvedChildrenCount > 0 &&
            childrenAges?.size != resolvedChildrenCount
        ) {
            missingFields += AssistantHotelConstraintField.CHILDREN_AGES
        }

        if (rooms == null) missingFields += AssistantHotelConstraintField.ROOMS
        missingFields += unresolvedFields

        return AssistantHotelConstraintField.entries
            .filter(missingFields::contains)
            .map(AssistantHotelConstraintField::key)
    }
}

enum class AssistantHotelConstraintField(
    val key: String,
) {
    DESTINATION("destination"),
    CHECK_IN("check-in"),
    CHECK_OUT("check-out"),
    ADULTS("adults"),
    CHILDREN("children"),
    CHILDREN_AGES("children-ages"),
    ROOMS("rooms"),
}
