package com.travelassistant.backend.infrastructure.provider

internal data class HotelsApiHttpResponse(
    val statusCode: Int,
    val contentType: String?,
    val body: String,
)
