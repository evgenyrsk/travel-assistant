package com.travelassistant.backend.application.assistant

import java.text.Normalizer
import java.util.Locale

class ExplicitHotelStarPreferenceParser {

    fun parse(message: String): Set<Int>? {
        val normalized = message.normalizedForMatching()
        if (normalized.isBlank() || CLEAR_MARKER.containsMatchIn(normalized)) {
            return null
        }

        val matches = linkedSetOf<Int>()
        STAR_EXPRESSIONS.forEach { expression ->
            expression.pattern.findAll(normalized).forEach { match ->
                if (match.isAffirmative(normalized)) {
                    matches += expression.value
                }
            }
        }

        return matches.singleOrNull()?.let(::setOf)
    }

    private fun String.normalizedForMatching(): String =
        Normalizer.normalize(this, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace('ё', 'е')
            .replace('–', '-')
            .replace('—', '-')
            .replace(WHITESPACE, " ")
            .trim()

    private fun MatchResult.isAffirmative(message: String): Boolean {
        val prefix = message.substring(
            startIndex = (range.first - PREFIX_WINDOW).coerceAtLeast(0),
            endIndex = range.first,
        )
        return !NON_EXACT_PREFIX.containsMatchIn(prefix)
    }

    private data class StarExpression(
        val value: Int,
        val pattern: Regex,
    )

    private companion object {
        const val PREFIX_WINDOW = 48

        val WHITESPACE = Regex("""\s+""")
        val CLEAR_MARKER = Regex(
            """\b(?:убери|убрать|сними|снять|отмени|отменить|исключи|исключить)\b""",
        )
        val NON_EXACT_PREFIX = Regex(
            """(?:\b(?:до|от|минимум|максимум|не\s+(?:менее|более|ниже|выше))\s*|""" +
                """\b(?:не|без)\s+|\bне\s+(?:хочу|нужен|нужна|нужно|должен|должна|""" +
                """должно|должны)(?:\s+быть)?\s*)$""",
        )

        val STAR_EXPRESSIONS = listOf(
            starExpression(1, "однозвездочн"),
            starExpression(2, "двухзвездочн"),
            starExpression(3, "трехзвездочн"),
            starExpression(4, "четырехзвездочн"),
            starExpression(5, "пятизвездочн"),
            numericStarExpression(1),
            numericStarExpression(2),
            numericStarExpression(3),
            numericStarExpression(4),
            numericStarExpression(5),
            wordStarExpression(1, "одна|одну|одной|один"),
            wordStarExpression(2, "две|двух"),
            wordStarExpression(3, "три|трех"),
            wordStarExpression(4, "четыре|четырех"),
            wordStarExpression(5, "пять|пяти"),
        )

        fun starExpression(value: Int, stem: String): StarExpression =
            StarExpression(
                value = value,
                pattern = Regex(
                    """(?<![\p{L}\p{N}])$stem[\p{L}]*(?![\p{L}\p{N}])""",
                ),
            )

        fun numericStarExpression(value: Int): StarExpression =
            StarExpression(
                value = value,
                pattern = Regex(
                    """(?<![\p{L}\p{N}-])$value\s*-?\s*звезд[\p{L}]*(?![\p{L}\p{N}-])""",
                ),
            )

        fun wordStarExpression(value: Int, numberWords: String): StarExpression =
            StarExpression(
                value = value,
                pattern = Regex(
                    """(?<![\p{L}\p{N}-])(?:$numberWords)\s+звезд[\p{L}]*(?![\p{L}\p{N}-])""",
                ),
            )
    }
}
