package com.travelassistant.backend.domain.assistant

import java.time.Instant

enum class RequirementSlotStatus(val value: String) {
    MISSING("missing"),
    UNKNOWN("unknown"),
    COLLECTED("collected"),
}

enum class HotelRequirementSlotKey(val value: String) {
    DESTINATION("destination"),
    STAY_DATES("stay_dates"),
    GUESTS("guests"),
    PREFERENCES("preferences"),
}

data class HotelRequirementSlotMetadata(
    val key: HotelRequirementSlotKey,
    val status: RequirementSlotStatus,
    val requiredForHotelSearch: Boolean,
    val order: Int,
)

data class HotelRequirementsState(
    val slots: List<HotelRequirementSlotMetadata>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun foundation(createdAt: Instant): HotelRequirementsState =
            HotelRequirementsState(
                slots = listOf(
                    HotelRequirementSlotMetadata(
                        key = HotelRequirementSlotKey.DESTINATION,
                        status = RequirementSlotStatus.MISSING,
                        requiredForHotelSearch = true,
                        order = 1,
                    ),
                    HotelRequirementSlotMetadata(
                        key = HotelRequirementSlotKey.STAY_DATES,
                        status = RequirementSlotStatus.MISSING,
                        requiredForHotelSearch = true,
                        order = 2,
                    ),
                    HotelRequirementSlotMetadata(
                        key = HotelRequirementSlotKey.GUESTS,
                        status = RequirementSlotStatus.MISSING,
                        requiredForHotelSearch = true,
                        order = 3,
                    ),
                    HotelRequirementSlotMetadata(
                        key = HotelRequirementSlotKey.PREFERENCES,
                        status = RequirementSlotStatus.UNKNOWN,
                        requiredForHotelSearch = false,
                        order = 4,
                    ),
                ),
                createdAt = createdAt,
                updatedAt = createdAt,
            )
    }
}
