package com.travelassistant.backend.application.llm

data class LlmHotelSearchPreferencesPatch(
    val maxTotalPrice: MaxTotalPrice? = null,
    val stars: Set<Int>? = null,
    val minimumGuestRating: Int? = null,
    val freeCancellationRequired: Boolean? = null,
    val breakfastIncludedRequired: Boolean? = null,
    val clear: Set<Field> = emptySet(),
) {
    val isEmpty: Boolean
        get() = maxTotalPrice == null &&
            stars == null &&
            minimumGuestRating == null &&
            freeCancellationRequired == null &&
            breakfastIncludedRequired == null &&
            clear.isEmpty()

    data class MaxTotalPrice(
        val amount: String,
        val currency: String? = null,
    )

    enum class Field(
        val wireName: String,
    ) {
        MAX_TOTAL_PRICE("max-total-price"),
        STARS("stars"),
        MINIMUM_GUEST_RATING("min-guest-rating"),
        FREE_CANCELLATION("free-cancellation"),
        BREAKFAST_INCLUDED("breakfast-included"),
        ;

        companion object {
            fun fromWireName(value: String): Field? =
                entries.firstOrNull { field -> field.wireName == value }
        }
    }
}
