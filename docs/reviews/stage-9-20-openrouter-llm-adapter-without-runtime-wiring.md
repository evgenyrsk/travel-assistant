# Stage 9.20 — OpenRouter LLM adapter без runtime wiring

## Роль документа

Это отчет о реализации Stage 9.20. Он фиксирует изолированный OpenRouter
adapter для существующего `LlmClient`, typed configuration и результаты
проверок через `MockEngine`. Текущий статус проекта задает
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

Проверить внешнюю LLM-интеграцию на уровне infrastructure boundary без
активации в runtime, live calls и изменения публичных контрактов Travel
Assistant.

## Подтвержденный wire contract

По официальной документации OpenRouter на дату этапа используются:

- `POST /api/v1/chat/completions` и `Authorization: Bearer <api-key>`;
- нестриминговый ответ через `stream=false`;
- strict structured output через `response_format.type=json_schema`;
- `provider.require_parameters=true`, чтобы выбранный upstream provider
  поддерживал переданные параметры.

Источники:

- [OpenRouter API Reference](https://openrouter.ai/docs/api_reference/overview);
- [Structured Outputs](https://openrouter.ai/docs/guides/features/structured-outputs);
- [Provider Routing](https://openrouter.ai/docs/guides/routing/provider-selection).

## Конфигурация

Добавлены internal configuration types:

- `LlmProviderMode` со значениями `FAKE` и `OPENROUTER`;
- `LlmProviderConfig` с `FAKE` по умолчанию;
- `OpenRouterConfig` с обязательными model и API key;
- `OpenRouterApiKey`, скрывающий значение в `toString()`;
- fail-closed `LlmProviderConfigurationException` без значения secret.

Поддержаны ключи environment:

- `LLM_PROVIDER_MODE`;
- `OPENROUTER_API_KEY`;
- `OPENROUTER_MODEL`;
- необязательные `OPENROUTER_BASE_URL` и `OPENROUTER_TIMEOUT_MS`.

Значения по умолчанию: `https://openrouter.ai/api/v1/` и 30 секунд. Base URL
обязан быть абсолютным HTTPS URI без credentials, query и fragment.

`Application.kt` не читает эту конфигурацию на Stage 9.20. Наличие mode
`OPENROUTER` в typed config не означает runtime activation.

## Adapter и request policy

`OpenRouterLlmClient` получает `HttpClient` и `OpenRouterConfig` через
конструктор. Lifecycle клиента остается обязанностью будущей runtime
composition.

Каждый вызов формирует один JSON request:

- model берется только из configuration;
- messages содержат system instruction и JSON payload с `userMessage`,
  `confirmedConstraints` и `missingRequiredFields`;
- `temperature=0`, `stream=false`;
- strict JSON Schema ограничивает candidate текущими `LlmCandidate` enums и
  каноническими hotel constraint keys;
- plugins, web search, tools и tool calling отсутствуют;
- optional attribution, cookies и session headers не добавляются.

## Response и failure policy

Первый non-streaming choice преобразуется в текущий `LlmCandidate` только при
валидной JSON-оболочке, content и enum values.

- пустой choices или пустой content → `LlmClientResponse.Empty`;
- HTTP failure, неверный content type, malformed JSON, provider error или
  transport failure → `LlmClientResponse.Failure`;
- `CancellationException` всегда пробрасывается;
- response body, prompt, URL, API key и provider error details не попадают в
  exception или application result.

Semantic consistency candidate по-прежнему проверяет существующий
`LlmCandidateValidator` в application layer.

## Проверки

Targeted tests через `MockEngine` покрывают:

- endpoint, Bearer header, media headers и точный request policy;
- strict schema, `require_parameters`, отсутствие plugins/tools;
- передачу накопленного контекста без изменения;
- candidate, empty и failure response mapping;
- invalid enum, malformed JSON, HTTP failures и non-JSON response;
- timeout, network failure и coroutine cancellation;
- fail-closed configuration и redaction API key.

Также выполнены полный backend test suite, `git diff --check` и scope checks.

## Границы и риски

Не добавлены:

- изменения `Application.kt` и runtime factory;
- production `HttpClient` для OpenRouter или его lifecycle;
- live calls, API key в repository и model slug в коде;
- public API, OpenAPI, frontend или generated clients;
- streaming, plugins, web search, tools или conversation transcript storage.

Поддержка structured outputs зависит от выбранной operator model. Фактическую
совместимость model следует проверить перед отдельно разрешенным QA call.

## Verdict

`PASS_OPENROUTER_ADAPTER_WITHOUT_RUNTIME_WIRING`.

Stage 9.20 завершен. Следующий разрешенный этап — Stage 9.21: opt-in OpenRouter
runtime composition с отдельным application-owned `HttpClient`, fail-closed
configuration и одним отдельно разрешенным QA call.
