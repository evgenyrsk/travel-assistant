# Stage 7.2 — Kotlin + Ktor Backend Application Foundation

## Цель Stage 7.2

Создать минимальную Kotlin + Ktor backend foundation для hotel-only MVP v1 после явной roadmap-aligned активации Stage 7.2.

Задача должна была расширить существующий Stage 7.0b skeleton только до уровня application boundaries: health endpoint, routing registration, common error handling, placeholder routes и минимальные domain/application/provider boundary markers.

## Что было реализовано

- `Application.module()` разделен на serialization, common error handling и API route registration.
- Добавлен `StatusPages` для structured `404` и `500` responses.
- Добавлена structured error model с `code`, `message`, optional `requestId` и optional `details`.
- Health endpoint `GET /api/v1/health` сохранен рабочим.
- Добавлены placeholder routes для Stage 6 hotel-only assistant/search/shortlist/explanation boundaries.
- Placeholder routes возвращают `501 Not Implemented` и не создают mock business data.
- Добавлены минимальные internal boundary markers для assistant session, hotel search и hotel provider future boundary.
- Добавлены tests для health endpoint, representative placeholder route и unknown route error response.

## Созданные файлы

- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ApiRoutes.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/AssistantPlaceholderRoutes.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ErrorHandling.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ErrorResponse.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/HotelSearchPlaceholderRoutes.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/PlaceholderResponses.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/Serialization.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantSessionBoundary.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/hotel/HotelSearchBoundary.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/domain/assistant/AssistantSessionPlaceholder.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/domain/hotel/HotelSearchPlaceholder.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/domain/provider/HotelOfferProviderBoundary.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/api/PlaceholderRoutesTest.kt`
- `docs/reviews/stage-7-2-backend-application-foundation.md`

## Изменённые файлы

- `services/backend/build.gradle.kts`
- `services/backend/README.md`
- `services/backend/src/main/kotlin/com/travelassistant/backend/Application.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/HealthRoutes.kt`
- `docs/roadmap/roadmap.md`

## Архитектурные границы

- Backend stack остается Kotlin + Ktor.
- Ktor routing остается тонким API layer.
- Domain/application placeholder files не импортируют Ktor, DB, Redis, LLM SDK или provider SDK.
- Provider boundary сохранен как future placeholder, а не concrete provider contract.
- API surface остается под `/api/v1`.
- Health endpoint является единственным реализованным behavior endpoint.

## Что осталось placeholder/future boundary

- Assistant session orchestration.
- Hotel search application flow.
- Hotel offer provider boundary.
- Shortlist behavior.
- Explanation/comparison behavior.
- Search intent extraction, ranking, reasoning и LLM orchestration.
- Provider mapping, provider errors, freshness/source markers и real/fake provider implementation.

## Что намеренно не реализовывалось

- Real provider integration.
- DB/storage, migrations, repositories или persistence.
- Redis/cache.
- Frontend или generated clients.
- Auth/account flows.
- Booking, payment, flights или combined itinerary.
- Production hardening, observability, deployment или Docker.
- Provider-specific DTOs/contracts.
- Fake business logic that looks production-ready.

## Проверки

- `git status --short`
- `git diff --check`
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` из `services/backend`

## Known limitations

- Placeholder routes intentionally return `501 Not Implemented`.
- Error taxonomy is minimal and scoped to foundation behavior.
- `requestId` is only echoed from `X-Request-ID`; request ID generation is not implemented.
- Domain/application/provider placeholders are marker boundaries, not accepted contracts.
- Current-session persistence behavior is not implemented.

## Recommended next task

Следующая задача должна быть отдельной roadmap-aligned Stage 7 task. Рекомендуемый bounded next step: выбрать один минимальный backend behavior slice, например assistant session creation placeholder-to-use-case transition with in-memory current-session boundary, без DB/storage, real provider integration, frontend/generated clients, booking/payment/flights или Stage 8+ work.

## Scope control confirmation

- Stage 7.2 выполнен как foundation-only implementation task.
- Stage 7.3+ не активированы.
- Roadmap order не изменен.
- Product baseline и architecture baseline не переписаны.
- OpenAPI draft не изменен.
- ADR не создавался.
- Unrelated documentation cleanup не выполнялся.
