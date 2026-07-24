package com.travelassistant.backend.api

import com.travelassistant.backend.application.hotel.HotelNoOffersRefinementPlan
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import com.travelassistant.backend.domain.hotel.AccommodationAnalysisMetadata
import com.travelassistant.backend.domain.hotel.AccommodationConcept
import com.travelassistant.backend.domain.hotel.HotelSearch
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import com.travelassistant.backend.domain.hotel.HotelSearchId
import com.travelassistant.backend.domain.hotel.HotelSearchPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HotelOffersResponseTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun `omits applied preferences when search has none`() {
        val encoded = json.encodeToString(HotelOffersResponse.from(search()))

        assertFalse(encoded.contains("\"appliedPreferences\""))
    }

    @Test
    fun `exposes active provider-neutral preferences without inferred defaults`() {
        val preferences = HotelSearchPreferences(
            maxTotalPrice = HotelSearchPreferences.MaxTotalPrice(
                amount = BigDecimal("80000.50"),
                currency = "RUB",
            ),
            stars = linkedSetOf(5, 4),
            minimumGuestRating = HotelSearchPreferences.MinimumGuestRating.EIGHT,
            freeCancellationRequired = true,
            breakfastIncludedRequired = true,
            accommodationConcept = AccommodationConcept.GLAMPING,
        )

        val encoded = json.encodeToString(HotelOffersResponse.from(search(preferences)))
        val applied = Json.parseToJsonElement(encoded)
            .jsonObject
            .getValue("appliedPreferences")
            .jsonObject

        assertEquals(
            setOf(
                "maxTotalPrice",
                "stars",
                "minimumGuestRating",
                "freeCancellationRequired",
                "breakfastIncludedRequired",
                "accommodationConcept",
            ),
            applied.keys,
        )
        assertEquals(
            "80000.5",
            applied.getValue("maxTotalPrice").jsonObject.getValue("amount").jsonPrimitive.content,
        )
        assertEquals(
            "RUB",
            applied.getValue("maxTotalPrice").jsonObject.getValue("currency").jsonPrimitive.content,
        )
        assertEquals(
            listOf(4, 5),
            applied.getValue("stars").jsonArray.map { star -> star.jsonPrimitive.content.toInt() },
        )
        assertEquals(8, applied.getValue("minimumGuestRating").jsonPrimitive.content.toInt())
        assertTrue(applied.getValue("freeCancellationRequired").jsonPrimitive.content.toBoolean())
        assertTrue(applied.getValue("breakfastIncludedRequired").jsonPrimitive.content.toBoolean())
        assertEquals("glamping", applied.getValue("accommodationConcept").jsonPrimitive.content)
    }

    @Test
    fun `exposes searching analysis metadata and poll interval`() {
        val encoded = json.encodeToString(
            HotelOffersResponse.from(
                search(
                    preferences = HotelSearchPreferences(
                        accommodationConcept = AccommodationConcept.GLAMPING,
                    ),
                    status = HotelSearch.Status.SEARCHING,
                    analysis = AccommodationAnalysisMetadata.searching(pollAfterMillis = 1_500),
                ),
            ),
        )
        val response = Json.parseToJsonElement(encoded).jsonObject
        val analysis = response.getValue("metadata").jsonObject
            .getValue("analysis").jsonObject

        assertEquals("searching", response.getValue("status").jsonPrimitive.content)
        assertEquals("searching", analysis.getValue("status").jsonPrimitive.content)
        assertEquals(0, analysis.getValue("analyzedCount").jsonPrimitive.content.toInt())
        assertEquals(1_500, analysis.getValue("pollAfterMillis").jsonPrimitive.content.toInt())
    }

    @Test
    fun `marks partial semantic analysis without changing terminal success shape`() {
        val encoded = json.encodeToString(
            HotelOffersResponse.from(
                search(
                    preferences = HotelSearchPreferences(
                        accommodationConcept = AccommodationConcept.GLAMPING,
                    ),
                    status = HotelSearch.Status.COMPLETED_NO_SEMANTIC_MATCHES,
                    analysis = AccommodationAnalysisMetadata(
                        status = AccommodationAnalysisMetadata.Status.PARTIAL,
                        analyzedCount = 12,
                        deepAnalyzedCount = 4,
                        matchCount = 0,
                        probableCount = 0,
                    ),
                ),
            ),
        )
        val metadata = Json.parseToJsonElement(encoded).jsonObject
            .getValue("metadata").jsonObject

        assertEquals("partial", metadata.getValue("resultCompleteness").jsonPrimitive.content)
        assertFalse(metadata.getValue("analysis").jsonObject.containsKey("pollAfterMillis"))
    }

    @Test
    fun `exposes one typed no-offers refinement suggestion`() {
        val encoded = json.encodeToString(
            HotelOffersResponse.from(
                search = search(
                    HotelSearchPreferences(
                        minimumGuestRating = HotelSearchPreferences.MinimumGuestRating.EIGHT,
                    ),
                ),
                refinementPlan = HotelNoOffersRefinementPlan.Suggestion(
                    preference = HotelNoOffersRefinementPlan.Preference.MINIMUM_GUEST_RATING,
                    message = "Уберите ограничение и подтвердите новый поиск.",
                ),
            ),
        )
        val suggestion = Json.parseToJsonElement(encoded)
            .jsonObject
            .getValue("refinementSuggestion")
            .jsonObject

        assertEquals("relax_preference", suggestion.getValue("type").jsonPrimitive.content)
        assertEquals(
            "minimumGuestRating",
            suggestion.getValue("preference").jsonPrimitive.content,
        )
        assertEquals(
            "Уберите ограничение и подтвердите новый поиск.",
            suggestion.getValue("message").jsonPrimitive.content,
        )
    }

    private fun search(
        preferences: HotelSearchPreferences = HotelSearchPreferences(),
        status: HotelSearch.Status = HotelSearch.Status.COMPLETED_NO_OFFERS,
        analysis: AccommodationAnalysisMetadata? = null,
    ): HotelSearch =
        HotelSearch(
            id = HotelSearchId("hotel-search-test"),
            sessionId = AssistantSessionId("assistant-session-test"),
            criteria = HotelSearchCriteria(
                destination = "Kazan",
                checkInDate = LocalDate.parse("2026-08-10"),
                checkOutDate = LocalDate.parse("2026-08-14"),
                guests = HotelSearchCriteria.Guests(adults = 2),
                rooms = 1,
                preferences = preferences,
            ),
            status = status,
            offers = emptyList(),
            analysis = analysis,
        )
}
