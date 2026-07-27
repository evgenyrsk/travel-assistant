package com.travelassistant.backend

import com.travelassistant.backend.api.configureApiRoutes
import com.travelassistant.backend.api.configureErrorHandling
import com.travelassistant.backend.api.configureOperationalHttpEvents
import com.travelassistant.backend.api.configureOperationalRoutes
import com.travelassistant.backend.api.configureRequestCorrelation
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
import com.travelassistant.backend.application.hotel.InMemoryHotelDetailsCache
import com.travelassistant.backend.application.hotel.SemanticHotelSearchScheduler
import com.travelassistant.backend.application.hotel.TwoPassSemanticHotelSearchJob
import com.travelassistant.backend.application.llm.GenerateLlmCandidateUseCase
import com.travelassistant.backend.application.llm.LlmCandidate
import com.travelassistant.backend.application.llm.LlmCandidateRetryPolicy
import com.travelassistant.backend.application.llm.LlmClient
import com.travelassistant.backend.application.llm.LlmClientResponse
import com.travelassistant.backend.application.observability.OperationalComponent
import com.travelassistant.backend.application.observability.OperationalError
import com.travelassistant.backend.application.observability.OperationalEvent
import com.travelassistant.backend.application.observability.OperationalEventName
import com.travelassistant.backend.application.observability.OperationalEventSink
import com.travelassistant.backend.application.observability.OperationalLevel
import com.travelassistant.backend.application.observability.OperationalMetricsExporter
import com.travelassistant.backend.application.observability.OperationalOperation
import com.travelassistant.backend.application.observability.OperationalOutcome
import com.travelassistant.backend.application.observability.ServiceReadiness
import com.travelassistant.backend.application.observability.recordSafely
import com.travelassistant.backend.infrastructure.llm.FakeLlmClient
import com.travelassistant.backend.infrastructure.llm.LlmProviderConfig
import com.travelassistant.backend.infrastructure.llm.LlmProviderFactory
import com.travelassistant.backend.infrastructure.llm.OpenRouterDiagnosticObserver
import com.travelassistant.backend.infrastructure.llm.SafeLlmDiagnosticLogger
import com.travelassistant.backend.infrastructure.llm.createProductionOpenRouterHttpClient
import com.travelassistant.backend.infrastructure.accommodation.AccommodationAnalysisProviderConfig
import com.travelassistant.backend.infrastructure.accommodation.AccommodationAnalysisProviderFactory
import com.travelassistant.backend.infrastructure.observability.JsonOperationalEventSink
import com.travelassistant.backend.infrastructure.observability.CompositeOperationalEventSink
import com.travelassistant.backend.infrastructure.observability.PrometheusOperationalMetrics
import com.travelassistant.backend.infrastructure.provider.HotelOfferProviderFactory
import com.travelassistant.backend.infrastructure.provider.HotelProviderConfig
import com.travelassistant.backend.infrastructure.provider.createProductionHotelsApiHttpClient
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.time.Clock

fun main() {
    embeddedServer(
        factory = Netty,
        host = resolveBackendHost(System.getenv()),
        port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
        module = Application::module,
    ).start(wait = true)
}

internal fun resolveBackendHost(environment: Map<String, String>): String =
    environment[BACKEND_HOST_ENVIRONMENT_KEY]
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: DEFAULT_BACKEND_HOST

fun Application.module() {
    val readiness = ServiceReadiness()
    val metrics = PrometheusOperationalMetrics(readiness)
    val eventSink = CompositeOperationalEventSink(
        JsonOperationalEventSink(),
        metrics,
    )
    try {
        moduleWithProviderConfigs(
            llmProviderConfig = LlmProviderConfig.fromEnvironment(),
            providerConfig = HotelProviderConfig.fromEnvironment(),
            accommodationAnalysisProviderConfig =
                AccommodationAnalysisProviderConfig.fromEnvironment(),
            eventSink = eventSink,
            metricsExporter = metrics,
            readiness = readiness,
        )
        environment.monitor.subscribe(ApplicationStopped) {
            metrics.close()
        }
    } catch (error: Throwable) {
        eventSink.recordSafely(
            OperationalEvent(
                name = OperationalEventName.SERVICE_LIFECYCLE,
                component = OperationalComponent.SERVICE,
                level = OperationalLevel.ERROR,
                operation = OperationalOperation.SERVICE_STARTUP,
                outcome = OperationalOutcome.STARTUP_FAILED,
                error = OperationalError.from(error),
            ),
        )
        metrics.close()
        throw error
    }
}

