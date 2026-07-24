package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.AccommodationConcept
import java.text.Normalizer
import java.util.Locale

class ExplicitAccommodationConceptPreferenceParser {

    fun parse(message: String): Change? {
        val normalized = message.normalizedForMatching()
        if (!GLAMPING.containsMatchIn(normalized)) {
            return null
        }

        return if (CLEAR_EXPRESSION.containsMatchIn(normalized)) {
            Change.Clear
        } else {
            Change.Set(AccommodationConcept.GLAMPING)
        }
    }

    sealed interface Change {
        data class Set(val concept: AccommodationConcept) : Change

        data object Clear : Change
    }

    private fun String.normalizedForMatching(): String =
        Normalizer.normalize(this, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace('ё', 'е')
            .replace(WHITESPACE, " ")
            .trim()

    private companion object {
        val WHITESPACE = Regex("""\s+""")
        val GLAMPING = Regex(
            """(?<![\p{L}\p{N}])(?:гл[еэ]мпинг[\p{L}]*|glamping)(?![\p{L}\p{N}])""",
        )
        val CLEAR_EXPRESSION = Regex(
            """(?:\b(?:убери|убрать|сними|снять|отмени|отменить|без)\b.{0,40}""" +
                """гл[еэ]мпинг|\bне\s+(?:нужен|нужна|нужно|хочу|ищи|искать)\b.{0,24}""" +
                """гл[еэ]мпинг|\bне\s+гл[еэ]мпинг|""" +
                """\bгл[еэ]мпинг\b.{0,24}\bне\s+(?:нужен|нужна|нужно)\b)""",
        )
    }
}
