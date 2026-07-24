# Stage 9.21a — безопасная диагностика отказов OpenRouter

## Роль документа

Это отчет о реализации Stage 9.21a. Он фиксирует внутреннюю категоризацию
неуспешных результатов OpenRouter после первого QA-вызова Stage 9.21. Текущий
статус проекта задает [`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Исходная точка

Первый разрешенный QA-вызов 19 июля 2026 года дошел до OpenRouter runtime, но
не дошел до confirmation prompt:

- публичный route вернул `200` и безопасный `nextAction=show_boundary_message`;
- `hotelSearchId` отсутствовал;
- Hotels API не вызывался;
- автоматический повтор не выполнялся;
- API key не попал в test report или tracked files.

До Stage 9.21a все HTTP-ошибки, некорректные wire-ответы и ошибки candidate
преобразовывались в один
`LlmClientResponse.Failure`. Поэтому сохраненного результата недостаточно,
чтобы ретроспективно определить точную причину первого отказа.

## Подтвержденный внешний контракт

Официальная документация OpenRouter разделяет:

- ошибки запроса с non-2xx HTTP status;
- ошибки после начала generation, которые могут прийти с HTTP `200` в
  `error` envelope или choice с `finish_reason=error`;
- стабильное поле `error.metadata.error_type` для безопасной машинной
  классификации.

Источники:

- [Errors and Debugging](https://openrouter.ai/docs/api/reference/errors-and-debugging);
- [Structured Outputs](https://openrouter.ai/docs/guides/features/structured-outputs);
- [DeepSeek V4 Flash](https://openrouter.ai/deepseek/deepseek-v4-flash).

## Реализация

Добавлены internal types:

- `OpenRouterDiagnosticEvent`;
- `OpenRouterDiagnosticObserver` с no-op реализацией по умолчанию.

Adapter сообщает только разрешенную категорию:

| Группа | Категории |
|---|---|
| Успешный adapter mapping | `CANDIDATE_DECODED` |
| HTTP/auth | `REQUEST_REJECTED`, `AUTHENTICATION_FAILED`, `INSUFFICIENT_CREDITS`, `RATE_LIMITED`, `HTTP_FAILURE` |
| Доступность | `TIMEOUT`, `PROVIDER_UNAVAILABLE`, `NETWORK_FAILURE` |
| Wire response | `IN_BAND_PROVIDER_ERROR`, `NON_JSON_RESPONSE`, `MALFORMED_RESPONSE` |
| Content | `EMPTY_CHOICES`, `EMPTY_CONTENT`, `INVALID_CANDIDATE` |
| Защитный fallback | `UNKNOWN_FAILURE` |

Top-level `error` и choice-level `error` распознаются без чтения или сохранения
`message`. Из `metadata` используется только известный `error_type`; неизвестные
значения не переносятся дальше.

Observer передается через `LlmProviderFactory` и runtime composition как
internal test/QA seam. По умолчанию он ничего не записывает. Public
`LlmClientResponse`, assistant routes и пользовательский fallback не изменены.

`CANDIDATE_DECODED` позволяет отличить transport/wire failure от последующего
application validation или decision fallback, не раскрывая поля candidate.

## Граница конфиденциальности

Диагностика не содержит и не сохраняет:

- API key и `Authorization`;
- prompt, user message или confirmed constraints;
- response body и provider message;
- model slug, URL, headers или routing metadata.

Observer error не меняет основное поведение adapter. `CancellationException`
по-прежнему пробрасывается.

## Проверки

Через `MockEngine` подтверждены:

- категории request/auth/credits/rate-limit/unavailable;
- in-band timeout error;
- non-JSON, malformed, empty и invalid candidate outcomes;
- network failure без exception details;
- передача категории через runtime composition;
- успешный candidate mapping и невлияние ошибки observer на основной result;
- прежний безопасный `show_boundary_message` без `hotelSearchId` и внутренних
  данных.

Targeted tests и полный backend test suite прошли. Повторный live-вызов в
Stage 9.21a не выполнялся.

## Границы

Stage 9.21a не добавляет:

- новый live call, retry или смену модели;
- логи raw provider data;
- изменения public API, OpenAPI или frontend;
- Hotels API call, streaming, plugins, tools или web search;
- production observability backend.

## Verdict

`PASS_SAFE_OPENROUTER_FAILURE_DIAGNOSTICS`.

Stage 9.21 остается открытым: первый QA-вызов не подтвердил confirmation flow.
Следующий разрешенный шаг — Stage 9.21b, один отдельно разрешенный QA-повтор с
тем же fail-closed сценарием. При отказе допускается сообщить только
`OpenRouterDiagnosticEvent`; автоматический retry и смена model запрещены.
