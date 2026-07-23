package com.travelassistant.backend.application.hotel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExactNamedHotelCandidateSelectionPolicyTest {
    private val policy = ExactNamedHotelCandidateSelectionPolicy()

    @Test
    fun `selects unique exact hotel without using provider order`() {
        val selected = assertIs<HotelCandidateSelectionResult.Selected>(
            policy.select(
                query = "  COSMOS   ВДНХ  ",
                candidates = listOf(
                    hotel("hotel-1", "Другой отель"),
                    hotel("hotel-2", "Cosmos ВДНХ"),
                ),
                hasLocationCandidates = true,
            ),
        )

        assertEquals("hotel-2", selected.candidate.providerReference)
    }

    @Test
    fun `selects sole hotel fallback only when locations are absent`() {
        val candidate = hotel("hotel-1", "Cosmos Москва ВДНХ Отель")

        assertIs<HotelCandidateSelectionResult.Selected>(
            policy.select(
                query = "Cosmos ВДНХ",
                candidates = listOf(candidate),
                hasLocationCandidates = false,
            ),
        )
        assertIs<HotelCandidateSelectionResult.NotSelected>(
            policy.select(
                query = "Москва",
                candidates = listOf(candidate),
                hasLocationCandidates = true,
            ),
        )
    }

    @Test
    fun `requires clarification for multiple hotels without a location`() {
        val result = assertIs<HotelCandidateSelectionResult.SelectionRequired>(
            policy.select(
                query = "Cosmos",
                candidates = listOf(
                    hotel("hotel-1", "Cosmos ВДНХ"),
                    hotel("hotel-2", "Cosmos Арбат"),
                ),
                hasLocationCandidates = false,
            ),
        )

        assertEquals(listOf("hotel-1", "hotel-2"), result.candidates.map { it.providerReference })
    }

    @Test
    fun `deduplicates provider reference and treats yo as e`() {
        val candidate = hotel("hotel-1", "Отель Ёлка")
        val duplicate = candidate.copy(name = "Дубликат")

        val result = assertIs<HotelCandidateSelectionResult.Selected>(
            policy.select(
                query = "отель елка",
                candidates = listOf(candidate, duplicate),
                hasLocationCandidates = true,
            ),
        )

        assertEquals(candidate, result.candidate)
    }

    private fun hotel(
        providerReference: String,
        name: String,
    ): HotelLocationResolution.HotelCandidate =
        HotelLocationResolution.HotelCandidate(
            providerReference = providerReference,
            name = name,
            signature = "$name, Москва",
            type = HotelLocationResolution.Type(code = "hotel", name = "Отель"),
        )
}
