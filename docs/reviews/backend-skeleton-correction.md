# Исправление backend skeleton на Kotlin + Ktor

Дата: 2026-06-03

## 1. Цель задачи

Исправить architecture drift в `services/backend`: заменить Stage 7.1 Java/Spring Boot skeleton на минимальный Kotlin + Ktor skeleton, не начиная Stage 7.2 и не добавляя business logic.

## 2. Что было исправлено

- Backend skeleton переведен на Kotlin + Ktor.
- Health endpoint сохранен по contract-friendly пути `GET /api/v1/health`.
- Placeholder endpoints для assistant sessions, hotel search, shortlist и explanations удалены из skeleton.
- Spring Boot plugins, dependencies, annotations, controllers и tests удалены из tracked backend files.
- Backend README обновлен под Ktor run/test workflow.

## 3. Удаленные или замененные Java/Spring Boot artifacts

- Spring Boot Gradle plugins и dependencies в `services/backend/build.gradle.kts`.
- Java entrypoint `TravelAssistantBackendApplication.java`.
- Spring controllers для health, assistant sessions, hotel search, shortlist и explanations.
- Spring error/model helper classes.
- Spring-specific `application.yml`.
- Spring Boot `MockMvc` test.
- README references к Spring Boot workflow и placeholder endpoints.

## 4. Добавленные Kotlin + Ktor artifacts

- `services/backend/src/main/kotlin/com/travelassistant/backend/Application.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/HealthRoutes.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/HealthResponse.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/api/HealthRoutesTest.kt`

## 5. Реализованные endpoints

- `GET /api/v1/health`

Response содержит:

- `status`
- `service`
- `version`

## 6. Намеренно не реализовано

- assistant sessions;
- hotel search;
- shortlist;
- explain/compare;
- business logic поиска, ранжирования и рекомендаций;
- real hotel provider integration;
- provider-specific DTO/contracts;
- DB/storage;
- Redis/cache;
- auth;
- LLM integration;
- frontend code;
- generated clients;
- booking, payment, flights, combined itinerary, account flows.

## 7. Validation results

- `git diff --check` — passed.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home GRADLE_USER_HOME=/private/tmp/travel-assistant-gradle ./gradlew build` из `services/backend` — passed.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home GRADLE_USER_HOME=/private/tmp/travel-assistant-gradle ./gradlew test --rerun-tasks` из `services/backend` — passed.
- Обычный запуск Gradle без явного `JAVA_HOME` не использовался для итоговой проверки, потому что окружение содержит невалидный `JAVA_HOME`.

## 8. Оставшиеся ограничения

- Skeleton реализует только health endpoint.
- Stage 6 OpenAPI endpoints beyond health остаются contract/documentation artifacts и не реализованы в этой задаче.
- Stage 7.2 не активирован.
- Дальнейшая implementation работа должна дождаться restart readiness review.

## 9. Рекомендация

Следующая задача: Stage 7 restart readiness review.

Review должен подтвердить, что roadmap, architecture baseline, governance docs и `services/backend` согласованы после correction задачи, прежде чем активировать Stage 7.2 или любую дальнейшую implementation работу.

## 10. Scope control confirmation

- Java/Spring Boot skeleton заменен.
- Backend теперь использует Kotlin + Ktor.
- Frontend не менялся.
- Business logic не реализовывалась.
- Stage 7.2 не начинался.
- MVP scope не расширялся.
