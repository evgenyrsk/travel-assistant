package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelOfferProviderBoundary
import com.travelassistant.backend.application.hotel.HotelOfferProviderResult
import com.travelassistant.backend.application.hotel.HotelLocationResolutionRequest
import com.travelassistant.backend.application.hotel.HotelLocationSuggestion
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import kotlinx.coroutines.CancellationException

internal class RealHotelOfferProviderAdapter(
    private val search: suspend (HotelsApiSearchOrchestrator.Request) -> HotelsApiSearchOrchestrator.Result,
    private val language: HotelLocationResolutionRequest.Language? = null,
) : HotelOfferProviderBoundary {

    override suspend fun search(criteria: HotelSearchCriteria): HotelOfferProviderResult =
        try {
            mapResult(
                search(
                    HotelsApiSearchOrchestrator.Request(
                        criteria = criteria,
                        language = language,
                    ),
                ),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HotelProviderException) {
            mapProviderException(exception)
        }

    private fun mapResult(result: HotelsApiSearchOrchestrator.Result): HotelOfferProviderResult =
        when (result) {
            is HotelsApiSearchOrchestrator.Result.Success ->
                HotelOfferProviderResult.SearchCompleted(result.offers)

            HotelsApiSearchOrchestrator.Result.LocationNotFound ->
                HotelOfferProviderResult.LocationNotFound

            is HotelsApiSearchOrchestrator.Result.LocationSelectionRequired ->
                HotelOfferProviderResult.LocationSelectionRequired(
                    suggestions = result.candidates.map { candidate ->
                        HotelLocationSuggestion(
                            name = candidate.name,
                            signature = candidate.signature,
                            typeCode = candidate.type.code,
                            typeName = candidate.type.name,
                        )
                    },
                )

            is HotelsApiSearchOrchestrator.Result.HotelSelectionRequired ->
                HotelOfferProviderResult.LocationSelectionRequired(
                    suggestions = result.candidates.map { candidate ->
                        HotelLocationSuggestion(
                            name = candidate.name,
                            signature = candidate.signature,
                            typeCode = candidate.type.code,
                            typeName = candidate.type.name,
                        )
                    },
                )

            is HotelsApiSearchOrchestrator.Result.RequestRejected ->
                HotelOfferProviderResult.RequestRejected(
                    reason = result.error.issue.toRequestRejectionReason(),
                )

            is HotelsApiSearchOrchestrator.Result.ResponseRejected ->
                HotelOfferProviderResult.ResponseRejected(
                    reason = result.errors.firstOrNull()?.issue.toResponseRejectionReason(),
                )
        }

    private fun mapProviderException(
        exception: HotelProviderException,
    ): HotelOfferProviderResult =
        when (exception.category) {
            HotelProviderErrorCategory.NOT_FOUND ->
                HotelOfferProviderResult.ResponseRejected(
                    HotelOfferProviderResult.ResponseRejectionReason.INVALID_PAYLOAD,
                )

            HotelProviderErrorCategory.INVALID_RESPONSE ->
                HotelOfferProviderResult.ResponseRejected(
                    HotelOfferProviderResult.ResponseRejectionReason.INVALID_PAYLOAD,
                )

            HotelProviderErrorCategory.MAPPING_FAILED ->
                HotelOfferProviderResult.ResponseRejected(
                    HotelOfferProviderResult.ResponseRejectionReason.UNKNOWN,
                )

            HotelProviderErrorCategory.TIMEOUT -> unavailable(
                HotelOfferProviderResult.UnavailableReason.TIMEOUT,
            )

            HotelProviderErrorCategory.RATE_LIMITED -> unavailable(
                HotelOfferProviderResult.UnavailableReason.RATE_LIMITED,
            )

            HotelProviderErrorCategory.AUTHENTICATION_FAILED -> unavailable(
                HotelOfferProviderResult.UnavailableReason.AUTHENTICATION_FAILED,
            )

            HotelProviderErrorCategory.UNAVAILABLE -> unavailable(
                HotelOfferProviderResult.UnavailableReason.UNAVAILABLE,
            )

            HotelProviderErrorCategory.UNKNOWN -> unavailable(
                HotelOfferProviderResult.UnavailableReason.UNKNOWN,
            )
        }

    private fun unavailable(
        reason: HotelOfferProviderResult.UnavailableReason,
    ): HotelOfferProviderResult.ProviderUnavailable =
        HotelOfferProviderResult.ProviderUnavailable(reason)

    private fun HotelsApiSearchMappingError.Issue.toRequestRejectionReason():
        HotelOfferProviderResult.RequestRejectionReason =
        when (this) {
            HotelsApiSearchMappingError.Issue.INVALID_DESTINATION_ID ->
                HotelOfferProviderResult.RequestRejectionReason.INVALID_DESTINATION

            HotelsApiSearchMappingError.Issue.INVALID_DATE_RANGE ->
                HotelOfferProviderResult.RequestRejectionReason.INVALID_DATE_RANGE

            HotelsApiSearchMappingError.Issue.INVALID_ROOM_COUNT,
            HotelsApiSearchMappingError.Issue.INVALID_ADULTS_COUNT,
            HotelsApiSearchMappingError.Issue.INVALID_CHILD_AGE,
            -> HotelOfferProviderResult.RequestRejectionReason.INVALID_OCCUPANCY

            HotelsApiSearchMappingError.Issue.INVALID_MAX_TOTAL_PRICE,
            HotelsApiSearchMappingError.Issue.UNSUPPORTED_MAX_TOTAL_PRICE_CURRENCY,
            HotelsApiSearchMappingError.Issue.INVALID_STARS,
            -> HotelOfferProviderResult.RequestRejectionReason.INVALID_PREFERENCES

            else -> HotelOfferProviderResult.RequestRejectionReason.UNKNOWN
        }

    private fun HotelsApiSearchMappingError.Issue?.toResponseRejectionReason():
        HotelOfferProviderResult.ResponseRejectionReason =
        when (this) {
            HotelsApiSearchMappingError.Issue.INVALID_PROVIDER_REFERENCE ->
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_PROVIDER_REFERENCE

            HotelsApiSearchMappingError.Issue.INVALID_HOTEL_NAME ->
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_HOTEL_DATA

            HotelsApiSearchMappingError.Issue.INVALID_STAR_RATING,
            HotelsApiSearchMappingError.Issue.INVALID_CANCELLATION,
            -> HotelOfferProviderResult.ResponseRejectionReason.INVALID_HOTEL_DATA

            HotelsApiSearchMappingError.Issue.INVALID_LOCATION ->
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_LOCATION_DATA

            HotelsApiSearchMappingError.Issue.INVALID_PRICE ->
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_PRICE

            HotelsApiSearchMappingError.Issue.INVALID_CURRENCY ->
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_CURRENCY

            HotelsApiSearchMappingError.Issue.INVALID_REVIEW ->
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_REVIEW

            HotelsApiSearchMappingError.Issue.INVALID_AVAILABILITY ->
                HotelOfferProviderResult.ResponseRejectionReason.INVALID_AVAILABILITY

            else -> HotelOfferProviderResult.ResponseRejectionReason.UNKNOWN
        }
}
