package com.travelassistant.backend.application.assistant

import java.text.Normalizer

class ExplicitNamedHotelDestinationParser {

    fun parse(message: String): String? {
        val normalized = Normalizer.normalize(message, Normalizer.Form.NFKC)
            .replace(WHITESPACE, " ")
            .trim()
        if (normalized.isEmpty()) {
            return null
        }

        return EXPLICIT_HOTEL_MARKER.findAll(normalized)
            .mapNotNull { match -> match.groups[NAME_GROUP]?.value?.toHotelNameOrNull() }
            .firstOrNull()
            ?: PREPOSITION_PROPER_NAME.findAll(normalized)
                .mapNotNull { match ->
                    match.groups[NAME_GROUP]?.value?.toProperHotelNameOrNull()
                }
                .firstOrNull()
    }

    private fun String.toHotelNameOrNull(): String? {
        val candidate = cleanCandidate()
        if (
            candidate.isEmpty() ||
            candidate.length > MAX_DESTINATION_LENGTH ||
            !candidate.any(Char::isLetter) ||
            GENERIC_HOTEL_DESCRIPTION_START.containsMatchIn(candidate) ||
            GENERIC_HOTEL_WORD.matches(candidate)
        ) {
            return null
        }
        return candidate
    }

    private fun String.toProperHotelNameOrNull(): String? {
        val candidate = cleanCandidate()
        val tokens = candidate.split(' ').filter(String::isNotBlank)
        val containsLatin = candidate.any { character -> character in 'A'..'Z' || character in 'a'..'z' }
        val containsAcronym = tokens.any { token ->
            val letters = token.filter(Char::isLetter)
            letters.length >= MIN_ACRONYM_LENGTH && letters.all(Char::isUpperCase)
        }
        if (tokens.size < MIN_PROPER_NAME_TOKENS && !containsLatin && !containsAcronym) {
            return null
        }
        return candidate.toHotelNameOrNull()
    }

    private fun String.cleanCandidate(): String =
        trim()
            .trim(*BOUNDARY_PUNCTUATION)
            .replace(LEADING_HOTEL_WORD, "")
            .replace(WHITESPACE, " ")
            .trim()

    private companion object {
        const val NAME_GROUP = "name"
        const val MAX_DESTINATION_LENGTH = 120
        const val MIN_ACRONYM_LENGTH = 2
        const val MIN_PROPER_NAME_TOKENS = 2

        val BOUNDARY_PUNCTUATION = charArrayOf('"', '\'', '«', '»', '(', ')', '[', ']')
        val WHITESPACE = Regex("""\s+""")
        val LEADING_HOTEL_WORD = Regex(
            pattern = """(?iu)^(?:отель|гостиница|hotel)\s+""",
        )
        val GENERIC_HOTEL_WORD = Regex(
            pattern = """(?iu)(?:отель|гостиница|hotel)""",
        )
        val GENERIC_HOTEL_DESCRIPTION_START = Regex(
            pattern = """(?iu)^(?:должен|должна|должно|должны|нужен|нужна|нужно|""" +
                """не\s+нужен|не\s+нужна|с\b|без\b|на\b|до\b|от\b|где\b)""",
        )
        val EXPLICIT_HOTEL_MARKER = Regex(
            pattern = """(?iu)(?<![\p{L}\p{N}_])(?:""" +
                """(?:отель|гостиница|hotel)|""" +
                """(?:в|во)\s+(?:отеле|гостинице|hotel))\s+""" +
                """[\"'«]?(?<$NAME_GROUP>[\p{L}\p{N}][^,.;!?]{0,119}?)""" +
                """(?=[\"'»]?(?:\s+(?:""" +
                """в\s+(?:начале|конце|середине)|""" +
                """на\s+(?:\d+\s+)?(?:ноч|сут|недел)|""" +
                """с\s+(?:\d|завтрак|супруг|жен|муж|партнер|ребен|деть)|""" +
                """для\s+|без\s+дет|сегодня|завтра|послезавтра""" +
                """)|[,.;!?]|$))""",
        )
        val PREPOSITION_PROPER_NAME = Regex(
            pattern = """(?u)(?<![\p{L}\p{N}_])(?:в|во)\s+""" +
                """(?<$NAME_GROUP>[\p{Lu}][\p{L}\p{N}.'’\-]*""" +
                """(?:\s+[\p{Lu}][\p{L}\p{N}.'’\-]*){0,4})""",
        )
    }
}
