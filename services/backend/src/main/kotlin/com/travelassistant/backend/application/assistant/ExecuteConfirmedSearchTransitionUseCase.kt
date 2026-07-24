package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.hotel.CreateHotelSearchResult
import com.travelassistant.backend.application.hotel.HotelSearchBoundary
import com.travelassistant.backend.domain.hotel.HotelSearch
import kotlinx.coroutines.CancellationException

class ExecuteConfirmedSearchTransitionUseCase(
    private val planSearchCreation: PlanConfirmedSearchCreationUseCase =
        PlanConfirmedSearchCreationUseCase(),
    private val buildCommand: BuildConfirmedSearchCreationCommandUseCase =
        BuildConfirmedSearchCreationCommandUseCase(),
    private val guardUseCase: PlanConfirmedSearchExecutionGuardUseCase =
        PlanConfirmedSearchExecutionGuardUseCase(),
    private val planAttempt: PlanConfirmedSearchExecutionAttemptUseCase =
        PlanConfirmedSearchExecutionAttemptUseCase(),
    private val attemptStore: ConfirmedSearchExecutionAttemptStore,
    private val hotelSearchBoundary: HotelSearchBoundary,
) {

    suspend operator fun invoke(
        request: ExecuteConfirmedSearchTransitionRequest,
    ): ExecuteConfirmedSearchTransitionResult {
        val creationPlan = when (val plan = planSearchCreation(request.decision)) {
            is ConfirmedSearchCreationPlan.ReadyToCreateSearch -> plan
        }
        val commandPlan = when (val command = buildCommand(request.sessionId, creationPlan)) {
            is ConfirmedSearchCreationCommandPlan.CommandReady -> command
        }
        val idempotencyKey = ConfirmedSearchExecutionIdempotencyKey.from(commandPlan)

        val guardResult = guardUseCase(
            ConfirmedSearchExecutionGuardRequest(
                sessionId = request.sessionId,
                commandPlan = commandPlan,
                pendingConfirmation = request.pendingConfirmation,
                now = request.now,
            ),
        )

        val existingAttempt = attemptStore.findByIdempotencyKey(idempotencyKey, request.now)

        val planningInput = if (existingAttempt.isRetryEligible()) null else existingAttempt

        val attemptPlanningResult = planAttempt(guardResult, request.now, planningInput)

        return handlePlanningResult(attemptPlanningResult, commandPlan, request)
    }

    private suspend fun handlePlanningResult(
        result: ConfirmedSearchExecutionAttemptResult,
        commandPlan: ConfirmedSearchCreationCommandPlan.CommandReady,
        request: ExecuteConfirmedSearchTransitionRequest,
    ): ExecuteConfirmedSearchTransitionResult =
        when (result) {
            is ConfirmedSearchExecutionAttemptResult.Rejected ->
                ExecuteConfirmedSearchTransitionResult.GuardRejected(
                    attemptRejectionReason = result.reason,
                    lifecyclePolicy = result.lifecyclePolicy,
                    executionPolicy = result.executionPolicy,
                )

            is ConfirmedSearchExecutionAttemptResult.DuplicateDetected ->
                buildDuplicateResult(result)

            is ConfirmedSearchExecutionAttemptResult.AttemptPreparedButExecutionBlocked ->
                persistAndExecute(result, commandPlan, request)
        }

    private suspend fun persistAndExecute(
        planningResult: ConfirmedSearchExecutionAttemptResult.AttemptPreparedButExecutionBlocked,
        commandPlan: ConfirmedSearchCreationCommandPlan.CommandReady,
        request: ExecuteConfirmedSearchTransitionRequest,
    ): ExecuteConfirmedSearchTransitionResult {
        val saveResult = attemptStore.savePrepared(planningResult.attempt)

        return when (saveResult) {
            is ConfirmedSearchExecutionAttemptStoreResult.Stored ->
                executeSearchCreation(saveResult.attempt, commandPlan, planningResult, request)

            is ConfirmedSearchExecutionAttemptStoreResult.Duplicate ->
                buildDuplicateFromStore(saveResult.existingAttempt, planningResult)

            is ConfirmedSearchExecutionAttemptStoreResult.Rejected ->
                ExecuteConfirmedSearchTransitionResult.StoreRejected(
                    reason = saveResult.reason,
                    lifecyclePolicy = planningResult.lifecyclePolicy,
                    executionPolicy = planningResult.executionPolicy,
                )
        }
    }

    private suspend fun executeSearchCreation(
        storedAttempt: ConfirmedSearchExecutionAttempt,
        commandPlan: ConfirmedSearchCreationCommandPlan.CommandReady,
        planningResult: ConfirmedSearchExecutionAttemptResult.AttemptPreparedButExecutionBlocked,
        request: ExecuteConfirmedSearchTransitionRequest,
    ): ExecuteConfirmedSearchTransitionResult {
        val inProgressResult = attemptStore.markInProgress(
            idempotencyKey = storedAttempt.idempotencyKey,
            now = request.now,
        )

        if (inProgressResult !is ConfirmedSearchExecutionAttemptStoreResult.Stored) {
            return ExecuteConfirmedSearchTransitionResult.StoreRejected(
                reason = ConfirmedSearchExecutionAttemptStoreResult.RejectionReason.ATTEMPT_NOT_FOUND,
                lifecyclePolicy = planningResult.lifecyclePolicy,
                executionPolicy = planningResult.executionPolicy,
            )
        }

        val creationResult = try {
            hotelSearchBoundary.createSearch(commandPlan.command)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            attemptStore.markFailed(
                idempotencyKey = storedAttempt.idempotencyKey,
                reason = ConfirmedSearchExecutionAttemptFailureReason.SEARCH_CREATION_FAILED,
                now = request.now,
            )
            return ExecuteConfirmedSearchTransitionResult.StoreRejected(
                reason = ConfirmedSearchExecutionAttemptStoreResult.RejectionReason.ATTEMPT_NOT_IN_PROGRESS,
                lifecyclePolicy = planningResult.lifecyclePolicy,
                executionPolicy = planningResult.executionPolicy,
            )
        }

        return when (creationResult) {
            is CreateHotelSearchResult.Created ->
                recordSuccessfulSearchCreation(
                    createdSearch = creationResult.search,
                    inProgressAttempt = inProgressResult.attempt,
                    planningResult = planningResult,
                    request = request,
                )

            is CreateHotelSearchResult.NotCreated ->
                recordSearchNotCreated(
                    notCreated = creationResult,
                    inProgressAttempt = inProgressResult.attempt,
                    planningResult = planningResult,
                    request = request,
                )
        }
    }

    private fun recordSuccessfulSearchCreation(
        createdSearch: HotelSearch,
        inProgressAttempt: ConfirmedSearchExecutionAttempt,
        planningResult: ConfirmedSearchExecutionAttemptResult.AttemptPreparedButExecutionBlocked,
        request: ExecuteConfirmedSearchTransitionRequest,
    ): ExecuteConfirmedSearchTransitionResult {
        val succeededResult = attemptStore.markSucceeded(
            idempotencyKey = inProgressAttempt.idempotencyKey,
            createdSearchId = createdSearch.id,
            now = request.now,
        )

        val finalAttempt = when (succeededResult) {
            is ConfirmedSearchExecutionAttemptStoreResult.Stored -> succeededResult.attempt
            else -> inProgressAttempt
        }

        val executionResult = ConfirmedSearchExecutionResult.SearchCreated(
            searchId = createdSearch.id,
            searchStatus = createdSearch.status,
            lifecyclePolicy = planningResult.lifecyclePolicy,
            executionPolicy = planningResult.executionPolicy,
        )

        return ExecuteConfirmedSearchTransitionResult.Transitioned(
            attempt = finalAttempt,
            executionResult = executionResult,
            pendingConsumptionDecision =
                ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision
                    .CONSUME_AFTER_SUCCESSFUL_RECORDING,
            lifecyclePolicy = planningResult.lifecyclePolicy,
            executionPolicy = planningResult.executionPolicy,
        )
    }

    private fun recordSearchNotCreated(
        notCreated: CreateHotelSearchResult.NotCreated,
        inProgressAttempt: ConfirmedSearchExecutionAttempt,
        planningResult: ConfirmedSearchExecutionAttemptResult.AttemptPreparedButExecutionBlocked,
        request: ExecuteConfirmedSearchTransitionRequest,
    ): ExecuteConfirmedSearchTransitionResult {
        val failedResult = attemptStore.markFailed(
            idempotencyKey = inProgressAttempt.idempotencyKey,
            reason = ConfirmedSearchExecutionAttemptFailureReason.SEARCH_CREATION_FAILED,
            now = request.now,
        )

        return when (failedResult) {
            is ConfirmedSearchExecutionAttemptStoreResult.Stored ->
                ExecuteConfirmedSearchTransitionResult.SearchNotCreated(
                    attempt = failedResult.attempt,
                    outcome = notCreated.outcome,
                    lifecyclePolicy = planningResult.lifecyclePolicy,
                    executionPolicy = planningResult.executionPolicy,
                )

            is ConfirmedSearchExecutionAttemptStoreResult.Duplicate ->
                buildDuplicateFromStore(failedResult.existingAttempt, planningResult)

            is ConfirmedSearchExecutionAttemptStoreResult.Rejected ->
                ExecuteConfirmedSearchTransitionResult.StoreRejected(
                    reason = failedResult.reason,
                    lifecyclePolicy = planningResult.lifecyclePolicy,
                    executionPolicy = planningResult.executionPolicy,
                )
        }
    }

    private fun buildDuplicateResult(
        result: ConfirmedSearchExecutionAttemptResult.DuplicateDetected,
    ): ExecuteConfirmedSearchTransitionResult.DuplicateDetected =
        ExecuteConfirmedSearchTransitionResult.DuplicateDetected(
            existingAttempt = result.originalAttempt,
            duplicateReason = result.reason,
            pendingConsumptionDecision = pendingConsumptionForExisting(result.originalAttempt),
            lifecyclePolicy = result.lifecyclePolicy,
            executionPolicy = result.executionPolicy,
        )

    private fun buildDuplicateFromStore(
        existingAttempt: ConfirmedSearchExecutionAttempt,
        planningResult: ConfirmedSearchExecutionAttemptResult.AttemptPreparedButExecutionBlocked,
    ): ExecuteConfirmedSearchTransitionResult.DuplicateDetected =
        ExecuteConfirmedSearchTransitionResult.DuplicateDetected(
            existingAttempt = existingAttempt,
            duplicateReason = existingAttempt.status.toDuplicateReason(),
            pendingConsumptionDecision = pendingConsumptionForExisting(existingAttempt),
            lifecyclePolicy = planningResult.lifecyclePolicy,
            executionPolicy = planningResult.executionPolicy,
        )

    private fun pendingConsumptionForExisting(
        attempt: ConfirmedSearchExecutionAttempt,
    ): ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision =
        when (attempt.status) {
            ConfirmedSearchExecutionAttemptStatus.SUCCEEDED ->
                ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision
                    .CONSUME_AFTER_SUCCESSFUL_RECORDING

            else ->
                ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision
                    .DO_NOT_CONSUME
        }

    private fun ConfirmedSearchExecutionAttempt?.isRetryEligible(): Boolean =
        this != null &&
            status == ConfirmedSearchExecutionAttemptStatus.FAILED &&
            failureReason?.isRetryAllowed() == true

    private fun ConfirmedSearchExecutionAttemptStatus.toDuplicateReason():
        ConfirmedSearchExecutionAttemptResult.DuplicateReason =
        when (this) {
            ConfirmedSearchExecutionAttemptStatus.PREPARED ->
                ConfirmedSearchExecutionAttemptResult.DuplicateReason.PREPARED

            ConfirmedSearchExecutionAttemptStatus.IN_PROGRESS ->
                ConfirmedSearchExecutionAttemptResult.DuplicateReason.IN_PROGRESS

            ConfirmedSearchExecutionAttemptStatus.SUCCEEDED ->
                ConfirmedSearchExecutionAttemptResult.DuplicateReason.SUCCEEDED

            ConfirmedSearchExecutionAttemptStatus.FAILED ->
                ConfirmedSearchExecutionAttemptResult.DuplicateReason.FAILED

            ConfirmedSearchExecutionAttemptStatus.DUPLICATE_BLOCKED ->
                ConfirmedSearchExecutionAttemptResult.DuplicateReason.DUPLICATE_BLOCKED
        }

}
