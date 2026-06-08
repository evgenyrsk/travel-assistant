package com.travelassistant.backend.domain.assistant

data class HotelRequirementsCoveragePlan(
    val requiredSlotCount: Int,
    val missingRequiredSlotCount: Int,
    val missingRequiredSlotKeys: List<HotelRequirementSlotKey>,
    val optionalSlotKeys: List<HotelRequirementSlotKey>,
    val nextMissingRequiredSlotKey: HotelRequirementSlotKey?,
    val requiredHotelSearchInputsComplete: Boolean,
)

/**
 * Internal deterministic Stage 7.9 planner for future clarification flow.
 *
 * It only reads slot metadata. It does not extract, fill, infer, persist, or
 * generate user-facing clarification questions.
 */
object HotelRequirementsCoveragePlanner {
    fun plan(requirementsState: HotelRequirementsState): HotelRequirementsCoveragePlan {
        val orderedSlots = requirementsState.slots.sortedBy { it.order }
        val requiredSlots = orderedSlots.filter { it.requiredForHotelSearch }
        val missingRequiredSlots = requiredSlots.filter { it.status != RequirementSlotStatus.COLLECTED }

        return HotelRequirementsCoveragePlan(
            requiredSlotCount = requiredSlots.size,
            missingRequiredSlotCount = missingRequiredSlots.size,
            missingRequiredSlotKeys = missingRequiredSlots.map { it.key },
            optionalSlotKeys = orderedSlots.filterNot { it.requiredForHotelSearch }.map { it.key },
            nextMissingRequiredSlotKey = missingRequiredSlots.firstOrNull()?.key,
            requiredHotelSearchInputsComplete = missingRequiredSlots.isEmpty(),
        )
    }
}
