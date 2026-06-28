package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchCriteria

class PlanConfirmedSearchCreationUseCase(
    private val mapCriteria: (ProceedWithCandidateCriteria) -> HotelSearchCriteria =
        ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper()::invoke,
) {

    operator fun invoke(
        decision: PostConfirmationDecision.Confirmed,
    ): ConfirmedSearchCreationPlan =
        ConfirmedSearchCreationPlan.ReadyToCreateSearch(
            criteria = mapCriteria(decision.criteria),
        )
}