internal fun Application.moduleWithProviderConfigs(
    llmProviderConfig: LlmProviderConfig = LlmProviderConfig(),
    providerConfig: HotelProviderConfig = HotelProviderConfig(),
    accommodationAnalysisProviderConfig: AccommodationAnalysisProviderConfig =
        AccommodationAnalysisProviderConfig(),
    pendingConfirmationStore: PendingConfirmationStore = InMemoryPendingConfirmationStore(),
    hotelConstraintsStore: AssistantHotelConstraintsStore = InMemoryAssistantHotelConstraintsStore(),
    clock: Clock = Clock.systemUTC(),
    openRouterHttpClientFactory: () -> HttpClient = ::createProductionOpenRouterHttpClient,
    accommodationAnalysisHttpClientFactory: () -> HttpClient =
        ::createProductionOpenRouterHttpClient,
    openRouterDiagnosticObserver: OpenRouterDiagnosticObserver? = null,
    assistantLlmDiagnosticObserver: AssistantLlmDiagnosticObserver? = null,
    realHotelHttpClientFactory: () -> HttpClient = ::createProductionHotelsApiHttpClient,
    eventSink: OperationalEventSink = OperationalEventSink.NONE,
    metricsExporter: OperationalMetricsExporter = OperationalMetricsExporter.NONE,
    readiness: ServiceReadiness = ServiceReadiness(),
) {
    val safeLlmDiagnosticLogger = SafeLlmDiagnosticLogger(eventSink)
    val llmProviderRuntime = LlmProviderFactory.create(
        config = llmProviderConfig,
        fakeClientFactory = ::defaultAssistantLlmClient,
        openRouterHttpClientFactory = openRouterHttpClientFactory,
        openRouterDiagnosticObserver = openRouterDiagnosticObserver ?: safeLlmDiagnosticLogger,
    )
    environment.monitor.subscribe(ApplicationStopped) {
        llmProviderRuntime.close()
    }

    try {
        moduleWithAssistantLlm(
            llmClient = llmProviderRuntime.client,
            llmCandidateRetryPolicy = llmProviderRuntime.candidateRetryPolicy,
            providerConfig = providerConfig,
            accommodationAnalysisProviderConfig = accommodationAnalysisProviderConfig,
            pendingConfirmationStore = pendingConfirmationStore,
            hotelConstraintsStore = hotelConstraintsStore,
            clock = clock,
            assistantLlmDiagnosticObserver =
                assistantLlmDiagnosticObserver ?: safeLlmDiagnosticLogger,
            realHotelHttpClientFactory = realHotelHttpClientFactory,
            accommodationAnalysisHttpClientFactory = accommodationAnalysisHttpClientFactory,
            eventSink = eventSink,
            metricsExporter = metricsExporter,
            readiness = readiness,
        )
        eventSink.recordSafely(
            OperationalEvent(
                name = OperationalEventName.SERVICE_LIFECYCLE,
                component = OperationalComponent.SERVICE,
                operation = OperationalOperation.SERVICE_STARTUP,
                outcome = OperationalOutcome.STARTED,
            ),
        )
        environment.monitor.subscribe(ApplicationStopping) {
            eventSink.recordSafely(
                OperationalEvent(
                    name = OperationalEventName.SERVICE_LIFECYCLE,
                    component = OperationalComponent.SERVICE,
                    operation = OperationalOperation.SERVICE_SHUTDOWN,
                    outcome = OperationalOutcome.STOPPING,
                ),
            )
        }
        environment.monitor.subscribe(ApplicationStopped) {
            eventSink.recordSafely(
                OperationalEvent(
                    name = OperationalEventName.SERVICE_LIFECYCLE,
                    component = OperationalComponent.SERVICE,
                    operation = OperationalOperation.SERVICE_SHUTDOWN,
                    outcome = OperationalOutcome.STOPPED,
                ),
            )
        }
    } catch (error: Throwable) {
        llmProviderRuntime.close()
        throw error
    }
}

