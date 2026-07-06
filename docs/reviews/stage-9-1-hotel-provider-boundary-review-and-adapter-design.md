# Stage 9.1 — Hotel Provider Boundary Review and Adapter Design

## 1. Scope

Stage 9.1 — docs/review/design-only stage. Первый implementation-facing
design step в Stage 9.

Stage 9.1:

1. Inspect существующую hotel search/provider/domain boundary.
2. Определяет, как `FakeHotelOfferProvider` вписан в локальный search flow.
3. Определяет будущий provider integration seam без реализации real provider calls.
4. Решает, что должно принадлежать provider boundary vs domain/application mapping.
5. Определяет требуемую DTO/domain normalization для Stage 9.2.
6. Определяет adapter design options для Stage 9.3.
7. Сохраняет Stage 8 confirmation lifecycle behavior.
8. Производит prompt для Stage 9.2.

Stage 9.1 не меняет production code, tests, runtime, routes, API,
OpenAPI, frontend, generated clients, product baseline или
architecture baseline.

## 2. Sources inspected

### 2.1 Backend source

| File | Layer | Purpose |
|---|---|---|
| `domain/provider/HotelOfferProviderBoundary.kt` | Domain | Provider-agnostic fun interface |
| `domain/hotel/HotelSearchCriteria.kt` | Domain | Search criteria (destination, dates, guests, rooms) |
| `domain/hotel/HotelOffer.kt` | Domain | Domain offer model (provider-independent) |
| `domain/hotel/HotelSearch.kt` | Domain | Search result (id, session, criteria, status, offers) |
| `domain/hotel/RankedHotelOffer.kt` | Domain | Ranked offer wrapper (offer + matchSummary) |
| `domain/hotel/HotelOfferRanker.kt` | Domain | Deterministic ranking by availability/rating/price/id |
| `infrastructure/provider/FakeHotelOfferProvider.kt` | Infrastructure | Deterministic local adapter |
| `infrastructure/llm/FakeLlmClient.kt` | Infrastructure | Deterministic LLM fake (parallel boundary pattern) |
| `application/llm/LlmClient.kt` | Application | LLM fun interface (parallel boundary pattern) |
| `application/hotel/CreateHotelSearchUseCase.kt` | Application | Search creation: session check → provider call → rank → save |
| `application/hotel/CreateHotelSearchCommand.kt` | Application | Search creation command (sessionId + criteria) |
| `application/hotel/HotelSearchBoundary.kt` | Application | Hotel search boundary interface |
| `application/assistant/AssistantLlmRouteWiringUseCase.kt` | Application | Stage 8 route wiring: LLM + confirmation + transition |
| `application/assistant/AssistantHotelSearchHandoffUseCase.kt` | Application | Stage 7 strict handoff |
| `application/assistant/ExecuteConfirmedSearchTransitionUseCase.kt` | Application | Stage 8 confirmed-search execution with attempt lifecycle |
| `application/assistant/ProceedWithCandidateCriteria.kt` | Application | LLM-extracted criteria model |
| `application/assistant/ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper.kt` | Application | Criteria mapping: LLM → domain |
| `Application.kt` | Wiring | DI composition root (all InMemory + Fake) |
| `api/HotelSearchRoutes.kt` | API | POST /hotel-searches, GET /{id}/offers |
| `api/HotelSearchResponse.kt` | API | Search response mapping |
| `api/HotelOfferResponse.kt` | API | Offer response mapping with providerFacts |
| `api/ApiRoutes.kt` | API | Route composition |

### 2.2 Documentation

| Document | Role |
|---|---|
| `AGENTS.md` | Governance |
| `docs/roadmap/roadmap.md` | Primary roadmap |
| `docs/architecture/architecture-baseline.md` | Architecture baseline |
| `docs/product/product-baseline.md` | Product baseline |
| `docs/reviews/stage-9-0-documentation-audit-and-stage-9-planning-readiness-review.md` | Stage 9.0 planning |

## 3. Current hotel search/provider flow

### 3.1 Provider call chain

