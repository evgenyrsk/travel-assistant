package com.travelassistant.backend.domain.assistant

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HotelRequirementsCoveragePlannerTest {

    @Test
    fun identifiesMissingRequiredSlotsFromFoundationState() {
        val plan = HotelRequirementsCoveragePlanner.plan(
            HotelRequirementsState.foundation(Instant.parse("2026-06-04T00:00:00Z")),
        )

        assertEquals(3, plan.requiredSlotCount)
        assertEquals(3, plan.missingRequiredSlotCount)
        assertEquals(
            listOf(
                HotelRequirementSlotKey.DESTINATION,
                HotelRequirementSlotKey.STAY_DATES,
                HotelRequirementSlotKey.GUESTS,
            ),
            plan.missingRequiredSlotKeys,
        )
        assertEquals(false, plan.requiredHotelSearchInputsComplete)
    }

    @Test
    fun returnsNextMissingRequiredSlotByDeterministicOrdering() {
        val state = HotelRequirementsState.foundation(
            Instant.parse("2026-06-04T00:00:00Z"),
        ).copy(
            slots = listOf(
                HotelRequirementSlotMetadata(
                    key = HotelRequirementSlotKey.GUESTS,
                    status = RequirementSlotStatus.MISSING,
                    requiredForHotelSearch = true,
                    order = 3,
                ),
                HotelRequirementSlotMetadata(
                    key = HotelRequirementSlotKey.DESTINATION,
                    status = RequirementSlotStatus.COLLECTED,
                    requiredForHotelSearch = true,
                    order = 1,
                ),
                HotelRequirementSlotMetadata(
                    key = HotelRequirementSlotKey.STAY_DATES,
                    status = RequirementSlotStatus.MISSING,
                    requiredForHotelSearch = true,
                    order = 2,
                ),
            ),
        )

        val plan = HotelRequirementsCoveragePlanner.plan(state)

        assertEquals(HotelRequirementSlotKey.STAY_DATES, plan.nextMissingRequiredSlotKey)
        assertEquals(
            listOf(
                HotelRequirementSlotKey.STAY_DATES,
                HotelRequirementSlotKey.GUESTS,
            ),
            plan.missingRequiredSlotKeys,
        )
    }

    @Test
    fun optionalPreferencesSlotDoesNotBlockRequiredCompletion() {
        val state = HotelRequirementsState.foundation(
            Instant.parse("2026-06-04T00:00:00Z"),
        ).copy(
            slots = listOf(
                HotelRequirementSlotMetadata(
                    key = HotelRequirementSlotKey.DESTINATION,
                    status = RequirementSlotStatus.COLLECTED,
                    requiredForHotelSearch = true,
                    order = 1,
                ),
                HotelRequirementSlotMetadata(
                    key = HotelRequirementSlotKey.STAY_DATES,
                    status = RequirementSlotStatus.COLLECTED,
                    requiredForHotelSearch = true,
                    order = 2,
                ),
                HotelRequirementSlotMetadata(
                    key = HotelRequirementSlotKey.GUESTS,
                    status = RequirementSlotStatus.COLLECTED,
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
        )

        val plan = HotelRequirementsCoveragePlanner.plan(state)

        assertEquals(3, plan.requiredSlotCount)
        assertEquals(0, plan.missingRequiredSlotCount)
        assertEquals(emptyList(), plan.missingRequiredSlotKeys)
        assertEquals(listOf(HotelRequirementSlotKey.PREFERENCES), plan.optionalSlotKeys)
        assertNull(plan.nextMissingRequiredSlotKey)
        assertEquals(true, plan.requiredHotelSearchInputsComplete)
    }
}
