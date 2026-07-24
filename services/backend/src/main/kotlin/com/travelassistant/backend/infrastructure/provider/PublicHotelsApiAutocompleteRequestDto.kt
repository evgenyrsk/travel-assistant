package com.travelassistant.backend.infrastructure.provider

import kotlinx.serialization.Serializable

@Serializable
internal data class PublicHotelsApiAutocompleteRequestDto(
    val input: String,
)
