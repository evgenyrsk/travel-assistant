package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.HotelRequirementSlotKey
import com.travelassistant.backend.domain.assistant.HotelRequirementSlotMetadata
import com.travelassistant.backend.domain.assistant.HotelRequirementsCoveragePlanner
import com.travelassistant.backend.domain.assistant.HotelRequirementsState
import com.travelassistant.backend.domain.assistant.RequirementSlotStatus
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class AssistantResponseSemanticsTest {

    @Test
    fun keepsClarificationNextActionWhenRequiredInputsAreMissing() {
        val plan = HotelRequirementsCoveragePlanner.plan(
            HotelRequirementsState.foundation(Instant.parse("2026-06-04T00:00:00Z")),
        )

        assertEquals(
            AssistantSearchReadiness.MISSING_REQUIRED_INPUTS,
            AssistantResponseSemantics.searchReadinessFor(plan),
        )
        assertEquals(
            AssistantNextAction.ASK_CLARIFICATION,
            AssistantResponseSemantics.nextActionFor(plan),
        )
    }

    @Test
    fun returnsBoundaryNextActionWhenRequiredInputsAreInternallyCollected() {
        val plan = HotelRequirementsCoveragePlanner.plan(
            HotelRequirementsState(
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
                createdAt = Instant.parse("2026-06-04T00:00:00Z"),
                updatedAt = Instant.parse("2026-06-04T00:00:00Z"),
            ),
        )

        assertEquals(
            AssistantSearchReadiness.REQUIRED_INPUTS_COLLECTED,
            AssistantResponseSemantics.searchReadinessFor(plan),
        )
        assertEquals(
            AssistantNextAction.SHOW_BOUNDARY_MESSAGE,
            AssistantResponseSemantics.nextActionFor(plan),
        )
    }
}
