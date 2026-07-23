package com.travelassistant.backend.application.hotel

import java.text.Normalizer
import java.util.Locale

internal object HotelSearchCandidateTextNormalizer {
    fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .trim()
            .replace(REPEATED_WHITESPACE, " ")
            .lowercase(Locale.ROOT)
            .replace('ё', 'е')

    private val REPEATED_WHITESPACE = Regex("\\s+")
}
