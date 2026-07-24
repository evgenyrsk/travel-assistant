package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelLocationResolution
import com.travelassistant.backend.domain.hotel.HotelOffer
import com.travelassistant.backend.domain.hotel.HotelOfferCandidate
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.Locale

internal object HotelsApiExactHotelResponseMapper {
    fun map(
        candidate: HotelLocationResolution.HotelCandidate,
        criteria: HotelSearchCriteria,
        details: HotelsApiHotelDetailsResponseDto,
        rates: HotelsApiHotelRatesResponseDto,
    ): Result {
        val providerReference = candidate.providerReference.trim()
        if (
            providerReference.isEmpty() ||
            details.payload.hotelId.trim() != providerReference
        ) {
            return rejected(HotelsApiSearchMappingError.Issue.INVALID_PROVIDER_REFERENCE)
        }

        val hotelName = details.payload.hotelName.trim()
        if (hotelName.isEmpty()) {
            return rejected(
                issue = HotelsApiSearchMappingError.Issue.INVALID_HOTEL_NAME,
                providerReference = providerReference,
            )
        }
        val starRating = details.payload.starRating
        if (starRating != null && starRating !in MIN_STAR_RATING..MAX_STAR_RATING) {
            return rejected(
                issue = HotelsApiSearchMappingError.Issue.INVALID_STAR_RATING,
                providerReference = providerReference,
            )
        }

        val areaLocation = details.payload.areaLocation
        if (
            areaLocation == null ||
            areaLocation.destinationName.isBlank() ||
            areaLocation.countryName.isBlank()
        ) {
            return rejected(
                issue = HotelsApiSearchMappingError.Issue.INVALID_LOCATION,
                providerReference = providerReference,
            )
        }
        if (criteria.preferences.minimumGuestRating != null) {
            return rejected(
                issue = HotelsApiSearchMappingError.Issue.INVALID_REVIEW,
                providerReference = providerReference,
            )
        }
        if (
            criteria.preferences.stars.isNotEmpty() &&
            starRating == null
        ) {
            return rejected(
                issue = HotelsApiSearchMappingError.Issue.INVALID_STAR_RATING,
                providerReference = providerReference,
            )
        }
        if (
            criteria.preferences.stars.isNotEmpty() &&
            starRating !in criteria.preferences.stars
        ) {
            return Result.Mapped(emptyList())
        }

        val roomImages = rates.payload.rooms.associate { room ->
            room.roomId to room.images?.map { image -> image.url }
        }
        val mappedRates = rates.payload.rates.map { rate ->
            when (val mapped = mapRate(rate)) {
                is RateResult.Mapped -> mapped.value
                is RateResult.Rejected -> return rejected(
                    issue = mapped.issue,
                    providerReference = providerReference,
                )
            }
        }
        val matchingRates = mappedRates.filter { rate ->
            rate.matches(criteria)
        }
        val selectedRate = matchingRates.minWithOrNull(
            compareBy<RateValue> { rate -> rate.availableRoomsCount <= 0 }
                .thenBy { rate -> rate.priceAmount }
                .thenBy { rate -> rate.roomId },
        ) ?: return Result.Mapped(emptyList())

        val detailsImage = HotelsApiSafeImageUrlPolicy.firstOrNull(details.payload.images)
        val roomImage = HotelsApiSafeImageUrlPolicy.firstOrNull(roomImages[selectedRate.roomId])

        return Result.Mapped(
            listOf(
                HotelOfferCandidate(
                    providerReference = providerReference,
                    hotelName = hotelName,
                    city = areaLocation.destinationName.trim(),
                    country = areaLocation.countryName.trim(),
                    totalPrice = selectedRate.priceAmount,
                    currency = selectedRate.currency,
                    rating = null,
                    reviewCount = null,
                    amenities = null,
                    availability = if (selectedRate.availableRoomsCount > 0) {
                        HotelOffer.Availability.AVAILABLE
                    } else {
                        HotelOffer.Availability.UNKNOWN
                    },
                    source = SOURCE,
                    freshness = HotelOffer.Freshness.UNKNOWN,
                    starRating = starRating,
                    freeCancellationUntil = selectedRate.freeCancellationUntil,
                    imageUrl = detailsImage ?: roomImage,
                    breakfastIncluded = selectedRate.breakfastIncluded,
                ),
            ),
        )
    }

