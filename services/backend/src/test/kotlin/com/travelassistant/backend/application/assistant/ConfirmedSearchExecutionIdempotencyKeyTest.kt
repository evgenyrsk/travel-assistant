package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.hotel.CreateHotelSearchCommand
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ConfirmedSearchExecutionIdempotencyKeyTest {
    @Test
    fun changesWhenConfirmedPreferencesChange() {
        val baseline = keyFor(HotelSearchPreferences())

        listOf(
            HotelSearchPreferences(
                maxTotalPrice = HotelSearchPreferences.MaxTotalPrice(
                    amount = BigDecimal("80000"),
                    currency = "RUB",
                ),
            ),
            HotelSearchPreferences(stars = setOf(4, 5)),
            HotelSearchPreferences(
                minimumGuestRating = HotelSearchPreferences.MinimumGuestRating.EIGHT,
            ),
            HotelSearchPreferences(freeCancellationRequired = true),
        ).forEach { preferences ->
            assertNotEquals(baseline, keyFor(preferences))
        }
    }

    @Test
    fun canonicalizesStarOrderAndEquivalentPriceScale() {
        val first = HotelSearchPreferences(
            maxTotalPrice = HotelSearchPreferences.MaxTotalPrice(
                amount = BigDecimal("80000.0"),
                currency = "RUB",
            ),
            stars = linkedSetOf(5, 4),
        )
        val second = HotelSearchPreferences(
            maxTotalPrice = HotelSearchPreferences.MaxTotalPrice(
                amount = BigDecimal("80000.00"),
                currency = "RUB",
            ),
            stars = linkedSetOf(4, 5),
        )

        assertEquals(keyFor(first), keyFor(second))
    }

    private fun keyFor(
        preferences: HotelSearchPreferences,
    ): ConfirmedSearchExecutionIdempotencyKey =
        ConfirmedSearchExecutionIdempotencyKey.from(
            ConfirmedSearchCreationCommandPlan.CommandReady(
                command = CreateHotelSearchCommand(
                    sessionId = AssistantSessionId("assistant-preferences-idempotency-000001"),
                    criteria = HotelSearchCriteria(
                        destination = "Казань",
                        checkInDate = LocalDate.parse("2026-08-10"),
                        checkOutDate = LocalDate.parse("2026-08-14"),
                        guests = HotelSearchCriteria.Guests(
                            adults = 2,
                            childrenAges = emptyList(),
                        ),
                        rooms = 1,
                        preferences = preferences,
                    ),
                ),
                lifecyclePolicy = ConfirmedSearchCreationLifecyclePolicy(),
            ),
        )
}
