package com.travelassistant.backend.application.hotel

sealed interface HotelNoOffersRefinementPlan {
    data object NotApplicable : HotelNoOffersRefinementPlan

    data class Suggestion(
        val preference: Preference,
        val message: String,
    ) : HotelNoOffersRefinementPlan

    enum class Preference(
        val apiValue: String,
    ) {
        MINIMUM_GUEST_RATING("minimumGuestRating"),
        STARS("stars"),
        FREE_CANCELLATION_REQUIRED("freeCancellationRequired"),
        BREAKFAST_INCLUDED_REQUIRED("breakfastIncludedRequired"),
        MAX_TOTAL_PRICE("maxTotalPrice"),
    }
}
