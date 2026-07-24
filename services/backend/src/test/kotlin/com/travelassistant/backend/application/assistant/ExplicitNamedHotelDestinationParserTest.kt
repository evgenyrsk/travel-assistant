package com.travelassistant.backend.application.assistant

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExplicitNamedHotelDestinationParserTest {
    private val parser = ExplicitNamedHotelDestinationParser()

    @Test
    fun `extracts a distinctive hotel name after destination preposition`() {
        assertEquals(
            "Cosmos ВДНХ",
            parser.parse(
                "Хочу вместе с супругой в Cosmos ВДНХ в начале августа на 7 ночей с завтраками",
            ),
        )
    }

    @Test
    fun `extracts a quoted hotel after explicit hotel marker`() {
        assertEquals(
            "Метрополь",
            parser.parse("Нужен отель «Метрополь» с 1 по 3 августа"),
        )
    }

    @Test
    fun `does not treat a hotel requirement as a hotel name`() {
        assertNull(parser.parse("Отель должен быть пятизвёздочным"))
    }

    @Test
    fun `does not turn a vague time phrase into a destination`() {
        assertNull(parser.parse("Хочу в начале августа на семь ночей"))
    }

    @Test
    fun `does not duplicate ordinary one-word city extraction`() {
        assertNull(parser.parse("Хочу в Москву на выходные"))
    }
}
