package com.travelassistant.backend.domain.hotel

class HotelOfferRanker {

    fun rank(offers: List<HotelOffer>): List<RankedHotelOffer> =
        offers
            .sortedWith(
                compareBy<HotelOffer> { availabilityPriority(it.availability) }
                    .thenBy { if (it.rating == null) 1 else 0 }
                    .thenByDescending { it.rating ?: Double.NEGATIVE_INFINITY }
                    .thenBy { it.totalPrice }
                    .thenBy { it.id },
            )
            .map { offer ->
                RankedHotelOffer(
                    offer = offer,
                    matchSummary = rankingReason(offer),
                )
            }

    private fun availabilityPriority(availability: HotelOffer.Availability): Int =
        when (availability) {
            HotelOffer.Availability.AVAILABLE -> 0
            HotelOffer.Availability.LIMITED -> 1
            HotelOffer.Availability.UNKNOWN -> 2
        }

    private fun rankingReason(offer: HotelOffer): String =
        when (offer.availability) {
            HotelOffer.Availability.AVAILABLE ->
                if (offer.rating == null) {
                    "Available; rating unavailable, ranked by total stay price, then offer ID."
                } else {
                    "Available; ranked by rating, total stay price, then offer ID."
                }

            HotelOffer.Availability.LIMITED ->
                if (offer.rating == null) {
                    "Limited availability; rating unavailable, ranked after available offers, then by total stay price."
                } else {
                    "Limited availability; ranked after available offers, then by rating and total stay price."
                }

            HotelOffer.Availability.UNKNOWN ->
                if (offer.rating == null) {
                    "Availability unknown; rating unavailable, ranked after confirmed offers, then by total stay price."
                } else {
                    "Availability unknown; ranked after confirmed offers, then by rating and total stay price."
                }
        }
}
