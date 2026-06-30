# Stage 8.47 — Confirmed Search Transition Integration Readiness Gate

## 1. Scope

Stage 8.47 — review/design-only gate. Проверить, готовы ли skeletons и
policies Stage 8.40–8.46 к future route/runtime wiring, и зафиксировать
explicit pre-wiring checklist для Stage 8.48+.

Stage 8.47 не меняет production code, tests, runtime behavior, routes,
public API, OpenAPI, frontend, generated clients или roadmap/root status
files.

## 2. Current inspected state

После Stage 8.46 internal confirmed-search transition chain включает:

| Компонент | Stage | Статус |
|---|---|---|
| `ExecuteConfirmedSearchTransitionUseCase` | 8.40 | Internal skeleton; не подключён к runtime. |
| `ExecuteConfirmedSearchTransitionResult` | 8.40 | Typed result (4 variants). |
| `expiresAt` в attempt model | 8.43 | TTL metadata; no route wiring. |
| `STALE_EXECUTION` failure reason | 8.43 | Stale classification. |
| Stale `IN_PROGRESS` detection | 8.43 | Store-level; stale → `FAILED(STALE_EXECUTION)`. |
| Retry transition support | 8.44 | Store/use-case level; retry-allowed `FAILED`. |
| Stage 7 compatibility proof | 8.45 | Route-level tests; no production changes. |
| `ConfirmedSearchTransitionResponseDirective` | 8.46 | Internal typed directive. |
| `MapConfirmedSearchTransitionResultToResponseDirectiveUseCase` | 8.46 | Internal mapper; non-results only. |

Текущий runtime (`AssistantLlmRouteWiringUseCase`):

- `PostConfirmationDecision.Confirmed` → immediate `consumePendingConfirmation` →
  text-only `CONFIRMATION_RECEIVED_MESSAGE`;
- не использует `ExecuteConfirmedSearchTransitionUseCase`;
- не использует response mapper;
- не использует attempt store;
- не создаёт `hotelSearchId`;
- не возвращает `show_hotel_results`.

## 3. Implemented pieces

Stage 8.40–8.46 produced internally coherent skeleton chain:

1. **Orchestration** — `ExecuteConfirmedSearchTransitionUseCase` связывает
   guard, attempt planning, store persistence, retry eligibility и fake/no-op
   transition в единый flow.
2. **Attempt lifecycle** — `expiresAt` TTL, `STALE_EXECUTION` failure reason,
   stale detection в store.
3. **Retry support** — `FAILED(STALE_EXECUTION)` и `FAILED(SEARCH_CREATION_FAILED)`
   allow retry через `savePrepared`; `FAILED(EXECUTION_STATE_UNKNOWN)` blocks.
4. **Response mapping** — internal typed directive mapper; all current results
   map to safe non-results directives.
5. **Compatibility proof** — 2 route-level tests + existing coverage доказывают
   Stage 7 strict handoff unchanged.

## 4. Readiness assessment

### 4.1 Orchestration readiness

`ExecuteConfirmedSearchTransitionUseCase` sufficient как internal skeleton.
Он корректно обрабатывает guard, attempt planning, store persistence, stale
detection и retry eligibility.

**Но**: use case остаётся fake/no-op — он не вызывает
`CreateHotelSearchUseCase`, не создаёт actual search, и execution result
всегда `PreparedButNotExecuted`. Attempt всегда `IN_PROGRESS` (без
`SUCCEEDED` с real `HotelSearchId`).

**Verdict**: skeleton ready for internal composition; not ready for
actual search creation.

### 4.2 Lifecycle readiness

TTL/stale/retry rules sufficient для in-memory stage:

- `expiresAt` + stale detection → `FAILED(STALE_EXECUTION)`.
- Retry allowed для `STALE_EXECUTION` и `SEARCH_CREATION_FAILED`.
- Retry blocked для `EXECUTION_STATE_UNKNOWN`.

**Limitations**:

- No durable attempt history. In-memory store loses all attempts on restart.
- No attempt history per key — retry replaces existing failed attempt.
- No retry counter — unlimited retries for retry-allowed reasons.

**Verdict**: lifecycle sufficient for skeleton wiring; not sufficient for
production readiness. Отсутствие durable history не блокирует narrow
non-results wiring, но блокирует production deployment.

### 4.3 Response mapping readiness

Internal mapper sufficient для non-results directives:

- `Transitioned` → `ASK_CLARIFICATION` + `PROCESSING`.
- `DuplicateDetected` → `ASK_CLARIFICATION` + `ALREADY_PROCESSING`.
- `GuardRejected` → `ASK_CLARIFICATION` + `CONFIRMATION_REJECTED`.
- `StoreRejected` → `ASK_CLARIFICATION` + `TEMPORARY_FAILURE`.

