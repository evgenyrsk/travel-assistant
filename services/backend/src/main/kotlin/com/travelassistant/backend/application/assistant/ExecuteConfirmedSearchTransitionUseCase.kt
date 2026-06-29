package com.travelassistant.backend.application.assistant

class ExecuteConfirmedSearchTransitionUseCase(
    private val planSearchCreation: PlanConfirmedSearchCreationUseCase =
        PlanConfirmedSearchCreationUseCase(),
    private val buildCommand: BuildConfirmedSearchCreationCommandUseCase =
        BuildConfirmedSearchCreationCommandUseCase(),
    private val planExecution: PlanConfirmedSearchExecutionUseCase =
        PlanConfirmedSearchExecutionUseCase(),
    private val guardUseCase: PlanConfirmedSearchExecutionGuardUseCase =
        PlanConfirmedSearchExecutionGuardUseCase(),
    private val planAttempt: PlanConfirmedSearchExecutionAttemptUseCase =
        PlanConfirmedSearchExecutionAttemptUseCase(),
    private val attemptStore: ConfirmedSearchExecutionAttemptStore,
) {

    operator fun invoke(
        request: ExecuteConfirmedSearchTransitionRequest,
    ): ExecuteConfirmedSearchTransitionResult {
        val creationPlan = when (val plan = planSearchCreation(request.decision)) {
            is ConfirmedSearchCreationPlan.ReadyToCreateSearch -> plan
        }
        val commandPlan = when (val command = buildCommand(request.sessionId, creationPlan)) {
            is ConfirmedSearchCreationCommandPlan.CommandReady -> command
        }
        val executionResult = planExecution(commandPlan)
        val idempotencyKey = ConfirmedSearchExecutionIdempotencyKey.from(commandPlan)

        val guardResult = guardUseCase(
            ConfirmedSearchExecutionGuardRequest(
                sessionId = request.sessionId,
                commandPlan = commandPlan,
                pendingConfirmation = request.pendingConfirmation,
                now = request.now,
            ),
        )

        val existingAttempt = attemptStore.findByIdempotencyKey(idempotencyKey)

        val attemptPlanningResult = planAttempt(guardResult, request.now, existingAttempt)

        return handlePlanningResult(attemptPlanningResult, executionResult, request)
    }

    private fun handlePlanningResult(
        result: ConfirmedSearchExecutionAttemptResult,
        executionResult: ConfirmedSearchExecutionResult,
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
                persistAndTransition(result, executionResult, request)
        }

    private fun persistAndTransition(
        planningResult: ConfirmedSearchExecutionAttemptResult.AttemptPreparedButExecutionBlocked,
        executionResult: ConfirmedSearchExecutionResult,
        request: ExecuteConfirmedSearchTransitionRequest,
    ): ExecuteConfirmedSearchTransitionResult {
        val saveResult = attemptStore.savePrepared(planningResult.attempt)

        return when (saveResult) {
            is ConfirmedSearchExecutionAttemptStoreResult.Stored ->
                transitionExecution(saveResult.attempt, executionResult, planningResult, request)

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

    private fun transitionExecution(
        storedAttempt: ConfirmedSearchExecutionAttempt,
        executionResult: ConfirmedSearchExecutionResult,
        planningResult: ConfirmedSearchExecutionAttemptResult.AttemptPreparedButExecutionBlocked,
        request: ExecuteConfirmedSearchTransitionRequest,
    ): ExecuteConfirmedSearchTransitionResult {
        val inProgressResult = attemptStore.markInProgress(
            idempotencyKey = storedAttempt.idempotencyKey,
            now = request.now,
        )

        val transitionedAttempt = when (inProgressResult) {
            is ConfirmedSearchExecutionAttemptStoreResult.Stored -> inProgressResult.attempt
            else -> storedAttempt
        }

        return ExecuteConfirmedSearchTransitionResult.Transitioned(
            attempt = transitionedAttempt,
            executionResult = executionResult,
            pendingConsumptionDecision =
                ExecuteConfirmedSearchTransitionResult.PendingConsumptionDecision
                    .CONSUME_AFTER_SUCCESSFUL_RECORDING,
            lifecyclePolicy = planningResult.lifecyclePolicy,
            executionPolicy = planningResult.executionPolicy,
        )
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
