package com.travelassistant.backend.application.llm

enum class LlmClientRetryableFailureReason {
    EMPTY_RESPONSE,
    CLIENT_FAILURE,
    INVALID_CANDIDATE,
}
