package com.travelassistant.backend.api

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.contentLength
import io.ktor.server.request.receiveChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.core.readBytes

internal const val API_JSON_REQUEST_MAX_BYTES = 32 * 1024

internal sealed interface BoundedRequestBodyReadResult {
    data object NoBody : BoundedRequestBodyReadResult

    data class Accepted(val text: String) : BoundedRequestBodyReadResult

    data object TooLarge : BoundedRequestBodyReadResult
}

internal suspend fun ApplicationCall.readBoundedRequestBody(
    maxBytes: Int = API_JSON_REQUEST_MAX_BYTES,
): BoundedRequestBodyReadResult {
    if ((request.contentLength() ?: 0L) > maxBytes) {
        return BoundedRequestBodyReadResult.TooLarge
    }

    val channel = receiveChannel()
    val packet = channel.readRemaining(maxBytes.toLong() + 1L)
    val bytes = packet.use { body -> body.readBytes() }
    if (bytes.size > maxBytes) {
        channel.cancel()
        return BoundedRequestBodyReadResult.TooLarge
    }
    if (bytes.isEmpty()) {
        return BoundedRequestBodyReadResult.NoBody
    }

    return BoundedRequestBodyReadResult.Accepted(bytes.toString(Charsets.UTF_8))
}
