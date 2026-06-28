package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.hotel.CreateHotelSearchCommand

sealed interface ConfirmedSearchCreationCommandPlan {

    data class CommandReady(
        val command: CreateHotelSearchCommand,
        val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy,
    ) : ConfirmedSearchCreationCommandPlan
}