    private fun mapRate(rate: HotelsApiHotelRatesResponseDto.Rate): RateResult {
        if (rate.roomId.isBlank()) {
            return RateResult.Rejected(
                HotelsApiSearchMappingError.Issue.INVALID_PROVIDER_REFERENCE,
            )
        }
        if (rate.availableRoomsCount < 0) {
            return RateResult.Rejected(HotelsApiSearchMappingError.Issue.INVALID_AVAILABILITY)
        }
        if (!rate.shownPrice.amount.isFinite() || rate.shownPrice.amount < 0.0) {
            return RateResult.Rejected(HotelsApiSearchMappingError.Issue.INVALID_PRICE)
        }
        val currency = rate.shownPrice.currency.trim()
        if (currency.isEmpty()) {
            return RateResult.Rejected(HotelsApiSearchMappingError.Issue.INVALID_CURRENCY)
        }
        val freeCancellationUntil = try {
            rate.cancellationPolicyRules?.freeCancellationUntil
                ?.takeIf(String::isNotBlank)
                ?.let { value -> OffsetDateTime.parse(value).toInstant() }
        } catch (_: DateTimeParseException) {
            return RateResult.Rejected(HotelsApiSearchMappingError.Issue.INVALID_CANCELLATION)
        }

        return RateResult.Mapped(
            RateValue(
                roomId = rate.roomId,
                availableRoomsCount = rate.availableRoomsCount,
                priceAmount = rate.shownPrice.amount,
                currency = currency,
                freeCancellationUntil = freeCancellationUntil,
                breakfastIncluded = rate.mealType.toBreakfastFact(),
            ),
        )
    }

    private fun RateValue.matches(criteria: HotelSearchCriteria): Boolean {
        val preferences = criteria.preferences
        val maxTotalPrice = preferences.maxTotalPrice
        if (
            maxTotalPrice != null &&
            (
                currency != maxTotalPrice.currency ||
                    BigDecimal.valueOf(priceAmount) > maxTotalPrice.amount
                )
        ) {
            return false
        }
        if (preferences.freeCancellationRequired && freeCancellationUntil == null) {
            return false
        }
        if (preferences.breakfastIncludedRequired && breakfastIncluded != true) {
            return false
        }
        return true
    }

    private fun String?.toBreakfastFact(): Boolean? =
        when (this?.trim()?.lowercase(Locale.ROOT)) {
            BREAKFAST_MEAL_TYPE -> true
            NO_MEAL_TYPE -> false
            else -> null
        }

    private fun rejected(
        issue: HotelsApiSearchMappingError.Issue,
        providerReference: String? = null,
    ): Result.Rejected =
        Result.Rejected(
            HotelsApiSearchMappingError(
                issue = issue,
                providerReference = providerReference,
            ),
        )

    sealed interface Result {
        data class Mapped(val offers: List<HotelOfferCandidate>) : Result

        data class Rejected(val error: HotelsApiSearchMappingError) : Result
    }

    private sealed interface RateResult {
        data class Mapped(val value: RateValue) : RateResult

        data class Rejected(val issue: HotelsApiSearchMappingError.Issue) : RateResult
    }

    private data class RateValue(
        val roomId: String,
        val availableRoomsCount: Int,
        val priceAmount: Double,
        val currency: String,
        val freeCancellationUntil: java.time.Instant?,
        val breakfastIncluded: Boolean?,
    )

    private const val MIN_STAR_RATING = 0
    private const val MAX_STAR_RATING = 5
    private const val BREAKFAST_MEAL_TYPE = "breakfast"
    private const val NO_MEAL_TYPE = "nomeal"
    private const val SOURCE = "tbank_hotels_api"
}
