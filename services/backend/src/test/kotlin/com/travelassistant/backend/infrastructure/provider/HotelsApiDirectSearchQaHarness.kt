package com.travelassistant.backend.infrastructure.provider

import io.ktor.client.HttpClient
import io.ktor.http.ContentType
import java.time.LocalDate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal class HotelsApiDirectSearchQaHarness(
    private val environment: Map<String, String>,
    private val clientFactory: () -> HttpClient,
) {
    suspend fun run(): Result {
        if (environment[ENABLED_KEY] != ENABLED_VALUE) {
            return Result.Disabled
        }

        val input = when (val parsed = parseInput()) {
            is InputResult.Valid -> parsed.input
            is InputResult.Invalid -> return Result.Rejected(parsed.issue)
        }
        val client = clientFactory()

        return try {
            execute(client, input)
        } finally {
            client.close()
        }
    }

    private suspend fun execute(client: HttpClient, input: Input): Result {
        val transport = PublicHotelsApiHttpTransport(
            httpClient = client,
            publicTarget = HotelsApiTargetConfig.publicDefault(),
        )
        val request = HotelsApiSearchRequestDto(
            destinationId = input.destinationId,
            checkinDate = input.checkInDate.toString(),
            checkoutDate = input.checkOutDate.toString(),
            guests = listOf(
                HotelsApiSearchRequestDto.Guest(
                    adultsCount = ADULTS_COUNT,
                    childrenAge = emptyList(),
                ),
            ),
            offset = FIRST_PAGE_OFFSET,
            limit = MAX_CANDIDATES,
        )
        val response = transport.postJson(
            path = SEARCH_PATH,
            body = HotelsApiJson.codec.encodeToString(request),
        )
        if (!response.contentType.isJson()) {
            return Result.Rejected(Issue.UNEXPECTED_CONTENT_TYPE)
        }

        val providerResponse = try {
            HotelsApiJson.codec.decodeFromString<HotelsApiSearchResponseDto>(response.body)
        } catch (_: SerializationException) {
            return Result.Rejected(Issue.INVALID_RESPONSE)
        }
        val offers = when (val mapping = HotelsApiSearchResponseMapper.map(providerResponse)) {
            is HotelsApiSearchResponseMapper.Result.Mapped -> mapping.offers
            is HotelsApiSearchResponseMapper.Result.Rejected ->
                return Result.Rejected(Issue.MAPPING_REJECTED)
        }

        return Result.Success(
            statusCode = response.statusCode,
            contentType = requireNotNull(response.contentType),
            hotelCount = providerResponse.payload.hotels.size,
            offerCount = offers.size,
            isLoadingCompleted = providerResponse.payload.isLoadingCompleted,
            hasNextOffset = providerResponse.payload.nextOffset != null,
        )
    }

    private fun parseInput(): InputResult {
        val destinationId = environment[DESTINATION_ID_KEY]?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?: return InputResult.Invalid(Issue.INVALID_DESTINATION_ID)
        val checkInDate = environment[CHECK_IN_DATE_KEY].parseDate()
            ?: return InputResult.Invalid(Issue.INVALID_CHECK_IN_DATE)
        val checkOutDate = environment[CHECK_OUT_DATE_KEY].parseDate()
            ?: return InputResult.Invalid(Issue.INVALID_CHECK_OUT_DATE)
        if (!checkOutDate.isAfter(checkInDate)) {
            return InputResult.Invalid(Issue.INVALID_DATE_RANGE)
        }

        return InputResult.Valid(
            Input(
                destinationId = destinationId,
                checkInDate = checkInDate,
                checkOutDate = checkOutDate,
            ),
        )
    }

    private fun String?.parseDate(): LocalDate? =
        this?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }

    private fun String?.isJson(): Boolean {
        val contentType = this?.let { value ->
            runCatching { ContentType.parse(value) }.getOrNull()
        }
        return contentType?.match(ContentType.Application.Json) == true
    }

    sealed interface Result {
        data object Disabled : Result

        data class Rejected(val issue: Issue) : Result

        data class Success(
            val statusCode: Int,
            val contentType: String,
            val hotelCount: Int,
            val offerCount: Int,
            val isLoadingCompleted: Boolean,
            val hasNextOffset: Boolean,
        ) : Result
    }

    enum class Issue {
        INVALID_DESTINATION_ID,
        INVALID_CHECK_IN_DATE,
        INVALID_CHECK_OUT_DATE,
        INVALID_DATE_RANGE,
        UNEXPECTED_CONTENT_TYPE,
        INVALID_RESPONSE,
        MAPPING_REJECTED,
    }

    private sealed interface InputResult {
        data class Valid(val input: Input) : InputResult

        data class Invalid(val issue: Issue) : InputResult
    }

    private data class Input(
        val destinationId: Int,
        val checkInDate: LocalDate,
        val checkOutDate: LocalDate,
    )

    companion object {
        internal const val ENABLED_KEY = "HOTELS_API_QA_ENABLED"
        internal const val DESTINATION_ID_KEY = "HOTELS_API_QA_DESTINATION_ID"
        internal const val CHECK_IN_DATE_KEY = "HOTELS_API_QA_CHECKIN_DATE"
        internal const val CHECK_OUT_DATE_KEY = "HOTELS_API_QA_CHECKOUT_DATE"

        private const val ENABLED_VALUE = "true"
        private const val SEARCH_PATH = "/api/v1/hotels/search"
        private const val ADULTS_COUNT = 2
        private const val FIRST_PAGE_OFFSET = 0
        private const val MAX_CANDIDATES = 20
    }
}
