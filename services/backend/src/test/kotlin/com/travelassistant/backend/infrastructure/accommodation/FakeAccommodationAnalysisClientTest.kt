package com.travelassistant.backend.infrastructure.accommodation

import com.travelassistant.backend.application.accommodation.AccommodationAnalysisRequest
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisResult
import com.travelassistant.backend.domain.hotel.AccommodationConcept
import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FakeAccommodationAnalysisClientTest {
    private val client = FakeAccommodationAnalysisClient()

    @Test
    fun `classifies positive synthetic fixtures deterministically`() = runBlocking {
        val result = analyze(
            candidate("c01", "Лесной глемпинг"),
            candidate(
                "c02",
                "Купольный дом",
                descriptions = listOf("Уединённое размещение в лесу"),
            ),
            candidate("c03", "Юрта"),
            candidate("c04", "Safari tent"),
            candidate("c05", "Tiny house", amenities = listOf("Fire pit")),
            candidate(
                "c06",
                "Купольный дом",
                descriptions = listOf("Формат standard hotel"),
            ),
            candidate("c07", "Домик в горах"),
            candidate("c08", "Неизвестное размещение"),
        )

        assertEquals(
            listOf(
                AccommodationMatchVerdict.MATCH,
                AccommodationMatchVerdict.MATCH,
                AccommodationMatchVerdict.PROBABLE,
                AccommodationMatchVerdict.PROBABLE,
                AccommodationMatchVerdict.MATCH,
                AccommodationMatchVerdict.PROBABLE,
                AccommodationMatchVerdict.MATCH,
                AccommodationMatchVerdict.UNKNOWN,
            ),
            result.decisions.map { decision -> decision.verdict },
        )
    }

    @Test
    fun `does not treat city or domestic words as glamping signals`() = runBlocking {
        val result = analyze(
            candidate("c01", "Город"),
            candidate("c02", "Городской отель"),
            candidate("c03", "Домашний отель"),
            candidate("c04", "Business hotel in the city"),
            candidate(
                "c05",
                "Отель для деловых поездок",
                descriptions = listOf("Обычное размещение в центре города"),
            ),
            candidate("c06", "Горячий источник"),
        )

        assertEquals(
            List(6) { AccommodationMatchVerdict.UNKNOWN },
            result.decisions.map { decision -> decision.verdict },
        )
        assertTrue(result.decisions.all { decision -> decision.evidence.isEmpty() })
    }

    @Test
    fun `does not expose nature or amenity evidence without glamping structure`() = runBlocking {
        val result = analyze(
            candidate("c01", "Отель у реки"),
            candidate("c02", "Загородный отель", descriptions = listOf("Вид на природу")),
            candidate("c03", "Тихий отель", descriptions = listOf("Рядом лес")),
            candidate("c04", "Семейный отель", amenities = listOf("Костровая зона")),
            candidate(
                "c05",
                "Riverside stay",
                descriptions = listOf("Nature and forest views"),
                amenities = listOf("Fire pit"),
            ),
            candidate(
                "c06",
                "Обычный отель",
                descriptions = listOf("Расположен у реки"),
            ),
        )

        assertEquals(
            listOf(
                AccommodationMatchVerdict.UNKNOWN,
                AccommodationMatchVerdict.UNKNOWN,
                AccommodationMatchVerdict.UNKNOWN,
                AccommodationMatchVerdict.UNKNOWN,
                AccommodationMatchVerdict.UNKNOWN,
                AccommodationMatchVerdict.NO_MATCH,
            ),
            result.decisions.map { decision -> decision.verdict },
        )
    }

    @Test
    fun `keeps ordinary accommodation exclusions deterministic`() = runBlocking {
        val result = analyze(
            candidate("c01", "Обычный отель"),
            candidate("c02", "Standard hotel"),
            candidate("c03", "Hostel"),
            candidate("c04", "Апарт-отель"),
            candidate("c05", "Место под палатку"),
            candidate("c06", "Standard cottage"),
        )

        assertEquals(
            List(6) { AccommodationMatchVerdict.NO_MATCH },
            result.decisions.map { decision -> decision.verdict },
        )
    }

    @Test
    fun `ignores image urls in fake analysis`() = runBlocking {
        val result = analyze(
            candidate(
                "c01",
                "Неизвестное размещение",
                imageUrls = listOf("https://images.invalid/glamping-dome.jpg"),
            ),
        )

        assertEquals(AccommodationMatchVerdict.UNKNOWN, result.decisions.single().verdict)
        assertTrue(result.decisions.single().evidence.isEmpty())
    }

    private suspend fun analyze(
        vararg candidates: AccommodationAnalysisRequest.Candidate,
    ): AccommodationAnalysisResult.Completed =
        assertIs(
            client.analyze(
                AccommodationAnalysisRequest(
                    concept = AccommodationConcept.GLAMPING,
                    candidates = candidates.toList(),
                ),
            ),
        )

    private fun candidate(
        id: String,
        name: String,
        descriptions: List<String> = emptyList(),
        amenities: List<String> = emptyList(),
        imageUrls: List<String> = listOf("https://images.invalid/not-inspected.jpg"),
    ) = AccommodationAnalysisRequest.Candidate(
        ephemeralCandidateId = id,
        hotelName = name,
        descriptions = descriptions,
        amenities = amenities,
        imageUrls = imageUrls,
    )
}