**Все current results**: `hotelSearchId = null`, `mayShowHotelResults = false`,
`shouldConsumePendingConfirmation = false`.

**Verdict**: non-results mapping sufficient for safe wiring. Successful-results
mapping (`SHOW_HOTEL_RESULTS` + real search id) remains blocked until actual
execution stage.

### 4.4 Consume ordering readiness

**Consume ordering is the primary blocker.**

Current runtime: `PostConfirmationDecision.Confirmed` → immediate
`consumePendingConfirmation` → text-only response.

Stage 8.42 policy: `markConsumed` допустим только после actual `SUCCEEDED`
attempt recording с real `HotelSearchId`. Current skeleton не имеет actual
`SUCCEEDED` recording.

Если future wiring stage изменит `Confirmed` branch, чтобы вызвать
orchestration вместо immediate consume, это **behavioral change**:

- Existing route test `positiveConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch`
  explicitly asserts pending becomes `null` after "да".
- Future wiring with non-results directive would leave pending active.
- This breaks existing test and changes user-visible behavior.

**Verdict**: consume ordering не готов. Любое wiring, которое изменяет
`Confirmed` branch, должно быть separate bounded stage с explicit test
updates и behavior change documentation.

### 4.5 Stage 7 compatibility readiness

Stage 8.45 доказал compatibility на current runtime:

- `stage8CompatibilityFullConfirmationCycleDoesNotCreateHotelSearch` — full
  Stage 8 flow не создаёт search.
- `stage8CompatibilityStrictHandoffRemainsOnlySearchCreationPath` — strict
  handoff остаётся единственным search creation path.

**Для future wiring** потребуются additional tests:

- Non-results directive test: orchestration + mapper produce safe directive
  без `hotelSearchId` и `show_hotel_results`.
- `markConsumed` absence test: wiring не вызывает `markConsumed` для new
  execution path.
- Stage 7 priority test: strict handoff takes priority over orchestration
  даже после wiring.

**Verdict**: current tests sufficient for current runtime. Future wiring
stage needs dedicated compatibility tests.

### 4.6 Runtime integration point readiness

Потенциальный integration point: `AssistantLlmRouteWiringUseCase
.withPostConfirmationDecision` для `PostConfirmationDecision.Confirmed`.

Текущий branch:

```
Confirmed -> consumePendingConfirmation -> withClarification(CONFIRMATION_RECEIVED_MESSAGE)
```

Future wiring мог бы:

```
Confirmed
  -> ExecuteConfirmedSearchTransitionUseCase(request)
  -> MapToResponseDirective(result)
  -> MapDirectiveToResponse(directive) // future
  -> response (ask_clarification + safe text)
  -> conditional markConsumed (future, only after SUCCEEDED)
```

**Blocker**: нет explicit integration composition boundary. Wiring
требует:

- создание `ExecuteConfirmedSearchTransitionRequest` из runtime context
  (session id, decision, pending confirmation, clock);
- вызов orchestration use case;
- вызов mapper;
- mapping directive → `AcceptedAssistantMessage`;
- conditional consume logic.

Эта композиция может быть:

a) inline в `AssistantLlmRouteWiringUseCase`;
b) отдельный composition use case (e.g. `ComposeConfirmedSearchTransitionResponseUseCase`).

Option (b) предпочтительнее: separate composition use case позволяет
unit-test composition logic до route wiring.

**Verdict**: integration point identified, но composition boundary не
определена. Нужен отдельный design/implementation step.

## 5. Blockers

| # | Blocker | Тип | Status |
|---|---|---|---|
| B1 | **No actual execution.** Orchestration produces `PreparedButNotExecuted` only. No `SUCCEEDED` with real `HotelSearchId`. | Architecture | Blocked until execution stage. |
| B2 | **Consume ordering conflict.** Current runtime consumes immediately. Future wiring must not consume without actual `SUCCEEDED`. Changing this requires test updates and behavior change stage. | Behavioral | Blocked. |
| B3 | **No integration composition boundary.** Нет explicit composition use case или wiring plan для orchestration + mapper в route context. | Design | Needs separate stage. |
| B4 | **Successful-results mapping absent.** Mapper maps all results to non-results. `SHOW_HOTEL_RESULTS` + real search id mapping remains future work. | Architecture | Blocked until execution stage. |
| B5 | **Pending confirmation lifecycle after wiring.** Если pending не consumed, как он expires? Как interact с future re-confirmation? | Design | Needs explicit policy. |
| B6 | **Wiring-stage compatibility tests.** Нет route-level tests для future wiring behavior. | Testing | Needed before wiring. |

