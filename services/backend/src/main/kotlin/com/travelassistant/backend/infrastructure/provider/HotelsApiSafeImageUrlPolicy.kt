package com.travelassistant.backend.infrastructure.provider

import java.net.URI

internal object HotelsApiSafeImageUrlPolicy {

    fun firstOrNull(values: List<String>?): String? =
        values?.firstNotNullOfOrNull(::normalizeOrNull)

    fun collect(values: List<String>?, limit: Int): List<String>? =
        values
            ?.asSequence()
            ?.mapNotNull(::normalizeOrNull)
            ?.distinct()
            ?.take(limit)
            ?.toList()
            ?.takeIf(List<String>::isNotEmpty)

    private fun normalizeOrNull(value: String): String? {
        val normalized = value.trim()
        val uri = runCatching { URI(normalized) }.getOrNull() ?: return null
        return normalized.takeIf {
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.fragment == null
        }
    }
}