```
Route (API)
  → CreateHotelSearchUseCase.createSearch(command)
    → assistantSessionStateStore.findById(sessionId)  // session validation
    → hotelOfferProvider.search(criteria)              // provider call
    → hotelOfferRanker.rank(providerOffers)             // domain ranking
    → hotelSearchStateStore.save(HotelSearch(...))      // local storage
```

### 3.2 Two entry paths to provider

| Path | Entry | Provider call via |
|---|---|---|
| Stage 7 strict handoff | `AssistantHotelSearchHandoffUseCase` → `hotelSearchBoundary.createSearch(command)` | `CreateHotelSearchUseCase` |
| Stage 8 confirmation | `ExecuteConfirmedSearchTransitionUseCase` → `hotelSearchBoundary.createSearch(command)` (line 110) | `CreateHotelSearchUseCase` |

Оба пути проходят через `HotelSearchBoundary` interface → `CreateHotelSearchUseCase`
→ `HotelOfferProviderBoundary.search(criteria)`.

### 3.3 DI wiring

`Application.kt` hardcodes:

```kotlin
val hotelSearchBoundary = CreateHotelSearchUseCase(
    hotelOfferProvider = FakeHotelOfferProvider(),
    ...
)
```

Никакого configuration injection, provider selection или runtime switching.

## 4. Current FakeHotelOfferProvider role

### 4.1 What it does

- Возвращает 2 deterministic offers per destination.
- Генерирует `id` и `providerReference` из slug destination.
- Hardcoded amenities: `["Wi-Fi", "Gym"]` и `["Wi-Fi", "Breakfast"]`.
- Hardcoded rating, price, review count.
- `source = "local_fake_provider"`.
- `freshness = FRESH`.
- `availability = AVAILABLE` или `LIMITED`.

### 4.2 What it does NOT do

- No external I/O.
- No network calls.
- No error simulation (never throws).
- No partial/degraded results.
- No pagination.
- No rate limiting.
- No authentication.
- No configuration.

### 4.3 Current acceptability

Stage 8 carryover явно принимает `FakeHotelOfferProvider` как acceptable.
Он обеспечивает:

- Predictable behavior для Stage 7 strict handoff tests.
- Predictable behavior для Stage 8 confirmation lifecycle tests.
- Deterministic search results.
- Local-only execution без external dependencies.

## 5. Current domain/application boundaries

### 5.1 Domain layer (provider-independent)

| Type | Responsibility | Provider coupling |
|---|---|---|
| `HotelSearchCriteria` | Search input parameters | None — pure domain |
| `HotelOffer` | Provider-independent offer representation | `source`, `freshness`, `providerReference` — metadata fields, not provider DTOs |
| `HotelSearch` | Search result aggregate | None |
| `RankedHotelOffer` | Ranked offer with explanation | None |
| `HotelOfferRanker` | Deterministic ranking | None |

### 5.2 Provider boundary (domain layer)

`HotelOfferProviderBoundary` — fun interface в domain layer:

```kotlin
fun interface HotelOfferProviderBoundary {
    fun search(criteria: HotelSearchCriteria): List<HotelOffer>
}
```

Ключевые свойства:

- **Provider-agnostic**: не ссылается на конкретного провайдера.
- **Synchronous**: блокирующий вызов.
- **No error taxonomy**: исключения propagate как `Exception`.
- **No configuration**: нет credentials, URL, timeout.
- **No retry**: retry handled by Stage 8 attempt lifecycle.
- **No pagination**: returns all results.

### 5.3 Application layer

| Type | Responsibility | Provider awareness |
|---|---|---|
| `CreateHotelSearchUseCase` | Orchestrate: session check → provider → rank → save | Knows `HotelOfferProviderBoundary` interface only |
| `HotelSearchBoundary` | Application-level search interface | No provider knowledge |
| `ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper` | LLM criteria → domain criteria | No provider knowledge |

### 5.4 Infrastructure layer

| Type | Responsibility |
|---|---|
| `FakeHotelOfferProvider` | Deterministic adapter implementing `HotelOfferProviderBoundary` |
| `FakeLlmClient` | Deterministic adapter implementing `LlmClient` (parallel pattern) |

