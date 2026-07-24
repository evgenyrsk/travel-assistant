package com.travelassistant.backend.application.accommodation

import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict
import com.travelassistant.backend.domain.hotel.AccommodationSemanticMatch
import com.travelassistant.backend.domain.hotel.RankedHotelOffer

class SelectSemanticHotelOffersUseCase {

    operator fun invoke(candidates: List<Candidate>): List<RankedHotelOffer> =
        candidates
            .withIndex()
            .filter { indexed -> indexed.value.match.verdict in VISIBLE_VERDICTS }
            .sortedWith(
                compareBy<IndexedValue<Candidate>> { indexed ->
                    verdictPriority(indexed.value.match.verdict)
                }.thenBy { indexed -> indexed.index },
            )
            .map { indexed ->
                indexed.value.rankedOffer.copy(semanticMatch = indexed.value.match)
            }

    data class Candidate(
        val rankedOffer: RankedHotelOffer,
        val match: AccommodationSemanticMatch,
    )

    private fun verdictPriority(verdict: AccommodationMatchVerdict): Int =
        when (verdict) {
            AccommodationMatchVerdict.MATCH -> 0
            AccommodationMatchVerdict.PROBABLE -> 1
            AccommodationMatchVerdict.NO_MATCH,
            AccommodationMatchVerdict.UNKNOWN,
            -> 2
        }

    private companion object {
        val VISIBLE_VERDICTS = setOf(
            AccommodationMatchVerdict.MATCH,
            AccommodationMatchVerdict.PROBABLE,
        )
    }
}
