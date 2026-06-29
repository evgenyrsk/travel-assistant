package com.travelassistant.backend.application.assistant

class PlanConfirmedSearchExecutionUseCase {

    operator fun invoke(
        commandPlan: ConfirmedSearchCreationCommandPlan.CommandReady,
    ): ConfirmedSearchExecutionResult =
        ConfirmedSearchExecutionResult.PreparedButNotExecuted(
            commandPlan = commandPlan,
            reason = ConfirmedSearchExecutionResult.NotExecutedReason.IDEMPOTENCY_GUARD_REQUIRED,
            lifecyclePolicy = commandPlan.lifecyclePolicy,
            executionPolicy = ConfirmedSearchExecutionPolicy(),
        )
}
