package com.travelassistant.backend

import com.travelassistant.backend.api.configureApiRoutes
import com.travelassistant.backend.api.configureErrorHandling
import com.travelassistant.backend.api.configureSerialization
import com.travelassistant.backend.application.assistant.AssistantHotelConstraintsStore
import com.travelassistant.backend.application.assistant.AssistantLlmDiagnosticObserver
import com.travelassistant.backend.application.assistant.AssistantHotelSearchHandoffUseCase
import com.travelassistant.backend.application.assistant.AssistantLlmRouteWiringUseCase
import com.travelassistant.backend.application.assistant.ComposeConfirmedSearchTransitionResponseUseCase
import com.travelassistant.backend.application.assistant.CreateAssistantSessionUseCase
import com.travelassistant.backend.application.assistant.ExecuteConfirmedSearchTransitionUseCase
import com.travelassistant.backend.application.assistant.InMemoryAssistantHotelConstraintsStore
import com.travelassistant.backend.application.assistant.InMemoryAssistantSessionStateStore
import com.travelassistant.backend.application.assistant.InMemoryConfirmedSearchExecutionAttemptStore
import com.travelassistant.backend.application.assistant.InMemoryPendingConfirmationStore
import com.travelassistant.backend.application.assistant.PendingConfirmationStore
import com.travelassistant.backend.application.assistant.PlanAssistantLlmDecisionUseCase
import com.travelassistant.backend.application.hotel.CreateHotelSearchUseCase
import com.travelassistant.backend.application.hotel.InMemoryHotelSearchStateStore
import com.travelassistant.backend.application.hotel.LoadSelectedHotelDetailsUseCase
import com.travelassistant.backend.application.hotel.ResolveSelectedHotelOfferUseCase
import com.travelassistant.backend.application.llm.GenerateLlmCandidateUseCase
import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateRetryPolicy
import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.infrastructure.llm.FakeLlmClient
import com.travelassistant.backend.infrastructure.llm.LlmProviderConfig
import com.travelassistant.backend.infrastructure.llm.LlmProviderFactory
import com.travelassistant.backend.infrastructure.llm.OpenRouterDiagnosticObserver
import com.travelassistant.backend.infrastructure.llm.SafeLlmDiagnosticLogger
import com.travelassistant.backend.infrastructure.llm.createProductionOpenRouterHttpClient
import com.travelassistant.backend.infrastructure.provider.HotelOfferProviderFactory
import com.travelassistant.backend.infrastructure.provider.HotelProviderConfig
import com.travelassistant.backend.infrastructure.provider.createProductionHotelsApiHttpClient
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
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
    moduleWithProviderConfigs(
        llmProviderConfig = LlmProviderConfig.fromEnvironment(),
        providerConfig = HotelProviderConfig.fromEnvironment(),
    )
}

internal fun Application.moduleWithProviderConfigs(
    llmProviderConfig: LlmProviderConfig = LlmProviderConfig(),
    providerConfig: HotelProviderConfig = HotelProviderConfig(),
    pendingConfirmationStore: PendingConfirmationStore = InMemoryPendingConfirmationStore(),
    hotelConstraintsStore: AssistantHotelConstraintsStore = InMemoryAssistantHotelConstraintsStore(),
    clock: Clock = Clock.systemUTC(),
    openRouterHttpClientFactory: () -> HttpClient = ::createProductionOpenRouterHttpClient,
    openRouterDiagnosticObserver: OpenRouterDiagnosticObserver = SafeLlmDiagnosticLogger,
    assistantLlmDiagnosticObserver: AssistantLlmDiagnosticObserver = SafeLlmDiagnosticLogger,
    realHotelHttpClientFactory: () -> HttpClient = ::createProductionHotelsApiHttpClient,
) {
    val llmProviderRuntime = LlmProviderFactory.create(
        config = llmProviderConfig,
        fakeClientFactory = ::defaultAssistantLlmClient,
        openRouterHttpClientFactory = openRouterHttpClientFactory,
        openRouterDiagnosticObserver = openRouterDiagnosticObserver,
    )
    environment.monitor.subscribe(ApplicationStopped) {
        llmProviderRuntime.close()
    }

    try {
        moduleWithAssistantLlm(
            llmClient = llmProviderRuntime.client,
            llmCandidateRetryPolicy = llmProviderRuntime.candidateRetryPolicy,
            providerConfig = providerConfig,
            pendingConfirmationStore = pendingConfirmationStore,
            hotelConstraintsStore = hotelConstraintsStore,
            clock = clock,
            assistantLlmDiagnosticObserver = assistantLlmDiagnosticObserver,
            realHotelHttpClientFactory = realHotelHttpClientFactory,
        )
    } catch (error: Throwable) {
        llmProviderRuntime.close()
        throw error
    }
}

internal fun Application.moduleWithAssistantLlm(
    llmClient: LlmClient,
    llmCandidateRetryPolicy: LlmCandidateRetryPolicy = LlmCandidateRetryPolicy.NO_RETRY,
    providerConfig: HotelProviderConfig = HotelProviderConfig(),
    pendingConfirmationStore: PendingConfirmationStore = InMemoryPendingConfirmationStore(),
    hotelConstraintsStore: AssistantHotelConstraintsStore = InMemoryAssistantHotelConstraintsStore(),
    clock: Clock = Clock.systemUTC(),
    realHotelHttpClientFactory: () -> HttpClient = ::createProductionHotelsApiHttpClient,
    assistantLlmDiagnosticObserver: AssistantLlmDiagnosticObserver =
        AssistantLlmDiagnosticObserver.NONE,
) {
    val assistantSessionStateStore = InMemoryAssistantSessionStateStore()
    val hotelProviderRuntime = HotelOfferProviderFactory.create(
        config = providerConfig,
        realHttpClientFactory = realHotelHttpClientFactory,
    )
    environment.monitor.subscribe(ApplicationStopped) {
        hotelProviderRuntime.close()
    }
    val hotelSearchStateStore = InMemoryHotelSearchStateStore()
    val hotelSearchBoundary = CreateHotelSearchUseCase(
        assistantSessionStateStore = assistantSessionStateStore,
        hotelOfferProvider = hotelProviderRuntime.provider,
        hotelSearchStateStore = hotelSearchStateStore,
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
                retryPolicy = llmCandidateRetryPolicy,
            ),
        ),
        pendingConfirmationStore = pendingConfirmationStore,
        hotelConstraintsStore = hotelConstraintsStore,
        composeTransitionResponse = ComposeConfirmedSearchTransitionResponseUseCase(
            executeTransition = ExecuteConfirmedSearchTransitionUseCase(
                attemptStore = InMemoryConfirmedSearchExecutionAttemptStore(),
                hotelSearchBoundary = hotelSearchBoundary,
            ),
        ),
        clock = clock,
        diagnosticObserver = assistantLlmDiagnosticObserver,
    )

    configureSerialization()
    configureErrorHandling()
    configureApiRoutes(
        assistantSessionBoundary = assistantSessionBoundary,
        hotelSearchBoundary = hotelSearchBoundary,
        loadSelectedHotelDetails = LoadSelectedHotelDetailsUseCase(
            resolveSelectedOffer = ResolveSelectedHotelOfferUseCase(hotelSearchStateStore),
            hotelDetailsProvider = hotelProviderRuntime.detailsProvider,
        ),
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
    "Расскажите, куда и когда планируете поездку и кто едет с вами."
