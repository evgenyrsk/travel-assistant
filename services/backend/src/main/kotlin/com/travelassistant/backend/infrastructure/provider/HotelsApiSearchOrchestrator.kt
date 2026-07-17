package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelLocationCandidateSelectionPolicy
import com.travelassistant.backend.application.hotel.HotelLocationCandidateSelectionResult
import com.travelassistant.backend.application.hotel.HotelLocationResolution
import com.travelassistant.backend.application.hotel.HotelLocationResolutionRequest
import com.travelassistant.backend.application.hotel.HotelLocationResolverBoundary
import com.travelassistant.backend.domain.hotel.HotelOffer
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal class HotelsApiSearchOrchestrator(
    private val locationResolver: HotelLocationResolverBoundary,
    private val locationSelectionPolicy: HotelLocationCandidateSelectionPolicy,
    private val transport: PublicHotelsApiHttpTransport,
) {

    suspend fun search(request: Request): Result {
        val resolution = locationResolver.resolve(
            HotelLocationResolutionRequest(
                query = request.criteria.destination,
                language = request.language,
            ),
        )

        val location = when (
            val selection = locationSelectionPolicy.select(
                query = request.criteria.destination,
                candidates = resolution.candidates,
            )
        ) {
            is HotelLocationCandidateSelectionResult.Selected -> selection.candidate
            HotelLocationCandidateSelectionResult.NotFound -> return Result.LocationNotFound
            is HotelLocationCandidateSelectionResult.SelectionRequired ->
                return Result.LocationSelectionRequired(candidates = selection.candidates)
        }

        val mappedRequest = when (
            val mapping = HotelsApiSearchRequestMapper.map(
                location = location,
                criteria = request.criteria,
            )
        ) {
            is HotelsApiSearchRequestMapper.Result.Mapped -> mapping.request.copy(
                offset = FIRST_PAGE_OFFSET,
                limit = MAX_CANDIDATES,
            )
            is HotelsApiSearchRequestMapper.Result.Rejected ->
                return Result.RequestRejected(error = mapping.error)
        }

        val httpResponse = transport.postJson(
            path = SEARCH_PATH,
            body = HotelsApiJson.codec.encodeToString(mappedRequest),
            userLanguage = request.language?.name,
        )
        val providerResponse = decodeResponse(httpResponse.body)

        return when (val mapping = HotelsApiSearchResponseMapper.map(providerResponse)) {
            is HotelsApiSearchResponseMapper.Result.Mapped ->
                Result.Success(
                    location = location,
                    offers = mapping.offers.take(MAX_CANDIDATES),
                )

            is HotelsApiSearchResponseMapper.Result.Rejected ->
                Result.ResponseRejected(errors = mapping.errors)
        }
    }

    private fun decodeResponse(body: String): HotelsApiSearchResponseDto =
        try {
            HotelsApiJson.codec.decodeFromString(body)
        } catch (_: SerializationException) {
            throw HotelProviderException(
                category = HotelProviderErrorCategory.INVALID_RESPONSE,
                message = "Hotels API response is invalid",
            )
        }

    data class Request(
        val criteria: HotelSearchCriteria,
        val language: HotelLocationResolutionRequest.Language? = null,
    )

    sealed interface Result {
        data class Success(
            val location: HotelLocationResolution.Candidate,
            val offers: List<HotelOffer>,
        ) : Result

        data object LocationNotFound : Result

        data class LocationSelectionRequired(
            val candidates: List<HotelLocationResolution.Candidate>,
        ) : Result

        data class RequestRejected(
            val error: HotelsApiSearchMappingError,
        ) : Result

        data class ResponseRejected(
            val errors: List<HotelsApiSearchMappingError>,
        ) : Result
    }

    private companion object {
        const val SEARCH_PATH = "/api/v1/hotels/search"
        const val FIRST_PAGE_OFFSET = 0
        const val MAX_CANDIDATES = 20
    }
}
