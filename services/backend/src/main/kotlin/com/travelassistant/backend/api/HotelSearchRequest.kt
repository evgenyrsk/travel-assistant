package com.travelassistant.backend.api

import com.travelassistant.backend.application.hotel.CreateHotelSearchCommand
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

    @Serializable
    data class Guests(
        val adults: Int? = null,
        val children: Int = 0,
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
        if (guests.children < 0) {
            return ValidationResult.Invalid(
                field = "criteria.guests.children",
                message = "Children count must not be negative.",
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
                        children = guests.children,
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
