package com.travelassistant.backend

import com.travelassistant.backend.api.configureApiRoutes
import com.travelassistant.backend.api.configureErrorHandling
import com.travelassistant.backend.api.configureSerialization
import com.travelassistant.backend.application.assistant.AssistantHotelSearchHandoffUseCase
import com.travelassistant.backend.application.assistant.AssistantLlmRouteWiringUseCase
import com.travelassistant.backend.application.assistant.ComposeConfirmedSearchTransitionResponseUseCase
import com.travelassistant.backend.application.assistant.CreateAssistantSessionUseCase
import com.travelassistant.backend.application.assistant.ExecuteConfirmedSearchTransitionUseCase
import com.travelassistant.backend.application.assistant.InMemoryAssistantSessionStateStore
import com.travelassistant.backend.application.assistant.InMemoryConfirmedSearchExecutionAttemptStore
import com.travelassistant.backend.application.assistant.InMemoryPendingConfirmationStore
import com.travelassistant.backend.application.assistant.PendingConfirmationStore
import com.travelassistant.backend.application.assistant.PlanAssistantLlmDecisionUseCase
import com.travelassistant.backend.application.hotel.CreateHotelSearchUseCase
import com.travelassistant.backend.application.hotel.InMemoryHotelSearchStateStore
import com.travelassistant.backend.application.llm.GenerateLlmCandidateUseCase
import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.infrastructure.llm.FakeLlmClient
import com.travelassistant.backend.infrastructure.provider.FakeHotelOfferProvider
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.time.Clock

fun main() {
    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
        module = Application::module,
    ).start(wait = true)
}

fun Application.module() {
    moduleWithAssistantLlm(defaultAssistantLlmClient())
}

internal fun Application.moduleWithAssistantLlm(
    llmClient: LlmClient,
    pendingConfirmationStore: PendingConfirmationStore = InMemoryPendingConfirmationStore(),
    clock: Clock = Clock.systemUTC(),
) {
    val assistantSessionStateStore = InMemoryAssistantSessionStateStore()
    val hotelSearchBoundary = CreateHotelSearchUseCase(
        assistantSessionStateStore = assistantSessionStateStore,
        hotelOfferProvider = FakeHotelOfferProvider(),
        hotelSearchStateStore = InMemoryHotelSearchStateStore(),
    )
    val assistantHotelSearchHandoffBoundary = AssistantHotelSearchHandoffUseCase(
        assistantSessionBoundary = CreateAssistantSessionUseCase(
            clock = clock,
            sessionStateStore = assistantSessionStateStore,
        ),
        hotelSearchBoundary = hotelSearchBoundary,
    )
    val assistantSessionBoundary = AssistantLlmRouteWiringUseCase(
        assistantSessionBoundary = assistantHotelSearchHandoffBoundary,
        planAssistantLlmDecisionUseCase = PlanAssistantLlmDecisionUseCase(
            generateLlmCandidateUseCase = GenerateLlmCandidateUseCase(
                llmClient = llmClient,
            ),
        ),
        pendingConfirmationStore = pendingConfirmationStore,
        composeTransitionResponse = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = InMemoryConfirmedSearchExecutionAttemptStore(),
                hotelSearchBoundary = hotelSearchBoundary,
            ),
        ),
        clock = clock,
    )

    configureSerialization()
    configureErrorHandling()
    configureApiRoutes(
        assistantSessionBoundary = assistantSessionBoundary,
        hotelSearchBoundary = hotelSearchBoundary,
    )
}

private fun defaultAssistantLlmClient(): LlmClient =
    FakeLlmClient(
        LlmClientResponse.Candidate(
            LlmCandidate(
                outcome = LlmCandidate.Outcome.NEEDS_CLARIFICATION,
                intent = LlmCandidate.Intent.HOTEL_SEARCH,
                missingRequiredFields = listOf("destination", "stay_dates", "guests"),
                clarificationQuestion = DEFAULT_LLM_CLARIFICATION_MESSAGE,
            ),
        ),
    )

private const val DEFAULT_LLM_CLARIFICATION_MESSAGE =
    "I received your hotel request. Please share destination, dates, guests, and budget so I can continue."
