package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.domain.hotel.HotelSearchId
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class InMemoryConfirmedSearchExecutionAttemptStore :
    ConfirmedSearchExecutionAttemptStore {

    private val attempts =
        ConcurrentHashMap<ConfirmedSearchExecutionIdempotencyKey, ConfirmedSearchExecutionAttempt>()

    @Synchronized
    override fun savePrepared(
        attempt: ConfirmedSearchExecutionAttempt,
    ): ConfirmedSearchExecutionAttemptStoreResult {
        if (attempt.status != ConfirmedSearchExecutionAttemptStatus.PREPARED) {
            return ConfirmedSearchExecutionAttemptStoreResult.Rejected(
                reason = ConfirmedSearchExecutionAttemptStoreResult.RejectionReason
                    .PREPARED_ATTEMPT_REQUIRED,
                existingAttempt = attempt,
            )
        }

        val existingAttempt = attempts[attempt.idempotencyKey]
        if (existingAttempt != null) {
            if (existingAttempt.isRetryAllowed()) {
                attempts[attempt.idempotencyKey] = attempt
                return ConfirmedSearchExecutionAttemptStoreResult.Stored(attempt)
            }
            return ConfirmedSearchExecutionAttemptStoreResult.Duplicate(existingAttempt)
        }

        attempts[attempt.idempotencyKey] = attempt
        return ConfirmedSearchExecutionAttemptStoreResult.Stored(attempt)
    }

    @Synchronized
    override fun findByIdempotencyKey(
        idempotencyKey: ConfirmedSearchExecutionIdempotencyKey,
        now: Instant,
    ): ConfirmedSearchExecutionAttempt? {
        val existingAttempt = attempts[idempotencyKey] ?: return null

        if (existingAttempt.status == ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS &&
            !now.isBefore(existingAttempt.expiresAt)
        ) {
            val staleAttempt = existingAttempt.copy(
                status = ConfirmedSearchExecutionAttemptStatus.FAILED,
                failureReason = ConfirmedSearchExecutionAttemptFailureReason.STALE_EXECUTION,
                updatedAt = now,
            )
            attempts[idempotencyKey] = staleAttempt
            return staleAttempt
        }

        return existingAttempt
    }

    @Synchronized
    override fun markInProgress(
        idempotencyKey: ConfirmedSearchExecutionIdempotencyKey,
        now: Instant,
    ): ConfirmedSearchExecutionAttemptStoreResult {
        val existingAttempt = attempts[idempotencyKey]
            ?: return attemptNotFound()

        if (existingAttempt.status != ConfirmedSearchExecutionAttemptStatus.PREPARED) {
            return ConfirmedSearchExecutionAttemptStoreResult.Duplicate(existingAttempt)
        }

        return store(
            existingAttempt.copy(
                status = ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS,
                updatedAt = now,
            ),
        )
    }

    @Synchronized
    override fun markSucceeded(
        idempotencyKey: ConfirmedSearchExecutionIdempotencyKey,
        createdSearchId: HotelSearchId,
        now: Instant,
    ): ConfirmedSearchExecutionAttemptStoreResult {
        val existingAttempt = attempts[idempotencyKey]
            ?: return attemptNotFound()

        return when (existingAttempt.status) {
            ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS ->
                store(
                    existingAttempt.copy(
                        status = ConfirmedSearchExecutionAttemptStatus.SUCCEEDED,
                        createdSearchId = createdSearchId,
                        failureReason = null,
                        updatedAt = now,
                    ),
                )

            ConfirmedSearchExecutionAttemptStatus.SUCCEEDED ->
                ConfirmedSearchExecutionAttemptStoreResult.Duplicate(existingAttempt)

            ConfirmedSearchExecutionAttemptStatus.PREPARED,
            ConfirmedSearchExecutionAttemptStatus.FAILED,
            ConfirmedSearchExecutionAttemptStatus.DUPLICATE_BLOCKED ->
                ConfirmedSearchExecutionAttemptStoreResult.Rejected(
                    reason = ConfirmedSearchExecutionAttemptStoreResult.RejectionReason
                        .ATTEMPT_NOT_IN_PROGRESS,
                    existingAttempt = existingAttempt,
                )
        }
    }

    @Synchronized
    override fun markFailed(
        idempotencyKey: ConfirmedSearchExecutionIdempotencyKey,
        reason: ConfirmedSearchExecutionAttemptFailureReason,
        now: Instant,
    ): ConfirmedSearchExecutionAttemptStoreResult {
        val existingAttempt = attempts[idempotencyKey]
            ?: return attemptNotFound()

        return when (existingAttempt.status) {
            ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS ->
                store(
                    existingAttempt.copy(
                        status = ConfirmedSearchExecutionAttemptStatus.FAILED,
                        failureReason = reason,
                        createdSearchId = null,
                        updatedAt = now,
                    ),
                )

            ConfirmedSearchExecutionAttemptStatus.FAILED ->
                ConfirmedSearchExecutionAttemptStoreResult.Duplicate(existingAttempt)

            ConfirmedSearchExecutionAttemptStatus.PREPARED,
            ConfirmedSearchExecutionAttemptStatus.SUCCEEDED,
            ConfirmedSearchExecutionAttemptStatus.DUPLICATE_BLOCKED ->
                ConfirmedSearchExecutionAttemptStoreResult.Rejected(
                    reason = ConfirmedSearchExecutionAttemptStoreResult.RejectionReason
                        .ATTEMPT_NOT_IN_PROGRESS,
                    existingAttempt = existingAttempt,
                )
        }
    }

    private fun store(
        attempt: ConfirmedSearchExecutionAttempt,
    ): ConfirmedSearchExecutionAttemptStoreResult {
        attempts[attempt.idempotencyKey] = attempt
        return ConfirmedSearchExecutionAttemptStoreResult.Stored(attempt)
    }

    private fun ConfirmedSearchExecutionAttempt.isRetryAllowed(): Boolean =
        status == ConfirmedSearchExecutionAttemptStatus.FAILED &&
            failureReason?.isRetryAllowed() == true

    private fun attemptNotFound(): ConfirmedSearchExecutionAttemptStoreResult =
        ConfirmedSearchExecutionAttemptStoreResult.Rejected(
            reason = ConfirmedSearchExecutionAttemptStoreResult.RejectionReason
                .ATTEMPT_NOT_FOUND,
        )
}
