# Stage 9.21 — opt-in OpenRouter runtime и QA

## Роль документа

Это итоговый отчет Stage 9.21. Он фиксирует runtime-композицию OpenRouter,
application-owned lifecycle отдельного `HttpClient` и результат контролируемого
QA. Текущий статус проекта задает
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

Подключить существующий `OpenRouterLlmClient` только при явном
`LLM_PROVIDER_MODE=OPENROUTER`, сохранить `FAKE` режимом по умолчанию и
проверить полный путь assistant message до confirmation prompt без вызова
Hotels API.

## Runtime composition

Добавлены:

- `LlmProviderRuntime` с идемпотентным закрытием ресурсов;
- `LlmProviderFactory` с ленивым созданием OpenRouter client только для режима
  `OPENROUTER`;
- отдельный production `HttpClient` на Ktor CIO с `HttpTimeout` и запрещенными
  redirects;
- application composition, которая читает `LlmProviderConfig`, закрывает client
  по `ApplicationStopped` и при ошибке дальнейшей инициализации.

OpenRouter client не используется transport-ом Hotels API. `Authorization`
добавляется только к конкретному OpenRouter request и не настроен глобально на
`HttpClient`.

## Конфигурация

Runtime использует:

- `LLM_PROVIDER_MODE=FAKE|OPENROUTER`;
- `OPENROUTER_API_KEY`;
- `OPENROUTER_MODEL`;
- необязательные `OPENROUTER_BASE_URL` и `OPENROUTER_TIMEOUT_MS`.

`FAKE` остается default. Для `OPENROUTER` неполная конфигурация блокирует
startup до сетевого вызова. Model slug задается только оператором; значение из
`.env.example` является примером для QA, а не default в коде.

## Безопасная диагностика

Первый разрешенный QA-вызов завершился `show_boundary_message` без
`hotelSearchId`. Причина не могла быть восстановлена из прежнего единого
`LlmClientResponse.Failure`.

Stage 9.21a добавил `OpenRouterDiagnosticEvent` и no-op observer по умолчанию.
Диагностика передает только ограниченную категорию и не содержит API key,
prompt, response body, provider message, model или headers.

## Stage 9.21b QA

После Stage 9.21a выполнен ровно один отдельно разрешенный диагностический
QA-вызов:

| Проверка | Результат |
|---|---|
| HTTP status assistant route | `200` |
| `nextAction` | `ask_clarification` |
| Confirmation prompt | Достигнут |
| `hotelSearchId` | Отсутствует |
| Safe diagnostic event | `CANDIDATE_DECODED` |
| Hotels provider mode | `FAKE` |
| Подтверждение пользователем | Не отправлялось |

Автоматический повтор, смена модели и Hotels API call не выполнялись. Локальный
постоянный `OPENROUTER_RUNTIME_QA_ENABLED` после проверки остается `false`.

## Проверки

Пройдены:

- targeted tests `OpenRouterLlmClient`, factory, `HttpClient` policy и runtime
  composition через `MockEngine`;
- полный backend test suite без включенного live QA;
- один opt-in QA test с `--no-daemon --rerun-tasks`;
- `git diff --check`;
- проверка отсутствия `runBlocking` в production;
- secret scan: локальный API key не найден вне игнорируемого `.env`.

## Границы

Stage 9.21 не меняет:

- public API, OpenAPI, frontend или generated clients;
- Hotels API transport/runtime и hotel ranking;
- streaming, plugins, web search или tool calling;
- retries, model fallback или conversation transcript storage;
- durable storage и production observability.

QA подтверждает технический opt-in runtime flow, но не означает production
readiness или готовность к внешним пользователям.

## Verdict

`PASS_OPT_IN_OPENROUTER_RUNTIME_AND_QA`.

Stage 9.21 и диагностический Stage 9.21b завершены. Следующий разрешенный этап —
Stage 9.22: chat-first frontend поверх существующих assistant и hotel-search
routes без прямых вызовов OpenRouter или Hotels API из браузера.
