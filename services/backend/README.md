# Travel Assistant Backend

Минимальная Kotlin + Ktor backend foundation для Stage 7.2.

Backend остается foundation для hotel-only MVP v1. Он следует Stage 6 OpenAPI draft только на уровне application boundaries: health endpoint реализован, а hotel-only assistant/search routes добавлены как явные placeholder endpoints без бизнес-логики:

- `../../docs/architecture/stage-6/openapi-draft.yaml`

## Запуск

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home \
./gradlew run
```

Команду нужно запускать из директории `services/backend`.

## Проверка

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home \
./gradlew test
```

## Endpoints

Все endpoints находятся под `/api/v1`.

Реализован:

- `GET /health`

Фактический путь проверки доступности: `GET /api/v1/health`.

Placeholder routes для будущих hotel-only MVP boundaries:

- `POST /api/v1/assistant/sessions`
- `POST /api/v1/assistant/sessions/{sessionId}/messages`
- `GET /api/v1/assistant/sessions/{sessionId}/shortlist`
- `PUT /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`
- `DELETE /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`
- `POST /api/v1/assistant/sessions/{sessionId}/explanations`
- `POST /api/v1/hotel-searches`
- `GET /api/v1/hotel-searches/{searchId}/offers`

Эти routes возвращают structured `501 Not Implemented` response и не вызывают provider, DB, LLM или mock business logic.

## Намеренно не реализовано

- production assistant sessions;
- hotel search business logic;
- shortlist behavior;
- explanation/compare behavior;
- business logic поиска и ранжирования;
- реальные hotel provider integrations;
- provider-specific DTO/contracts;
- DB migrations, entities, repositories и storage model;
- Redis/cache;
- LLM integration и orchestration;
- frontend/generated clients;
- booking, payment, flights, combined itinerary, account flows.
