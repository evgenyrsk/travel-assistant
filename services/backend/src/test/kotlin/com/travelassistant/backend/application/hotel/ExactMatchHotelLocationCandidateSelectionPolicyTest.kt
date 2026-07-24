package com.travelassistant.backend.application.hotel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExactMatchHotelLocationCandidateSelectionPolicyTest {
    private val policy = ExactMatchHotelLocationCandidateSelectionPolicy()

    @Test
    fun `returns not found for empty candidates`() {
        assertIs<HotelLocationCandidateSelectionResult.NotFound>(
            policy.select(query = "Казань", candidates = emptyList()),
        )
    }

    @Test
    fun `selects the only unique candidate`() {
        val candidate = candidate(destinationId = 1, name = "Другой город")

        val result = assertIs<HotelLocationCandidateSelectionResult.Selected>(
            policy.select(query = "Казань", candidates = listOf(candidate)),
        )

        assertEquals(candidate, result.candidate)
    }

    @Test
    fun `selects one exact name match among multiple candidates`() {
        val exact = candidate(destinationId = 2, name = "Казань")

        val result = assertIs<HotelLocationCandidateSelectionResult.Selected>(
            policy.select(
                query = "Казань",
                candidates = listOf(
                    candidate(destinationId = 1, name = "Казань, аэропорт"),
                    exact,
                ),
            ),
        )

        assertEquals(exact, result.candidate)
    }

    @Test
    fun `selects one exact signature match among multiple candidates`() {
        val exact = candidate(
            destinationId = 2,
            name = "Казань",
            signature = "Казань, Республика Татарстан, Россия",
        )

        val result = assertIs<HotelLocationCandidateSelectionResult.Selected>(
            policy.select(
                query = "Казань, Республика Татарстан, Россия",
                candidates = listOf(
                    candidate(destinationId = 1, name = "Казань, аэропорт"),
                    exact,
                ),
            ),
        )

        assertEquals(exact, result.candidate)
    }

    @Test
    fun `normalizes unicode whitespace case and yo`() {
        listOf(
            "  ОРЁЛ\t" to candidate(destinationId = 1, name = "орел"),
            "Нижний    Новгород" to candidate(destinationId = 2, name = "нижний новгород"),
            "И\u0306ошкар-Ола" to candidate(destinationId = 3, name = "Йошкар-Ола"),
        ).forEach { (query, exact) ->
            val result = assertIs<HotelLocationCandidateSelectionResult.Selected>(
                policy.select(
                    query = query,
                    candidates = listOf(
                        candidate(destinationId = 10, name = "Другой город"),
                        exact,
                    ),
                ),
            )

            assertEquals(exact, result.candidate)
        }
    }

    @Test
    fun `deduplicates by destination id and preserves first candidate`() {
        val first = candidate(destinationId = 1, name = "Первое представление")

        val result = assertIs<HotelLocationCandidateSelectionResult.Selected>(
            policy.select(
                query = "Второе представление",
                candidates = listOf(
                    first,
                    candidate(destinationId = 1, name = "Второе представление"),
                ),
            ),
        )

        assertEquals(first, result.candidate)
    }

    @Test
    fun `requires selection for two exact matches with different ids`() {
        val candidates = listOf(
            candidate(destinationId = 1, name = "Казань"),
            candidate(destinationId = 2, name = "Казань"),
        )

        val result = assertIs<HotelLocationCandidateSelectionResult.SelectionRequired>(
            policy.select(query = "Казань", candidates = candidates),
        )

        assertEquals(candidates, result.candidates)
    }

    @Test
    fun `does not select first candidate for partial or absent exact match`() {
        val candidates = listOf(
            candidate(destinationId = 1, name = "Казань, аэропорт"),
            candidate(destinationId = 2, name = "Новая Казань"),
        )

        listOf("Казань", "Неизвестный город").forEach { query ->
            val result = assertIs<HotelLocationCandidateSelectionResult.SelectionRequired>(
                policy.select(query = query, candidates = candidates),
            )

            assertEquals(candidates, result.candidates)
        }
    }

    private fun candidate(
        destinationId: Int,
        name: String,
        signature: String = "$name, Россия",
    ): HotelLocationResolution.Candidate =
        HotelLocationResolution.Candidate(
            destinationId = destinationId,
            name = name,
            signature = signature,
            type = HotelLocationResolution.Type(code = "city", name = "Город"),
        )
}
