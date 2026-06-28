package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchCriteria

sealed interface ConfirmedSearchCreationPlan {

    data class ReadyToCreateSearch(
        val criteria: HotelSearchCriteria,
        val lifecyclePolicy: ConfirmedSearchCreationLifecyclePolicy =
            ConfirmedSearchCreationLifecyclePolicy(),
    ) : ConfirmedSearchCreationPlan
}
