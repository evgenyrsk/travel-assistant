package com.travelassistant.backend.domain.hotel

class HotelOfferRanker {

    fun rank(offers: List<HotelOffer>): List<RankedHotelOffer> =
        offers
            .sortedWith(
                compareBy<HotelOffer> { availabilityPriority(it.availability) }
                    .thenByDescending { it.rating }
                    .thenBy { it.totalPrice }
                    .thenBy { it.id },
            )
            .map { offer ->
                RankedHotelOffer(
                    offer = offer,
                    matchSummary = rankingReason(offer.availability),
                )
            }

    private fun availabilityPriority(availability: HotelOffer.Availability): Int =
        when (availability) {
            HotelOffer.Availability.AVAILABLE -> 0
            HotelOffer.Availability.LIMITED -> 1
            HotelOffer.Availability.UNKNOWN -> 2
        }

    private fun rankingReason(availability: HotelOffer.Availability): String =
        when (availability) {
            HotelOffer.Availability.AVAILABLE ->
                "Available; ranked by rating, total stay price, then offer ID."

            HotelOffer.Availability.LIMITED ->
                "Limited availability; ranked after available offers, then by rating and total stay price."

            HotelOffer.Availability.UNKNOWN ->
                "Availability unknown; ranked after confirmed offers, then by rating and total stay price."
        }
}
