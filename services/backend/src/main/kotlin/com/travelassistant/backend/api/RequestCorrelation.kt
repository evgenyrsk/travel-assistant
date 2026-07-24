package com.travelassistant.backend.api

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callId
import io.ktor.server.response.header
import io.ktor.util.AttributeKey
import java.util.UUID

const val REQUEST_ID_HEADER = "X-Request-ID"

private val fallbackRequestIdKey = AttributeKey<String>("travel-assistant-request-id")
private val safeRequestIdPattern = Regex("[A-Za-z0-9._-]{1,128}")

fun Application.configureRequestCorrelation() {
    install(CallId) {
        header(REQUEST_ID_HEADER)
        verify(::isSafeRequestId)
        generate { UUID.randomUUID().toString() }
    }
}

fun ApplicationCall.requestId(): String {
    val resolved = callId
        ?: attributes.getOrNull(fallbackRequestIdKey)
        ?: request.headers[REQUEST_ID_HEADER]
            ?.takeIf(::isSafeRequestId)
        ?: UUID.randomUUID().toString()

    if (attributes.getOrNull(fallbackRequestIdKey) == null) {
        attributes.put(fallbackRequestIdKey, resolved)
    }
    response.header(REQUEST_ID_HEADER, resolved)
    return resolved
}

internal fun isSafeRequestId(value: String): Boolean = safeRequestIdPattern.matches(value)
