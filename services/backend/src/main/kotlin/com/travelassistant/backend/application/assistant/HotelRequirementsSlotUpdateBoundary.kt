package com.travelassistant.backend.application.assistant

interface HotelRequirementsSlotUpdateBoundary {
    fun updateHotelRequirementSlot(
        command: UpdateHotelRequirementSlotCommand,
    ): UpdateHotelRequirementSlotResult
}
