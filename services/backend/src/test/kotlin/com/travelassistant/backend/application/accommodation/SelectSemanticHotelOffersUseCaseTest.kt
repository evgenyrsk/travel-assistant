package com.travelassistant.backend.application.accommodation

import com.travelassistant.backend.domain.hotel.AccommodationConcept
import com.travelassistant.backend.domain.hotel.AccommodationEvidenceSource
import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict
import com.travelassistant.backend.domain.hotel.AccommodationSemanticMatch
import com.travelassistant.backend.domain.hotel.HotelOffer
import com.travelassistant.backend.domain.hotel.RankedHotelOffer
import kotlin.test.Test
import kotlin.test.assertEquals

class SelectSemanticHotelOffersUseCaseTest {
    private val useCase = SelectSemanticHotelOffersUseCase()

    @Test
    fun `shows match then probable and preserves existing order within group`() {
        val selected = useCase(
            listOf(
                candidate("offer-1", AccommodationMatchVerdict.PROBABLE),
                candidate("offer-2", AccommodationMatchVerdict.MATCH),
                candidate("offer-3", AccommodationMatchVerdict.NO_MATCH),
                candidate("offer-4", AccommodationMatchVerdict.MATCH),
                candidate("offer-5", AccommodationMatchVerdict.UNKNOWN),
                candidate("offer-6", AccommodationMatchVerdict.PROBABLE),
            ),
        )

        assertEquals(
            listOf("offer-2", "offer-4", "offer-1", "offer-6"),
            selected.map { ranked -> ranked.offer.id },
        )
        assertEquals(
            listOf(
                AccommodationMatchVerdict.MATCH,
                AccommodationMatchVerdict.MATCH,
                AccommodationMatchVerdict.PROBABLE,
                AccommodationMatchVerdict.PROBABLE,
            ),
            selected.map { ranked -> ranked.semanticMatch?.verdict },
        )
    }

    private fun candidate(
        offerId: String,
        verdict: AccommodationMatchVerdict,
    ): SelectSemanticHotelOffersUseCase.Candidate =
        SelectSemanticHotelOffersUseCase.Candidate(
            rankedOffer = RankedHotelOffer(
                offer = HotelOffer(
                    id = offerId,
                    providerReference = "internal-$offerId",
                    hotelName = "Synthetic hotel",
                    city = "Test city",
                    country = "Test country",
                    totalPrice = 100.0,
                    currency = "RUB",
                    rating = null,
                    reviewCount = null,
                    amenities = null,
                    availability = HotelOffer.Availability.AVAILABLE,
                    source = "fake",
                    freshness = HotelOffer.Freshness.FRESH,
                ),
                matchSummary = "Existing deterministic rank",
            ),
            match = AccommodationSemanticMatch(
                concept = AccommodationConcept.GLAMPING,
                verdict = verdict,
                evidenceSources = setOf(AccommodationEvidenceSource.NAME),
            ),
        )
}
