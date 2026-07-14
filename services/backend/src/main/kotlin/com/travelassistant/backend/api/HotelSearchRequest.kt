package com.travelassistant.backend.api

import com.travelassistant.backend.application.hotel.CreateHotelSearchCommand
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import java.time.LocalDate
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val MIN_CHILD_AGE = 0
private const val MAX_CHILD_AGE = 17

@Serializable
data class HotelSearchRequest(
    val sessionId: String? = null,
    val criteria: Criteria? = null,
    val searchIntentSummary: JsonElement? = null,
) {
    @Serializable
    data class Criteria(
        val destination: String? = null,
        val checkInDate: String? = null,
        val checkOutDate: String? = null,
        val guests: Guests? = null,
        val rooms: Int? = null,
        val budget: JsonElement? = null,
        val preferences: List<JsonElement> = emptyList(),
        val requiredAmenities: List<String> = emptyList(),
        val assistantAssumptions: List<JsonElement> = emptyList(),
        val derivedAssumptions: List<JsonElement> = emptyList(),
        val unknowns: List<JsonElement> = emptyList(),
    )

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Guests(
        val adults: Int? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val children: Int? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val childrenAges: List<Int>? = null,
    )

    sealed interface ValidationResult {
        data class Valid(val command: CreateHotelSearchCommand) : ValidationResult

        data class Invalid(
            val field: String,
            val message: String,
        ) : ValidationResult
    }

    fun validate(): ValidationResult {
        val validSessionId = sessionId?.trim().orEmpty()
        if (validSessionId.isBlank()) {
            return ValidationResult.Invalid(
                field = "sessionId",
                message = "Session identifier must be present and not blank.",
            )
        }

        val validCriteria = criteria ?: return ValidationResult.Invalid(
            field = "criteria",
            message = "Hotel search criteria must be present.",
        )
        val destination = validCriteria.destination?.trim().orEmpty()
        if (destination.isBlank()) {
            return ValidationResult.Invalid(
                field = "criteria.destination",
                message = "Destination must be present and not blank.",
            )
        }

        val checkInDate = validCriteria.checkInDate.toDateOrNull()
            ?: return ValidationResult.Invalid(
                field = "criteria.checkInDate",
                message = "Check-in date must use ISO-8601 date format.",
            )
        val checkOutDate = validCriteria.checkOutDate.toDateOrNull()
            ?: return ValidationResult.Invalid(
                field = "criteria.checkOutDate",
                message = "Check-out date must use ISO-8601 date format.",
            )
        if (!checkOutDate.isAfter(checkInDate)) {
            return ValidationResult.Invalid(
                field = "criteria.checkOutDate",
                message = "Check-out date must be after check-in date.",
            )
        }

        val guests = validCriteria.guests ?: return ValidationResult.Invalid(
            field = "criteria.guests",
            message = "Guest composition must be present.",
        )
        val adults = guests.adults
        if (adults == null || adults < 1) {
            return ValidationResult.Invalid(
                field = "criteria.guests.adults",
                message = "At least one adult is required.",
            )
        }
        val children = guests.children ?: guests.childrenAges?.size ?: 0
        if (children < 0) {
            return ValidationResult.Invalid(
                field = "criteria.guests.children",
                message = "Children count must not be negative.",
            )
        }
        val childrenAges = guests.childrenAges
        if (children > 0 && childrenAges == null) {
            return ValidationResult.Invalid(
                field = "criteria.guests.childrenAges",
                message = "An age is required for every child.",
            )
        }
        if (childrenAges != null && childrenAges.any { it !in MIN_CHILD_AGE..MAX_CHILD_AGE }) {
            return ValidationResult.Invalid(
                field = "criteria.guests.childrenAges",
                message = "Each child age must be between 0 and 17.",
            )
        }
        if (guests.children != null && childrenAges != null && childrenAges.size != children) {
            return ValidationResult.Invalid(
                field = "criteria.guests.childrenAges",
                message = "Children count must match the number of child ages.",
            )
        }
        if (validCriteria.rooms != null && validCriteria.rooms < 1) {
            return ValidationResult.Invalid(
                field = "criteria.rooms",
                message = "Rooms count must be at least one when provided.",
            )
        }
        if (
            validCriteria.rooms == null &&
            validCriteria.derivedAssumptions.none { it.isRoomCountAssumption() }
        ) {
            return ValidationResult.Invalid(
                field = "criteria.rooms",
                message = "Rooms count or a visible room_count derived assumption is required.",
            )
        }

        return ValidationResult.Valid(
            CreateHotelSearchCommand(
                sessionId = AssistantSessionId(validSessionId),
                criteria = HotelSearchCriteria(
                    destination = destination,
                    checkInDate = checkInDate,
                    checkOutDate = checkOutDate,
                    guests = HotelSearchCriteria.Guests(
                        adults = adults,
                        childrenAges = childrenAges.orEmpty(),
                    ),
                    rooms = validCriteria.rooms,
                ),
            ),
        )
    }

    private fun String?.toDateOrNull(): LocalDate? =
        this?.let { value ->
            runCatching { LocalDate.parse(value) }.getOrNull()
        }

    private fun JsonElement.isRoomCountAssumption(): Boolean =
        runCatching {
            jsonObject["category"]?.jsonPrimitive?.content == "room_count"
        }.getOrDefault(false)
}
