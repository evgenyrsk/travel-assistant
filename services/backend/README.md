# Travel Assistant Backend

Минимальный Kotlin + Ktor backend skeleton для Stage 7.0b.

Backend исправляет ранее задокументированный stack drift и остается только foundation для hotel-only MVP v1. Skeleton следует Stage 6 OpenAPI draft на уровне health endpoint:

- `../../docs/architecture/stage-6/openapi-draft.yaml`

## Запуск

```bash
./gradlew run
```

Команду нужно запускать из директории `services/backend`.

## Проверка

```bash
./gradlew test
```

## Skeleton endpoints

Все endpoints находятся под `/api/v1`. В Stage 7.0b реализован только:

- `GET /health`

Фактический путь проверки доступности: `GET /api/v1/health`.

## Намеренно не реализовано

- assistant sessions;
- hotel search endpoints;
- shortlist endpoints;
- explanation/compare endpoints;
- business logic поиска и ранжирования;
- реальные hotel provider integrations;
- provider-specific DTO/contracts;
- DB migrations, entities, repositories и storage model;
- Redis/cache;
- LLM integration и orchestration;
- frontend/generated clients;
- booking, payment, flights, combined itinerary, account flows.
