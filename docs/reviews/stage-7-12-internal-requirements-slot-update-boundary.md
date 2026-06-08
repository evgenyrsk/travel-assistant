# Stage 7.12 — Internal Requirements Slot Update Boundary

## 1. Цель Stage 7.12

Реализовать минимальный internal application/domain boundary для обновления hotel requirements slots через explicit structured internal input.

Stage 7.12 не является public slot filling, requirements extraction, dynamic clarification, generated-client/API finalization или hotel search behavior. Целью было подготовить безопасную process-local основу, через которую будущая clarification/extraction логика сможет обновлять `hotelRequirementsState`, когда такая логика будет отдельно активирована.

Roadmap до этой задачи не содержал заранее прописанную строку с названием Stage 7.12. Название `Stage 7.12 — Internal Requirements Slot Update Boundary` пришло из явной текущей задачи и было зафиксировано в roadmap как completed bounded Stage 7 slice.

## 2. Проверенные источники

- `AGENTS.md`
- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/product/product-baseline.md`
- `docs/architecture/architecture-baseline.md`
- `docs/reviews/README.md`
- `services/backend/README.md`
- `docs/reviews/stage-7-10-backend-api-contract-alignment-checkpoint.md`
- `docs/reviews/stage-7-11-assistant-api-runtime-contract-alignment-cleanup.md`
- `docs/reviews/stage-7-11-assistant-api-runtime-contract-alignment-cleanup-review.md`
- `docs/architecture/stage-6/openapi-draft.yaml`
- `docs/architecture/stage-6/openapi-contract-notes.md`
- `docs/development/roadmap.md`
- `docs/development/milestones.md`
- `docs/development/implementation-strategy.md`
- `docs/guides/documentation-style-guide.md`
- `docs/PROJECT_BRIEF.md`
- `docs/ARCHITECTURE.md`
- `docs/decisions/README.md`
- `services/backend/src/main/kotlin/**`
- `services/backend/src/test/kotlin/**`
- `git status --short`

## 3. Что было реализовано

- Добавлен internal `HotelRequirementsSlotUpdateBoundary`.
- Добавлен `UpdateHotelRequirementSlotUseCase` для process-local slot status updates.
- Добавлен structured command `UpdateHotelRequirementSlotCommand`:
  - `sessionId`;
  - `slotKey`;
  - `slotStatus`.
- Добавлен explicit internal result type `UpdateHotelRequirementSlotResult`:
  - `Updated`;
  - `SessionNotFound`;
  - `UnknownSlotKey`.
- `HotelRequirementsState` получил доменную операцию обновления status существующего slot.
- `AssistantSession` получил доменную операцию обновления hotel requirements slot с пересчетом coverage plan.
- Добавлены application tests для known slot update, coverage recomputation, deterministic next missing slot, required completion, optional preferences, unknown session и unknown slot key.
- Public assistant routes остались без slot update endpoint и без новых public response fields.
- `services/backend/README.md`, `docs/roadmap/roadmap.md` и `docs/reviews/README.md` обновлены минимально под Stage 7.12.

## 4. Созданные файлы

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/UpdateHotelRequirementSlotUseCase.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/UpdateHotelRequirementSlotUseCaseTest.kt`
- `docs/reviews/stage-7-12-internal-requirements-slot-update-boundary.md`

## 5. Изменённые файлы

- `docs/roadmap/roadmap.md`
- `docs/reviews/README.md`
- `services/backend/README.md`
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantSessionBoundary.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/domain/assistant/AssistantSession.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/domain/assistant/HotelRequirementsState.kt`

## 6. Internal slot update behavior

Internal boundary обновляет только существующие slots текущего process-local `hotelRequirementsState`.

Behavior:

- принимает только explicit structured internal input;
- не читает и не анализирует user message text;
- не извлекает destination, dates, guests или preferences;
- не хранит slot values;
- обновляет только `RequirementSlotStatus`;
- сохраняет обновленную session обратно в process-local `AssistantSessionStateStore`;
- не доступен через public API.

Known required slot может быть отмечен как `COLLECTED` только через явный structured command. Это foundation-only behavior, а не финальный public/domain contract для slot filling.

## 7. Coverage plan behavior

После успешного slot update use case пересчитывает `HotelRequirementsCoveragePlanner.plan(updatedRequirementsState)`.

Проверенное поведение:

- после `destination = COLLECTED` missing required count уменьшается с `3` до `2`;
- `nextMissingRequiredSlotKey` детерминированно меняется с `DESTINATION` на `STAY_DATES`;
- после сбора `destination`, `stay_dates` и `guests` internal coverage становится complete;
- optional `preferences` остается optional и не блокирует required completion.

## 8. Public API behavior

Public API не изменился.

Существующие endpoints остаются прежними:

- `POST /api/v1/assistant/sessions`;
- `POST /api/v1/assistant/sessions/{sessionId}/messages`;
- placeholder hotel search, shortlist и explanation routes.

Stage 7.12 не добавляет public slot update endpoint. Public success responses по-прежнему не возвращают:

- `clarificationState`;
- `hotelRequirementsState`;
- `hotelRequirementsCoveragePlan`;
- slot keys;
- slot statuses;
- coverage metadata.

## 9. Error/result behavior

Для нового internal use case используются explicit result types вместо generic exceptions:

- unknown process-local session возвращает `UpdateHotelRequirementSlotResult.SessionNotFound`;
- unknown slot key возвращает `UpdateHotelRequirementSlotResult.UnknownSlotKey`;
- successful update возвращает `UpdateHotelRequirementSlotResult.Updated`.

Public structured error handling не менялся:

- `SESSION_NOT_FOUND` для public message intake preserved;
- `VALIDATION_ERROR` preserved;
- placeholder `NOT_IMPLEMENTED` preserved;
- generic `NOT_FOUND` preserved.

## 10. Assistant reply / nextAction behavior

`assistantMessage` и `nextAction` не менялись.

- `assistantMessage.content` остается deterministic/static placeholder-only.
- `nextAction` остается static `ask_clarification`.
- Stage 7.12 не вводит dynamic clarification, next-action planning, real assistant state machine или user-facing question generation.

## 11. Архитектурные границы

- Backend stack остается Kotlin + Ktor.
- Новый boundary находится в application/domain layer и не зависит от Ktor.
- Ktor routes не расширялись.
- State остается process-local через injectable `AssistantSessionStateStore`.
- Не добавлены DB, Redis, provider SDK, LLM SDK, frontend tooling или generated-client tooling.
- Slot update работает с internal foundation metadata, а не с final public API/domain contract.

## 12. Что осталось placeholder/future boundary

- Requirements extraction.
- Natural-language slot filling.
- Structured slot values.
- Dynamic clarification questions.
- Real stateful clarification flow.
- Dynamic `nextAction`.
- `hotelSearchRequest`.
- Generated clients.
- Full runtime/OpenAPI conformance.
- Hotel search, offers, ranking, shortlist и explanations.
- Durable persistence, session retrieval/listing и message history.
- LLM orchestration и provider integration.

## 13. Что намеренно не реализовывалось

- Public slot update endpoints.
- User message parsing.
- Requirements extraction.
- Slot filling from text.
- Intent classification.
- Dynamic assistant replies.
- Real clarification flow.
- Message history или сохранение full message text.
- Slot values или extracted requirement values.
- DB/storage, Redis/cache или durable persistence.
- OpenAPI rewrite, OpenAPI generation или generated clients.
- Hotel provider integration.
- Hotel search behavior.
- Ranking, shortlist behavior, explanations/comparison.
- Frontend.
- Booking, payment, flights или combined itinerary.
- Docker/deployment infrastructure.
- Product baseline или architecture baseline rewrite.

## 14. Проверки

- `git status --short` — passed, worktree был чистым перед изменениями.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` из `services/backend` — passed after code changes before documentation/report updates.
- `git diff --check` — passed after report creation.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` из `services/backend` — passed after report creation.

## 15. Known limitations

- Slot update boundary принимает только status, без structured value storage.
- Slot key задается как internal string и сверяется с текущим `hotelRequirementsState`.
- State остается process-local и теряется при restart.
- No public API exposes the new boundary.
- `assistantMessage` и `nextAction` остаются static foundation placeholders.
- Runtime/OpenAPI conformance still incomplete before generated-client work.

## 16. Recommended next task

Recommended next task: Stage 7.12a review / quality gate для проверки нового internal slot update boundary.

Alternative bounded next task: следующий internal clarification foundation slice, если он не начинает public slot filling, natural-language extraction, dynamic clarification, generated clients, LLM/provider integration, DB/storage или hotel search behavior.

## 17. Scope control confirmation

- Stage 7.12 выполнен как bounded internal backend foundation slice.
- Public API shape не изменен.
- Internal state не exposed.
- Roadmap order не изменен.
- Stage 7.13+ не активирован.
- OpenAPI draft не переписан.
- Product baseline и architecture baseline не переписаны.
- Requirements extraction, slot filling from text, dynamic clarification, LLM/provider integration, DB/storage, frontend, generated clients, hotel search, booking, payment и flights не добавлены.
