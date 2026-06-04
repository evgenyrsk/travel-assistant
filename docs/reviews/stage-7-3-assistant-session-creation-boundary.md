# Stage 7.3 — Assistant Session Creation Use-Case Boundary

## 1. Цель Stage 7.3

Реализовать первый минимальный backend behavior slice для создания assistant session в рамках hotel-only MVP foundation.

Задача переводит `POST /api/v1/assistant/sessions` из placeholder route в маленький testable use-case boundary без persistence, provider integration, LLM orchestration, frontend, generated clients или production hardening.

## 2. Что было реализовано

- `POST /api/v1/assistant/sessions` теперь возвращает `201 Created`.
- Добавлен минимальный application use case для создания локальной assistant session.
- Добавлена минимальная domain-модель session identity, status и snapshot.
- `sessionId` создается process-local deterministic counter-based generator.
- `createdAt` формируется через injectable `Clock`.
- Остальные assistant/search routes сохранены как `501 Not Implemented` placeholders.

## 3. Созданные файлы

- `services/backend/src/main/kotlin/com/travelassistant/backend/domain/assistant/AssistantSession.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/CreateAssistantSessionUseCaseTest.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/api/AssistantSessionRoutesTest.kt`
- `docs/reviews/stage-7-3-assistant-session-creation-boundary.md`

## 4. Изменённые файлы

- `services/backend/src/main/kotlin/com/travelassistant/backend/api/AssistantPlaceholderRoutes.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantSessionBoundary.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/domain/assistant/AssistantSessionPlaceholder.kt` — заменен на `AssistantSession.kt`
- `services/backend/README.md`
- `docs/roadmap/roadmap.md`

## 5. Endpoint behavior

`POST /api/v1/assistant/sessions` принимает empty body и возвращает structured JSON:

```json
{
  "sessionId": "assistant-session-local-000001",
  "status": "collecting_requirements",
  "createdAt": "2026-06-04T00:00:00Z"
}
```

Фактическое `createdAt` в runtime берется из системного UTC clock. Endpoint не принимает initial message и не выполняет request validation, потому что request body не входит в Stage 7.3 scope.

## 6. Архитектурные границы

- Backend stack остается Kotlin + Ktor.
- Ktor route остается тонким слоем над application use case.
- Application/domain слой не зависит от Ktor, DB, Redis, provider SDK, LLM SDK или frontend tooling.
- Session identity является local process-only identifier, а не production contract, storage key или account identity.
- Storage/repository boundary намеренно не добавлялся, потому что persistence вне scope.

## 7. Что осталось placeholder/future boundary

- Session persistence и retrieval.
- Assistant message handling.
- Clarification flow и LLM orchestration.
- Search intent extraction.
- Hotel search behavior.
- Shortlist behavior.
- Explanation/comparison behavior.
- Provider integration и provider mapping.

## 8. Что намеренно не реализовывалось

- DB/storage, migrations, repositories или persistence.
- Redis/cache.
- Authentication/account flows.
- LLM provider integration или `LlmClient`.
- Hotel provider integration.
- Hotel search, ranking, shortlist, explanations или comparison.
- Frontend и generated clients.
- OpenAPI generation или OpenAPI draft changes.
- Booking, payment, flights или combined itinerary.
- Docker/deployment infrastructure.
- Product baseline или architecture baseline rewrite.

## 9. Проверки

- `git status --short` — passed, worktree был чистым перед изменениями.
- `git diff --check` — passed.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` из `services/backend` — passed.

Первый sandboxed Gradle run не смог открыть `~/.gradle` wrapper lock file; проверка была повторена с разрешенным доступом к Gradle cache.

## 10. Known limitations

- Created session lives only as response metadata; retrieval/resume behavior is not implemented.
- `sessionId` is deterministic and process-local, not globally unique or persistent.
- `createdAt` is runtime metadata, not persisted state.
- Response is intentionally minimal and does not implement full Stage 6 `AssistantMessageResponse`.
- Request body is not supported in Stage 7.3.

## 11. Recommended next task

Следующая задача должна быть отдельной roadmap-aligned Stage 7 task. Хороший bounded next step: assistant message handling или минимальный clarification/use-case boundary, если он будет явно активирован, без DB/storage, real provider integration, frontend/generated clients или Stage 8+ work.

## 12. Scope control confirmation

- Stage 7.3 выполнен как один bounded backend behavior slice.
- Stage 7.4+ не активированы.
- Roadmap order не изменен.
- Product baseline, architecture baseline, OpenAPI draft и ADR не изменялись.
- Реальные integrations, storage, frontend и broad documentation cleanup не выполнялись.
