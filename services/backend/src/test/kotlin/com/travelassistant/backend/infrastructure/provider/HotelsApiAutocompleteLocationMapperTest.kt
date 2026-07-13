package com.travelassistant.backend.infrastructure.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HotelsApiAutocompleteLocationMapperTest {

    @Test
    fun `maps location suggestions to destination candidates in provider order`() {
        val response = HotelsApiAutocompleteResponseDto(
            payload = HotelsApiAutocompleteResponseDto.Payload(
                locations = listOf(
                    location(id = 77, name = "Казань", typeCode = "city"),
                    location(id = 78, name = "Казанский район", typeCode = "district"),
                ),
            ),
        )

        val result = HotelsApiAutocompleteLocationMapper.map(response)

        assertEquals(listOf(77, 78), result.candidates.map { it.destinationId })
        assertEquals(listOf("Казань", "Казанский район"), result.candidates.map { it.name })
        assertEquals(listOf("city", "district"), result.candidates.map { it.type.code })
    }

    @Test
    fun `does not treat hotel suggestion id as destination id`() {
        val response = HotelsApiAutocompleteResponseDto(
            payload = HotelsApiAutocompleteResponseDto.Payload(
                locations = null,
                hotels = listOf(
                    HotelsApiAutocompleteResponseDto.Hotel(
                        id = "hotel-master-1",
                        name = "Тестовый отель",
                        signature = "Отель • Россия • Казань",
                        type = HotelsApiAutocompleteResponseDto.SuggestionType(
                            code = "hotel",
                            name = "Отель",
                        ),
                    ),
                ),
            ),
        )

        val result = HotelsApiAutocompleteLocationMapper.map(response)

        assertTrue(result.candidates.isEmpty())
    }

    @Test
    fun `returns empty candidate list when locations are absent`() {
        val response = HotelsApiAutocompleteResponseDto(
            payload = HotelsApiAutocompleteResponseDto.Payload(),
        )

        assertTrue(HotelsApiAutocompleteLocationMapper.map(response).candidates.isEmpty())
    }

    private fun location(
        id: Int,
        name: String,
        typeCode: String,
    ): HotelsApiAutocompleteResponseDto.Location =
        HotelsApiAutocompleteResponseDto.Location(
            id = id,
            name = name,
            signature = "$name, Россия",
            type = HotelsApiAutocompleteResponseDto.SuggestionType(
                code = typeCode,
                name = typeCode,
            ),
        )
}
