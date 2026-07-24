package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.AccommodationConcept
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExplicitAccommodationConceptPreferenceParserTest {
    private val parser = ExplicitAccommodationConceptPreferenceParser()

    @Test
    fun `recognizes only active glamping concept`() {
        assertEquals(
            ExplicitAccommodationConceptPreferenceParser.Change.Set(
                AccommodationConcept.GLAMPING,
            ),
            parser.parse("Хочу забронировать глемпинг"),
        )
        assertEquals(
            ExplicitAccommodationConceptPreferenceParser.Change.Set(
                AccommodationConcept.GLAMPING,
            ),
            parser.parse("Looking for GLAMPING near Kazan"),
        )
        assertNull(parser.parse("Нужны апартаменты"))
        assertNull(parser.parse("Подберите необычный домик"))
    }

    @Test
    fun `recognizes explicit concept removal`() {
        assertEquals(
            ExplicitAccommodationConceptPreferenceParser.Change.Clear,
            parser.parse("Убери требование глемпинга"),
        )
        assertEquals(
            ExplicitAccommodationConceptPreferenceParser.Change.Clear,
            parser.parse("Теперь не глемпинг"),
        )
    }
}
