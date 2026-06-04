# Stage 7.5 — Minimal Clarification Response Boundary

## 1. Цель Stage 7.5

Реализовать следующий минимальный backend behavior slice: вернуть deterministic placeholder clarification reply после приема user message в assistant session.

Stage 7.5 является reply-boundary задачей. Она не реализует stateful clarification flow, requirements extraction, intent classification, LLM orchestration, hotel search, storage, retrieval или provider integration.

## 2. Что было реализовано

- `POST /api/v1/assistant/sessions/{sessionId}/messages` продолжает принимать JSON с `message`.
- Response дополнен `assistantReply` с `replyType = clarification` и коротким placeholder message.
- Application boundary теперь возвращает minimal assistant reply model вместе с intake metadata.
- Validation behavior Stage 7.4 сохранен без расширения taxonomy.
- Добавлены route/use-case assertions для нового response shape.

## 3. Созданные файлы

- `docs/reviews/stage-7-5-minimal-clarification-response-boundary.md`

## 4. Изменённые файлы

- `services/backend/src/main/kotlin/com/travelassistant/backend/api/AssistantPlaceholderRoutes.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantSessionBoundary.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/api/AssistantSessionRoutesTest.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/CreateAssistantSessionUseCaseTest.kt`
- `services/backend/README.md`
- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/reviews/README.md`

## 5. Endpoint behavior

`POST /api/v1/assistant/sessions/{sessionId}/messages` принимает body:

```json
{
  "message": "I want a hotel in Rome for two adults next weekend"
}
```

Успешный response возвращает `200 OK`:

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

Фактическое `receivedAt` в runtime берется из системного UTC clock.

`assistantReply` intentionally deterministic: он не анализирует message text, не извлекает slots и не вызывает LLM.

## 6. Validation behavior

Validation остается прежней:

- missing `message` returns `400 Bad Request`;
- missing body or invalid body returns `400 Bad Request`;
- blank `message` returns `400 Bad Request`;
- validation response uses structured `ErrorResponse` with `code = VALIDATION_ERROR`;
- `details.field` is `message`.

## 7. Архитектурные границы

- Backend stack остается Kotlin + Ktor.
- Ktor route остается thin layer над application boundary.
- Application/domain code не зависит от Ktor, DB, Redis, provider SDK, LLM SDK или frontend tooling.
- Reply model является local/foundation-only boundary, а не финальным Stage 6 `AssistantMessageResponse`.
- Clarification reply является placeholder response, а не stateful clarification flow.

## 8. Что осталось placeholder/future boundary

- Session persistence и retrieval.
- Message history.
- Stateful clarification flow.
- Requirements extraction и intent classification.
- LLM orchestration и `LlmClient`.
- Hotel search behavior.
- Shortlist behavior.
- Explanation/comparison behavior.
- Provider integration и provider mapping.

## 9. Что намеренно не реализовывалось

- DB/storage, migrations, repositories или persistence.
- Redis/cache.
- Authentication/account flows.
- Session retrieval или session validation through storage.
- Message history или message id.
- Stateful clarification flow, extracted entities или search intent summary.
- LLM provider integration или `LlmClient`.
- Hotel provider integration.
- Hotel search, ranking, shortlist, explanations или comparison.
- Frontend и generated clients.
- OpenAPI generation или OpenAPI draft changes.
- Booking, payment, flights или combined itinerary.
- Docker/deployment infrastructure.
- Product baseline или architecture baseline rewrite.

## 10. Проверки

- `git status --short` — passed, worktree был чистым перед изменениями.
- `git diff --check` — passed.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` из `services/backend` — passed.

Первый sandboxed Gradle run не смог открыть `~/.gradle` wrapper lock file; проверка была повторена с разрешенным доступом к Gradle cache.

## 11. Known limitations

- Clarification reply is static and does not inspect message content.
- `sessionId` is not looked up or validated through storage.
- `assistantReply` is not persisted as message history.
- Response intentionally does not implement full Stage 6 `AssistantMessageResponse`.
- No max length validation is implemented yet.

## 12. Recommended next task

Следующая задача должна быть отдельной roadmap-aligned Stage 7 task. Хороший bounded next step: minimal session-local state boundary или first requirements extraction placeholder, если он будет явно активирован, без DB/storage, real provider integration, frontend/generated clients или Stage 8+ work.

## 13. Scope control confirmation

- Stage 7.5 выполнен как один bounded backend behavior slice.
- Stage 7.6+ не активированы.
- Roadmap order не изменен.
- Product baseline, architecture baseline, OpenAPI draft и ADR не изменялись.
- Реальные integrations, storage, frontend и broad documentation cleanup не выполнялись.
