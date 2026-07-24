package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.LlmHotelSearchPreferencesPatch
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MapLlmHotelSearchPreferencesPatchUseCaseTest {
    private val mapper = MapLlmHotelSearchPreferencesPatchUseCase()

    @Test
    fun mapsExplicitValuesAndClearToTypedOperations() {
        val result = assertIs<MapLlmHotelSearchPreferencesPatchResult.Mapped>(
            mapper(
                LlmHotelSearchPreferencesPatch(
                    maxTotalPrice = LlmHotelSearchPreferencesPatch.MaxTotalPrice(
                        amount = "80000.00",
                    ),
                    stars = setOf(5, 4),
                    freeCancellationRequired = true,
                    breakfastIncludedRequired = true,
                    clear = setOf(
                        LlmHotelSearchPreferencesPatch.Field.MINIMUM_GUEST_RATING,
                    ),
                ),
            ),
        )

        assertEquals(
            HotelSearchPreferencesPatch(
                maxTotalPrice = HotelSearchPreferencePatch.Set(
                    HotelSearchPreferencesPatch.MaxTotalPriceInput(
                        amount = BigDecimal("80000.00"),
                    ),
                ),
                stars = HotelSearchPreferencePatch.Set(linkedSetOf(4, 5)),
                minimumGuestRating = HotelSearchPreferencePatch.Clear,
                freeCancellationRequired = HotelSearchPreferencePatch.Set(true),
                breakfastIncludedRequired = HotelSearchPreferencePatch.Set(true),
            ),
            result.patch,
        )
    }

    @Test
    fun mapsAbsentPreferenceValuesToKeep() {
        val result = assertIs<MapLlmHotelSearchPreferencesPatchResult.Mapped>(
            mapper(LlmHotelSearchPreferencesPatch()),
        )

        assertEquals(HotelSearchPreferencesPatch(), result.patch)
    }

    @Test
    fun mappedPatchesCanAccumulateAndClearSessionPreferencesWithoutProviderExecution() {
        val store = InMemoryAssistantHotelConstraintsStore()
        val applyPatch = ApplyHotelSearchPreferencesPatchUseCase(store)
        val sessionId = AssistantSessionId("assistant-llm-preferences-000001")
        store.save(
            sessionId,
            AssistantHotelConstraints(destination = "Казань", adults = 2, rooms = 1),
        )

        val firstPatch = assertIs<MapLlmHotelSearchPreferencesPatchResult.Mapped>(
            mapper(
                LlmHotelSearchPreferencesPatch(
                    maxTotalPrice = LlmHotelSearchPreferencesPatch.MaxTotalPrice("80000"),
                    stars = setOf(4, 5),
                    minimumGuestRating = 8,
                    freeCancellationRequired = true,
                    breakfastIncludedRequired = true,
                ),
            ),
        ).patch
        applyPatch(ApplyHotelSearchPreferencesPatchCommand(sessionId, firstPatch))

        val clearRating = assertIs<MapLlmHotelSearchPreferencesPatchResult.Mapped>(
            mapper(
                LlmHotelSearchPreferencesPatch(
                    clear = setOf(
                        LlmHotelSearchPreferencesPatch.Field.MINIMUM_GUEST_RATING,
                    ),
                ),
            ),
        ).patch
        val applied = assertIs<ApplyHotelSearchPreferencesPatchResult.Applied>(
            applyPatch(ApplyHotelSearchPreferencesPatchCommand(sessionId, clearRating)),
        )

        assertEquals("80000", applied.preferences.maxTotalPrice?.amount?.toPlainString())
        assertEquals("RUB", applied.preferences.maxTotalPrice?.currency)
        assertEquals(setOf(4, 5), applied.preferences.stars)
        assertEquals(null, applied.preferences.minimumGuestRating)
        assertEquals(true, applied.preferences.freeCancellationRequired)
        assertEquals(true, applied.preferences.breakfastIncludedRequired)
        assertEquals("Казань", store.findBySession(sessionId)?.destination)
    }

    @Test
    fun rejectsInvalidValuesAndConflictingOperationsAtomically() {
        val result = assertIs<MapLlmHotelSearchPreferencesPatchResult.Rejected>(
            mapper(
                LlmHotelSearchPreferencesPatch(
                    maxTotalPrice = LlmHotelSearchPreferencesPatch.MaxTotalPrice(
                        amount = "not-a-price",
                        currency = "EUR",
                    ),
                    stars = setOf(4, 6),
                    minimumGuestRating = 10,
                    freeCancellationRequired = false,
                    breakfastIncludedRequired = false,
                    clear = setOf(
                        LlmHotelSearchPreferencesPatch.Field.MINIMUM_GUEST_RATING,
                    ),
                ),
            ),
        )

        assertEquals(
            setOf(
                MapLlmHotelSearchPreferencesPatchIssue.INVALID_MAX_TOTAL_PRICE,
                MapLlmHotelSearchPreferencesPatchIssue.UNSUPPORTED_MAX_TOTAL_PRICE_CURRENCY,
                MapLlmHotelSearchPreferencesPatchIssue.INVALID_STARS,
                MapLlmHotelSearchPreferencesPatchIssue.INVALID_MINIMUM_GUEST_RATING,
                MapLlmHotelSearchPreferencesPatchIssue.INVALID_FREE_CANCELLATION_REQUIREMENT,
                MapLlmHotelSearchPreferencesPatchIssue.INVALID_BREAKFAST_REQUIREMENT,
                MapLlmHotelSearchPreferencesPatchIssue.CONFLICTING_OPERATION,
            ),
            result.issues,
        )
    }

    @Test
    fun mapsExactDiscreteGuestRatingThresholdsOnly() {
        HotelSearchPreferences.MinimumGuestRating.entries.forEach { rating ->
            val result = assertIs<MapLlmHotelSearchPreferencesPatchResult.Mapped>(
                mapper(
                    LlmHotelSearchPreferencesPatch(
                        minimumGuestRating = rating.value,
                    ),
                ),
            )

            assertEquals(
                HotelSearchPreferencePatch.Set(rating.value),
                result.patch.minimumGuestRating,
            )
        }
    }
}