## 6. Pre-wiring checklist

Перед любым route wiring должны быть выполнены:

| # | Item | Type |
|---|---|---|
| C1 | Integration composition boundary defined (composition use case или explicit wiring plan). | Design/implementation. |
| C2 | Consume ordering decision: explicit policy for when `markConsumed` is called. | Design. |
| C3 | Pending confirmation lifecycle after non-consume: expiry behavior documented. | Design. |
| C4 | Existing route test `positiveConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch` updated или replaced. | Test update. |
| C5 | New wiring compatibility tests: non-results directive, no `hotelSearchId`, no `show_hotel_results`, no `markConsumed` from new path. | Tests. |
| C6 | Stage 7 strict handoff priority test для post-wiring behavior. | Tests. |
| C7 | Directive-to-`AcceptedAssistantMessage` mapping defined. | Design/implementation. |

Items that remain blocked until actual execution stage:

- `markConsumed` for new execution path (requires actual `SUCCEEDED`).
- `show_hotel_results` response (requires actual search creation).
- `hotelSearchId` in response (requires actual search creation).
- Successful-results mapping (requires real `HotelSearchId`).

## 7. Recommendation

Stage 8.40–8.46 skeletons are internally coherent and useful. They
provide correct orchestration, lifecycle, retry, response mapping и
compatibility proof на internal level.

**Full route wiring is not ready** из-за:

- consume ordering conflict с existing behavior;
- absence of integration composition boundary;
- absence of actual execution/search creation;
- need for explicit test updates.

**Narrow non-results wiring may be planned** только после:

- integration composition boundary (C1);
- consume ordering decision (C2);
- pending lifecycle policy (C3);
- test updates (C4-C6);
- directive-to-response mapping (C7).

Ближайший safe implementation stage — **integration composition skeleton**
(отдельный composition use case, который связывает orchestration + mapper
без route wiring), followed by wiring readiness gate и только затем
narrow route wiring stage.

`markConsumed`, `hotelSearchId`, `show_hotel_results`, `CreateHotelSearchUseCase`
и successful-results mapping **должны оставаться blocked** до отдельного
actual execution stage.

## 8. Explicit non-goals

Stage 8.47 не создаёт и не меняет:

- Production code.
- Tests.
- Route wiring или runtime composition.
- Integration composition use case.
- `Application.kt`, `AssistantLlmRouteWiringUseCase`, assistant routes.
- `CreateHotelSearchUseCase` call.
- Actual `hotelSearchId` creation.
- `show_hotel_results` response.
- `markConsumed` wiring.
- Provider, network, API keys, auth, durable storage, booking flow.
- OpenAPI contracts, frontend, generated clients.
- Roadmap/root status files.

## 9. Suggested next stage

Safe Stage 8.48: **integration composition skeleton** (backend-only).

Добавить internal composition use case, который связывает:

- `ExecuteConfirmedSearchTransitionUseCase`;
- `MapConfirmedSearchTransitionResultToResponseDirectiveUseCase`;
- directive-to-response text mapping (internal, non-results only);
- pending confirmation lookup (read-only, no consume).

Composition use case не подключается к routes. Он позволяет unit-test
полный composition flow до route wiring.

Out of scope:

- Route wiring.
- `markConsumed`.
- `CreateHotelSearchUseCase`.
- `hotelSearchId` / `show_hotel_results`.
- Existing test updates.
- Actual execution.

## 10. Validation

Review-only inspection:

- `ExecuteConfirmedSearchTransitionUseCase` — not referenced from `Application.kt` или `AssistantLlmRouteWiringUseCase`.
- `MapConfirmedSearchTransitionResultToResponseDirectiveUseCase` — not referenced from runtime.
- `AssistantLlmRouteWiringUseCase.withPostConfirmationDecision` — `Confirmed` branch uses immediate `consumePendingConfirmation` без orchestration.
- All existing route tests pass unchanged.
- Stage 7 strict handoff tests pass.
- `git status --short` — working tree clean.

## 11. Verdict

**Blocked** — route wiring is not safe yet.

Stage 8.40–8.46 produced internally coherent skeleton chain, но route wiring
blocked из-за consume ordering conflict (B2), отсутствия integration
composition boundary (B3), отсутствия actual execution (B1), и
необходимости test updates (B6). Narrow non-results wiring может быть
запланирована после integration composition skeleton stage, consume
ordering decision, pending lifecycle policy и explicit test updates.
`markConsumed`, `hotelSearchId`, `show_hotel_results` и
`CreateHotelSearchUseCase` должны оставаться blocked до отдельного
actual execution stage.
