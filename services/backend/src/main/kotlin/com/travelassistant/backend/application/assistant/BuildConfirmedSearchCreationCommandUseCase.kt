package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.hotel.CreateHotelSearchCommand
import com.travelassistant.backend.domain.assistant.AssistantSessionId

class BuildConfirmedSearchCreationCommandUseCase {

    operator fun invoke(
        sessionId: AssistantSessionId,
        plan: ConfirmedSearchCreationPlan.ReadyToCreateSearch,
    ): ConfirmedSearchCreationCommandPlan =
        ConfirmedSearchCreationCommandPlan.CommandReady(
            command = CreateHotelSearchCommand(
                sessionId = sessionId,
                criteria = plan.criteria,
            ),
            lifecyclePolicy = plan.lifecyclePolicy,
        )
}
