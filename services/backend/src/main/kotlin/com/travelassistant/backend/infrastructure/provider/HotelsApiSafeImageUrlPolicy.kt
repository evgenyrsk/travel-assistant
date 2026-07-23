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
        val source = value.trim()
        val containsSizePlaceholder = IMAGE_SIZE_PLACEHOLDER in source
        if (
            containsSizePlaceholder &&
            source.windowed(IMAGE_SIZE_PLACEHOLDER.length)
                .count { part -> part == IMAGE_SIZE_PLACEHOLDER } != 1
        ) {
            return null
        }

        val normalized = source.replace(IMAGE_SIZE_PLACEHOLDER, CARD_IMAGE_SIZE)
        if ('{' in normalized || '}' in normalized) {
            return null
        }

        val uri = runCatching { URI(normalized) }.getOrNull() ?: return null
        return normalized.takeIf {
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.fragment == null &&
                (!containsSizePlaceholder || uri.host.equals(TEMPLATE_HOST, ignoreCase = true))
        }
    }

    private const val IMAGE_SIZE_PLACEHOLDER = "{size}"
    private const val CARD_IMAGE_SIZE = "1024x768"
    private const val TEMPLATE_HOST = "extranet-cdn.tinkoff.ru"
}
