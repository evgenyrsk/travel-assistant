# Travel Assistant Backend

Минимальная Kotlin + Ktor backend foundation для Stage 7.2 и bounded behavior / cleanup slices до Stage 7.48.

Backend остается foundation для hotel-only MVP v1. Он следует Stage 6 OpenAPI draft на уровне текущих application boundaries: Stage 7.3-7.15 формируют assistant/session foundation, а Stage 7.48 добавляет минимальный process-local hotel search flow с детерминированным `FakeHotelOfferProvider`. Shortlist и explanation routes остаются явными placeholder endpoints без бизнес-логики:

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
- `POST /hotel-searches`
- `GET /hotel-searches/{searchId}/offers`

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

Stage 7.15 добавляет minimal application-level `AssistantResponseSemantics` boundary. Public `nextAction` теперь вычисляется детерминированно из internal `hotelRequirementsCoveragePlan`:

- если required hotel slots incomplete, `nextAction = ask_clarification`;
- если required hotel slots internally collected через explicit structured internal input, `nextAction = show_boundary_message`.

`show_boundary_message` является foundation-only safe boundary signal. Он не означает, что real hotel search можно выполнить, не создает `hotelSearchRequest`, не вызывает provider/search route и не добавляет fake search values.

Эти metadata не возвращаются в public response, не являются финальным API contract и не означают production state machine. Это не durable persistence, не DB/storage, не account state и не multi-instance coordination.

Фактический путь локального приема user message: `POST /api/v1/assistant/sessions/{sessionId}/messages`.

Stage 7.4 - Stage 7.11 message intake принимает JSON с `message` и возвращает `200 OK` со structured JSON:

- `session` с `sessionId`, `status`, `createdAt`, `updatedAt`;
- `assistantMessage` с deterministic placeholder `role` и `content`;
- `nextAction`.

`assistantMessage` является deterministic placeholder clarification response. Этот endpoint обновляет только минимальные session-local clarification metadata, сохраняет internal hotel slot metadata без заполнения значений и пересчитывает internal coverage plan без анализа текста. Он не сохраняет message history, не проверяет session через durable storage, не выполняет stateful clarification flow, не извлекает requirements и не возвращает hotel offers.

Поскольку public message intake не парсит текст и не заполняет slots, обычные user messages не делают session search-ready. Optional initial `message` также остается foundation intake only и не создает `hotelSearchRequest`.

Если `sessionId` не найден в process-local state текущего процесса, endpoint возвращает structured `404 Not Found` с `code = SESSION_NOT_FOUND`. Этот error code является foundation-level behavior, а не финальным generated-client/API contract.

Invalid или blank `message` возвращает structured `400 Bad Request` с `code = VALIDATION_ERROR` и `fields`. Foundation-only `NOT_IMPLEMENTED` и generic `NOT_FOUND` остаются runtime placeholder codes и не считаются финальной generated-client taxonomy.

## Минимальный hotel search flow

Stage 7.48 реализует process-local flow поверх существующего Stage 6 contract shape:

1. Создать assistant session через `POST /api/v1/assistant/sessions`.
2. Передать `sessionId` и criteria в `POST /api/v1/hotel-searches`.
3. Получить `202 Accepted` с `searchId` и terminal foundation status.
4. Прочитать нормализованные fake offers через `GET /api/v1/hotel-searches/{searchId}/offers`.

Минимальные criteria:

- `destination`;
- `checkInDate` и `checkOutDate` в ISO-8601 date format;
- `guests.adults`, optional `guests.children`;
- `rooms` или видимая `room_count` entry в `derivedAssumptions`.

Search и offers сохраняются только в памяти текущего процесса. `FakeHotelOfferProvider` не выполняет HTTP/network calls, возвращает детерминированные local offers и сохраняет provider order без ranking. Response включает stable offer identifiers, hotel name, location, total price, rating, amenities, availability, source/freshness markers и provider facts.

Это foundation behavior, а не real provider integration, generated-client readiness, production search, pricing guarantee или availability guarantee.

Placeholder routes для оставшихся hotel-only MVP boundaries:

- `GET /api/v1/assistant/sessions/{sessionId}/shortlist`
- `PUT /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`
- `DELETE /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`
- `POST /api/v1/assistant/sessions/{sessionId}/explanations`

Эти placeholder routes возвращают structured `501 Not Implemented` response и не вызывают provider, DB, LLM или mock business logic.

Stage 7.14 strategy для generated-client readiness:

- shortlist и explanation endpoints остаются runtime-only foundation placeholders;
- Stage 7.48 hotel search endpoints возвращают fake-provider foundation data, но не входят автоматически в generated-client-ready subset;
- оставшиеся placeholder endpoints не входят в будущий generated-client-ready subset, пока отдельная roadmap-aligned задача не заменит `501 NOT_IMPLEMENTED` на contract-aligned success/error behavior;
- placeholder responses не должны имитировать реальные `ShortlistResponse`, `ShortlistItem` или `AssistantExplanationResponse`;
- `NOT_IMPLEMENTED` и generic `NOT_FOUND` являются foundation-only runtime codes, а не финальной generated-client taxonomy;
- `HOTEL_SEARCH_NOT_FOUND` используется для process-local Stage 7.48 search resource; `HOTEL_OFFER_NOT_FOUND` и `SHORTLIST_ITEM_NOT_FOUND` остаются future behavior;
- generated clients, OpenAPI finalization и runtime/OpenAPI conformance gate остаются будущей отдельной задачей.

## Намеренно не реализовано

- production assistant sessions, session persistence/retrieval и message history;
- durable persistence, DB/storage и multi-instance session state;
- stateful clarification flow, intent classification или requirements extraction;
- public slot update endpoints, natural-language slot filling или сохранение extracted hotel requirement values;
- dynamic clarification planning или user-facing clarification question generation;
- final generated-client-ready API contract semantics;
- real public search readiness semantics и `hotelSearchRequest` construction;
- production hotel search business logic и provider mapping;
- hotel offer ranking;
- shortlist behavior;
- explanation/compare behavior;
- реальные hotel provider integrations;
- provider-specific DTO/contracts;
- DB migrations, entities, repositories и storage model;
- Redis/cache;
- LLM integration и orchestration;
- frontend/generated clients;
- generated-client-ready subset для placeholder endpoints;
- OpenAPI/runtime conformance gate;
- booking, payment, flights, combined itinerary, account flows.
