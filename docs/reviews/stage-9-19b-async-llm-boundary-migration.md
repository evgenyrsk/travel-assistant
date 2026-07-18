# Stage 9.19b — асинхронная LLM-граница

## Роль документа

Это отчет о реализации Stage 9.19b. Он фиксирует перевод внутренней LLM-цепочки
на `suspend`, правила обработки cancellation и результаты проверок. Текущий
статус проекта задает
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

Подготовить provider-independent LLM boundary к будущему внешнему I/O без
блокировки coroutine thread и без временного параллельного API. Поведение
`FakeLlmClient`, assistant flow и публичные HTTP-контракты должны остаться
неизменными.

## Реализация

Сквозная application-цепочка переведена на `suspend`:

- `LlmClient.generateCandidate()`;
- `GenerateLlmCandidateUseCase.invoke()`;
- generation-step и `invoke()` в `PlanAssistantLlmDecisionUseCase`;
- внутренний `AssistantLlmRouteWiringUseCase.withLlmDecision()`.

`AssistantSessionBoundary.acceptUserMessage()` уже был асинхронным, поэтому
Ktor routes и public response mapping не потребовали изменений.

`FakeLlmClient` реализует новую сигнатуру и по-прежнему возвращает заданный
результат детерминированно. Runtime composition продолжает использовать
существующий `LlmClient` и не подключает реального LLM provider.

## Политика ошибок и cancellation

- `CancellationException` пробрасывается через generation и decision planning
  без преобразования в fallback.
- Другой неожиданный `RuntimeException` от `LlmClient` преобразуется в
  существующий безопасный `LlmClientResponse.Failure`.
- Неожиданная ошибка decision planning по-прежнему дает
  `AssistantCandidateDecision.Fallback` с причиной `CLIENT_FAILURE`.
- `runBlocking` используется только в тестах; production-код его не содержит.

## Совместимость

Не изменены:

- public API, OpenAPI, frontend и generated clients;
- assistant `nextAction`, тексты текущих ответов и confirmation lifecycle;
- накопление hotel constraints из Stage 9.19a;
- Hotels API transport, provider DTO, mapping, ranking и runtime mode;
- `FAKE` как LLM/provider mode по умолчанию.

## Проверки

Точечные тесты покрывают:

- прежние candidate, empty и failure outcomes;
- безопасный fallback при неожиданном `RuntimeException`;
- проброс `CancellationException` через оба application use case;
- детерминированность `FakeLlmClient`;
- отсутствие регрессий в assistant context accumulation и routes.

Также выполнены полный backend test suite, `git diff --check` и отдельная
проверка отсутствия `runBlocking` в `src/main/kotlin`.

## Границы и риски

Этап не добавляет OpenRouter adapter, HTTP client, конфигурацию реального LLM,
network calls, API keys или runtime wiring. До следующего отдельного этапа
backend продолжает использовать `FakeLlmClient`.

Асинхронная сигнатура лишь подготавливает корректную I/O-границу и сама по себе
не подтверждает качество, доступность или безопасность внешнего LLM provider.

## Verdict

`PASS_ASYNC_LLM_BOUNDARY_MIGRATION`.

Stage 9.19b завершен. Следующий разрешенный этап — Stage 9.20: изолированный
OpenRouter adapter с проверками через `MockEngine`, без runtime wiring и live
calls.
