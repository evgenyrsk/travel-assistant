# Stage 8.45 — Stage 7 Compatibility Proof

## 1. Scope

Stage 8.45 — test-only stage. Добавить test coverage, который доказывает,
что после Stage 8.40–8.44:

- Stage 7 strict `hotel-search;` handoff remains the only current automatic
  search creation path;
- `ExecuteConfirmedSearchTransitionUseCase` не подключён к runtime;
- confirmation flow после `PostConfirmationDecision.Confirmed(criteria)` не
  создаёт actual hotel search;
- confirmation flow не возвращает `show_hotel_results` или real `hotelSearchId`;
- `CreateHotelSearchUseCase` не вызывается из confirmation branch;
- `markConsumed` не wire'ится в new execution path;
- existing runtime/API contract remains unchanged.

Stage 8.45 не меняет production code, runtime behavior, routes, API,
OpenAPI, frontend или generated clients.

## 2. Compatibility question

После Stage 8.40–8.44 internal orchestration skeleton
(`ExecuteConfirmedSearchTransitionUseCase`), attempt TTL/stale detection
(Stage 8.43) и retry transition support (Stage 8.44) были добавлены как
backend-only model/store skeletons. Ключевой вопрос: остались ли runtime
semantics и Stage 7 strict handoff unchanged?

Ответ: **да**. Stage 8.45 предоставляет test-based proof.

## 3. Tests added or updated

Добавлены 2 new route-level compatibility tests в
`AssistantSessionRoutesTest.kt`:

| Test | Proof target |
|---|---|
| `stage8CompatibilityFullConfirmationCycleDoesNotCreateHotelSearch` | Full Stage 8 flow (criteria proposal → confirmation prompt → positive reply) does not create hotel search. No `hotelSearchId`, no `show_hotel_results`. Both `hotel-search-local-000001` and `000002` offers endpoints return 404. Pending consumed via existing text-only path. |
| `stage8CompatibilityStrictHandoffRemainsOnlySearchCreationPath` | After full Stage 8 confirmation cycle (proposal + "да" reply), subsequent strict `hotel-search;` message still creates hotel search via Stage 7 handoff. First `hotelSearchId` is `hotel-search-local-000001`, proving no search was created by confirmation flow. |

Existing tests already providing compatibility proof (not modified):

| Existing test | Proof provided |
|---|---|
| `completeExplicitAssistantMessageCreatesSearchAndExposesRankedOffers` | Stage 7 strict handoff works via `module()`. |
| `explicitHotelSearchHandoffStillCreatesSearchWhenLlmWouldProceed` | Stage 7 strict handoff works even when LLM would proceed. |
| `positiveConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch` | Confirmation "да" consumes pending, returns text-only, no search created, offers 404. |
| `llmProceedCandidateReturnsConfirmationPromptWithoutCreatingHotelSearch` | LLM proceed candidate returns confirmation prompt without creating search. |
| `ambiguousConfirmationReplyKeepsPendingStateActiveWithoutCreatingHotelSearch` | Ambiguous reply keeps pending, no search, offers 404. |
| `negativeConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch` | Negative reply consumes pending, no search, offers 404. |
| `correctionConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch` | Correction reply consumes pending, no search, offers 404. |
| `unknownConfirmationReplyKeepsPendingStateActiveWithoutCreatingHotelSearch` | Unknown reply keeps pending, no search, offers 404. |

## 4. Proof points

### 4.1 Stage 7 strict handoff remains active

**Proved by**: `stage8CompatibilityStrictHandoffRemainsOnlySearchCreationPath`,
`completeExplicitAssistantMessageCreatesSearchAndExposesRankedOffers`,
`explicitHotelSearchHandoffStillCreatesSearchWhenLlmWouldProceed`.

- Strict `hotel-search;` message creates hotel search and returns
  `show_hotel_results` + `hotelSearchId`.
- После Stage 8 confirmation cycle (proposal + "да"), следующий strict
  handoff message получает `hotel-search-local-000001` как первый search,
  доказывая, что confirmation flow не создал ни одного search.
