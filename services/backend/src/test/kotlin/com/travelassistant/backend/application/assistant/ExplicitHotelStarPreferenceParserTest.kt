package com.travelassistant.backend.application.assistant

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExplicitHotelStarPreferenceParserTest {
    private val parser = ExplicitHotelStarPreferenceParser()

    @Test
    fun `recognizes explicit five star adjective from refinement message`() {
        assertEquals(
            setOf(5),
            parser.parse("Обязательно отель должен быть пятизвездочным"),
        )
        assertEquals(setOf(5), parser.parse("Нужен пятизвёздочный отель"))
    }

    @Test
    fun `recognizes explicit numeric and word category`() {
        assertEquals(setOf(5), parser.parse("Только 5 звёзд"))
        assertEquals(setOf(5), parser.parse("Только пять звезд"))
    }

    @Test
    fun `keeps ranges and comparisons for LLM interpretation`() {
        assertNull(parser.parse("Отель 4–5 звёзд"))
        assertNull(parser.parse("Рейтинг от 4 звёзд"))
        assertNull(parser.parse("Не более 4 звёзд"))
    }

    @Test
    fun `does not turn removal or negation into an affirmative requirement`() {
        assertNull(parser.parse("Убери ограничение на пятизвёздочный отель"))
        assertNull(parser.parse("Отель не должен быть пятизвёздочным"))
    }
}
