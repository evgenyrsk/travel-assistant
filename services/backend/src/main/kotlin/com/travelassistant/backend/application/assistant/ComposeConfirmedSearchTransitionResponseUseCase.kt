package com.travelassistant.backend.application.assistant

class ComposeConfirmedSearchTransitionResponseUseCase(
    private val executeTransition: ExecuteConfirmedSearchTransitionUseCase,
    private val mapToDirective: MapConfirmedSearchTransitionResultToResponseDirectiveUseCase =
        MapConfirmedSearchTransitionResultToResponseDirectiveUseCase(),
) {

    suspend operator fun invoke(
        request: ComposeConfirmedSearchTransitionResponseRequest,
    ): ComposeConfirmedSearchTransitionResponseResult {
        val transitionResult = executeTransition(
            ExecuteConfirmedSearchTransitionRequest(
                sessionId = request.sessionId,
                decision = request.decision,
                pendingConfirmation = request.pendingConfirmation,
                now = request.now,
            ),
        )

        val directive = mapToDirective(transitionResult)

        return ComposeConfirmedSearchTransitionResponseResult(
            transitionResult = transitionResult,
            responseDirective = directive,
            messageText = safeMessageText(directive.messageKind),
            pendingConsumeInstruction = consumeInstruction(directive),
            hotelSearchId = directive.hotelSearchId,
        )
    }

    private fun consumeInstruction(
        directive: ConfirmedSearchTransitionResponseDirective,
    ): PendingConsumeInstruction =
        if (directive.shouldConsumePendingConfirmation) {
            PendingConsumeInstruction.CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS
        } else {
            PendingConsumeInstruction.DO_NOT_CONSUME_PENDING_CONFIRMATION
        }

    private fun safeMessageText(kind: TransitionMessageKind): String =
        when (kind) {
            TransitionMessageKind.PROCESSING ->
                PROCESSING_MESSAGE

            TransitionMessageKind.ALREADY_PROCESSING ->
                ALREADY_PROCESSING_MESSAGE

            TransitionMessageKind.CONFIRMATION_REJECTED ->
                CONFIRMATION_REJECTED_MESSAGE

            TransitionMessageKind.LOCATION_NOT_FOUND ->
                LOCATION_NOT_FOUND_MESSAGE

            TransitionMessageKind.LOCATION_SELECTION_REQUIRED ->
                LOCATION_SELECTION_REQUIRED_MESSAGE

            TransitionMessageKind.SEARCH_REQUEST_REJECTED ->
                SEARCH_REQUEST_REJECTED_MESSAGE

            TransitionMessageKind.TEMPORARY_FAILURE ->
                TEMPORARY_FAILURE_MESSAGE

            TransitionMessageKind.RESULTS_READY ->
                RESULTS_READY_MESSAGE
        }

    private companion object {
        const val PROCESSING_MESSAGE =
            "Поиск уже выполняется, результаты пока не готовы."

        const val ALREADY_PROCESSING_MESSAGE =
            "Этот поиск уже выполняется."

        const val CONFIRMATION_REJECTED_MESSAGE =
            "Не удалось продолжить поиск с текущим подтверждением."

        const val LOCATION_NOT_FOUND_MESSAGE =
            "Не удалось определить направление. Уточните город или место."

        const val LOCATION_SELECTION_REQUIRED_MESSAGE =
            "Найдено несколько подходящих направлений. Уточните город или место."

        const val SEARCH_REQUEST_REJECTED_MESSAGE =
            "Не удалось безопасно подготовить поиск. Проверьте направление, даты, состав гостей и количество номеров."

        const val TEMPORARY_FAILURE_MESSAGE =
            "Сейчас не удалось завершить поиск отелей. Попробуйте ещё раз."

        const val RESULTS_READY_MESSAGE =
            "Поиск завершён. Результат готов."
    }
}
