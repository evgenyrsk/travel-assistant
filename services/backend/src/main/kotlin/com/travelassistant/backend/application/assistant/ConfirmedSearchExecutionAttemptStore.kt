package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchId
import java.time.Instant

interface ConfirmedSearchExecutionAttemptStore {
    fun savePrepared(
        attempt: ConfirmedSearchExecutionAttempt,
    ): ConfirmedSearchExecutionAttemptStoreResult

    fun findByIdempotencyKey(
        idempotencyKey: ConfirmedSearchExecutionIdempotencyKey,
        now: Instant,
    ): ConfirmedSearchExecutionAttempt?

    fun markInProgress(
        idempotencyKey: ConfirmedSearchExecutionIdempotencyKey,
        now: Instant,
    ): ConfirmedSearchExecutionAttemptStoreResult

    fun markSucceeded(
        idempotencyKey: ConfirmedSearchExecutionIdempotencyKey,
        createdSearchId: HotelSearchId,
        now: Instant,
    ): ConfirmedSearchExecutionAttemptStoreResult

    fun markFailed(
        idempotencyKey: ConfirmedSearchExecutionIdempotencyKey,
        reason: ConfirmedSearchExecutionAttemptFailureReason,
        now: Instant,
    ): ConfirmedSearchExecutionAttemptStoreResult
}
