package com.travelassistant.backend.application.hotel

import com.travelassistant.backend.application.observability.OperationalComponent
import com.travelassistant.backend.application.observability.OperationalDependency
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalLevel
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.application.observability.recordSafely
import kotlinx.coroutines.CancellationException
import kotlin.math.max

class LoadSelectedHotelDetailsUseCase(
    private val resolveSelectedOffer: ResolveSelectedHotelOfferUseCase,
    private val hotelDetailsProvider: HotelDetailsProviderBoundary,
    private val eventSink: OperationalEventSink = OperationalEventSink.NONE,
) {
    suspend operator fun invoke(
        request: ResolveSelectedHotelOfferRequest,
    ): LoadSelectedHotelDetailsResult {
        val detailsStartedAt = System.nanoTime()
        val result = when (val selected = resolveSelectedOffer(request)) {
            ResolveSelectedHotelOfferResult.SearchNotFound ->
                LoadSelectedHotelDetailsResult.SearchNotFound
            ResolveSelectedHotelOfferResult.OfferNotFound ->
                LoadSelectedHotelDetailsResult.OfferNotFound
            is ResolveSelectedHotelOfferResult.Resolved ->
                loadProviderDetails(
                    providerReference = selected.offer.providerReference,
                    hotelSearchId = request.searchId.value,
                )
        }
        recordDetailsOutcome(
            hotelSearchId = request.searchId.value,
            result = result,
            durationMillis = elapsedMillis(detailsStartedAt),
        )
        return result
    }

    private suspend fun loadProviderDetails(
        providerReference: String,
        hotelSearchId: String,
    ): LoadSelectedHotelDetailsResult {
        val startedAt = System.nanoTime()
        val providerResult = try {
            hotelDetailsProvider.load(providerReference)
        } catch (error: CancellationException) {
            throw error
        } catch (error: RuntimeException) {
            recordProviderCall(
                hotelSearchId = hotelSearchId,
                outcome = OperationalOutcome.FAILED,
                level = OperationalLevel.ERROR,
                startedAt = startedAt,
            )
            throw error
        }
        recordProviderCall(
            hotelSearchId = hotelSearchId,
            outcome = providerResult.toOperationalOutcome(),
            level = providerResult.toOperationalLevel(),
            startedAt = startedAt,
        )

        return when (providerResult) {
            is HotelDetailsProviderResult.Loaded ->
                LoadSelectedHotelDetailsResult.Loaded(providerResult.details)
            HotelDetailsProviderResult.NotFound ->
                LoadSelectedHotelDetailsResult.DetailsNotFound
            is HotelDetailsProviderResult.ResponseRejected ->
                LoadSelectedHotelDetailsResult.ResponseRejected(providerResult.reason)
            is HotelDetailsProviderResult.ProviderUnavailable ->
                LoadSelectedHotelDetailsResult.ProviderUnavailable(providerResult.reason)
        }
    }

    private fun recordProviderCall(
        hotelSearchId: String,
        outcome: OperationalOutcome,
        level: OperationalLevel,
        startedAt: Long,
    ) {
        eventSink.recordSafely(
            OperationalEvent(
                name = OperationalEventName.DEPENDENCY_CALL_COMPLETED,
                component = OperationalComponent.PROVIDER,
                level = level,
                hotelSearchId = hotelSearchId,
                operation = OperationalOperation.PROVIDER_HOTEL_DETAILS,
                dependency = OperationalDependency.HOTEL_PROVIDER,
                outcome = outcome,
                durationMillis = elapsedMillis(startedAt),
            ),
        )
    }

    private fun recordDetailsOutcome(
        hotelSearchId: String,
        result: LoadSelectedHotelDetailsResult,
        durationMillis: Long,
    ) {
        eventSink.recordSafely(
            OperationalEvent(
                name = OperationalEventName.HOTEL_DETAILS_COMPLETED,
                component = OperationalComponent.HOTEL_DETAILS,
                level = if (result is LoadSelectedHotelDetailsResult.Loaded) {
                    OperationalLevel.INFO
                } else {
                    OperationalLevel.WARNING
                },
                hotelSearchId = hotelSearchId,
                operation = OperationalOperation.GET_HOTEL_DETAILS,
                outcome = result.toOperationalOutcome(),
                durationMillis = durationMillis,
            ),
        )
    }

    private fun HotelDetailsProviderResult.toOperationalOutcome(): OperationalOutcome =
        when (this) {
            is HotelDetailsProviderResult.Loaded -> OperationalOutcome.SUCCEEDED
            HotelDetailsProviderResult.NotFound -> OperationalOutcome.NOT_FOUND
            is HotelDetailsProviderResult.ResponseRejected -> OperationalOutcome.RESPONSE_REJECTED
            is HotelDetailsProviderResult.ProviderUnavailable -> when (reason) {
                HotelDetailsProviderResult.UnavailableReason.TIMEOUT -> OperationalOutcome.TIMEOUT
                HotelDetailsProviderResult.UnavailableReason.RATE_LIMITED ->
                    OperationalOutcome.RATE_LIMITED
                HotelDetailsProviderResult.UnavailableReason.AUTHENTICATION_FAILED ->
                    OperationalOutcome.AUTHENTICATION_FAILED
                HotelDetailsProviderResult.UnavailableReason.UNAVAILABLE,
                HotelDetailsProviderResult.UnavailableReason.UNKNOWN,
                -> OperationalOutcome.UNAVAILABLE
            }
        }

    private fun HotelDetailsProviderResult.toOperationalLevel(): OperationalLevel =
        when (this) {
            is HotelDetailsProviderResult.Loaded -> OperationalLevel.INFO
            HotelDetailsProviderResult.NotFound -> OperationalLevel.WARNING
            is HotelDetailsProviderResult.ProviderUnavailable,
            is HotelDetailsProviderResult.ResponseRejected,
            -> OperationalLevel.ERROR
        }

    private fun LoadSelectedHotelDetailsResult.toOperationalOutcome(): OperationalOutcome =
        when (this) {
            is LoadSelectedHotelDetailsResult.Loaded -> OperationalOutcome.SUCCEEDED
            LoadSelectedHotelDetailsResult.SearchNotFound,
            LoadSelectedHotelDetailsResult.OfferNotFound,
            LoadSelectedHotelDetailsResult.DetailsNotFound,
            -> OperationalOutcome.NOT_FOUND
            is LoadSelectedHotelDetailsResult.ResponseRejected -> OperationalOutcome.RESPONSE_REJECTED
            is LoadSelectedHotelDetailsResult.ProviderUnavailable -> when (reason) {
                HotelDetailsProviderResult.UnavailableReason.TIMEOUT -> OperationalOutcome.TIMEOUT
                HotelDetailsProviderResult.UnavailableReason.RATE_LIMITED ->
                    OperationalOutcome.RATE_LIMITED
                HotelDetailsProviderResult.UnavailableReason.AUTHENTICATION_FAILED ->
                    OperationalOutcome.AUTHENTICATION_FAILED
                HotelDetailsProviderResult.UnavailableReason.UNAVAILABLE,
                HotelDetailsProviderResult.UnavailableReason.UNKNOWN,
                -> OperationalOutcome.UNAVAILABLE
            }
        }

    private fun elapsedMillis(startedAtNanos: Long): Long =
        max(0, (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND)

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