### 5.5 API layer

`HotelOfferResponse` уже содержит `source`, `freshness`, `providerFacts` fields —
infrastructure для future provider metadata exposure уже существует в API response shape.

`HotelSearchResponse.Metadata` содержит `providerState` field — также ready
для future provider state signaling.

## 6. Stage 8 confirmation lifecycle touchpoints

### 6.1 Provider call from Stage 8

`ExecuteConfirmedSearchTransitionUseCase:109-122`:

```kotlin
val createdSearch = try {
    hotelSearchBoundary.createSearch(commandPlan.command)
} catch (e: Exception) {
    attemptStore.markFailed(...SEARCH_CREATION_FAILED...)
    return StoreRejected(...)
}
```

**Key observation**: Stage 8 ловит `Exception` от `createSearch`.
Это означает, что любые provider errors, которые propagate как
исключения, будут caught и handled:

- `createSearch` throws → attempt marked FAILED(SEARCH_CREATION_FAILED).
- Attempt lifecycle retry support (Stage 8.44) already handles FAILED attempts.

### 6.2 Stage 8 retry and provider errors

Stage 8 attempt lifecycle (Stage 8.44) уже поддерживает retry:

- `FAILED(SEARCH_CREATION_FAILED)` → retry allowed.
- `FAILED(STALE_EXECUTION)` → retry allowed.
- `FAILED(EXECUTION_STATE_UNKNOWN)` → retry blocked.

Это означает, что **provider error taxonomy уже partially anticipated**:
любая ошибка provider, приводящая к exception в `createSearch`,
классифицируется как `SEARCH_CREATION_FAILED` и допускает retry.

### 6.3 What Stage 8 does NOT know about providers

- Provider-specific error types.
- Provider-specific retry policies.
- Provider rate limiting.
- Provider partial results.
- Provider timeout vs network error distinction.

Stage 8 treats all `createSearch` failures uniformly.

### 6.4 Preservation requirements

Любое изменение provider boundary в Stage 9.1-9.5 должно:

1. Сохранить `HotelSearchBoundary.createSearch(command): HotelSearch` signature.
2. Сохранить Stage 8 `try/catch` behavior: provider failure → `SEARCH_CREATION_FAILED` → retry eligible.
3. Сохранить `show_hotel_results` + `hotelSearchId` response shape.
4. Сохранить Stage 7 strict handoff behavior (через `AssistantHotelSearchHandoffUseCase`).
5. Не изменить `ExecuteConfirmedSearchTransitionUseCase` behavior.
6. Не изменить `AssistantLlmRouteWiringUseCase` behavior.

## 7. Provider integration risks

### 7.1 Error propagation

**Current**: `FakeHotelOfferProvider.search()` never throws. `CreateHotelSearchUseCase`
does not catch provider exceptions (caught upstream by `ExecuteConfirmedSearchTransitionUseCase`).

**Risk**: real provider will throw network/auth/rate-limit errors. Current catch
at `ExecuteConfirmedSearchTransitionUseCase:109-122` handles all as
`SEARCH_CREATION_FAILED`. This is acceptable for Stage 9.3 skeleton but may
need refinement in Stage 9.4 error taxonomy.

### 7.2 Configuration

**Current**: `FakeHotelOfferProvider()` hardcoded in `Application.kt`.

**Risk**: real provider needs URL, credentials, timeouts. Need configuration seam
before real provider integration.

### 7.3 Result shape

**Current**: `HotelOffer` has fixed fields (hotelName, city, country, totalPrice,
currency, rating, reviewCount, amenities, availability, source, freshness).

