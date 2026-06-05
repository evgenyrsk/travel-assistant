# Travel Assistant Backend

Минимальная Kotlin + Ktor backend foundation для Stage 7.2 и первые bounded behavior slices Stage 7.3 - Stage 7.6.

Backend остается foundation для hotel-only MVP v1. Он следует Stage 6 OpenAPI draft только на уровне application boundaries: health endpoint реализован, Stage 7.3 добавляет локальное создание assistant session, Stage 7.4 добавляет локальный message intake boundary, Stage 7.5 добавляет минимальный placeholder clarification reply, Stage 7.6 добавляет process-local session state, а остальные hotel-only assistant/search routes остаются явными placeholder endpoints без бизнес-логики:

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
- `POST /assistant/sessions`
- `POST /assistant/sessions/{sessionId}/messages`

Фактический путь проверки доступности: `GET /api/v1/health`.

Фактический путь создания локальной assistant session: `POST /api/v1/assistant/sessions`.

Stage 7.3 session creation возвращает `201 Created` со structured JSON:

- `sessionId`;
- `status`;
- `createdAt`.

`sessionId` является process-local deterministic identifier и не подразумевает persistence, retrieval, account history или cross-device storage.

Stage 7.6 регистрирует созданную session только в process-local memory. Это не durable persistence, не DB/storage, не account state и не multi-instance coordination.

Фактический путь локального приема user message: `POST /api/v1/assistant/sessions/{sessionId}/messages`.

Stage 7.4 - Stage 7.5 message intake принимает JSON с `message` и возвращает `200 OK` со structured JSON:

- `sessionId`;
- `status`;
- `receivedAt`.
- `assistantReply` с `replyType` и `message`.

`assistantReply` является deterministic placeholder clarification response. Этот endpoint не сохраняет message history, не проверяет session через storage, не выполняет stateful clarification flow, не извлекает requirements и не возвращает hotel offers.

Если `sessionId` не найден в process-local state текущего процесса, endpoint возвращает structured `404 Not Found` с `code = SESSION_NOT_FOUND`. Этот error code является foundation-level behavior, а не финальным generated-client/API contract.

Placeholder routes для будущих hotel-only MVP boundaries:

- `GET /api/v1/assistant/sessions/{sessionId}/shortlist`
- `PUT /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`
- `DELETE /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`
- `POST /api/v1/assistant/sessions/{sessionId}/explanations`
- `POST /api/v1/hotel-searches`
- `GET /api/v1/hotel-searches/{searchId}/offers`

Эти placeholder routes возвращают structured `501 Not Implemented` response и не вызывают provider, DB, LLM или mock business logic.

## Намеренно не реализовано

- production assistant sessions, session persistence/retrieval и message history;
- durable persistence, DB/storage и multi-instance session state;
- stateful clarification flow, intent classification или requirements extraction;
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
