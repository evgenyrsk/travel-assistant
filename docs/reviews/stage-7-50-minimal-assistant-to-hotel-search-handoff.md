# Stage 7.50 — Минимальная передача от Assistant к поиску отелей

## 1. Verdict

Passed — минимальная передача от Assistant к hotel search добавлена.

Stage 7.50 связывает существующий Assistant message endpoint с process-local hotel search из Stage 7.48 и deterministic ranking из Stage 7.49 без LLM, real provider или frontend.

## 2. Объём этапа

Реализовано:

- strict deterministic parser для явного `hotel-search; ...` message format;
- application-layer decorator над существующим `AssistantSessionBoundary`;
- создание hotel search через существующий `HotelSearchBoundary`;
- `nextAction = show_hotel_results` и optional `hotelSearchId` после успешного handoff;
- сохранение прежнего clarification behavior для ordinary messages;
- отдельный ответ для неполного explicit format без создания search;
- targeted parser, application и API tests.

Не реализованы natural-language intent extraction, stateful conversational planning, LLM orchestration, real provider integration, frontend, generated clients, manifest expansion, durable storage, auth и production hardening.

## 3. Краткое описание реализации

| Область | Файлы | Решение |
|---|---|---|
| Parser | `application/assistant/MinimalHotelSearchMessageParser.kt` | Поддерживает только documented explicit key/value format и возвращает complete/incomplete/not-requested result |
| Handoff orchestration | `application/assistant/AssistantHotelSearchHandoffUseCase.kt` | Делегирует обычный intake существующему boundary и вызывает `HotelSearchBoundary` только для complete parse result |
| Assistant result | `application/assistant/AssistantSessionBoundary.kt`, `AssistantNextAction.kt` | Accepted result содержит selected `nextAction` и optional `HotelSearchId` |
| API response | `api/AssistantPlaceholderRoutes.kt` | Возвращает optional `hotelSearchId` только для созданного search |
| Composition root | `Application.kt` | Assistant handoff и direct hotel-search routes используют один `HotelSearchBoundary` и общие process-local stores |

Route не содержит parser или search orchestration logic. Fake provider и ranking policy не менялись.

## 4. Assistant behavior

Поддерживаемый минимальный format:

```text
hotel-search; destination=Rome; check-in=2026-07-01; check-out=2026-07-04; adults=2; rooms=1
```

`children` является optional non-negative integer. Dates используют ISO-8601; `check-out` должен быть позже `check-in`; `adults` и `rooms` должны быть положительными.

Для complete message Assistant response возвращает:

- обычный `assistantMessage` с текстом `Hotel search created. Ranked offers are ready.`;
- `nextAction = show_hotel_results`;
- opaque `hotelSearchId`.

Неполный explicit format не создает search, возвращает format guidance и `nextAction = ask_clarification`. Ordinary messages не проходят parser и сохраняют прежний placeholder clarification response. Internal requirement slots по-прежнему не заполняются из natural language.

## 5. Hotel search behavior

`AssistantHotelSearchHandoffUseCase` преобразует complete parse result в `CreateHotelSearchCommand` и вызывает существующий `HotelSearchBoundary`. Search использует ту же process-local session, `FakeHotelOfferProvider`, `InMemoryHotelSearchStateStore` и `HotelOfferRanker`, что и direct hotel-search endpoint.

Offers читаются через:

```text
GET /api/v1/hotel-searches/{hotelSearchId}/offers
```

Stage 7.49 ranking сохраняется: API test подтверждает, что первым возвращается available offer и присутствует deterministic `matchSummary`.

## 6. API / OpenAPI

OpenAPI изменен минимально: в `AssistantMessageResponse` добавлен optional opaque `hotelSearchId`.

Существующий `nextAction = show_hotel_results` описывает действие, но не идентифицирует уже созданный resource. Future-only `hotelSearchRequest` также не подходит, потому что Stage 7.50 создает search внутри backend, а не просит client повторно отправить request.

Другие Assistant/hotel-search schemas не менялись. Generated clients не генерировались; generated-client readiness, manifest expansion и OpenAPI finalization не заявлены.

## 7. Tests

Добавлены:

- `MinimalHotelSearchMessageParserTest.kt`:
  - complete explicit message;
  - incomplete message;
  - invalid optional `children`;
  - ordinary message;
- `AssistantHotelSearchHandoffUseCaseTest.kt`:
  - complete message создает search и возвращает ranked offers;
  - incomplete message не создает search.

Обновлены:

- `AssistantSessionRoutesTest.kt` — полный API flow от Assistant message до `GET` ranked offers;
- `CreateAssistantSessionUseCaseTest.kt` — подтверждает прежний `ask_clarification` result без `hotelSearchId`.

Существующие Assistant runtime contract tests и Stage 7.48-7.49 hotel search/ranking tests проходят без изменения expected error behavior.

## 8. Явно вне этапа

- real hotel provider/API integration;
- external HTTP/network calls;
- frontend implementation;
- generated clients;
- manifest changes или expansion;
- CI/Gradle integration;
- conformance tool changes;
- LLM orchestration;
- natural-language intent parser;
- полноценный conversational planning;
- durable DB/storage;
- authentication/authorization;
- production readiness;
- финальная готовность Stage 7.

## 9. Оставшаяся работа Stage 7

- минимальный frontend-сценарий поверх Assistant handoff и ranked offers;
- итоговая сверка hotel-only MVP slice;
- generated clients и manifest expansion остаются отложенными до отдельного фактического решения;
- richer natural-language extraction, LLM orchestration и real provider остаются вне текущего этапа.

## 10. Проверки

| Command | Result |
|---|---|
| `git status --short` перед изменениями | Passed; working tree clean. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test --no-daemon` из `services/backend` | Passed; backend build и tests successful. |
| `./tools/openapi-conformance/check` | Passed; OpenAPI parsed, `blockingFindings: []`, `status: "not_ready"`, `readinessClaim: false`. |
| `git diff --check` | Passed; whitespace errors отсутствуют. |
| Targeted link/status search | Passed; report зарегистрирован в reviews index и primary roadmap. |
| Final diff scope inspection | Passed; frontend, generated clients, manifest, Gradle/CI и conformance tool не менялись. |

Backend server не запускался. External HTTP/network calls не выполнялись.