internal fun Application.moduleWithAssistantLlm(
    llmClient: LlmClient,
    llmCandidateRetryPolicy: LlmCandidateRetryPolicy = LlmCandidateRetryPolicy.NO_RETRY,
    providerConfig: HotelProviderConfig = HotelProviderConfig(),
    accommodationAnalysisProviderConfig: AccommodationAnalysisProviderConfig =
        AccommodationAnalysisProviderConfig(),
    pendingConfirmationStore: PendingConfirmationStore = InMemoryPendingConfirmationStore(),
    hotelConstraintsStore: AssistantHotelConstraintsStore = InMemoryAssistantHotelConstraintsStore(),
    clock: Clock = Clock.systemUTC(),
    realHotelHttpClientFactory: () -> HttpClient = ::createProductionHotelsApiHttpClient,
    accommodationAnalysisHttpClientFactory: () -> HttpClient =
        ::createProductionOpenRouterHttpClient,
    assistantLlmDiagnosticObserver: AssistantLlmDiagnosticObserver =
        AssistantLlmDiagnosticObserver.NONE,
    eventSink: OperationalEventSink = OperationalEventSink.NONE,
    metricsExporter: OperationalMetricsExporter = OperationalMetricsExporter.NONE,
    readiness: ServiceReadiness = ServiceReadiness(),
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
    val detailsCache = InMemoryHotelDetailsCache()
    val semanticSearchLauncher = SemanticHotelSearchRuntimePolicy.createLauncher(
        hotelProviderMode = providerConfig.mode,
        accommodationAnalysisMode = accommodationAnalysisProviderConfig.mode,
    ) {
        val accommodationAnalysisRuntime = AccommodationAnalysisProviderFactory.create(
            config = accommodationAnalysisProviderConfig,
            openRouterHttpClientFactory = accommodationAnalysisHttpClientFactory,
        )
        environment.monitor.subscribe(ApplicationStopped) {
            accommodationAnalysisRuntime.close()
        }
        SemanticHotelSearchScheduler(
            stateStore = hotelSearchStateStore,
            semanticJob = TwoPassSemanticHotelSearchJob(
                hotelOfferProvider = hotelProviderRuntime.provider,
                hotelDetailsProvider = hotelProviderRuntime.detailsProvider,
                analysisClient = accommodationAnalysisRuntime.client,
                detailsCache = detailsCache,
                eventSink = eventSink,
            ),
            eventSink = eventSink,
        ).also { scheduler ->
            environment.monitor.subscribe(ApplicationStopping) {
                scheduler.close()
            }
        }
    }
    val hotelSearchBoundary = CreateHotelSearchUseCase(
        assistantSessionStateStore = assistantSessionStateStore,
        hotelOfferProvider = hotelProviderRuntime.provider,
        hotelSearchStateStore = hotelSearchStateStore,
        semanticSearchLauncher = semanticSearchLauncher,
        eventSink = eventSink,
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
                eventSink = eventSink,
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
        eventSink = eventSink,
    )

    configureRequestCorrelation()
    configureOperationalHttpEvents(eventSink)
    configureSerialization()
    configureErrorHandling(eventSink)
    configureApiRoutes(
        assistantSessionBoundary = assistantSessionBoundary,
        hotelSearchBoundary = hotelSearchBoundary,
        loadSelectedHotelDetails = LoadSelectedHotelDetailsUseCase(
            resolveSelectedOffer = ResolveSelectedHotelOfferUseCase(hotelSearchStateStore),
            hotelDetailsProvider = hotelProviderRuntime.detailsProvider,
            detailsCache = detailsCache,
            eventSink = eventSink,
        ),
        eventSink = eventSink,
    )
    configureOperationalRoutes(
        readiness = readiness,
        metricsExporter = metricsExporter,
    )
    environment.monitor.subscribe(ApplicationStopping) {
        readiness.markNotReady()
    }
    readiness.markReady()
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

private const val BACKEND_HOST_ENVIRONMENT_KEY = "HOST"
internal const val DEFAULT_BACKEND_HOST = "127.0.0.1"
