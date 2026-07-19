package com.travelassistant.backend

import com.travelassistant.backend.infrastructure.llm.LlmProviderConfig
import com.travelassistant.backend.infrastructure.llm.LlmProviderMode
import com.travelassistant.backend.infrastructure.llm.OpenRouterDiagnosticEvent
import com.travelassistant.backend.infrastructure.llm.OpenRouterDiagnosticObserver
import com.travelassistant.backend.infrastructure.provider.HotelProviderConfig
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.fail

class OpenRouterRuntimeQaCallTest {

    @Test
    fun `runs one OpenRouter runtime smoke only with explicit opt in`() {
        val environment = System.getenv()
        if (environment[ENABLED_KEY] != ENABLED_VALUE) {
            return
        }

        val llmProviderConfig = LlmProviderConfig.fromEnvironment(environment)
        val openRouterConfig = llmProviderConfig.openRouter
        if (
            llmProviderConfig.mode != LlmProviderMode.OPENROUTER ||
            openRouterConfig == null ||
            openRouterConfig.model != EXPECTED_MODEL ||
            openRouterConfig.baseUrl.trimEnd('/') != EXPECTED_BASE_URL
        ) {
            fail(
                "OpenRouter runtime QA configuration rejected safely: " +
                    "mode=${llmProviderConfig.mode}, " +
                    "expectedModelConfigured=${openRouterConfig?.model == EXPECTED_MODEL}, " +
                    "expectedBaseUrlConfigured=" +
                    "${openRouterConfig?.baseUrl?.trimEnd('/') == EXPECTED_BASE_URL}",
            )
        }

        testApplication {
            val diagnosticEvents = mutableListOf<OpenRouterDiagnosticEvent>()
            application {
                moduleWithProviderConfigs(
                    llmProviderConfig = llmProviderConfig,
                    providerConfig = HotelProviderConfig(),
                    openRouterDiagnosticObserver = OpenRouterDiagnosticObserver { event ->
                        diagnosticEvents += event
                    },
                )
            }

            val sessionResponse = client.post("/api/v1/assistant/sessions")
            val sessionId = Json.parseToJsonElement(sessionResponse.bodyAsText())
                .jsonObject
                .getValue("session")
                .jsonObject
                .getValue("sessionId")
                .jsonPrimitive
                .content
            val assistantResponse = client.post("/api/v1/assistant/sessions/$sessionId/messages") {
                headers.append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    buildJsonObject {
                        put("message", COMPLETE_HOTEL_REQUEST)
                    }.toString(),
                )
            }
            val responseBody = Json.parseToJsonElement(assistantResponse.bodyAsText()).jsonObject
            val nextAction = responseBody["nextAction"]?.jsonPrimitive?.content
            val assistantMessage = responseBody["assistantMessage"]
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.content
                .orEmpty()
            val confirmationReached = assistantMessage.contains(CONFIRMATION_QUESTION)
            val hasHotelSearchId = responseBody.containsKey("hotelSearchId")

            if (
                assistantResponse.status != HttpStatusCode.OK ||
                nextAction != "ask_clarification" ||
                !confirmationReached ||
                hasHotelSearchId
            ) {
                fail(
                    "OpenRouter runtime QA failed safely: " +
                        "status=${assistantResponse.status.value}, " +
                        "nextAction=${nextAction ?: "none"}, " +
                        "confirmationReached=$confirmationReached, " +
                        "hasHotelSearchId=$hasHotelSearchId, " +
                        "diagnosticEvents=${diagnosticEvents.safeNames()}",
                )
            }

            println(
                "STAGE_9_21_SAFE_RESULT " +
                    "status=${assistantResponse.status.value} " +
                    "nextAction=$nextAction " +
                    "confirmationReached=$confirmationReached " +
                    "hasHotelSearchId=$hasHotelSearchId " +
                    "diagnosticEvents=${diagnosticEvents.safeNames()}",
            )
        }
    }

    private fun List<OpenRouterDiagnosticEvent>.safeNames(): String =
        if (isEmpty()) "NONE" else joinToString(separator = ",", transform = Enum<*>::name)

    private companion object {
        const val ENABLED_KEY = "OPENROUTER_RUNTIME_QA_ENABLED"
        const val ENABLED_VALUE = "true"
        const val EXPECTED_MODEL = "deepseek/deepseek-v4-flash"
        const val EXPECTED_BASE_URL = "https://openrouter.ai/api/v1"
        const val COMPLETE_HOTEL_REQUEST =
            "Найди отель в Казани с 10 по 14 августа 2026 года для двух взрослых без детей, одна комната"
        const val CONFIRMATION_QUESTION = "Проверить отели по этим параметрам?"
    }
}
