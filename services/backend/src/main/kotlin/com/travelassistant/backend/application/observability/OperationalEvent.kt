package com.travelassistant.backend.application.observability

data class OperationalEvent(
    val name: OperationalEventName,
    val component: OperationalComponent,
    val level: OperationalLevel = OperationalLevel.INFO,
    val requestId: String? = null,
    val sessionId: String? = null,
    val hotelSearchId: String? = null,
    val operation: OperationalOperation? = null,
    val method: OperationalHttpMethod? = null,
    val statusCode: Int? = null,
    val dependency: OperationalDependency? = null,
    val outcome: OperationalOutcome? = null,
    val diagnostic: OperationalDiagnostic? = null,
    val durationMillis: Long? = null,
    val offerCount: Int? = null,
    val error: OperationalError? = null,
)

enum class OperationalEventName(val wireValue: String) {
    SERVICE_LIFECYCLE("service.lifecycle"),
    HTTP_REQUEST_STARTED("http.request.started"),
    HTTP_REQUEST_COMPLETED("http.request.completed"),
    ASSISTANT_SESSION_CREATED("assistant.session.created"),
    ASSISTANT_TURN_COMPLETED("assistant.turn.completed"),
    CONFIRMATION_OUTCOME("assistant.confirmation.outcome"),
    HOTEL_SEARCH_COMPLETED("hotel.search.completed"),
    HOTEL_DETAILS_COMPLETED("hotel.details.completed"),
    DEPENDENCY_CALL_COMPLETED("dependency.call.completed"),
    LLM_DIAGNOSTIC("llm.diagnostic"),
    UNEXPECTED_ERROR("error.unhandled"),
}

enum class OperationalComponent(val wireValue: String) {
    SERVICE("service"),
    HTTP("http"),
    ASSISTANT("assistant"),
    HOTEL_SEARCH("hotel_search"),
    HOTEL_DETAILS("hotel_details"),
    LLM("llm"),
    PROVIDER("provider"),
}

enum class OperationalLevel(val wireValue: String) {
    INFO("info"),
    WARNING("warning"),
    ERROR("error"),
}

enum class OperationalOperation(val wireValue: String) {
    SERVICE_STARTUP("service_startup"),
    SERVICE_SHUTDOWN("service_shutdown"),
    LEGACY_HEALTH("legacy_health"),
    LIVENESS("liveness"),
    READINESS("readiness"),
    METRICS("metrics"),
    CREATE_ASSISTANT_SESSION("create_assistant_session"),
    POST_ASSISTANT_MESSAGE("post_assistant_message"),
    READ_SHORTLIST("read_shortlist"),
    UPSERT_SHORTLIST("upsert_shortlist"),
    DELETE_SHORTLIST("delete_shortlist"),
    CREATE_EXPLANATION("create_explanation"),
    CREATE_HOTEL_SEARCH("create_hotel_search"),
    GET_HOTEL_OFFERS("get_hotel_offers"),
    GET_HOTEL_DETAILS("get_hotel_details"),
    GENERATE_LLM_CANDIDATE("generate_llm_candidate"),
    PROVIDER_HOTEL_SEARCH("provider_hotel_search"),
    PROVIDER_HOTEL_DETAILS("provider_hotel_details"),
    UNMATCHED("unmatched"),
}

enum class OperationalHttpMethod(val wireValue: String) {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    OTHER("OTHER"),
}

enum class OperationalDependency(val wireValue: String) {
    LLM("llm"),
    OPENROUTER("openrouter"),
    HOTEL_PROVIDER("hotel_provider"),
}

enum class OperationalOutcome(val wireValue: String) {
    STARTED("started"),
    STARTUP_FAILED("startup_failed"),
    STOPPING("stopping"),
    STOPPED("stopped"),
    SUCCEEDED("succeeded"),
    CREATED("created"),
    CLARIFICATION("clarification"),
    CONFIRMATION_REQUIRED("confirmation_required"),
    CONFIRMED("confirmed"),
    DECLINED("declined"),
    NEEDS_CLARIFICATION("needs_clarification"),
    NEEDS_REPLANNING("needs_replanning"),
    UNKNOWN("unknown"),
    RESULTS("results"),
    NO_OFFERS("no_offers"),
    VALIDATION_ERROR("validation_error"),
    NOT_FOUND("not_found"),
    REQUEST_REJECTED("request_rejected"),
    RESPONSE_REJECTED("response_rejected"),
    UNAVAILABLE("unavailable"),
    TIMEOUT("timeout"),
    RATE_LIMITED("rate_limited"),
    AUTHENTICATION_FAILED("authentication_failed"),
    INSUFFICIENT_CREDITS("insufficient_credits"),
    CLIENT_FAILURE("client_failure"),
    INVALID_CANDIDATE("invalid_candidate"),
    UNSUPPORTED("unsupported"),
    FAILED("failed"),
}

