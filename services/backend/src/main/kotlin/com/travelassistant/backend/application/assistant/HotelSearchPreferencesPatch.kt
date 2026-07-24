package com.travelassistant.backend.application.assistant

import java.math.BigDecimal

data class HotelSearchPreferencesPatch(
    val maxTotalPrice: HotelSearchPreferencePatch<MaxTotalPriceInput> =
        HotelSearchPreferencePatch.Keep,
    val stars: HotelSearchPreferencePatch<Set<Int>> = HotelSearchPreferencePatch.Keep,
    val minimumGuestRating: HotelSearchPreferencePatch<Int> = HotelSearchPreferencePatch.Keep,
    val freeCancellationRequired: HotelSearchPreferencePatch<Boolean> =
        HotelSearchPreferencePatch.Keep,
    val breakfastIncludedRequired: HotelSearchPreferencePatch<Boolean> =
        HotelSearchPreferencePatch.Keep,
) {
    data class MaxTotalPriceInput(
        val amount: BigDecimal,
        val currency: String? = null,
    )
}
