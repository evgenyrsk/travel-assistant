package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import java.math.BigDecimal

internal object HotelsApiSearchFilterMapper {

    fun map(preferences: HotelSearchPreferences): Result {
        val maxTotalPrice = preferences.maxTotalPrice
        if (maxTotalPrice != null && maxTotalPrice.amount <= BigDecimal.ZERO) {
            return rejected(HotelsApiSearchMappingError.Issue.INVALID_MAX_TOTAL_PRICE)
        }
        if (maxTotalPrice != null && maxTotalPrice.currency != SUPPORTED_CURRENCY) {
            return rejected(
                HotelsApiSearchMappingError.Issue.UNSUPPORTED_MAX_TOTAL_PRICE_CURRENCY,
            )
        }
        if (preferences.stars.any { star -> star !in MIN_STARS..MAX_STARS }) {
            return rejected(HotelsApiSearchMappingError.Issue.INVALID_STARS)
        }

        val filters = buildList {
            maxTotalPrice?.let { price ->
                add(
                    HotelsApiSearchFilterDto.Range(
                        filterId = PRICE_FILTER_ID,
                        min = BigDecimal.ZERO,
                        max = price.amount,
                    ),
                )
            }
            if (preferences.stars.isNotEmpty()) {
                add(
                    HotelsApiSearchFilterDto.StringArray(
                        filterId = STARS_FILTER_ID,
                        values = preferences.stars.sorted().map(Int::toString),
                    ),
                )
            }
            preferences.minimumGuestRating?.let { rating ->
                add(
                    HotelsApiSearchFilterDto.Radio(
                        filterId = REVIEW_RATING_FILTER_ID,
                        value = rating.value.toString(),
                    ),
                )
            }
            if (preferences.freeCancellationRequired) {
                add(
                    HotelsApiSearchFilterDto.BooleanValue(
                        filterId = FREE_CANCELLATION_FILTER_ID,
                        value = true,
                    ),
                )
            }
            if (preferences.breakfastIncludedRequired) {
                add(
                    HotelsApiSearchFilterDto.StringArray(
                        filterId = MEAL_TYPES_FILTER_ID,
                        values = listOf(BREAKFAST_FILTER_VALUE),
                    ),
                )
            }
        }

        return Result.Mapped(filters)
    }

    sealed interface Result {
        data class Mapped(
            val filters: List<HotelsApiSearchFilterDto>,
        ) : Result

        data class Rejected(
            val error: HotelsApiSearchMappingError,
        ) : Result
    }

    private fun rejected(
        issue: HotelsApiSearchMappingError.Issue,
    ): Result.Rejected =
        Result.Rejected(HotelsApiSearchMappingError(issue = issue))

    private const val SUPPORTED_CURRENCY = "RUB"
    private const val MIN_STARS = 0
    private const val MAX_STARS = 5
    private const val PRICE_FILTER_ID = "price"
    private const val STARS_FILTER_ID = "stars"
    private const val REVIEW_RATING_FILTER_ID = "review_rating"
    private const val FREE_CANCELLATION_FILTER_ID = "free_cancellation_allowed"
    private const val MEAL_TYPES_FILTER_ID = "meal_types"
    private const val BREAKFAST_FILTER_VALUE = "breakfast"
}
