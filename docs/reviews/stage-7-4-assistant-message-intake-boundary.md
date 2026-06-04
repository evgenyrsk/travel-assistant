# Stage 7.4 — Assistant Message Intake Boundary

## 1. Цель Stage 7.4

Реализовать следующий минимальный backend behavior slice: прием user message для assistant session через тонкий Ktor route и небольшой application use-case boundary.

Stage 7.4 является intake-only задачей. Она не реализует assistant reasoning, clarification flow, hotel search, LLM orchestration, storage, retrieval или provider integration.

## 2. Что было реализовано

- `POST /api/v1/assistant/sessions/{sessionId}/messages` теперь принимает JSON с `message`.
- Добавлен минимальный application boundary для local message intake.
- Добавлен response с `sessionId`, `status` и `receivedAt`.
- Добавлена минимальная validation для missing или blank `message`.
- Добавлен `VALIDATION_ERROR` в существующую structured error model без redesign error taxonomy.
- Добавлены route/use-case tests для success и validation behavior.

## 3. Созданные файлы

- `docs/reviews/stage-7-4-assistant-message-intake-boundary.md`

## 4. Изменённые файлы

- `services/backend/src/main/kotlin/com/travelassistant/backend/api/AssistantPlaceholderRoutes.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ErrorResponse.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantSessionBoundary.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/api/AssistantSessionRoutesTest.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/CreateAssistantSessionUseCaseTest.kt`
- `services/backend/README.md`
- `docs/roadmap/roadmap.md`

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
  "receivedAt": "2026-06-04T00:00:00Z"
}
```

Фактическое `receivedAt` в runtime берется из системного UTC clock.

Endpoint не возвращает assistant answer, clarification question, extracted requirements, hotel search request, provider offers, message history или persistence marker.

## 6. Validation behavior

Validation intentionally minimal:

- missing `message` returns `400 Bad Request`;
- missing body or invalid body returns `400 Bad Request`;
- blank `message` returns `400 Bad Request`;
- validation response uses structured `ErrorResponse` with `code = VALIDATION_ERROR`;
- `details.field` is `message`;
- max length не добавлялся, потому что в backend еще нет принятой runtime validation convention для длины message.

## 7. Архитектурные границы

- Backend stack остается Kotlin + Ktor.
- Ktor route остается thin layer над application boundary.
- Application/domain code не зависит от Ktor, DB, Redis, provider SDK, LLM SDK или frontend tooling.
- Session lookup/storage intentionally absent: `sessionId` из path используется как opaque local/foundation-only identifier.
- Message intake не создает message id, чтобы не подразумевать persistent message storage.

## 8. Что осталось placeholder/future boundary

- Session persistence и retrieval.
- Message history.
- Assistant replies.
- Clarification flow.
- Requirements extraction и intent classification.
- Hotel search behavior.
- Shortlist behavior.
- Explanation/comparison behavior.
- Provider integration и provider mapping.
- LLM orchestration.

## 9. Что намеренно не реализовывалось

- DB/storage, migrations, repositories или persistence.
- Redis/cache.
- Authentication/account flows.
- Session retrieval или session validation through storage.
- Message history или message id.
- Assistant replies, clarification questions или extracted entities.
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

Промежуточные повторные runs той же команды выявили Kotlin compile issues в route nullability и test `contentType` import; оба исправлены до финального successful run.

## 11. Known limitations

- Message is accepted only as response metadata; it is not persisted.
- `sessionId` is not looked up or validated through storage.
- `status` remains a minimal string mapping, not finalized public enum/API contract.
- No max length validation is implemented yet.
- Response intentionally does not implement full Stage 6 `AssistantMessageResponse`.

## 12. Recommended next task

Следующая задача должна быть отдельной roadmap-aligned Stage 7 task. Хороший bounded next step: minimal clarification response boundary или session-local state boundary, если он будет явно активирован, без DB/storage, real provider integration, frontend/generated clients или Stage 8+ work.

## 13. Scope control confirmation

- Stage 7.4 выполнен как один bounded backend behavior slice.
- Stage 7.5+ не активированы.
- Roadmap order не изменен.
- Product baseline, architecture baseline, OpenAPI draft и ADR не изменялись.
- Реальные integrations, storage, frontend и broad documentation cleanup не выполнялись.
- Roadmap заранее не называл этот next step как Stage 7.4; текущая явная задача активировала bounded Stage 7.4 implementation step.
