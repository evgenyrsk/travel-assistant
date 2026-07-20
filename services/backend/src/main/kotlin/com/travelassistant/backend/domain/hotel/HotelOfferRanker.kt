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
                    "Доступно; рейтинг неизвестен, поэтому место определено по общей цене за проживание."
                } else {
                    "Доступно; выше размещены варианты с лучшим рейтингом, затем — с меньшей общей ценой за проживание."
                }

            HotelOffer.Availability.LIMITED ->
                if (offer.rating == null) {
                    "Мало мест; рейтинг неизвестен, вариант расположен после доступных предложений по общей цене за проживание."
                } else {
                    "Мало мест; вариант расположен после доступных предложений, затем учтены рейтинг и общая цена за проживание."
                }

            HotelOffer.Availability.UNKNOWN ->
                if (offer.rating == null) {
                    "Доступность и рейтинг неизвестны; вариант расположен после предложений с подтверждённой доступностью по общей цене за проживание."
                } else {
                    "Доступность неизвестна; вариант расположен после предложений с подтверждённой доступностью, затем учтены рейтинг и общая цена за проживание."
                }
        }
}
