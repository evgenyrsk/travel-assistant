package com.travelassistant.backend.domain.hotel

import java.math.BigDecimal

data class HotelSearchPreferences(
    val maxTotalPrice: MaxTotalPrice? = null,
    val stars: Set<Int> = emptySet(),
    val minimumGuestRating: MinimumGuestRating? = null,
    val freeCancellationRequired: Boolean = false,
    val breakfastIncludedRequired: Boolean = false,
) {
    val isEmpty: Boolean
        get() = maxTotalPrice == null &&
            stars.isEmpty() &&
            minimumGuestRating == null &&
            !freeCancellationRequired &&
            !breakfastIncludedRequired

    data class MaxTotalPrice internal constructor(
        val amount: BigDecimal,
        val currency: String,
    )

    enum class MinimumGuestRating(
        val value: Int,
    ) {
        FIVE(5),
        SIX(6),
        SEVEN(7),
        EIGHT(8),
        NINE(9),
        ;

        companion object {
            fun fromValue(value: Int): MinimumGuestRating? =
                entries.firstOrNull { rating -> rating.value == value }
        }
    }
}
