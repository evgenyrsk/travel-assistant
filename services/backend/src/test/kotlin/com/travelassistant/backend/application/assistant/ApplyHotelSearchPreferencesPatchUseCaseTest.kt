package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApplyHotelSearchPreferencesPatchUseCaseTest {
    private val store = InMemoryAssistantHotelConstraintsStore()
    private val useCase = ApplyHotelSearchPreferencesPatchUseCase(store)
    private val sessionId = AssistantSessionId("assistant-preferences-000001")

    @Test
    fun appliesAllSupportedPreferencesAsOneAtomicPatch() {
        val result = assertIs<ApplyHotelSearchPreferencesPatchResult.Applied>(
            useCase(
                command(
                    HotelSearchPreferencesPatch(
                        maxTotalPrice = HotelSearchPreferencePatch.Set(
                            HotelSearchPreferencesPatch.MaxTotalPriceInput(
                                amount = BigDecimal("80000.00"),
                            ),
                        ),
                        stars = HotelSearchPreferencePatch.Set(setOf(5, 4)),
                        minimumGuestRating = HotelSearchPreferencePatch.Set(8),
                        freeCancellationRequired = HotelSearchPreferencePatch.Set(true),
                    ),
                ),
            ),
        )

        assertEquals("80000", result.preferences.maxTotalPrice?.amount?.toPlainString())
        assertEquals("RUB", result.preferences.maxTotalPrice?.currency)
        assertEquals(linkedSetOf(4, 5), result.preferences.stars)
        assertEquals(
            HotelSearchPreferences.MinimumGuestRating.EIGHT,
            result.preferences.minimumGuestRating,
        )
        assertEquals(true, result.preferences.freeCancellationRequired)
        assertEquals(result.preferences, store.findBySession(sessionId)?.preferences)
    }

    @Test
    fun keepSetAndClearPreserveOnlyIntendedValues() {
        applyCompletePreferences()

        val result = assertIs<ApplyHotelSearchPreferencesPatchResult.Applied>(
            useCase(
                command(
                    HotelSearchPreferencesPatch(
                        stars = HotelSearchPreferencePatch.Clear,
                        minimumGuestRating = HotelSearchPreferencePatch.Set(9),
                        freeCancellationRequired = HotelSearchPreferencePatch.Clear,
                    ),
                ),
            ),
        )

        assertEquals("80000", result.preferences.maxTotalPrice?.amount?.toPlainString())
        assertEquals(emptySet(), result.preferences.stars)
        assertEquals(
            HotelSearchPreferences.MinimumGuestRating.NINE,
            result.preferences.minimumGuestRating,
        )
        assertEquals(false, result.preferences.freeCancellationRequired)
    }

    @Test
    fun rejectsInvalidPatchWithoutPartiallyMutatingSessionState() {
        val original = applyCompletePreferences()

        val result = assertIs<ApplyHotelSearchPreferencesPatchResult.Rejected>(
            useCase(
                command(
                    HotelSearchPreferencesPatch(
                        maxTotalPrice = HotelSearchPreferencePatch.Set(
                            HotelSearchPreferencesPatch.MaxTotalPriceInput(
                                amount = BigDecimal.ZERO,
                                currency = "EUR",
                            ),
                        ),
                        stars = HotelSearchPreferencePatch.Set(setOf(4, 6)),
                        minimumGuestRating = HotelSearchPreferencePatch.Set(10),
                        freeCancellationRequired = HotelSearchPreferencePatch.Set(false),
                    ),
                ),
            ),
        )

        assertEquals(
            setOf(
                HotelSearchPreferencesPatchIssue.INVALID_MAX_TOTAL_PRICE,
                HotelSearchPreferencesPatchIssue.UNSUPPORTED_MAX_TOTAL_PRICE_CURRENCY,
                HotelSearchPreferencesPatchIssue.INVALID_STARS,
                HotelSearchPreferencesPatchIssue.INVALID_MINIMUM_GUEST_RATING,
                HotelSearchPreferencesPatchIssue.INVALID_FREE_CANCELLATION_REQUIREMENT,
            ),
            result.issues,
        )
        assertEquals(original, result.currentPreferences)
        assertEquals(original, store.findBySession(sessionId)?.preferences)
    }

    @Test
    fun acceptsConfirmedProviderStarAndRatingBoundaries() {
        val result = assertIs<ApplyHotelSearchPreferencesPatchResult.Applied>(
            useCase(
                command(
                    HotelSearchPreferencesPatch(
                        stars = HotelSearchPreferencePatch.Set(setOf(0, 5)),
                        minimumGuestRating = HotelSearchPreferencePatch.Set(5),
                    ),
                ),
            ),
        )

        assertEquals(linkedSetOf(0, 5), result.preferences.stars)
        assertEquals(
            HotelSearchPreferences.MinimumGuestRating.FIVE,
            result.preferences.minimumGuestRating,
        )
    }

    @Test
    fun keepsSessionPreferencesIsolated() {
        val otherSessionId = AssistantSessionId("assistant-preferences-000002")

        useCase(
            command(
                HotelSearchPreferencesPatch(
                    stars = HotelSearchPreferencePatch.Set(setOf(4, 5)),
                ),
            ),
        )
        useCase(
            ApplyHotelSearchPreferencesPatchCommand(
                sessionId = otherSessionId,
                patch = HotelSearchPreferencesPatch(
                    minimumGuestRating = HotelSearchPreferencePatch.Set(9),
                ),
            ),
        )

        assertEquals(setOf(4, 5), store.findBySession(sessionId)?.preferences?.stars)
        assertEquals(
            HotelSearchPreferences.MinimumGuestRating.NINE,
            store.findBySession(otherSessionId)?.preferences?.minimumGuestRating,
        )
        assertEquals(emptySet(), store.findBySession(otherSessionId)?.preferences?.stars)
    }

    @Test
    fun preservesExistingCoreConstraintsWhenApplyingPreferences() {
        val constraints = AssistantHotelConstraints(destination = "Казань", adults = 2, rooms = 1)
        store.save(sessionId, constraints)

        useCase(
            command(
                HotelSearchPreferencesPatch(
                    freeCancellationRequired = HotelSearchPreferencePatch.Set(true),
                ),
            ),
        )

        val stored = store.findBySession(sessionId)
        assertEquals("Казань", stored?.destination)
        assertEquals(2, stored?.adults)
        assertEquals(1, stored?.rooms)
        assertEquals(true, stored?.preferences?.freeCancellationRequired)
    }

    private fun applyCompletePreferences(): HotelSearchPreferences {
        val result = assertIs<ApplyHotelSearchPreferencesPatchResult.Applied>(
            useCase(
                command(
                    HotelSearchPreferencesPatch(
                        maxTotalPrice = HotelSearchPreferencePatch.Set(
                            HotelSearchPreferencesPatch.MaxTotalPriceInput(
                                amount = BigDecimal("80000"),
                                currency = " rub ",
                            ),
                        ),
                        stars = HotelSearchPreferencePatch.Set(setOf(4, 5)),
                        minimumGuestRating = HotelSearchPreferencePatch.Set(8),
                        freeCancellationRequired = HotelSearchPreferencePatch.Set(true),
                    ),
                ),
            ),
        )
        return result.preferences
    }

    private fun command(
        patch: HotelSearchPreferencesPatch,
    ): ApplyHotelSearchPreferencesPatchCommand =
        ApplyHotelSearchPreferencesPatchCommand(
            sessionId = sessionId,
            patch = patch,
        )
}
