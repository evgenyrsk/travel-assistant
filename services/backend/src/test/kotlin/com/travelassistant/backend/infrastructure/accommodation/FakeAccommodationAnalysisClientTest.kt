package com.travelassistant.backend.infrastructure.accommodation

import com.travelassistant.backend.application.accommodation.AccommodationAnalysisRequest
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisResult
import com.travelassistant.backend.domain.hotel.AccommodationConcept
import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FakeAccommodationAnalysisClientTest {
    private val client = FakeAccommodationAnalysisClient()

    @Test
    fun `classifies synthetic fixtures deterministically without image inspection`() = runBlocking {
        val result = assertIs<AccommodationAnalysisResult.Completed>(
            client.analyze(
                AccommodationAnalysisRequest(
                    concept = AccommodationConcept.GLAMPING,
                    candidates = listOf(
                        candidate("c01", "Лесной глемпинг"),
                        candidate(
                            "c02",
                            "Купольный дом",
                            descriptions = listOf("Уединённое размещение в лесу"),
                        ),
                        candidate("c03", "Обычный отель"),
                        candidate("c04", "Неизвестное размещение"),
                        candidate(
                            "c05",
                            "Купольный дом",
                            descriptions = listOf("Формат standard hotel"),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            listOf(
                AccommodationMatchVerdict.MATCH,
                AccommodationMatchVerdict.MATCH,
                AccommodationMatchVerdict.NO_MATCH,
                AccommodationMatchVerdict.UNKNOWN,
                AccommodationMatchVerdict.PROBABLE,
            ),
            result.decisions.map { decision -> decision.verdict },
        )
    }

    private fun candidate(
        id: String,
        name: String,
        descriptions: List<String> = emptyList(),
    ) = AccommodationAnalysisRequest.Candidate(
        ephemeralCandidateId = id,
        hotelName = name,
        descriptions = descriptions,
        imageUrls = listOf("https://images.invalid/not-inspected.jpg"),
    )
}
