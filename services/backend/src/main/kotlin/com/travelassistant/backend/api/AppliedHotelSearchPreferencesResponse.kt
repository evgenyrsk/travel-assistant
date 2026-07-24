package com.travelassistant.backend.api

import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AppliedHotelSearchPreferencesResponse(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val maxTotalPrice: MaximumTotalPrice? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val stars: List<Int>? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val minimumGuestRating: Int? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val freeCancellationRequired: Boolean? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val breakfastIncludedRequired: Boolean? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val accommodationConcept: String? = null,
) {
    @Serializable
    data class MaximumTotalPrice(
        val amount: String,
        val currency: String,
    )

    companion object {
        fun from(
            preferences: HotelSearchPreferences,
        ): AppliedHotelSearchPreferencesResponse? =
            if (preferences.isEmpty) {
                null
            } else {
                AppliedHotelSearchPreferencesResponse(
                    maxTotalPrice = preferences.maxTotalPrice?.let { price ->
                        MaximumTotalPrice(
                            amount = price.amount.stripTrailingZeros().toPlainString(),
                            currency = price.currency,
                        )
                    },
                    stars = preferences.stars
                        .takeIf(Set<Int>::isNotEmpty)
                        ?.sorted(),
                    minimumGuestRating = preferences.minimumGuestRating?.value,
                    freeCancellationRequired = if (preferences.freeCancellationRequired) {
                        true
                    } else {
                        null
                    },
                    breakfastIncludedRequired = if (preferences.breakfastIncludedRequired) {
                        true
                    } else {
                        null
                    },
                    accommodationConcept = preferences.accommodationConcept?.code,
                )
            }
    }
}