**Risk**: real providers may have additional fields (photos, description,
cancellation policy, coordinates, property type, star rating) or missing
fields (some providers don't return review count). Need normalization strategy.

### 7.4 Search criteria

**Current**: `HotelSearchCriteria` has destination, dates, guests, rooms.

**Risk**: real providers may require or support additional criteria (star rating,
amenity filters, price range, property type, meal plan). May need criteria
expansion or optional fields.

### 7.5 Source/freshness metadata

**Current**: `source = "local_fake_provider"`, `freshness = FRESH` — static.

**Risk**: real provider needs actual source identification and freshness tracking.
`HotelOffer.source` and `HotelOffer.freshness` fields already exist — good.

### 7.6 Synchronous boundary

**Current**: `HotelOfferProviderBoundary.search()` is synchronous.

**Risk**: real provider calls are I/O-bound. May need coroutine/async support
in future. Not blocking for Stage 9.1-9.5 (synchronous is acceptable for
MVP hotel search).

### 7.7 DI composition

**Current**: `Application.kt` hardcodes `FakeHotelOfferProvider()`.

**Risk**: need provider selection at startup (fake vs real). Need configuration-driven
provider injection. Not blocking for Stage 9.1 (design-only).

## 8. Boundary ownership decision

| Concern | Owner | Layer | Rationale |
|---|---|---|---|
| Provider request building (criteria → provider request format) | **Adapter** (infrastructure) | Infrastructure | Provider-specific format details; domain stays provider-agnostic. |
| Provider response parsing (raw response → intermediate DTO) | **Adapter** (infrastructure) | Infrastructure | Provider-specific parsing; isolate format changes. |
| Provider error translation (provider error → domain error) | **Adapter** (infrastructure) | Infrastructure | Provider-specific error codes; translate to domain-typed errors. |
| Provider result normalization (intermediate DTO → domain `HotelOffer`) | **Adapter** (infrastructure) | Infrastructure | Normalization rules specific to each provider; keeps domain clean. |
| Domain mapping (criteria enrichment, source tagging) | **Adapter** (infrastructure) | Infrastructure | Adding source, freshness, provider reference — adapter responsibility. |
| Application use case orchestration | **CreateHotelSearchUseCase** (application) | Application | Session validation, provider call, ranking, storage — unchanged. |
| Attempt lifecycle behavior | **ExecuteConfirmedSearchTransitionUseCase** (application) | Application | Stage 8 lifecycle — unchanged. |
| `show_hotel_results` response behavior | **Route + response mapping** (application/API) | Application/API | Response shape — unchanged. |
| Persistence | **Stores** (application/infrastructure) | Application/Infrastructure | InMemory now; durable — future infrastructure stage. |
| Frontend/OpenAPI exposure | **API response models** (API) | API | `HotelOfferResponse` already has source/freshness/providerFacts — unchanged. |

**Key principle**: domain model (`HotelOffer`, `HotelSearchCriteria`) remains
provider-agnostic. All provider-specific knowledge lives in infrastructure adapter.

## 9. Adapter design options

### Option A: Keep current provider interface; add adapter behind it later

**Description**: сохранить `HotelOfferProviderBoundary` как есть. В Stage 9.3
создать `RealHotelOfferProviderAdapter : HotelOfferProviderBoundary`,
который внутри вызывает real provider API и нормализует результат в
`List<HotelOffer>`. `FakeHotelOfferProvider` остается как default.

| Aspect | Assessment |
|---|---|
| Benefits | Минимальные изменения; сохраняет Stage 7/Stage 8 behavior; domain model unchanged; easy fake-vs-real switch via DI. |
| Risks | Нет typed provider errors на boundary; adapter может скрыть provider-specific behavior; error taxonomy добавляется позже. |
| Stage 8 compatibility | **Full** — `CreateHotelSearchUseCase` и `ExecuteConfirmedSearchTransitionUseCase` unchanged. |
| Suitability for 9.2/9.3 | High — 9.2 designs normalization rules; 9.3 implements adapter behind same interface. |

### Option B: Introduce provider-specific adapter interface later

**Description**: добавить новый interface (например, `HotelProviderAdapter`)
с более широким контрактом: typed errors, provider config, result metadata.
`HotelOfferProviderBoundary` остается как domain-facing, adapter внутри
делегиирует к нему.

| Aspect | Assessment |
|---|---|
| Benefits | Typed errors на adapter level; explicit config injection; clean separation. |
| Risks | Дополнительный interface layer; может over-engineer для одного provider; сложнее test setup. |
| Stage 8 compatibility | **Full** — domain-facing boundary unchanged. |
| Suitability for 9.2/9.3 | Medium — 9.2 needs to design two layers; 9.3 more complex. |

### Option C: Introduce normalized provider DTOs before adapter skeleton

**Description**: сначала создать intermediate normalized DTOs
(например, `NormalizedHotelOfferResult`), затем adapter skeleton.
Adapter преобразует provider-specific DTO → normalized DTO → domain `HotelOffer`.

| Aspect | Assessment |
|---|---|
| Benefits | Explicit normalization layer; testable in isolation; supports multi-provider future. |
| Risks | Extra DTO layer; no real provider yet to validate against; may pre-empt real provider contract. |
| Stage 8 compatibility | **Full** — domain boundary unchanged. |
| Suitability for 9.2/9.3 | Medium — 9.2 designs DTOs; 9.3 implements adapter; но без real provider DTOs to map from, DTOs speculative. |

### Option D: Defer all provider-specific DTOs until real provider selection

**Description**: не создавать provider-specific или normalized DTOs.
Дождаться выбора конкретного real provider, затем design DTOs под него.
Stage 9.2-9.3 focus на design rules и adapter skeleton без DTOs.

| Aspect | Assessment |
|---|---|
| Benefits | No speculative DTOs; avoid premature contract; adapter skeleton ready for any provider. |
| Risks | 9.3 skeleton будет minimal; normalization rules без DTOs — abstract. |
| Stage 8 compatibility | **Full** — no changes. |
| Suitability for 9.2/9.3 | High — 9.2 defines normalization rules abstractly; 9.3 creates skeleton; DTOs added when provider selected. |

## 10. Recommended adapter design direction

**Option A: keep current provider interface; add adapter behind it later.**

### Обоснование

1. **Сохраняет Stage 8 lifecycle behavior**: `HotelOfferProviderBoundary`
   остается единственным provider-facing контрактом в domain. `CreateHotelSearchUseCase`
   и `ExecuteConfirmedSearchTransitionUseCase` не меняются.

2. **Минимальные изменения**: не нужно вводить дополнительные interfaces,
   DTOs или abstraction layers. Adapter просто реализует существующий
   `HotelOfferProviderBoundary`.

3. **Параллель с LlmClient**: `LlmClient` (fun interface) + `FakeLlmClient`
   (infrastructure) — та же pattern. Она proven и consistent.

4. **Configuration injection via DI**: `Application.kt` уже injects
   `hotelOfferProvider` через constructor. Замена `FakeHotelOfferProvider()`
   на configurable provider selection — минимальное wiring change.

5. **Error taxonomy добавляется в Stage 9.4**: typed provider errors
   не нужны на boundary для adapter skeleton. Stage 8 уже ловит
   `Exception` как `SEARCH_CREATION_FAILED`.

6. **Normalized DTOs deferred**: без конкретного real provider,
   normalized DTOs speculative. Adapter может напрямую мапить
   provider response в domain `HotelOffer`.

7. **Source/freshness metadata**: `HotelOffer` уже имеет `source`
   и `freshness` fields — adapter заполняет их из provider metadata.

### Что не входит в recommendation

- Не создавать provider-specific adapter interface (Option B) — over-engineering для одного provider.
- Не создавать normalized DTOs (Option C) — speculative без real provider contract.
- Не откладывать design (Option D) — нужно design rules для Stage 9.2.

### Fake-vs-real seam

В Stage 9.3 adapter skeleton:

- `FakeHotelOfferProvider` остается default.
- Real adapter (имя TBD) implements `HotelOfferProviderBoundary`.
- Выбор определяется configuration (env var / config property).
- Без real provider credentials — real adapter не активируется.
- Tests всегда используют fake.

## 11. Stage 9.2 candidate scope

**Stage 9.2 — Provider Result Contract and Domain Mapping**

Это review/design-only sub-stage.

Scope:

1. Design provider result contract: какие fields обязательны, какие optional,
   какие derived. Определить mapping rules из provider response → domain `HotelOffer`.

2. Design `HotelSearchCriteria` gap analysis:
   - Какие fields нужны для real provider (star rating, amenity filters,
     price range, property type, meal plan, coordinates).
   - Какие fields optional vs required.
   - Нужно ли расширять domain `HotelSearchCriteria` или оставить adapter
     обогащать request внутри.

3. Design `HotelOffer` gap analysis:
   - Какие fields нужны для real provider offers (photos, description,
     cancellation policy, coordinates, property type, star rating,
     room type, meal plan, booking conditions).
   - Какие fields optional vs required.
   - Нужно ли расширять domain `HotelOffer` или adapter enriches via
     existing fields (amenities list, source, freshness).

4. Design provider result normalization rules:
   - Availability mapping (provider-specific → AVAILABLE/LIMITED/UNKNOWN).
   - Price normalization (per-night vs total, tax inclusion, currency).
   - Source tagging (provider name, response timestamp).
   - Freshness tracking (when data was fetched).
   - Rating normalization (different scales).

5. Design source/freshness marker strategy:
   - How `source` identifies the provider.
   - How `freshness` reflects data age.
   - How `providerReference` maps to provider's offer ID.

6. Produce mapping specification document.

Out of scope:

- Implementation.
- Tests.
- Runtime changes.
- Domain model changes (analysis only; changes deferred to Stage 9.3 if needed).
- API/OpenAPI changes.
- Frontend changes.
- Provider credentials.
- Real API calls.

## 12. Guardrails for Stage 9.2

- Stage 9.2 — review/design-only.
- Не создавать production code.
- Не создавать tests.
- Не менять domain models (`HotelOffer`, `HotelSearchCriteria`).
- Не менять runtime/routes/API/OpenAPI/frontend/generated clients.
- Не использовать и не хранить provider credentials/API keys.
- Не делать real API calls.
- Не менять `HotelOfferProviderBoundary` interface.
- Не менять `CreateHotelSearchUseCase` behavior.
- Не менять Stage 8 confirmation lifecycle.
- Не менять Stage 7 strict hotel-search handoff.
- Не claim production readiness.
- Не начинать Stage 9.3 implementation.
- Не смешивать provider mapping design с LLM, persistence, frontend, auth, booking или observability work.

## 13. Validation expectations for Stage 9.2

- Review-only inspection domain models и provider boundary.
- Design report в `docs/reviews/stage-9-2-provider-result-contract-and-domain-mapping.md`.
- Entry в `docs/reviews/README.md`.
- Minimal roadmap update (Stage 9.2 completion, next step).
- `git status --short` — только expected docs changes.
- `git diff --check` — no errors.
- Tests не запускаются: stage is docs/design-only.

## 14. Prompt для Stage 9.2

```
Мы продолжаем проект travel-assistant.

Пожалуйста, отвечай на русском языке. Технические имена классов, файлов,
enum values, commit messages и команды оставляй как есть.

Контекст

* Репозиторий: travel-assistant
* Branch: stage-9
* Последний завершённый sub-stage: Stage 9.1
* Stage 9 implementation ещё не начат.
* Production readiness не claimed.
* Все InMemory stores, FakeLlmClient, FakeHotelOfferProvider — accepted carryover.

Текущая provider boundary (после Stage 9.1)

* `HotelOfferProviderBoundary` — fun interface: `search(HotelSearchCriteria): List<HotelOffer>`.
* `FakeHotelOfferProvider` — deterministic local adapter без external I/O.
* `CreateHotelSearchUseCase` — session check → provider.search(criteria) → rank → save.
* `HotelSearchCriteria` — destination, checkInDate, checkOutDate, guests (adults/children), rooms.
* `HotelOffer` — id, providerReference, hotelName, city, country, totalPrice, currency, rating, reviewCount, amenities, availability, source, freshness.
* `HotelOffer.Availability` — AVAILABLE, LIMITED, UNKNOWN.
* `HotelOffer.Freshness` — FRESH, STALE, UNKNOWN.
* `HotelOffer.source` — string identifier ("local_fake_provider" for fake).
* `HotelOffer.providerReference` — provider-specific offer reference string.
* `HotelOffer.freshness` — data freshness marker.

Рекомендованное направление из Stage 9.1:

* Option A: keep current provider interface; add adapter behind it later.
* `HotelOfferProviderBoundary` остаётся единственным provider-facing контрактом в domain.
* Real adapter (Stage 9.3) реализует `HotelOfferProviderBoundary`.
* Normalized DTOs deferred до выбора конкретного real provider.
* Configuration injection через DI в Application.kt.

Задача

Выполни:

Stage 9.2 — Provider Result Contract and Domain Mapping

Это review/design-only sub-stage.

Цели Stage 9.2

1. Design provider result contract: какие fields обязательны, какие optional, какие derived. Определить mapping rules из provider response → domain `HotelOffer`.

2. Design `HotelSearchCriteria` gap analysis:
   - Какие fields нужны для real provider (star rating, amenity filters, price range, property type, meal plan, coordinates).
   - Какие fields optional vs required.
   - Нужно ли расширять domain `HotelSearchCriteria` или оставить adapter обогащать request внутри.

3. Design `HotelOffer` gap analysis:
   - Какие fields нужны для real provider offers (photos, description, cancellation policy, coordinates, property type, star rating, room type, meal plan, booking conditions).
   - Какие fields optional vs required.
   - Нужно ли расширять domain `HotelOffer` или adapter enriches via existing fields.

4. Design provider result normalization rules:
   - Availability mapping (provider-specific → AVAILABLE/LIMITED/UNKNOWN).
   - Price normalization (per-night vs total, tax inclusion, currency).
   - Source tagging (provider name, response timestamp).
   - Freshness tracking (when data was fetched).
   - Rating normalization (different scales).

5. Design source/freshness marker strategy:
   - How `source` identifies the provider.
   - How `freshness` reflects data age.
   - How `providerReference` maps to provider offer ID.

6. Produce mapping specification document.

Required output file

* `docs/reviews/stage-9-2-provider-result-contract-and-domain-mapping.md`

Отчёт должен включить:

1. Scope
2. Sources inspected
3. Current domain model assessment (HotelSearchCriteria)
4. Current domain model assessment (HotelOffer)
5. Criteria gap analysis
6. Offer gap analysis
7. Domain model extension recommendations (what to add now vs defer)
8. Provider result normalization rules
9. Source/freshness marker strategy
10. Mapping specification (provider response shape → domain HotelOffer)
11. Impact on existing Stage 7/Stage 8 flow
12. Guardrails для Stage 9.3
13. Prompt для Stage 9.3
14. Verdict

Strict guardrails

Do not implement any production code, tests, runtime changes, domain model
changes, API/OpenAPI changes, frontend changes, or provider credential
handling.

Do not claim production readiness.

Do not start Stage 9.3.

Validation commands

git status --short
git diff --check

Final response format

1. Созданные файлы
2. Изменённые файлы
3. Краткий итог
4. Checks
5. Commit recommendation: Stage 9.2 provider result contract and domain mapping
```

## 15. Stage 9.1 verdict

**Passed** — provider boundary reviewed, adapter design direction defined.

Stage 9.1:

1. Inspect 21 backend source files и 5 документов.
2. Задокументировал current hotel search/provider flow и два entry paths
   (Stage 7 strict handoff и Stage 8 confirmation).
3. Определил роль `FakeHotelOfferProvider`: deterministic local adapter,
   no I/O, no errors, accepted carryover.
4. Классифицировал boundary ownership: provider-specific knowledge
   (request building, response parsing, error translation, normalization,
   source tagging) принадлежит infrastructure adapter; domain model
   остается provider-agnostic.
5. Сравнил 4 adapter design options (A-D).
6. Рекомендовал Option A: сохранить `HotelOfferProviderBoundary`,
   добавить adapter за ним в Stage 9.3, parallel с `LlmClient` pattern.
7. Определил 7 provider integration risks и preservation requirements
   для Stage 8 lifecycle.
8. Определил scope, guardrails и validation для Stage 9.2.
9. Произвёл готовый prompt для Stage 9.2 на русском языке.

Production code не изменён. Runtime не изменён. Tests не запускались.
Production readiness не заявлена. Stage 9 implementation не начат.
