package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import java.math.BigDecimal
import java.util.Locale

class ApplyHotelSearchPreferencesPatchUseCase(
    private val store: AssistantHotelConstraintsStore,
) {
    operator fun invoke(
        command: ApplyHotelSearchPreferencesPatchCommand,
    ): ApplyHotelSearchPreferencesPatchResult {
        val currentConstraints = store.findBySession(command.sessionId)
            ?: AssistantHotelConstraints()
        val currentPreferences = currentConstraints.preferences
        val issues = linkedSetOf<HotelSearchPreferencesPatchIssue>()
        val updatedPreferences = currentPreferences.copy(
            maxTotalPrice = command.patch.maxTotalPrice.applyMaxTotalPrice(
                current = currentPreferences.maxTotalPrice,
                issues = issues,
            ),
            stars = command.patch.stars.applyStars(
                current = currentPreferences.stars,
                issues = issues,
            ),
            minimumGuestRating = command.patch.minimumGuestRating.applyMinimumGuestRating(
                current = currentPreferences.minimumGuestRating,
                issues = issues,
            ),
            freeCancellationRequired = command.patch.freeCancellationRequired.applyFreeCancellation(
                current = currentPreferences.freeCancellationRequired,
                issues = issues,
            ),
            breakfastIncludedRequired = command.patch.breakfastIncludedRequired
                .applyBreakfastIncluded(
                    current = currentPreferences.breakfastIncludedRequired,
                    issues = issues,
                ),
            accommodationConcept = command.patch.accommodationConcept.applyNullable(
                currentPreferences.accommodationConcept,
            ),
        )

        if (issues.isNotEmpty()) {
            return ApplyHotelSearchPreferencesPatchResult.Rejected(
                currentPreferences = currentPreferences,
                issues = issues,
            )
        }

        val savedConstraints = store.save(
            command.sessionId,
            currentConstraints.copy(preferences = updatedPreferences),
        )
        return ApplyHotelSearchPreferencesPatchResult.Applied(savedConstraints.preferences)
    }

    private fun HotelSearchPreferencePatch<HotelSearchPreferencesPatch.MaxTotalPriceInput>
        .applyMaxTotalPrice(
            current: HotelSearchPreferences.MaxTotalPrice?,
            issues: MutableSet<HotelSearchPreferencesPatchIssue>,
        ): HotelSearchPreferences.MaxTotalPrice? =
        when (this) {
            HotelSearchPreferencePatch.Keep -> current
            HotelSearchPreferencePatch.Clear -> null
            is HotelSearchPreferencePatch.Set -> {
                val normalizedCurrency = value.currency
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.uppercase(Locale.ROOT)
                    ?: DEFAULT_CURRENCY
                val invalidAmount = value.amount <= BigDecimal.ZERO
                val unsupportedCurrency = normalizedCurrency != DEFAULT_CURRENCY

                if (invalidAmount) {
                    issues += HotelSearchPreferencesPatchIssue.INVALID_MAX_TOTAL_PRICE
                }
                if (unsupportedCurrency) {
                    issues += HotelSearchPreferencesPatchIssue.UNSUPPORTED_MAX_TOTAL_PRICE_CURRENCY
                }

                if (invalidAmount || unsupportedCurrency) {
                    current
                } else {
                    HotelSearchPreferences.MaxTotalPrice(
                        amount = value.amount.stripTrailingZeros(),
                        currency = normalizedCurrency,
                    )
                }
            }
        }

    private fun HotelSearchPreferencePatch<Set<Int>>.applyStars(
        current: Set<Int>,
        issues: MutableSet<HotelSearchPreferencesPatchIssue>,
    ): Set<Int> =
        when (this) {
            HotelSearchPreferencePatch.Keep -> current
            HotelSearchPreferencePatch.Clear -> emptySet()
            is HotelSearchPreferencePatch.Set -> {
                if (value.isEmpty() || value.any { star -> star !in MIN_STARS..MAX_STARS }) {
                    issues += HotelSearchPreferencesPatchIssue.INVALID_STARS
                    current
                } else {
                    value.sorted().toCollection(linkedSetOf())
                }
            }
        }

    private fun HotelSearchPreferencePatch<Int>.applyMinimumGuestRating(
        current: HotelSearchPreferences.MinimumGuestRating?,
        issues: MutableSet<HotelSearchPreferencesPatchIssue>,
    ): HotelSearchPreferences.MinimumGuestRating? =
        when (this) {
            HotelSearchPreferencePatch.Keep -> current
            HotelSearchPreferencePatch.Clear -> null
            is HotelSearchPreferencePatch.Set ->
                HotelSearchPreferences.MinimumGuestRating.fromValue(value) ?: run {
                    issues += HotelSearchPreferencesPatchIssue.INVALID_MINIMUM_GUEST_RATING
                    current
                }
        }

    private fun HotelSearchPreferencePatch<Boolean>.applyFreeCancellation(
        current: Boolean,
        issues: MutableSet<HotelSearchPreferencesPatchIssue>,
    ): Boolean =
        when (this) {
            HotelSearchPreferencePatch.Keep -> current
            HotelSearchPreferencePatch.Clear -> false
            is HotelSearchPreferencePatch.Set ->
                if (value) {
                    true
                } else {
                    issues += HotelSearchPreferencesPatchIssue.INVALID_FREE_CANCELLATION_REQUIREMENT
                    current
                }
        }

    private fun HotelSearchPreferencePatch<Boolean>.applyBreakfastIncluded(
        current: Boolean,
        issues: MutableSet<HotelSearchPreferencesPatchIssue>,
    ): Boolean =
        when (this) {
            HotelSearchPreferencePatch.Keep -> current
            HotelSearchPreferencePatch.Clear -> false
            is HotelSearchPreferencePatch.Set ->
                if (value) {
                    true
                } else {
                    issues += HotelSearchPreferencesPatchIssue.INVALID_BREAKFAST_REQUIREMENT
                    current
                }
        }

    private fun <T> HotelSearchPreferencePatch<T>.applyNullable(current: T?): T? =
        when (this) {
            HotelSearchPreferencePatch.Keep -> current
            HotelSearchPreferencePatch.Clear -> null
            is HotelSearchPreferencePatch.Set -> value
        }

    private companion object {
        const val DEFAULT_CURRENCY = "RUB"
        const val MIN_STARS = 0
        const val MAX_STARS = 5
    }
}
