package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelCandidateSelectionPolicy
import com.travelassistant.backend.application.hotel.HotelCandidateSelectionResult
import com.travelassistant.backend.application.hotel.HotelLocationCandidateSelectionPolicy
import com.travelassistant.backend.application.hotel.HotelLocationCandidateSelectionResult
import com.travelassistant.backend.application.hotel.HotelLocationResolution
import com.travelassistant.backend.application.hotel.HotelLocationResolutionRequest
import com.travelassistant.backend.application.hotel.HotelLocationResolverBoundary
import com.travelassistant.backend.domain.hotel.HotelOfferCandidate
import com.travelassistant.backend.domain.hotel.HotelSearchCriteria
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal class HotelsApiSearchOrchestrator(
    private val locationResolver: HotelLocationResolverBoundary,
    private val locationSelectionPolicy: HotelLocationCandidateSelectionPolicy,
    private val hotelSelectionPolicy: HotelCandidateSelectionPolicy,
    private val exactHotelSearchOrchestrator: HotelsApiExactHotelSearchOrchestrator,
    private val transport: PublicHotelsApiHttpTransport,
) {

    suspend fun search(request: Request): Result {
        val resolution = locationResolver.resolve(
            HotelLocationResolutionRequest(
                query = request.criteria.destination,
                language = request.language,
            ),
        )

        when (
            val selection = hotelSelectionPolicy.select(
                query = request.criteria.destination,
                candidates = resolution.hotelCandidates,
                hasLocationCandidates = resolution.candidates.isNotEmpty(),
            )
        ) {
            is HotelCandidateSelectionResult.Selected ->
                return exactHotelSearchOrchestrator.search(
                    candidate = selection.candidate,
                    criteria = request.criteria,
                    language = request.language,
                ).toSearchResult(selection.candidate)

            is HotelCandidateSelectionResult.SelectionRequired ->
                return Result.HotelSelectionRequired(selection.candidates)

            HotelCandidateSelectionResult.NotSelected -> Unit
        }

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

    private fun HotelsApiExactHotelSearchOrchestrator.Result.toSearchResult(
        hotel: HotelLocationResolution.HotelCandidate,
    ): Result =
        when (this) {
            is HotelsApiExactHotelSearchOrchestrator.Result.Success ->
                Result.Success(
                    hotel = hotel,
                    offers = offers,
                )
            is HotelsApiExactHotelSearchOrchestrator.Result.RequestRejected ->
                Result.RequestRejected(error)
            is HotelsApiExactHotelSearchOrchestrator.Result.ResponseRejected ->
                Result.ResponseRejected(listOf(error))
        }

    data class Request(
        val criteria: HotelSearchCriteria,
        val language: HotelLocationResolutionRequest.Language? = null,
    )

    sealed interface Result {
        data class Success(
            val location: HotelLocationResolution.Candidate? = null,
            val hotel: HotelLocationResolution.HotelCandidate? = null,
            val offers: List<HotelOfferCandidate>,
        ) : Result

        data object LocationNotFound : Result

        data class LocationSelectionRequired(
            val candidates: List<HotelLocationResolution.Candidate>,
        ) : Result

        data class HotelSelectionRequired(
            val candidates: List<HotelLocationResolution.HotelCandidate>,
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