- Ranked offers доступны через existing GET endpoint.

### 4.2 Confirmed branch does not create hotel search

**Proved by**: `stage8CompatibilityFullConfirmationCycleDoesNotCreateHotelSearch`,
`positiveConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch`.

- После LLM proceed candidate → confirmation prompt → "да" reply:
  - `nextAction` = `ask_clarification`;
  - `hotelSearchId` absent;
  - `hotelSearchRequest` absent;
  - offers endpoint returns 404 для `000001` и `000002`;
  - response message: "Confirmation received. I will not start a hotel search automatically yet."

### 4.3 Transition use case is not runtime-wired

**Proved by**: all existing confirmation flow tests + new compatibility tests.

- `Application.kt` не содержит `ExecuteConfirmedSearchTransitionUseCase`
  (confirmed by code inspection).
- `AssistantLlmRouteWiringUseCase` не содержит references к
  `ExecuteConfirmedSearchTransitionUseCase` (confirmed by code inspection).
- Confirmation branch in `withPostConfirmationDecision` вызывает
  `consumePendingConfirmation` + `withClarification(CONFIRMATION_RECEIVED_MESSAGE)`
  без any transition use case invocation.
- Runtime composition в `moduleWithAssistantLlm` не instantiates
  `ExecuteConfirmedSearchTransitionUseCase`.

### 4.4 Runtime/API contract remains unchanged

**Proved by**: all existing route tests pass unchanged (no modifications needed).

- Session creation response shape: unchanged.
- Message intake response shape: unchanged.
- Confirmation prompt response: `ask_clarification` + text.
- Positive confirmation reply: `ask_clarification` + text.
- Negative/correction reply: `ask_clarification` + text.
- Ambiguous/unknown reply: `ask_clarification` + text.
- `hotelSearchId` absent во всех non-handoff paths.
- `show_hotel_results` only via strict `hotel-search;` handoff.
- `markConsumed` called only in existing text-only confirmation paths.

## 5. Runtime/API safety

Production code не менялся:

- `Application.kt` — unchanged.
- `AssistantLlmRouteWiringUseCase` — unchanged.
- Assistant routes — unchanged.
- API/OpenAPI/frontend/generated clients — unchanged.
- `ExecuteConfirmedSearchTransitionUseCase` — not connected to runtime.
- `CreateHotelSearchUseCase` — called only from Stage 7 strict handoff path.
- Real `hotelSearchId` — created only via strict handoff.
- `show_hotel_results` — returned only via strict handoff.
- `markConsumed` — called only in existing text-only confirmation paths.
- Response mapping skeleton — not added.

## 6. Explicit non-goals

Stage 8.45 не создаёт и не меняет:

- Production code.
- Application production classes.
- Runtime/routes behavior.
- API/OpenAPI contracts.
- Frontend.
- Generated clients.
- Response mapping skeleton.
- Actual hotel search execution.
- Retry/stale logic из Stage 8.43–8.44.
- Root roadmap/status files.

## 7. Validation

- `git status --short`: подтверждено — только 1 test file + 2 docs files.
- `git diff --check`: no errors.
- `./gradlew test --no-daemon`: BUILD SUCCESSFUL (227 tests, +2 new).
- Code inspection: `ExecuteConfirmedSearchTransitionUseCase` не referenced
  из `Application.kt` или `AssistantLlmRouteWiringUseCase`; `CreateHotelSearchUseCase`
  referenced only из `Application.kt` для Stage 7 handoff composition.

## 8. Verdict

**Passed** — Stage 7 compatibility proof added.

Stage 8.45 добавил 2 new route-level compatibility tests и подтвердил, что
все existing confirmation flow tests remain unchanged. Tests доказывают,
что Stage 7 strict `hotel-search;` handoff remains единственным current
automatic search creation path, `ExecuteConfirmedSearchTransitionUseCase`
не подключён к runtime, и confirmation flow после `Confirmed(criteria)`
не создаёт hotel search, не возвращает `show_hotel_results` или real
`hotelSearchId`. Production code не менялся. Response mapping skeleton
не добавлен.
