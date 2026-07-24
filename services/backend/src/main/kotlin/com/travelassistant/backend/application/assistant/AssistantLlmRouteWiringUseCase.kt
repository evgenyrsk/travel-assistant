package com.travelassistant.backend.application.assistant

import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateRequest
import com.travelassistant.backend.application.llm.LlmHotelSearchPreferencesPatch
import com.travelassistant.backend.application.observability.OperationalComponent
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.application.observability.recordSafely
import com.travelassistant.backend.domain.assistant.AssistantSession
import com.travelassistant.backend.domain.assistant.AssistantSessionId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AssistantLlmRouteWiringUseCase(
    private val assistantSessionBoundary: AssistantSessionBoundary,
    private val planAssistantLlmDecisionUseCase: PlanAssistantLlmDecisionUseCase,
    private val planProceedWithCandidateConfirmationUseCase: PlanProceedWithCandidateConfirmationUseCase =
        PlanProceedWithCandidateConfirmationUseCase(),
    private val pendingConfirmationStore: PendingConfirmationStore = InMemoryPendingConfirmationStore(),
    private val hotelConstraintsStore: AssistantHotelConstraintsStore =
        InMemoryAssistantHotelConstraintsStore(),
    private val planPostConfirmationDecisionUseCase: PlanPostConfirmationDecisionUseCase =
        PlanPostConfirmationDecisionUseCase(pendingConfirmationStore),
    private val composeTransitionResponse: ComposeConfirmedSearchTransitionResponseUseCase,
    private val clock: Clock = Clock.systemUTC(),
    private val dateInterpretationPolicy: AssistantDateInterpretationPolicy =
        AssistantDateInterpretationPolicy(),
    private val diagnosticObserver: AssistantLlmDiagnosticObserver =
        AssistantLlmDiagnosticObserver.NONE,
    private val eventSink: OperationalEventSink = OperationalEventSink.NONE,
    private val explicitNamedHotelDestinationParser: ExplicitNamedHotelDestinationParser =
        ExplicitNamedHotelDestinationParser(),
    private val explicitHotelStarPreferenceParser: ExplicitHotelStarPreferenceParser =
        ExplicitHotelStarPreferenceParser(),
    private val explicitStayLengthParser: ExplicitStayLengthParser = ExplicitStayLengthParser(),
    private val clarificationPolicy: AssistantHotelClarificationPolicy =
        AssistantHotelClarificationPolicy(),
    private val pendingConfirmationTtl: Duration = DEFAULT_PENDING_CONFIRMATION_TTL,
    private val explicitHotelSearchMessageParser: MinimalHotelSearchMessageParser =
        MinimalHotelSearchMessageParser(),
) : AssistantSessionBoundary {
    private val sessionTurnCoordinator = AssistantSessionTurnCoordinator()
    private val accumulateHotelConstraints =
        AccumulateAssistantHotelConstraintsUseCase(hotelConstraintsStore)
    private val mapHotelSearchPreferencesPatch =
        MapLlmHotelSearchPreferencesPatchUseCase()
    private val applyHotelSearchPreferencesPatch =
        ApplyHotelSearchPreferencesPatchUseCase(hotelConstraintsStore)

    override fun createSession(): AssistantSession =
        assistantSessionBoundary.createSession()

    override suspend fun acceptUserMessage(command: AcceptAssistantMessageCommand): AcceptedAssistantMessage =
        sessionTurnCoordinator.execute(command.sessionId) {
            acceptUserMessageInSession(command)
        }

    private suspend fun acceptUserMessageInSession(
        command: AcceptAssistantMessageCommand,
    ): AcceptedAssistantMessage {
        val acceptedMessage = assistantSessionBoundary.acceptUserMessage(command)

        val explicitHotelSearchMessage = explicitHotelSearchMessageParser.parse(command.message)
        if (explicitHotelSearchMessage != MinimalHotelSearchMessageParser.Result.NotRequested) {
            return acceptedMessage
        }

        val now = clock.instant()
        val activePendingConfirmation = pendingConfirmationStore.findActiveBySession(
            sessionId = command.sessionId,
            now = now,
        )
        if (activePendingConfirmation != null) {
            val postConfirmationDecision = planPostConfirmationDecisionUseCase(
                PlanPostConfirmationDecisionRequest(
                    sessionId = command.sessionId,
                    replyText = command.message,
                    now = now,
                ),
            )
            if (postConfirmationDecision != PostConfirmationDecision.NeedsReplanning) {
                return acceptedMessage.withPostConfirmationDecision(
                    decision = postConfirmationDecision,
                    decidedAt = now,
                    activePendingConfirmation = activePendingConfirmation,
                )
            }

            acceptedMessage.consumePendingConfirmation(now)
        }

        return acceptedMessage.withLlmDecision(command)
    }

    private suspend fun AcceptedAssistantMessage.withLlmDecision(
        command: AcceptAssistantMessageCommand,
    ): AcceptedAssistantMessage {
        val now = clock.instant()
        val referenceDate = command.clientTimeZone?.let { timeZone ->
            LocalDate.ofInstant(now, timeZone)
        }
        val minimumCheckInDate = referenceDate ?: LocalDate.ofInstant(
            now,
            EARLIEST_GLOBAL_DATE_OFFSET,
        )
        val request = requestFor(command, referenceDate)
        val decision = dateInterpretationPolicy(
            decision = planAssistantLlmDecisionUseCase(request),
            request = request,
        )

        return when (decision) {
            is AssistantCandidateDecision.AskClarification -> {
                val candidate = decision.candidate
                    ?.withExplicitNamedHotelDestination(command.message)
                    ?.withExplicitStarPreference(command.message)
                    ?.withExplicitStayLength(command.message)
                if (
                    candidate != null &&
                    candidate.isSafeForContextAccumulation()
                ) {
                    val contextUpdate = updateSearchContext(
                        sessionId,
                        candidate,
                        minimumCheckInDate,
                    ) ?: return withClarification(PREFERENCES_CLARIFICATION_MESSAGE)
                    val updatedDecision = AssistantCandidateDecision.ProceedWithCandidate(candidate)
                        .withUpdatedConstraints(contextUpdate)
                    if (updatedDecision.candidate.missingRequiredFields.isEmpty()) {
                        return withConfirmationPlan(
                            planProceedWithCandidateConfirmationUseCase(
                                updatedDecision,
                                contextUpdate.constraints.preferences,
                            ),
                        )
                    }
                    return withClarification(
                        preferredClarification(
                            decisionQuestion = updatedDecision.candidate.clarificationQuestion
                                ?: decision.question,
                            missingFields = updatedDecision.candidate.missingRequiredFields,
                        ),
                    )
                }
                val actualMissingFields = hotelConstraintsStore
                    .findBySession(sessionId)
                    ?.missingRequiredFields()
                    .orEmpty()
                withClarification(
                    preferredClarification(
                        decisionQuestion = decision.question,
                        missingFields = actualMissingFields,
                    ),
                )
            }

            is AssistantCandidateDecision.Fallback -> {
                report(decision.reason.toDiagnosticEvent())
                withSafeBoundaryMessage(decision.reason.userMessage())
            }

            is AssistantCandidateDecision.ProceedWithCandidate -> {
                val enrichedDecision = AssistantCandidateDecision.ProceedWithCandidate(
                    decision.candidate
                        .withExplicitNamedHotelDestination(command.message)
                        .withExplicitStarPreference(command.message)
                        .withExplicitStayLength(command.message),
                )
                if (!enrichedDecision.candidate.isSafeForContextAccumulation()) {
                    return withConfirmationPlan(
                        planProceedWithCandidateConfirmationUseCase(enrichedDecision),
                    )
                }

                val contextUpdate = updateSearchContext(
                    sessionId,
                    enrichedDecision.candidate,
                    minimumCheckInDate,
                )
                    ?: return withClarification(PREFERENCES_CLARIFICATION_MESSAGE)
                withConfirmationPlan(
                    planProceedWithCandidateConfirmationUseCase(
                        enrichedDecision.withUpdatedConstraints(contextUpdate),
                        contextUpdate.constraints.preferences,
                    ),
                )
            }
        }
    }

    private fun requestFor(
        command: AcceptAssistantMessageCommand,
        referenceDate: LocalDate?,
    ): LlmCandidateRequest {
        val constraints = hotelConstraintsStore.findBySession(command.sessionId)
            ?: AssistantHotelConstraints()

        return LlmCandidateRequest(
            userMessage = command.message,
            confirmedConstraints = constraints.toConfirmedConstraints(),
            missingRequiredFields = constraints.missingRequiredFields(),
            referenceDate = referenceDate,
        )
    }

    private fun AssistantCandidateDecision.ProceedWithCandidate.withUpdatedConstraints(
        accumulation: AssistantHotelConstraintsAccumulationResult,
    ): AssistantCandidateDecision.ProceedWithCandidate {
        val missingFields = (
            accumulation.constraints.missingRequiredFields() +
                accumulation.issues.map { issue -> issue.field.key }
            ).distinct()
        val hasInvalidDate = accumulation.issues.any { issue ->
            issue.field == AssistantHotelConstraintField.CHECK_IN ||
                issue.field == AssistantHotelConstraintField.CHECK_OUT
        }
        val hasUnsupportedRoomCount =
            AssistantHotelConstraintsAccumulationIssue.UNSUPPORTED_ROOM_COUNT in
                accumulation.issues
        if (hasUnsupportedRoomCount) {
            report(AssistantLlmDiagnosticEvent.UNSUPPORTED_ROOM_COUNT)
        }

        return AssistantCandidateDecision.ProceedWithCandidate(
            candidate.copy(
                outcome = if (missingFields.isEmpty()) {
                    LlmCandidate.Outcome.INTERPRETED
                } else {
                    LlmCandidate.Outcome.NEEDS_CLARIFICATION
                },
                extractedConstraints = accumulation.constraints.toCoreConstraints(),
                missingRequiredFields = missingFields,
                clarificationQuestion = if (missingFields.isEmpty()) {
                    null
                } else when {
                    hasUnsupportedRoomCount ->
                        SINGLE_ROOM_ONLY_CLARIFICATION_MESSAGE

                    candidate.clarificationQuestion ==
                        AssistantDateInterpretationPolicy.DATE_WITH_YEAR_CLARIFICATION_MESSAGE ->
                        AssistantDateInterpretationPolicy.DATE_WITH_YEAR_CLARIFICATION_MESSAGE

                    hasInvalidDate ->
                        AssistantDateInterpretationPolicy.DATE_WITH_YEAR_CLARIFICATION_MESSAGE

                    AssistantHotelConstraintField.CHILDREN.key in missingFields ->
                        CHILDREN_COUNT_CLARIFICATION_MESSAGE

                    AssistantHotelConstraintField.CHILDREN_AGES.key in missingFields ->
                        CHILDREN_AGES_CLARIFICATION_MESSAGE

                    AssistantHotelConstraintField.CHECK_IN.key in missingFields ||
                        AssistantHotelConstraintField.CHECK_OUT.key in missingFields ->
                        clarificationPolicy.questionFor(missingFields)

                    else -> clarificationPolicy.questionFor(missingFields)
                        ?: candidate.clarificationQuestion
                },
            ),
        )
    }

    private fun updateSearchContext(
        sessionId: AssistantSessionId,
        candidate: LlmCandidate,
        minimumCheckInDate: LocalDate,
    ): AssistantHotelConstraintsAccumulationResult? {
        val accumulation = accumulateConstraints(sessionId, candidate, minimumCheckInDate)
        val mappedPatch = when (
            val mappingResult = mapHotelSearchPreferencesPatch(candidate.preferencePatch)
        ) {
            is MapLlmHotelSearchPreferencesPatchResult.Mapped -> mappingResult.patch
            is MapLlmHotelSearchPreferencesPatchResult.Rejected -> return null
        }
        when (
            applyHotelSearchPreferencesPatch(
                ApplyHotelSearchPreferencesPatchCommand(
                    sessionId = sessionId,
                    patch = mappedPatch,
                ),
            )
        ) {
            is ApplyHotelSearchPreferencesPatchResult.Applied -> Unit
            is ApplyHotelSearchPreferencesPatchResult.Rejected -> return null
        }

        return accumulation.copy(
            constraints = checkNotNull(hotelConstraintsStore.findBySession(sessionId)),
        )
    }

    private fun accumulateConstraints(
        sessionId: AssistantSessionId,
        candidate: LlmCandidate,
        minimumCheckInDate: LocalDate,
    ): AssistantHotelConstraintsAccumulationResult =
        accumulateHotelConstraints(
            AccumulateAssistantHotelConstraintsCommand(
                sessionId = sessionId,
                extractedConstraints = candidate.extractedConstraints,
                minimumCheckInDate = minimumCheckInDate,
            ),
        )

    private fun LlmCandidate.isSafeForContextAccumulation(): Boolean =
        intent == LlmCandidate.Intent.HOTEL_SEARCH &&
            outcome in setOf(
                LlmCandidate.Outcome.INTERPRETED,
                LlmCandidate.Outcome.NEEDS_CLARIFICATION,
            ) &&
            conflicts.isEmpty() &&
            warnings.isEmpty()

    private fun LlmCandidate.withExplicitNamedHotelDestination(message: String): LlmCandidate {
        if (!extractedConstraints[AssistantHotelConstraintField.DESTINATION.key].isNullOrBlank()) {
            return this
        }
        val destination = explicitNamedHotelDestinationParser.parse(message) ?: return this

        report(AssistantLlmDiagnosticEvent.DESTINATION_ENRICHED)
        return copy(
            extractedConstraints = extractedConstraints +
                (AssistantHotelConstraintField.DESTINATION.key to destination),
        )
    }

    private fun LlmCandidate.withExplicitStarPreference(message: String): LlmCandidate {
        val explicitStars = explicitHotelStarPreferenceParser.parse(message) ?: return this
        val enrichedPatch = preferencePatch.copy(
            stars = explicitStars,
            clear = preferencePatch.clear - LlmHotelSearchPreferencesPatch.Field.STARS,
        )
        if (enrichedPatch == preferencePatch) {
            return this
        }

        report(AssistantLlmDiagnosticEvent.PREFERENCE_STARS_ENRICHED)
        return copy(preferencePatch = enrichedPatch)
    }

    private fun LlmCandidate.withExplicitStayLength(message: String): LlmCandidate {
        val stayLength = explicitStayLengthParser.parse(message) ?: return this
        if (extractedConstraints[AssistantHotelConstraintField.STAY_LENGTH_NIGHTS.key] == stayLength.toString()) {
            return this
        }
        return copy(
            extractedConstraints = extractedConstraints +
                (AssistantHotelConstraintField.STAY_LENGTH_NIGHTS.key to stayLength.toString()),
        )
    }

    private fun AcceptedAssistantMessage.withClarification(question: String): AcceptedAssistantMessage =
        copy(
            assistantReply = AssistantReply(
                type = AssistantReplyType.CLARIFICATION,
                message = question,
            ),
            nextAction = AssistantNextAction.ASK_CLARIFICATION,
            hotelSearchId = null,
        )

    private fun preferredClarification(
        decisionQuestion: String,
        missingFields: Collection<String>,
    ): String =
        if (
            decisionQuestion == AssistantDateInterpretationPolicy.DATE_WITH_YEAR_CLARIFICATION_MESSAGE ||
            decisionQuestion == SINGLE_ROOM_ONLY_CLARIFICATION_MESSAGE
        ) {
            decisionQuestion
        } else {
            clarificationPolicy.questionFor(missingFields) ?: decisionQuestion
        }

    private fun AcceptedAssistantMessage.withSafeBoundaryMessage(
        message: String,
    ): AcceptedAssistantMessage =
        copy(
            assistantReply = AssistantReply(
                type = AssistantReplyType.CLARIFICATION,
                message = message,
            ),
            nextAction = AssistantNextAction.SHOW_BOUNDARY_MESSAGE,
            hotelSearchId = null,
        )

    private fun AcceptedAssistantMessage.withConfirmationPlan(
        plan: ProceedWithCandidateConfirmationPlan,
    ): AcceptedAssistantMessage =
        when (plan) {
            is ProceedWithCandidateConfirmationPlan.ConfirmationRequired -> {
                savePendingConfirmation(plan)
                recordConfirmationOutcome(OperationalOutcome.CONFIRMATION_REQUIRED)
                withClarification(plan.proposal.confirmationPromptMessage())
            }

            is ProceedWithCandidateConfirmationPlan.ClarificationRequired ->
                withClarification(plan.question)

            is ProceedWithCandidateConfirmationPlan.Fallback -> {
                report(plan.reason.toDiagnosticEvent())
                withSafeBoundaryMessage(plan.reason.userMessage())
            }
        }

    private fun AssistantCandidateDecision.FallbackReason.toDiagnosticEvent():
        AssistantLlmDiagnosticEvent =
        when (this) {
            AssistantCandidateDecision.FallbackReason.EMPTY_RESPONSE ->
                AssistantLlmDiagnosticEvent.CANDIDATE_EMPTY_RESPONSE

            AssistantCandidateDecision.FallbackReason.CLIENT_FAILURE ->
                AssistantLlmDiagnosticEvent.CANDIDATE_CLIENT_FAILURE

            AssistantCandidateDecision.FallbackReason.INVALID_CANDIDATE ->
                AssistantLlmDiagnosticEvent.CANDIDATE_INVALID

            AssistantCandidateDecision.FallbackReason.UNSUPPORTED_INTENT ->
                AssistantLlmDiagnosticEvent.CANDIDATE_UNSUPPORTED_INTENT

            AssistantCandidateDecision.FallbackReason.MISSING_CLARIFICATION ->
                AssistantLlmDiagnosticEvent.CANDIDATE_MISSING_CLARIFICATION
        }

    private fun ProceedWithCandidateConfirmationPlan.FallbackReason.toDiagnosticEvent():
        AssistantLlmDiagnosticEvent =
        when (this) {
            ProceedWithCandidateConfirmationPlan.FallbackReason.UNSUPPORTED_INTENT ->
                AssistantLlmDiagnosticEvent.CONFIRMATION_UNSUPPORTED_INTENT

            ProceedWithCandidateConfirmationPlan.FallbackReason.UNSAFE_OR_UNSUPPORTED_OUTCOME ->
                AssistantLlmDiagnosticEvent.CONFIRMATION_UNSAFE_OR_UNSUPPORTED_OUTCOME

            ProceedWithCandidateConfirmationPlan.FallbackReason.CONFLICTS_OR_WARNINGS ->
                AssistantLlmDiagnosticEvent.CONFIRMATION_CONFLICTS_OR_WARNINGS
        }

    private fun AssistantCandidateDecision.FallbackReason.userMessage(): String =
        when (this) {
            AssistantCandidateDecision.FallbackReason.EMPTY_RESPONSE,
            AssistantCandidateDecision.FallbackReason.CLIENT_FAILURE,
            -> TEMPORARY_LLM_FAILURE_MESSAGE

            AssistantCandidateDecision.FallbackReason.INVALID_CANDIDATE,
            AssistantCandidateDecision.FallbackReason.MISSING_CLARIFICATION,
            -> AMBIGUOUS_LLM_RESULT_MESSAGE

            AssistantCandidateDecision.FallbackReason.UNSUPPORTED_INTENT ->
                HOTEL_ONLY_BOUNDARY_MESSAGE
        }

    private fun ProceedWithCandidateConfirmationPlan.FallbackReason.userMessage(): String =
        when (this) {
            ProceedWithCandidateConfirmationPlan.FallbackReason.UNSUPPORTED_INTENT ->
                HOTEL_ONLY_BOUNDARY_MESSAGE

            ProceedWithCandidateConfirmationPlan.FallbackReason.UNSAFE_OR_UNSUPPORTED_OUTCOME ->
                AMBIGUOUS_LLM_RESULT_MESSAGE

            ProceedWithCandidateConfirmationPlan.FallbackReason.CONFLICTS_OR_WARNINGS ->
                CONFLICTING_LLM_RESULT_MESSAGE
        }

    private fun report(event: AssistantLlmDiagnosticEvent) {
        runCatching { diagnosticObserver.record(event) }
    }

    private suspend fun AcceptedAssistantMessage.withPostConfirmationDecision(
        decision: PostConfirmationDecision,
        decidedAt: Instant,
        activePendingConfirmation: PendingProceedWithCandidateConfirmation? = null,
    ): AcceptedAssistantMessage {
        eventSink.recordSafely(
            OperationalEvent(
                name = OperationalEventName.CONFIRMATION_OUTCOME,
                component = OperationalComponent.ASSISTANT,
                sessionId = sessionId.value,
                operation = OperationalOperation.POST_ASSISTANT_MESSAGE,
                outcome = decision.toOperationalOutcome(),
            ),
        )
        return when (decision) {
            is PostConfirmationDecision.Confirmed -> {
                val composedResult = composeTransitionResponse(
                    ComposeConfirmedSearchTransitionResponseRequest(
                        sessionId = sessionId,
                        decision = decision,
                        pendingConfirmation = activePendingConfirmation,
                        now = decidedAt,
                    ),
                )
                if (composedResult.pendingConsumeInstruction ==
                    PendingConsumeInstruction.CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS
                ) {
                    consumePendingConfirmation(decidedAt)
                }
                when (composedResult.responseDirective.nextAction) {
                    InternalTransitionNextAction.SHOW_HOTEL_RESULTS ->
                        copy(
                            assistantReply = AssistantReply(
                                type = AssistantReplyType.CLARIFICATION,
                                message = composedResult.messageText,
                            ),
                            nextAction = AssistantNextAction.SHOW_HOTEL_RESULTS,
                            hotelSearchId = composedResult.hotelSearchId,
                        )

                    InternalTransitionNextAction.ASK_CLARIFICATION ->
                        withClarification(composedResult.messageText)
                }
            }

            PostConfirmationDecision.NeedsClarification ->
                withClarification(CONFIRMATION_NEEDS_CLARIFICATION_MESSAGE)

            PostConfirmationDecision.Declined -> {
                consumePendingConfirmation(decidedAt)
                withClarification(CONFIRMATION_DECLINED_MESSAGE)
            }

            PostConfirmationDecision.NeedsReplanning -> {
                consumePendingConfirmation(decidedAt)
                withClarification(CONFIRMATION_REPLANNING_MESSAGE)
            }

            PostConfirmationDecision.NoActivePendingConfirmation ->
                withClarification(NO_ACTIVE_CONFIRMATION_MESSAGE)

            PostConfirmationDecision.Unknown ->
                withClarification(CONFIRMATION_UNKNOWN_REPLY_MESSAGE)
        }
    }

    private fun PostConfirmationDecision.toOperationalOutcome(): OperationalOutcome =
        when (this) {
            is PostConfirmationDecision.Confirmed -> OperationalOutcome.CONFIRMED
            PostConfirmationDecision.Declined -> OperationalOutcome.DECLINED
            PostConfirmationDecision.NeedsClarification -> OperationalOutcome.NEEDS_CLARIFICATION
            PostConfirmationDecision.NeedsReplanning -> OperationalOutcome.NEEDS_REPLANNING
            PostConfirmationDecision.NoActivePendingConfirmation,
            PostConfirmationDecision.Unknown,
            -> OperationalOutcome.UNKNOWN
        }

    private fun AcceptedAssistantMessage.recordConfirmationOutcome(
        outcome: OperationalOutcome,
    ) {
        eventSink.recordSafely(
            OperationalEvent(
                name = OperationalEventName.CONFIRMATION_OUTCOME,
                component = OperationalComponent.ASSISTANT,
                sessionId = sessionId.value,
                operation = OperationalOperation.POST_ASSISTANT_MESSAGE,
                outcome = outcome,
            ),
        )
    }

    private fun ProceedWithCandidateConfirmationProposal.confirmationPromptMessage(): String =
        "$summary\n\n$confirmationQuestion"

    private fun AcceptedAssistantMessage.consumePendingConfirmation(
        consumedAt: Instant,
    ) {
        pendingConfirmationStore.markConsumed(
            sessionId = sessionId,
            consumedAt = consumedAt,
        )
    }

    private fun AcceptedAssistantMessage.savePendingConfirmation(
        plan: ProceedWithCandidateConfirmationPlan.ConfirmationRequired,
    ) {
        val createdAt = clock.instant()
        pendingConfirmationStore.save(
            PendingProceedWithCandidateConfirmation(
                sessionId = sessionId,
                criteria = plan.criteria,
                proposal = plan.proposal,
                createdAt = createdAt,
                updatedAt = createdAt,
                expiresAt = createdAt.plus(pendingConfirmationTtl),
            ),
        )
    }

    private companion object {
        val DEFAULT_PENDING_CONFIRMATION_TTL: Duration = Duration.ofMinutes(15)

        const val TEMPORARY_LLM_FAILURE_MESSAGE =
            "Не удалось обработать сообщение из-за временного сбоя. " +
                "Попробуйте отправить его ещё раз."

        const val AMBIGUOUS_LLM_RESULT_MESSAGE =
            "Не удалось однозначно разобрать параметры поездки. " +
                "Переформулируйте запрос, указав направление, даты и состав гостей."

        const val CONFLICTING_LLM_RESULT_MESSAGE =
            "В параметрах поездки осталось противоречие. " +
                "Переформулируйте запрос или уточните спорное условие."

        const val HOTEL_ONLY_BOUNDARY_MESSAGE =
            "Сейчас я помогаю только с поиском отелей. " +
                "Укажите направление, даты и состав гостей."

        const val CONFIRMATION_NEEDS_CLARIFICATION_MESSAGE =
            "Подтвердите параметры, отмените поиск или пришлите исправленные условия."

        const val CONFIRMATION_DECLINED_MESSAGE =
            "Хорошо, поиск отелей не запущен. Когда будете готовы, сообщите новые параметры."

        const val CONFIRMATION_REPLANNING_MESSAGE =
            "Уточните исправленные направление, даты или состав гостей."

        const val NO_ACTIVE_CONFIRMATION_MESSAGE =
            "Нет активного запроса, ожидающего подтверждения. Отправьте параметры поиска отелей ещё раз."

        const val CONFIRMATION_UNKNOWN_REPLY_MESSAGE =
            "Не удалось распознать ответ на подтверждение. Подтвердите параметры, отмените поиск или пришлите исправленные условия."

        const val CHILDREN_COUNT_CLARIFICATION_MESSAGE =
            "Укажите количество детей."

        const val CHILDREN_AGES_CLARIFICATION_MESSAGE =
            "Укажите возраст каждого ребёнка (от 0 до 17 лет)."

        const val PREFERENCES_CLARIFICATION_MESSAGE =
            "Уточните предпочтения: максимальную стоимость за весь период, звёзды, " +
                "минимальный рейтинг, бесплатную отмену или включённый завтрак."

        val EARLIEST_GLOBAL_DATE_OFFSET: ZoneOffset = ZoneOffset.ofHours(-12)
    }
}
