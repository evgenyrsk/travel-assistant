# Stage 7.9 — Internal Slot Coverage / Clarification Planning Boundary

## 1. Цель Stage 7.9

Реализовать следующий минимальный backend foundation slice: internal slot coverage / clarification planning boundary.

Stage 7.9 должен добавить небольшую deterministic internal planning boundary, которая читает `hotelRequirementsState` и вычисляет coverage metadata для будущего clarification flow. Это не requirements extraction, не slot filling, не dynamic clarification, не public API contract, не LLM orchestration, не provider integration и не hotel search.

Primary roadmap до задачи не называл конкретный следующий шаг как Stage 7.9, а фиксировал, что Stage 7.9+ требуют отдельной явной roadmap-aligned задачи. Текущая задача стала такой явной активацией для bounded Stage 7.9 slice.

## 2. Что было реализовано

- Добавлена internal domain-модель `HotelRequirementsCoveragePlan`.
- Добавлен deterministic `HotelRequirementsCoveragePlanner`.
- `AssistantSession` расширен internal `hotelRequirementsCoveragePlan`.
- При создании local session coverage plan вычисляется из foundation-only `hotelRequirementsState`.
- При валидном message intake coverage plan пересчитывается из сохраненного `hotelRequirementsState` без extraction, filling или анализа message text.
- Successful public response shape для session creation и message intake не расширялся.
- `assistantReply` остается deterministic/static placeholder-only.
- Добавлены domain/use-case/route tests для slot coverage planning, non-extraction behavior и отсутствия public API exposure.

## 3. Созданные файлы

- `services/backend/src/main/kotlin/com/travelassistant/backend/domain/assistant/HotelRequirementsCoveragePlanner.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/domain/assistant/HotelRequirementsCoveragePlannerTest.kt`
- `docs/reviews/stage-7-9-internal-slot-coverage-clarification-planning-boundary.md`

## 4. Изменённые файлы

- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/reviews/README.md`
- `services/backend/README.md`
- `services/backend/src/main/kotlin/com/travelassistant/backend/domain/assistant/AssistantSession.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantSessionBoundary.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantSessionStateStore.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/api/AssistantSessionRoutesTest.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/CreateAssistantSessionUseCaseTest.kt`

## 5. Endpoint behavior

`POST /api/v1/assistant/sessions` продолжает возвращать existing `201 Created` response shape:

```json
{
  "sessionId": "assistant-session-local-000001",
  "status": "collecting_requirements",
  "createdAt": "2026-06-04T00:00:00Z"
}
```

Дополнительно созданная local session получает internal `hotelRequirementsCoveragePlan` snapshot в process-local memory.

`POST /api/v1/assistant/sessions/{sessionId}/messages` для существующей local session продолжает возвращать existing success response shape:

```json
{
  "sessionId": "assistant-session-local-000001",
  "status": "collecting_requirements",
  "receivedAt": "2026-06-04T00:00:00Z",
  "assistantReply": {
    "replyType": "clarification",
    "message": "I received your hotel request. Please share destination, dates, guests, and budget so I can continue."
  }
}
```

Response не раскрывает internal `hotelRequirementsState`, `hotelRequirementsCoveragePlan`, slot metadata или future clarification planning fields.

## 6. Internal slot coverage / planning behavior

Internal `hotelRequirementsCoveragePlan` хранит только deterministic planning metadata:

- `requiredSlotCount`;
- `missingRequiredSlotCount`;
- `missingRequiredSlotKeys`;
- `optionalSlotKeys`;
- `nextMissingRequiredSlotKey`;
- `requiredHotelSearchInputsComplete`.

Planner читает только `HotelRequirementsState.slots`, сортирует slots по `order` и считает required slots complete только если required slot имеет `status = COLLECTED`.

Для foundation state Stage 7.8 plan показывает:

- required slots: `destination`, `stay_dates`, `guests`;
- missing required slots: `destination`, `stay_dates`, `guests`;
- optional slots: `preferences`;
- next missing required slot: `destination`;
- required hotel search inputs are not complete.

Test-only state с required slots в `COLLECTED` подтверждает, что optional `preferences` не блокирует required completion.

## 7. Error behavior

Existing structured error behavior сохранен:

- unknown `sessionId` возвращает `404 Not Found` с `code = SESSION_NOT_FOUND`;
- missing или blank `message` возвращает `400 Bad Request` с `code = VALIDATION_ERROR`;
- validation still runs before session lookup for missing/blank message.

Global error taxonomy не redesign'ился.

## 8. Assistant reply behavior

- `assistantReply.replyType` остается `clarification`.
- Reply text остается deterministic/static placeholder.
- Reply не зависит от текста user message.
- Reply не использует slot coverage plan для dynamic question selection.
- Reply не извлекает destination, stay dates, guests, preferences или другие requirements.
- Reply не вызывает LLM, provider, DB, Redis или external service.

## 9. Архитектурные границы

- Backend stack остается Kotlin + Ktor.
- Ktor route остается thin layer над application boundary.
- Domain/application code не зависит от Ktor, DB, Redis, provider SDK, LLM SDK или frontend tooling.
- `hotelRequirementsCoveragePlan` является internal process-local foundation metadata, а не final public API contract.
- `HotelRequirementsCoveragePlanner` является deterministic local planner, а не production state machine, orchestrator или clarification generator.
- `AssistantSessionStateStore` остается process-local/foundation-only boundary, а не production repository или durable storage contract.
- Durable persistence, multi-instance correctness и user/account ownership не подразумеваются.

## 10. Что осталось placeholder/future boundary

- Real requirements extraction.
- Slot filling и хранение extracted values.
- Real stateful clarification flow.
- Dynamic clarification question generation.
- Intent classification.
- Message history.
- Durable persistence, DB/storage и Redis/cache.
- Session retrieval/listing endpoints.
- LLM orchestration и `LlmClient`.
- Hotel search behavior, ranking, shortlist, explanations и comparison.
- Provider integration и provider mapping.
- Frontend/generated clients.

## 11. Что намеренно не реализовывалось

- DB migrations, entities, repositories или durable persistence.
- Redis/cache.
- Session retrieval/listing endpoints.
- Message persistence или message history.
- Dynamic assistant replies.
- Requirements extraction.
- Slot filling.
- Intent classification.
- LLM provider integration или `LlmClient`.
- Hotel provider integration.
- Hotel search, ranking, shortlist, explanations или comparison.
- Frontend и generated clients.
- OpenAPI generation или OpenAPI draft changes.
- Authentication/account flows.
- Booking, payment, flights или combined itinerary.
- Docker/deployment infrastructure.
- Product baseline или architecture baseline rewrite.

## 12. Проверки

- `git status --short` — passed, worktree был чистым перед изменениями.
- `git diff --check` — passed.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` из `services/backend` — passed.

## 13. Known limitations

- `hotelRequirementsCoveragePlan` exists only in process memory.
- `hotelRequirementsCoveragePlan` is lost on application restart.
- `hotelRequirementsCoveragePlan` is not shared across multiple backend instances.
- `hotelRequirementsCoveragePlan` has no user/account ownership model.
- Slot coverage semantics are foundation-only metadata and not final domain/API semantics.
- `COLLECTED` is used only by deterministic planner tests and future-facing internal planning semantics; Stage 7.9 does not collect values.
- Slot values are not represented or stored.
- Message content is not persisted or analyzed.
- `assistantReply` remains static and does not inspect message content, clarification metadata, slot metadata or coverage metadata.

## 14. Recommended next task

Следующая задача должна быть отдельной roadmap-aligned Stage 7 task. Хороший bounded next step: review/quality gate для Stage 7.9 или следующий минимальный clarification/requirements planning slice, если он будет явно активирован, без DB/storage, real provider integration, frontend/generated clients или Stage 8+ work.

## 15. Scope control confirmation

- Stage 7.9 выполнен как один bounded backend foundation slice.
- Stage 7.10+ не активированы.
- Roadmap order не изменен.
- Product baseline, architecture baseline, OpenAPI draft и ADR не изменялись.
- Public API response shape не расширялся.
- Requirements extraction, slot filling, dynamic clarification, real integrations, durable storage, frontend и broad documentation cleanup не выполнялись.
