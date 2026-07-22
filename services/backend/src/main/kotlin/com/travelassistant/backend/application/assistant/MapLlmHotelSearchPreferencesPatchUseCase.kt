package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.LlmHotelSearchPreferencesPatch
import java.math.BigDecimal
import java.util.Locale

class MapLlmHotelSearchPreferencesPatchUseCase {

    operator fun invoke(
        source: LlmHotelSearchPreferencesPatch,
    ): MapLlmHotelSearchPreferencesPatchResult {
        val issues = linkedSetOf<MapLlmHotelSearchPreferencesPatchIssue>()
        val amount = source.maxTotalPrice?.validatedAmount(issues)

        source.validateSetValues(issues)
        source.validateConflicts(issues)

        if (issues.isNotEmpty()) {
            return MapLlmHotelSearchPreferencesPatchResult.Rejected(issues)
        }

        return MapLlmHotelSearchPreferencesPatchResult.Mapped(
            HotelSearchPreferencesPatch(
                maxTotalPrice = source.operationFor(
                    field = LlmHotelSearchPreferencesPatch.Field.MAX_TOTAL_PRICE,
                    value = source.maxTotalPrice?.let { price ->
                        HotelSearchPreferencesPatch.MaxTotalPriceInput(
                            amount = checkNotNull(amount),
                            currency = price.currency,
                        )
                    },
                ),
                stars = source.operationFor(
                    field = LlmHotelSearchPreferencesPatch.Field.STARS,
                    value = source.stars?.sorted()?.toCollection(linkedSetOf()),
                ),
                minimumGuestRating = source.operationFor(
                    field = LlmHotelSearchPreferencesPatch.Field.MINIMUM_GUEST_RATING,
                    value = source.minimumGuestRating,
                ),
                freeCancellationRequired = source.operationFor(
                    field = LlmHotelSearchPreferencesPatch.Field.FREE_CANCELLATION,
                    value = source.freeCancellationRequired,
                ),
            ),
        )
    }

    private fun LlmHotelSearchPreferencesPatch.MaxTotalPrice.validatedAmount(
        issues: MutableSet<MapLlmHotelSearchPreferencesPatchIssue>,
    ): BigDecimal? {
        val amount = amount.trim().toBigDecimalOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) {
            issues += MapLlmHotelSearchPreferencesPatchIssue.INVALID_MAX_TOTAL_PRICE
        }

        val normalizedCurrency = currency
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.uppercase(Locale.ROOT)
        if (currency != null && normalizedCurrency == null) {
            issues += MapLlmHotelSearchPreferencesPatchIssue.INVALID_MAX_TOTAL_PRICE
        } else if (normalizedCurrency != null && normalizedCurrency != SUPPORTED_CURRENCY) {
            issues += MapLlmHotelSearchPreferencesPatchIssue.UNSUPPORTED_MAX_TOTAL_PRICE_CURRENCY
        }
        return amount
    }

    private fun LlmHotelSearchPreferencesPatch.validateSetValues(
        issues: MutableSet<MapLlmHotelSearchPreferencesPatchIssue>,
    ) {
        if (stars != null && (stars.isEmpty() || stars.any { star -> star !in MIN_STARS..MAX_STARS })) {
            issues += MapLlmHotelSearchPreferencesPatchIssue.INVALID_STARS
        }
        if (minimumGuestRating != null && minimumGuestRating !in SUPPORTED_GUEST_RATINGS) {
            issues += MapLlmHotelSearchPreferencesPatchIssue.INVALID_MINIMUM_GUEST_RATING
        }
        if (freeCancellationRequired == false) {
            issues += MapLlmHotelSearchPreferencesPatchIssue.INVALID_FREE_CANCELLATION_REQUIREMENT
        }
    }

    private fun LlmHotelSearchPreferencesPatch.validateConflicts(
        issues: MutableSet<MapLlmHotelSearchPreferencesPatchIssue>,
    ) {
        val setFields = buildSet {
            if (maxTotalPrice != null) add(LlmHotelSearchPreferencesPatch.Field.MAX_TOTAL_PRICE)
            if (stars != null) add(LlmHotelSearchPreferencesPatch.Field.STARS)
            if (minimumGuestRating != null) {
                add(LlmHotelSearchPreferencesPatch.Field.MINIMUM_GUEST_RATING)
            }
            if (freeCancellationRequired != null) {
                add(LlmHotelSearchPreferencesPatch.Field.FREE_CANCELLATION)
            }
        }
        if (setFields.any(clear::contains)) {
            issues += MapLlmHotelSearchPreferencesPatchIssue.CONFLICTING_OPERATION
        }
    }

    private fun <T> LlmHotelSearchPreferencesPatch.operationFor(
        field: LlmHotelSearchPreferencesPatch.Field,
        value: T?,
    ): HotelSearchPreferencePatch<T> =
        when {
            field in clear -> HotelSearchPreferencePatch.Clear
            value != null -> HotelSearchPreferencePatch.Set(value)
            else -> HotelSearchPreferencePatch.Keep
        }

    private companion object {
        const val SUPPORTED_CURRENCY = "RUB"
        const val MIN_STARS = 0
        const val MAX_STARS = 5
        val SUPPORTED_GUEST_RATINGS = 5..9
    }
}
