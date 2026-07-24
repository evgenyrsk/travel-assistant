package com.travelassistant.backend.infrastructure.accommodation

import java.net.URI
import java.util.Locale

internal class AccommodationAnalysisImageUrlPolicy(
    imageHosts: Set<String>,
) {
    private val allowedHosts = imageHosts.map { host -> host.lowercase(Locale.ROOT) }.toSet()

    fun allowedOrNull(value: String): String? {
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        val valid = uri.isAbsolute &&
            uri.scheme.equals("https", ignoreCase = true) &&
            uri.host?.lowercase(Locale.ROOT) in allowedHosts &&
            uri.userInfo == null &&
            uri.query == null &&
            uri.fragment == null &&
            uri.port == -1
        return value.takeIf { valid }
    }
}
