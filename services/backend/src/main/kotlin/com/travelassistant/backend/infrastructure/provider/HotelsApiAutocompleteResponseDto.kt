package com.travelassistant.backend.infrastructure.provider

import kotlinx.serialization.Serializable

@Serializable
internal data class HotelsApiAutocompleteResponseDto(
    val payload: Payload,
) {
    @Serializable
    data class Payload(
        val locations: List<Location>? = null,
        val hotels: List<Hotel>? = null,
    )

    @Serializable
    data class Location(
        val id: Int,
        val name: String,
        val signature: String,
        val type: SuggestionType,
    )

    @Serializable
    data class Hotel(
        val id: String,
        val name: String,
        val signature: String,
        val type: SuggestionType,
    )

    @Serializable
    data class SuggestionType(
        val code: String,
        val name: String,
    )
}
