package com.travelassistant.backend.application.assistant

import java.text.Normalizer
import java.util.Locale

class ExplicitStayLengthParser {

    fun parse(message: String): Int? {
        val normalized = message.normalizedForMatching()
        if (normalized.isBlank()) return null

        val durations = linkedSetOf<Int>()
        NIGHT_COUNT.findAll(normalized).forEach { match ->
            match.groupValues[1].toIntOrNull()?.let(durations::add)
        }
        WEEK_COUNT.findAll(normalized).forEach { match ->
            match.groupValues[1].toIntOrNull()?.let { weeks -> durations += weeks * DAYS_PER_WEEK }
        }
        if (SINGLE_WEEK.containsMatchIn(normalized)) {
            durations += DAYS_PER_WEEK
        }

        return durations.singleOrNull()
            ?.takeIf { nights -> nights in MIN_NIGHTS..MAX_NIGHTS }
    }

    private fun String.normalizedForMatching(): String =
        Normalizer.normalize(this, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace('ё', 'е')
            .replace(WHITESPACE, " ")
            .trim()

    private companion object {
        const val DAYS_PER_WEEK = 7
        const val MIN_NIGHTS = 1
        const val MAX_NIGHTS = 365

        val WHITESPACE = Regex("""\s+""")
        val NIGHT_COUNT = Regex(
            """(?<![\p{L}\p{N}])(\d{1,3})\s*(?:ноч(?:ь|и|ей)|сут(?:ки|ок)?)(?![\p{L}\p{N}])""",
        )
        val WEEK_COUNT = Regex(
            """(?<![\p{L}\p{N}])(\d{1,2})\s*недел(?:ю|и|ь)(?![\p{L}\p{N}])""",
        )
        val SINGLE_WEEK = Regex(
            """(?<![\p{L}\p{N}])(?:(?:на\s+)?одну\s+недел(?:ю|и)|на\s+недел(?:ю|и))(?![\p{L}\p{N}])""",
        )
    }
}
