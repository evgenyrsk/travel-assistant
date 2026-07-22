package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.domain.hotel.HotelSearch

class PlanHotelNoOffersRefinementUseCase {

    operator fun invoke(search: HotelSearch): HotelNoOffersRefinementPlan {
        if (
            search.status != HotelSearch.Status.COMPLETED_NO_OFFERS ||
            search.offers.isNotEmpty()
        ) {
            return HotelNoOffersRefinementPlan.NotApplicable
        }

        val preferences = search.criteria.preferences
        val preference = when {
            preferences.minimumGuestRating != null ->
                HotelNoOffersRefinementPlan.Preference.MINIMUM_GUEST_RATING

            preferences.stars.isNotEmpty() ->
                HotelNoOffersRefinementPlan.Preference.STARS

            preferences.freeCancellationRequired ->
                HotelNoOffersRefinementPlan.Preference.FREE_CANCELLATION_REQUIRED

            preferences.maxTotalPrice != null ->
                HotelNoOffersRefinementPlan.Preference.MAX_TOTAL_PRICE

            else -> return HotelNoOffersRefinementPlan.NotApplicable
        }

        return HotelNoOffersRefinementPlan.Suggestion(
            preference = preference,
            message = messageFor(preference),
        )
    }

    private fun messageFor(
        preference: HotelNoOffersRefinementPlan.Preference,
    ): String =
        when (preference) {
            HotelNoOffersRefinementPlan.Preference.MINIMUM_GUEST_RATING ->
                "По текущим условиям предложения не найдены. Можно убрать ограничение по гостевому рейтингу и подтвердить новый поиск."

            HotelNoOffersRefinementPlan.Preference.STARS ->
                "По текущим условиям предложения не найдены. Можно убрать ограничение по звёздам и подтвердить новый поиск."

            HotelNoOffersRefinementPlan.Preference.FREE_CANCELLATION_REQUIRED ->
                "По текущим условиям предложения не найдены. Можно убрать требование бесплатной отмены и подтвердить новый поиск."

            HotelNoOffersRefinementPlan.Preference.MAX_TOTAL_PRICE ->
                "По текущим условиям предложения не найдены. Можно убрать ограничение по общей стоимости и подтвердить новый поиск."
        }
}
