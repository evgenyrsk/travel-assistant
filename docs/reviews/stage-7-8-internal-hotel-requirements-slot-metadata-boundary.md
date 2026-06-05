# Stage 7.8 — Internal Hotel Requirements Slot Metadata Boundary

## 1. Цель Stage 7.8

Реализовать следующий минимальный backend foundation slice: internal hotel requirements slot metadata boundary.

Stage 7.8 должен подготовить небольшую internal process-local модель metadata для будущего сбора hotel requirements внутри local assistant session. Это не requirements extraction, не slot filling, не public API contract, не dynamic clarification, не LLM orchestration, не provider integration и не durable persistence.

Primary roadmap до задачи не называл конкретный следующий шаг как Stage 7.8, а фиксировал, что Stage 7.8+ требуют отдельной явной roadmap-aligned задачи. Текущая задача стала такой явной активацией для bounded Stage 7.8 slice.

## 2. Что было реализовано

- Добавлена internal domain-модель `HotelRequirementsState` с минимальными slot metadata.
- `AssistantSession` расширен internal `hotelRequirementsState`.
- При создании local session инициализируются foundation-only hotel requirement slots.
- Валидный message intake сохраняет `hotelRequirementsState` без extraction, filling или изменения slot statuses.
- Successful public response shape для session creation и message intake не расширялся.
- `assistantReply` остается deterministic/static placeholder-only.
- Добавлены use-case и route tests для initialization, internal state preservation и отсутствия slot metadata в public responses.

## 3. Созданные файлы

- `services/backend/src/main/kotlin/com/travelassistant/backend/domain/assistant/HotelRequirementsState.kt`
- `docs/reviews/stage-7-8-internal-hotel-requirements-slot-metadata-boundary.md`

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

Дополнительно созданная local session получает internal `hotelRequirementsState` snapshot в process-local memory.

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

Response не раскрывает internal `hotelRequirementsState`, slot metadata или будущие requirements fields.

## 6. Internal requirements/slot metadata behavior

Internal `hotelRequirementsState` хранит только foundation metadata:

- `destination` — `requiredForHotelSearch = true`, `status = missing`, `order = 1`;
- `stay_dates` — `requiredForHotelSearch = true`, `status = missing`, `order = 2`;
- `guests` — `requiredForHotelSearch = true`, `status = missing`, `order = 3`;
- `preferences` — `requiredForHotelSearch = false`, `status = unknown`, `order = 4`;
- `createdAt`;
- `updatedAt`.

Slot values не хранятся. Message text не анализируется и не копируется в state. Валидный message intake обновляет только `clarificationState`, а `hotelRequirementsState` остается неизменным foundation snapshot.

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
- Reply не использует slot metadata для dynamic question selection.
- Reply не извлекает destination, stay dates, guests, preferences или другие requirements.
- Reply не вызывает LLM, provider, DB, Redis или external service.

## 9. Архитектурные границы

- Backend stack остается Kotlin + Ktor.
- Ktor route остается thin layer над application boundary.
- Domain/application code не зависит от Ktor, DB, Redis, provider SDK, LLM SDK или frontend tooling.
- `hotelRequirementsState` является internal process-local foundation metadata, а не final public API contract.
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

- `hotelRequirementsState` exists only in process memory.
- `hotelRequirementsState` is lost on application restart.
- `hotelRequirementsState` is not shared across multiple backend instances.
- `hotelRequirementsState` has no user/account ownership model.
- Slot statuses are foundation-only metadata and not final domain/API semantics.
- Slot values are not represented or stored.
- Message content is not persisted.
- `assistantReply` remains static and does not inspect message content, clarification metadata or slot metadata.

## 14. Recommended next task

Следующая задача должна быть отдельной roadmap-aligned Stage 7 task. Хороший bounded next step: review/quality gate для Stage 7.8 или следующий минимальный clarification/requirements planning slice, если он будет явно активирован, без DB/storage, real provider integration, frontend/generated clients или Stage 8+ work.

## 15. Scope control confirmation

- Stage 7.8 выполнен как один bounded backend foundation slice.
- Stage 7.9+ не активированы.
- Roadmap order не изменен.
- Product baseline, architecture baseline, OpenAPI draft и ADR не изменялись.
- Public API response shape не расширялся.
- Requirements extraction, slot filling, real integrations, durable storage, frontend и broad documentation cleanup не выполнялись.
