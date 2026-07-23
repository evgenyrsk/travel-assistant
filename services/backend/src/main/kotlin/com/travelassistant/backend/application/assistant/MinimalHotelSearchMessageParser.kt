package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import java.time.LocalDate

class MinimalHotelSearchMessageParser {

    sealed interface Result {
        data object NotRequested : Result

        data object Incomplete : Result

        data class Complete(
            val criteria: HotelSearchCriteria,
        ) : Result
    }

    fun parse(message: String): Result {
        val normalizedMessage = message.trim()
        if (!normalizedMessage.startsWith(PREFIX, ignoreCase = true)) {
            return Result.NotRequested
        }

        val fields = parseFields(normalizedMessage.substring(PREFIX.length))
            ?: return Result.Incomplete
        val destination = fields["destination"]?.takeIf { it.isNotBlank() }
            ?: return Result.Incomplete
        val checkInDate = fields["check-in"].toDateOrNull()
            ?: return Result.Incomplete
        val checkOutDate = fields["check-out"].toDateOrNull()
            ?: return Result.Incomplete
        val adults = fields["adults"]?.toIntOrNull()?.takeIf { it >= 1 }
            ?: return Result.Incomplete
        val children = when (val rawChildren = fields["children"]) {
            null -> 0
            else -> rawChildren.toIntOrNull()?.takeIf { it >= 0 } ?: return Result.Incomplete
        }
        val childrenAges = fields["children-ages"]
            ?.toChildrenAgesOrNull()
            ?: if (children == 0 && "children-ages" !in fields) emptyList() else return Result.Incomplete
        if (childrenAges.size != children) {
            return Result.Incomplete
        }
        val rooms = when (val rawRooms = fields["rooms"]) {
            null -> DEFAULT_ASSISTANT_ROOM_COUNT
            else -> rawRooms.toIntOrNull()?.takeIf { it >= 1 } ?: return Result.Incomplete
        }

        if (!checkOutDate.isAfter(checkInDate)) {
            return Result.Incomplete
        }

        return Result.Complete(
            HotelSearchCriteria(
                destination = destination,
                checkInDate = checkInDate,
                checkOutDate = checkOutDate,
                guests = HotelSearchCriteria.Guests(
                    adults = adults,
                    childrenAges = childrenAges,
                ),
                rooms = rooms,
            ),
        )
    }

    private fun parseFields(rawFields: String): Map<String, String>? {
        val fields = linkedMapOf<String, String>()

        for (rawField in rawFields.split(';')) {
            val field = rawField.trim()
            if (field.isEmpty()) {
                continue
            }

            val parts = field.split('=', limit = 2)
            if (parts.size != 2) {
                return null
            }

            val key = parts[0].trim().lowercase()
            val value = parts[1].trim()
            if (key !in SUPPORTED_FIELDS || value.isEmpty() || fields.put(key, value) != null) {
                return null
            }
        }

        return fields
    }

    private fun String?.toDateOrNull(): LocalDate? =
        this?.let { value ->
            runCatching { LocalDate.parse(value) }.getOrNull()
        }

    private fun String.toChildrenAgesOrNull(): List<Int>? {
        if (isBlank()) {
            return null
        }

        val ages = split(',').map { value ->
            value.trim().toIntOrNull() ?: return null
        }
        return ages.takeIf { values -> values.all { it in MIN_CHILD_AGE..MAX_CHILD_AGE } }
    }

    private companion object {
        const val PREFIX = "hotel-search;"
        val SUPPORTED_FIELDS = setOf(
            "destination",
            "check-in",
            "check-out",
            "adults",
            "children",
            "children-ages",
            "rooms",
        )
        const val MIN_CHILD_AGE = 0
        const val MAX_CHILD_AGE = 17
    }
}
