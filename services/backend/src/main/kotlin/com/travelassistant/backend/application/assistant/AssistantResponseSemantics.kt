package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.HotelRequirementsCoveragePlan

object AssistantResponseSemantics {
    fun searchReadinessFor(
        coveragePlan: HotelRequirementsCoveragePlan,
    ): AssistantSearchReadiness =
        if (coveragePlan.requiredHotelSearchInputsComplete) {
            AssistantSearchReadiness.REQUIRED_INPUTS_COLLECTED
        } else {
            AssistantSearchReadiness.MISSING_REQUIRED_INPUTS
        }

    fun nextActionFor(
        coveragePlan: HotelRequirementsCoveragePlan,
    ): AssistantNextAction =
        when (searchReadinessFor(coveragePlan)) {
            AssistantSearchReadiness.MISSING_REQUIRED_INPUTS -> AssistantNextAction.ASK_CLARIFICATION
            AssistantSearchReadiness.REQUIRED_INPUTS_COLLECTED -> AssistantNextAction.SHOW_BOUNDARY_MESSAGE
        }
}
