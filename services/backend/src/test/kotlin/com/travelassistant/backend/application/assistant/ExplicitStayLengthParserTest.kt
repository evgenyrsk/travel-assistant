package com.travelassistant.backend.application.assistant

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExplicitStayLengthParserTest {
    private val parser = ExplicitStayLengthParser()

    @Test
    fun `extracts explicit night count`() {
        assertEquals(7, parser.parse("В Cosmos ВДНХ на 7 ночей"))
        assertEquals(1, parser.parse("Нужен отель на 1 ночь"))
    }

    @Test
    fun `maps one week to seven nights`() {
        assertEquals(7, parser.parse("Хочу остановиться на одну неделю"))
        assertEquals(14, parser.parse("Поездка на 2 недели"))
    }

    @Test
    fun `rejects missing ambiguous and out of range durations`() {
        assertNull(parser.parse("Хочу отель в августе"))
        assertNull(parser.parse("На 5 ночей или 7 ночей"))
        assertNull(parser.parse("На 0 ночей"))
        assertNull(parser.parse("На 999 ночей"))
    }
}
