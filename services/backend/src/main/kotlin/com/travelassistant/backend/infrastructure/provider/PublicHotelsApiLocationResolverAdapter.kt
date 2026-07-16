package com.travelassistant.backend.infrastructure.provider

import com.travelassistant.backend.application.hotel.HotelLocationResolution
import com.travelassistant.backend.application.hotel.HotelLocationResolutionRequest
import com.travelassistant.backend.application.hotel.HotelLocationResolverBoundary
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal class PublicHotelsApiLocationResolverAdapter(
    private val transport: PublicHotelsApiHttpTransport,
) : HotelLocationResolverBoundary {

    override suspend fun resolve(
        request: HotelLocationResolutionRequest,
    ): HotelLocationResolution {
        val response = transport.postJson(
            path = AUTOCOMPLETE_PATH,
            body = HotelsApiJson.codec.encodeToString(
                PublicHotelsApiAutocompleteRequestDto(input = request.query),
            ),
            userLanguage = request.language?.name,
        )

        return HotelsApiAutocompleteLocationMapper.map(
            decodeResponse(response.body),
        )
    }

    private fun decodeResponse(body: String): HotelsApiAutocompleteResponseDto =
        try {
            HotelsApiJson.codec.decodeFromString(body)
        } catch (_: SerializationException) {
            throw HotelProviderException(
                category = HotelProviderErrorCategory.INVALID_RESPONSE,
                message = "Hotels API autocomplete response is invalid",
            )
        }

    private companion object {
        const val AUTOCOMPLETE_PATH = "/search-api/search/autocomplete"
    }
}
