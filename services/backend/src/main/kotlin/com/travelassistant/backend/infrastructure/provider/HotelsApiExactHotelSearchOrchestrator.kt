package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelLocationResolution
import com.travelassistant.backend.application.hotel.HotelLocationResolutionRequest
import com.travelassistant.backend.domain.hotel.HotelOfferCandidate
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal class HotelsApiExactHotelSearchOrchestrator(
    private val transport: PublicHotelsApiHttpTransport,
) {
    suspend fun search(
        candidate: HotelLocationResolution.HotelCandidate,
        criteria: HotelSearchCriteria,
        language: HotelLocationResolutionRequest.Language?,
    ): Result {
        val request = when (val mapping = HotelsApiHotelRatesRequestMapper.map(criteria)) {
            is HotelsApiHotelRatesRequestMapper.Result.Mapped -> mapping.request
            is HotelsApiHotelRatesRequestMapper.Result.Rejected ->
                return Result.RequestRejected(mapping.error)
        }
        if (candidate.providerReference.isBlank()) {
            return Result.ResponseRejected(
                HotelsApiSearchMappingError(
                    HotelsApiSearchMappingError.Issue.INVALID_PROVIDER_REFERENCE,
                ),
            )
        }

        val encodedReference = candidate.providerReference.encodeURLPathPart()
        val detailsResponse = transport.getJson(
            path = "$DETAILS_PATH/$encodedReference",
            userLanguage = language?.name,
        )
        val ratesResponse = transport.postJson(
            path = "$RATES_PATH/$encodedReference/rates",
            body = HotelsApiJson.codec.encodeToString(request),
            userLanguage = language?.name,
        )

        val details = decodeDetails(detailsResponse.body)
        val rates = decodeRates(ratesResponse.body)
        return when (
            val mapping = HotelsApiExactHotelResponseMapper.map(
                candidate = candidate,
                criteria = criteria,
                details = details,
                rates = rates,
            )
        ) {
            is HotelsApiExactHotelResponseMapper.Result.Mapped ->
                Result.Success(mapping.offers)
            is HotelsApiExactHotelResponseMapper.Result.Rejected ->
                Result.ResponseRejected(mapping.error)
        }
    }

    private fun decodeDetails(body: String): HotelsApiHotelDetailsResponseDto =
        try {
            HotelsApiJson.codec.decodeFromString(body)
        } catch (_: SerializationException) {
            throw invalidResponse("Hotels API hotel details response is invalid")
        }

    private fun decodeRates(body: String): HotelsApiHotelRatesResponseDto =
        try {
            HotelsApiJson.codec.decodeFromString(body)
        } catch (_: SerializationException) {
            throw invalidResponse("Hotels API hotel rates response is invalid")
        }

    private fun invalidResponse(message: String): HotelProviderException =
        HotelProviderException(
            category = HotelProviderErrorCategory.INVALID_RESPONSE,
            message = message,
        )

    sealed interface Result {
        data class Success(val offers: List<HotelOfferCandidate>) : Result

        data class RequestRejected(val error: HotelsApiSearchMappingError) : Result

        data class ResponseRejected(val error: HotelsApiSearchMappingError) : Result
    }

    private companion object {
        const val DETAILS_PATH = "/api/v1/hotels"
        const val RATES_PATH = "/api/v3/hotels"
    }
}
