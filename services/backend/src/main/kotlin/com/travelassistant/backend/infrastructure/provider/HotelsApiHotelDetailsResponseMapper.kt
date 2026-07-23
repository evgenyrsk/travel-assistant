package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.domain.hotel.HotelDetails
import java.time.LocalTime
import java.time.format.DateTimeParseException
import java.util.Locale

internal object HotelsApiHotelDetailsResponseMapper {

    fun map(response: HotelsApiHotelDetailsResponseDto): Result {
        val payload = response.payload
        val providerReference = payload.hotelId.trim()
        if (providerReference.isEmpty()) {
            return rejected(HotelsApiHotelDetailsMappingError.Issue.INVALID_PROVIDER_REFERENCE)
        }

        val hotelName = payload.hotelName.trim()
        if (hotelName.isEmpty()) {
            return rejected(HotelsApiHotelDetailsMappingError.Issue.INVALID_HOTEL_NAME)
        }
        if (payload.starRating != null && payload.starRating !in MIN_STAR_RATING..MAX_STAR_RATING) {
            return rejected(HotelsApiHotelDetailsMappingError.Issue.INVALID_STAR_RATING)
        }

        val location = when (val mapped = mapLocation(payload.hotelLocation)) {
            is LocationResult.Mapped -> mapped.value
            is LocationResult.Rejected -> return rejected(mapped.issue)
        }
        val checkInTime = when (val mapped = mapTime(
            payload.checkInTime,
            HotelsApiHotelDetailsMappingError.Issue.INVALID_CHECK_IN_TIME,
        )) {
            is TimeResult.Mapped -> mapped.value
            is TimeResult.Rejected -> return rejected(mapped.issue)
        }
        val checkOutTime = when (val mapped = mapTime(
            payload.checkOutTime,
            HotelsApiHotelDetailsMappingError.Issue.INVALID_CHECK_OUT_TIME,
        )) {
            is TimeResult.Mapped -> mapped.value
            is TimeResult.Rejected -> return rejected(mapped.issue)
        }

        return Result.Mapped(
            providerReference = providerReference,
            details = HotelDetails(
                hotelName = hotelName,
                hotelChain = payload.hotelChain.normalizedOrNull(),
                starRating = payload.starRating,
                location = location,
                descriptionSections = HotelsApiHotelDetailsDescriptionPolicy.filter(
                    payload.description,
                ),
                imageUrls = HotelsApiSafeImageUrlPolicy.collect(
                    payload.images,
                    MAX_IMAGE_COUNT,
                ),
                amenityGroups = payload.facilitiesGroups?.mapNotNull { group ->
                    val name = group.groupName.normalizedOrNull()
                    val amenities = group.facilities
                        .mapNotNull { facility -> facility.name.normalizedOrNull() }
                        .distinct()
                    if (name == null && amenities.isEmpty()) {
                        null
                    } else {
                        HotelDetails.AmenityGroup(
                            name = name,
                            amenities = amenities,
                        )
                    }
                },
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                paymentMethods = payload.paymentMethods?.mapNotNull { paymentMethod ->
                    paymentMethod.paymentMethodOrNull()
                }
                    ?.distinct(),
            ),
        )
    }

    private fun mapLocation(
        value: HotelsApiHotelDetailsResponseDto.HotelLocation?,
    ): LocationResult {
        value ?: return LocationResult.Mapped(null)
        val address = value.address.normalizedOrNull()
        if (value.address != null && address == null) {
            return LocationResult.Rejected(
                HotelsApiHotelDetailsMappingError.Issue.INVALID_LOCATION,
            )
        }

        val coordinates = value.coordinates?.let { coordinates ->
            if (
                !coordinates.latitude.isFinite() ||
                !coordinates.longitude.isFinite() ||
                coordinates.latitude !in MIN_LATITUDE..MAX_LATITUDE ||
                coordinates.longitude !in MIN_LONGITUDE..MAX_LONGITUDE
            ) {
                return LocationResult.Rejected(
                    HotelsApiHotelDetailsMappingError.Issue.INVALID_LOCATION,
                )
            }
            HotelDetails.Coordinates(
                latitude = coordinates.latitude,
                longitude = coordinates.longitude,
            )
        }

        return LocationResult.Mapped(
            if (address == null && coordinates == null) {
                null
            } else {
                HotelDetails.Location(
                    address = address,
                    coordinates = coordinates,
                )
            },
        )
    }

    private fun mapTime(
        value: String?,
        issue: HotelsApiHotelDetailsMappingError.Issue,
    ): TimeResult {
        value ?: return TimeResult.Mapped(null)
        val normalized = value.trim()
        if (normalized.isEmpty()) {
            return TimeResult.Rejected(issue)
        }
        return try {
            TimeResult.Mapped(LocalTime.parse(normalized))
        } catch (_: DateTimeParseException) {
            TimeResult.Rejected(issue)
        }
    }

    private fun String?.normalizedOrNull(): String? =
        this?.trim()?.takeIf(String::isNotEmpty)

    private fun String.paymentMethodOrNull(): HotelDetails.PaymentMethod? =
        when (trim().lowercase(Locale.ROOT)) {
            "cash" -> HotelDetails.PaymentMethod.CASH
            "mir", "master_card", "visa" -> HotelDetails.PaymentMethod.CARD
            else -> null
        }

    private fun rejected(issue: HotelsApiHotelDetailsMappingError.Issue): Result.Rejected =
        Result.Rejected(HotelsApiHotelDetailsMappingError(issue))

    sealed interface Result {
        data class Mapped(
            val providerReference: String,
            val details: HotelDetails,
        ) : Result

        data class Rejected(val error: HotelsApiHotelDetailsMappingError) : Result
    }

    private sealed interface LocationResult {
        data class Mapped(val value: HotelDetails.Location?) : LocationResult

        data class Rejected(
            val issue: HotelsApiHotelDetailsMappingError.Issue,
        ) : LocationResult
    }

    private sealed interface TimeResult {
        data class Mapped(val value: LocalTime?) : TimeResult

        data class Rejected(
            val issue: HotelsApiHotelDetailsMappingError.Issue,
        ) : TimeResult
    }

    private const val MIN_STAR_RATING = 0
    private const val MAX_STAR_RATING = 5
    private const val MIN_LATITUDE = -90.0
    private const val MAX_LATITUDE = 90.0
    private const val MIN_LONGITUDE = -180.0
    private const val MAX_LONGITUDE = 180.0
    private const val MAX_IMAGE_COUNT = 10
}
