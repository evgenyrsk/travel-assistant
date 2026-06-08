# Travel Assistant Backend

Минимальная Kotlin + Ktor backend foundation для Stage 7.2 и первые bounded behavior slices Stage 7.3 - Stage 7.12.

Backend остается foundation для hotel-only MVP v1. Он следует Stage 6 OpenAPI draft только на уровне application boundaries: health endpoint реализован, Stage 7.3 добавляет локальное создание assistant session, Stage 7.4 добавляет локальный message intake boundary, Stage 7.5 добавляет минимальный placeholder clarification reply, Stage 7.6 добавляет process-local session state, Stage 7.7 добавляет session-local clarification metadata, Stage 7.8 добавляет internal hotel requirements slot metadata, Stage 7.9 добавляет internal slot coverage / clarification planning metadata, Stage 7.11 выравнивает assistant runtime response shape и validation error shape ближе к Stage 6 contract direction без включения real assistant behavior, Stage 7.12 добавляет internal requirements slot update boundary для explicit structured internal input, а остальные hotel-only assistant/search routes остаются явными placeholder endpoints без бизнес-логики:

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

Stage 7.3 - Stage 7.11 session creation возвращает `201 Created` со structured JSON в foundation version of Stage 6-like assistant response shape:

- `session` с `sessionId`, `status`, `createdAt`, `updatedAt`;
- `assistantMessage` с deterministic placeholder `role` и `content`;
- `nextAction`.

`sessionId` является process-local deterministic identifier и не подразумевает persistence, retrieval, account history или cross-device storage.

Stage 7.11 также принимает optional initial `message` body на `POST /api/v1/assistant/sessions`. Если initial `message` передан и не blank, он обрабатывается как bounded foundation intake: session создается, минимальные clarification metadata обновляются, internal coverage plan пересчитывается, но message text не сохраняется как history, не анализируется, не извлекает requirements, не заполняет slots и не вызывает LLM/provider.

Stage 7.6 регистрирует созданную session только в process-local memory. Stage 7.7 инициализирует для нее минимальное process-local `clarificationState` metadata: фазу `collecting_requirements`, признак ожидания пользовательского ввода, счетчик принятых user messages, timestamps создания/обновления и timestamp последнего принятого сообщения после message intake.

Stage 7.8 инициализирует для local session internal `hotelRequirementsState` metadata с foundation-only slots:

- `destination` — required, `missing`;
- `stay_dates` — required, `missing`;
- `guests` — required, `missing`;
- `preferences` — optional, `unknown`.

Stage 7.9 вычисляет internal `hotelRequirementsCoveragePlan` metadata на основе `hotelRequirementsState`: количество required slots, missing required slots, ordered missing slot keys, optional slot keys, следующий missing required slot и признак полноты required hotel search inputs. Это deterministic planning metadata только для будущего clarification flow.

Stage 7.12 добавляет внутренний `UpdateHotelRequirementSlotUseCase`, который может обновить status существующего hotel requirements slot только по explicit structured internal input:

- process-local `sessionId`;
- known internal `slotKey`;
- explicit `RequirementSlotStatus`.

Use case проверяет, что session существует в process-local store, проверяет наличие slot key в текущем `hotelRequirementsState`, обновляет только slot status и пересчитывает `hotelRequirementsCoveragePlan`. Unknown session и unknown slot key возвращаются как explicit internal result types. Boundary не доступен через public API, не хранит slot values, не анализирует пользовательский текст, не извлекает requirements и не делает dynamic clarification.

Эти metadata не возвращаются в public response, не являются финальным API contract и не означают production state machine. Это не durable persistence, не DB/storage, не account state и не multi-instance coordination.

Фактический путь локального приема user message: `POST /api/v1/assistant/sessions/{sessionId}/messages`.

Stage 7.4 - Stage 7.11 message intake принимает JSON с `message` и возвращает `200 OK` со structured JSON:

- `session` с `sessionId`, `status`, `createdAt`, `updatedAt`;
- `assistantMessage` с deterministic placeholder `role` и `content`;
- `nextAction`.

`assistantMessage` является deterministic placeholder clarification response. Этот endpoint обновляет только минимальные session-local clarification metadata, сохраняет internal hotel slot metadata без заполнения значений и пересчитывает internal coverage plan без анализа текста. Он не сохраняет message history, не проверяет session через durable storage, не выполняет stateful clarification flow, не извлекает requirements и не возвращает hotel offers.

Если `sessionId` не найден в process-local state текущего процесса, endpoint возвращает structured `404 Not Found` с `code = SESSION_NOT_FOUND`. Этот error code является foundation-level behavior, а не финальным generated-client/API contract.

Invalid или blank `message` возвращает structured `400 Bad Request` с `code = VALIDATION_ERROR` и `fields`. Foundation-only `NOT_IMPLEMENTED` и generic `NOT_FOUND` остаются runtime placeholder codes и не считаются финальной generated-client taxonomy.

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
- public slot update endpoints, natural-language slot filling или сохранение extracted hotel requirement values;
- dynamic clarification planning или user-facing clarification question generation;
- final generated-client-ready API contract semantics;
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
