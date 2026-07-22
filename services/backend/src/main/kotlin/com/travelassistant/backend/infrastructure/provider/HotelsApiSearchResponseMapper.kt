package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.domain.hotel.HotelOffer
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

internal object HotelsApiSearchResponseMapper {

    fun map(response: HotelsApiSearchResponseDto): Result {
        val errors = mutableListOf<HotelsApiSearchMappingError>()
        val offersByProviderReference = linkedMapOf<String, HotelOffer>()

        response.payload.hotels.forEach { hotel ->
            if (hotel.hotelId in offersByProviderReference) {
                return@forEach
            }

            when (val mapped = mapHotel(hotel)) {
                is HotelResult.Mapped ->
                    offersByProviderReference.putIfAbsent(
                        mapped.offer.providerReference,
                        mapped.offer,
                    )

                is HotelResult.Rejected -> errors += mapped.error
            }
        }

        return if (errors.isEmpty()) {
            Result.Mapped(offers = offersByProviderReference.values.toList())
        } else {
            Result.Rejected(errors = errors)
        }
    }

    private fun mapHotel(hotel: HotelsApiSearchResponseDto.Hotel): HotelResult {
        val providerReference = hotel.hotelId
        if (providerReference.isBlank()) {
            return rejected(
                issue = HotelsApiSearchMappingError.Issue.INVALID_PROVIDER_REFERENCE,
            )
        }
        if (hotel.hotelName.isBlank()) {
            return rejected(
                issue = HotelsApiSearchMappingError.Issue.INVALID_HOTEL_NAME,
                providerReference = providerReference,
            )
        }
        if (hotel.starRating !in MIN_STAR_RATING..MAX_STAR_RATING) {
            return rejected(
                issue = HotelsApiSearchMappingError.Issue.INVALID_STAR_RATING,
                providerReference = providerReference,
            )
        }
        if (
            hotel.areaLocation.destinationName.isBlank() ||
            hotel.areaLocation.countryName.isBlank()
        ) {
            return rejected(
                issue = HotelsApiSearchMappingError.Issue.INVALID_LOCATION,
                providerReference = providerReference,
            )
        }

        val price = hotel.rateForHotelsFeed.shownPrice
        if (!price.amount.isFinite() || price.amount < 0.0) {
            return rejected(
                issue = HotelsApiSearchMappingError.Issue.INVALID_PRICE,
                providerReference = providerReference,
            )
        }
        if (price.currency.isBlank()) {
            return rejected(
                issue = HotelsApiSearchMappingError.Issue.INVALID_CURRENCY,
                providerReference = providerReference,
            )
        }

        val review = hotel.review
        if (review != null && !review.isValid()) {
            return rejected(
                issue = HotelsApiSearchMappingError.Issue.INVALID_REVIEW,
                providerReference = providerReference,
            )
        }

        val availableRoomsCount = hotel.rateForHotelsFeed.availableRoomsCount
        if (availableRoomsCount < 0) {
            return rejected(
                issue = HotelsApiSearchMappingError.Issue.INVALID_AVAILABILITY,
                providerReference = providerReference,
            )
        }
        val freeCancellationUntil = try {
            hotel.rateForHotelsFeed.freeCancellationUntil?.let { value ->
                OffsetDateTime.parse(value).toInstant()
            }
        } catch (_: DateTimeParseException) {
            return rejected(
                issue = HotelsApiSearchMappingError.Issue.INVALID_CANCELLATION,
                providerReference = providerReference,
            )
        }

        return HotelResult.Mapped(
            offer = HotelOffer(
                id = "$OFFER_ID_PREFIX$providerReference",
                providerReference = providerReference,
                hotelName = hotel.hotelName,
                city = hotel.areaLocation.destinationName,
                country = hotel.areaLocation.countryName,
                totalPrice = price.amount,
                currency = price.currency,
                rating = review?.rating,
                reviewCount = review?.ratingsCount,
                amenities = null,
                availability = if (availableRoomsCount > 0) {
                    HotelOffer.Availability.AVAILABLE
                } else {
                    HotelOffer.Availability.UNKNOWN
                },
                source = SOURCE,
                freshness = HotelOffer.Freshness.UNKNOWN,
                starRating = hotel.starRating,
                freeCancellationUntil = freeCancellationUntil,
            ),
        )
    }

    sealed interface Result {
        data class Mapped(val offers: List<HotelOffer>) : Result

        data class Rejected(val errors: List<HotelsApiSearchMappingError>) : Result
    }

    private sealed interface HotelResult {
        data class Mapped(val offer: HotelOffer) : HotelResult

        data class Rejected(val error: HotelsApiSearchMappingError) : HotelResult
    }

    private fun rejected(
        issue: HotelsApiSearchMappingError.Issue,
        providerReference: String? = null,
    ): HotelResult.Rejected =
        HotelResult.Rejected(
            error = HotelsApiSearchMappingError(
                issue = issue,
                providerReference = providerReference,
            ),
        )

    private fun HotelsApiSearchResponseDto.Review.isValid(): Boolean =
        rating.isFinite() && rating in MIN_RATING..MAX_RATING && ratingsCount >= 0

    private const val SOURCE = "tbank_hotels_api"
    private const val OFFER_ID_PREFIX = "tbank-hotels-api:"
    private const val MIN_RATING = 0.0
    private const val MAX_RATING = 10.0
    private const val MIN_STAR_RATING = 0
    private const val MAX_STAR_RATING = 5
}
