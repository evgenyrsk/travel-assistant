# Travel Assistant Backend

Минимальный Spring Boot backend skeleton для Stage 7.1.

Backend создается как foundation для hotel-only MVP v1 и следует Stage 6 OpenAPI draft:

- `../../docs/architecture/stage-6/openapi-draft.yaml`

## Запуск

```bash
./gradlew bootRun
```

Команду нужно запускать из директории `services/backend`.

## Проверка

```bash
./gradlew test
```

## Skeleton endpoints

Все endpoints находятся под `/api/v1`:

- `GET /health`
- `POST /assistant/sessions`
- `POST /assistant/sessions/{sessionId}/messages`
- `POST /hotel-searches`
- `GET /hotel-searches/{searchId}/offers`
- `GET /assistant/sessions/{sessionId}/shortlist`
- `PUT /assistant/sessions/{sessionId}/shortlist/{offerId}`
- `DELETE /assistant/sessions/{sessionId}/shortlist/{offerId}`
- `POST /assistant/sessions/{sessionId}/explanations`

Explanation и comparison представлены одним Stage 6 endpoint:
`POST /assistant/sessions/{sessionId}/explanations`, где `mode` может быть
`explain` или `compare`.

## Намеренно не реализовано

- business logic поиска и ранжирования;
- реальные hotel provider integrations;
- provider-specific DTO/contracts;
- DB migrations, JPA entities, repositories и storage model;
- Redis/cache;
- LLM integration и orchestration;
- frontend/generated clients;
- booking, payment, flights, combined itinerary, account flows.

