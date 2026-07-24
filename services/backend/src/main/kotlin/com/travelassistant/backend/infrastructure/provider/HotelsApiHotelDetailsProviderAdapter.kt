package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelDetailsProviderBoundary
import com.travelassistant.backend.application.hotel.HotelDetailsProviderResult
import com.travelassistant.backend.application.hotel.HotelLocationResolutionRequest
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString

internal class HotelsApiHotelDetailsProviderAdapter(
    private val transport: PublicHotelsApiHttpTransport,
    private val language: HotelLocationResolutionRequest.Language? = null,
) : HotelDetailsProviderBoundary {

    override suspend fun load(providerReference: String): HotelDetailsProviderResult {
        if (providerReference.isBlank()) {
            return rejected(
                HotelDetailsProviderResult.ResponseRejectionReason.INVALID_PROVIDER_REFERENCE,
            )
        }

        return try {
            val response = transport.getJson(
                path = "$DETAILS_PATH/${providerReference.encodeURLPathPart()}",
                userLanguage = language?.name,
            )
            mapResponse(providerReference, decodeResponse(response.body))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HotelProviderException) {
            mapProviderException(exception)
        }
    }

    private fun decodeResponse(body: String): HotelsApiHotelDetailsResponseDto =
        try {
            HotelsApiJson.codec.decodeFromString(body)
        } catch (_: SerializationException) {
            throw HotelProviderException(
                category = HotelProviderErrorCategory.INVALID_RESPONSE,
                message = "Hotels API hotel details response is invalid",
            )
        }

    private fun mapResponse(
        requestedProviderReference: String,
        response: HotelsApiHotelDetailsResponseDto,
    ): HotelDetailsProviderResult =
        when (val mapped = HotelsApiHotelDetailsResponseMapper.map(response)) {
            is HotelsApiHotelDetailsResponseMapper.Result.Mapped ->
                if (mapped.providerReference == requestedProviderReference) {
                    HotelDetailsProviderResult.Loaded(mapped.details)
                } else {
                    rejected(
                        HotelDetailsProviderResult.ResponseRejectionReason
                            .INVALID_PROVIDER_REFERENCE,
                    )
                }

            is HotelsApiHotelDetailsResponseMapper.Result.Rejected ->
                rejected(mapped.error.issue.toResponseRejectionReason())
        }

    private fun mapProviderException(
        exception: HotelProviderException,
    ): HotelDetailsProviderResult =
        when (exception.category) {
            HotelProviderErrorCategory.NOT_FOUND -> HotelDetailsProviderResult.NotFound
            HotelProviderErrorCategory.INVALID_RESPONSE -> rejected(
                HotelDetailsProviderResult.ResponseRejectionReason.INVALID_PAYLOAD,
            )
            HotelProviderErrorCategory.MAPPING_FAILED -> rejected(
                HotelDetailsProviderResult.ResponseRejectionReason.UNKNOWN,
            )
            HotelProviderErrorCategory.TIMEOUT -> unavailable(
                HotelDetailsProviderResult.UnavailableReason.TIMEOUT,
            )
            HotelProviderErrorCategory.RATE_LIMITED -> unavailable(
                HotelDetailsProviderResult.UnavailableReason.RATE_LIMITED,
            )
            HotelProviderErrorCategory.AUTHENTICATION_FAILED -> unavailable(
                HotelDetailsProviderResult.UnavailableReason.AUTHENTICATION_FAILED,
            )
            HotelProviderErrorCategory.UNAVAILABLE -> unavailable(
                HotelDetailsProviderResult.UnavailableReason.UNAVAILABLE,
            )
            HotelProviderErrorCategory.UNKNOWN -> unavailable(
                HotelDetailsProviderResult.UnavailableReason.UNKNOWN,
            )
        }

    private fun HotelsApiHotelDetailsMappingError.Issue.toResponseRejectionReason():
        HotelDetailsProviderResult.ResponseRejectionReason =
        when (this) {
            HotelsApiHotelDetailsMappingError.Issue.INVALID_PROVIDER_REFERENCE ->
                HotelDetailsProviderResult.ResponseRejectionReason.INVALID_PROVIDER_REFERENCE
            HotelsApiHotelDetailsMappingError.Issue.INVALID_LOCATION ->
                HotelDetailsProviderResult.ResponseRejectionReason.INVALID_LOCATION_DATA
            HotelsApiHotelDetailsMappingError.Issue.INVALID_HOTEL_NAME,
            HotelsApiHotelDetailsMappingError.Issue.INVALID_STAR_RATING,
            HotelsApiHotelDetailsMappingError.Issue.INVALID_CHECK_IN_TIME,
            HotelsApiHotelDetailsMappingError.Issue.INVALID_CHECK_OUT_TIME,
            -> HotelDetailsProviderResult.ResponseRejectionReason.INVALID_HOTEL_DATA
        }

    private fun rejected(
        reason: HotelDetailsProviderResult.ResponseRejectionReason,
    ): HotelDetailsProviderResult.ResponseRejected =
        HotelDetailsProviderResult.ResponseRejected(reason)

    private fun unavailable(
        reason: HotelDetailsProviderResult.UnavailableReason,
    ): HotelDetailsProviderResult.ProviderUnavailable =
        HotelDetailsProviderResult.ProviderUnavailable(reason)

    private companion object {
        const val DETAILS_PATH = "/api/v1/hotels"
    }
}