enum class OperationalDiagnostic(val wireValue: String) {
    CANDIDATE_DECODED("candidate_decoded"),
    REQUEST_REJECTED("request_rejected"),
    AUTHENTICATION_FAILED("authentication_failed"),
    INSUFFICIENT_CREDITS("insufficient_credits"),
    TIMEOUT("timeout"),
    RATE_LIMITED("rate_limited"),
    PROVIDER_UNAVAILABLE("provider_unavailable"),
    HTTP_FAILURE("http_failure"),
    IN_BAND_PROVIDER_ERROR("in_band_provider_error"),
    NON_JSON_RESPONSE("non_json_response"),
    MALFORMED_RESPONSE("malformed_response"),
    EMPTY_CHOICES("empty_choices"),
    EMPTY_CONTENT("empty_content"),
    INVALID_CANDIDATE("invalid_candidate"),
    NETWORK_FAILURE("network_failure"),
    UNKNOWN_FAILURE("unknown_failure"),
    DESTINATION_ENRICHED("destination_enriched"),
    PREFERENCE_STARS_ENRICHED("preference_stars_enriched"),
    UNSUPPORTED_ROOM_COUNT("unsupported_room_count"),
    CANDIDATE_EMPTY_RESPONSE("candidate_empty_response"),
    CANDIDATE_CLIENT_FAILURE("candidate_client_failure"),
    CANDIDATE_INVALID("candidate_invalid"),
    CANDIDATE_UNSUPPORTED_INTENT("candidate_unsupported_intent"),
    CANDIDATE_MISSING_CLARIFICATION("candidate_missing_clarification"),
    CONFIRMATION_UNSUPPORTED_INTENT("confirmation_unsupported_intent"),
    CONFIRMATION_UNSAFE_OR_UNSUPPORTED_OUTCOME("confirmation_unsafe_or_unsupported_outcome"),
    CONFIRMATION_CONFLICTS_OR_WARNINGS("confirmation_conflicts_or_warnings"),
}

data class OperationalError(
    val exceptionType: String,
    val causeTypes: List<String>,
    val stackFrames: List<String>,
) {
    companion object {
        fun from(throwable: Throwable): OperationalError {
            val causeTypes = generateSequence(throwable.cause) { cause -> cause.cause }
                .take(MAX_CAUSES)
                .map { cause -> cause.javaClass.name }
                .toList()
            val applicationFrames = throwable.stackTrace
                .asSequence()
                .filter { frame -> frame.className.startsWith(APPLICATION_PACKAGE_PREFIX) }
                .take(MAX_STACK_FRAMES)
                .map(::formatFrame)
                .toList()
            val stackFrames = if (applicationFrames.isNotEmpty()) {
                applicationFrames
            } else {
                throwable.stackTrace
                    .asSequence()
                    .take(MAX_STACK_FRAMES)
                    .map(::formatFrame)
                    .toList()
            }

            return OperationalError(
                exceptionType = throwable.javaClass.name,
                causeTypes = causeTypes,
                stackFrames = stackFrames,
            )
        }

        private fun formatFrame(frame: StackTraceElement): String =
            buildString {
                append(frame.className)
                append('.')
                append(frame.methodName)
                append('(')
                append(frame.fileName ?: "Unknown Source")
                if (frame.lineNumber >= 0) {
                    append(':')
                    append(frame.lineNumber)
                }
                append(')')
            }

        private const val APPLICATION_PACKAGE_PREFIX = "com.travelassistant.backend."
        private const val MAX_CAUSES = 4
        private const val MAX_STACK_FRAMES = 8
    }
}
