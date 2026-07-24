# Stage 9.0 — Documentation Audit and Stage 9 Planning Readiness Review

## 1. Scope

Stage 9.0 — docs/review/design-only stage. Объединяет два closely related
направления в одном safe documentation/design boundary:

A. Аудит и минимальный cleanup активной документации перед стартом Stage 9.
B. Planning/readiness review для выбора первого направления Stage 9.

Stage 9.0 не меняет production code, tests, runtime, routes, API,
OpenAPI, frontend, generated clients, product baseline или
architecture baseline.

## 2. Sources inspected

| Document | Classification | Inspected |
|---|---|---|
| `AGENTS.md` | Active governance | Yes |
| `README.md` | Entry point / navigation | Yes |
| `docs/ROADMAP.md` | Navigation summary | Yes |
| `docs/roadmap/roadmap.md` | Primary roadmap / source of truth | Yes |
| `docs/product/README.md` | Product index | Yes |
| `docs/product/product-baseline.md` | Product baseline | Yes |
| `docs/architecture/README.md` | Architecture index | Yes |
| `docs/architecture/architecture-baseline.md` | Architecture baseline | Yes |
| `docs/development/README.md` | Development governance index | Yes |
| `docs/guides/documentation-style-guide.md` | Documentation rules | Yes |
| `docs/reviews/README.md` | Reviews index | Yes |
| `docs/reviews/stage-8-56-end-to-end-confirmation-lifecycle-verification.md` | Stage 8.56 review | Yes |
| `docs/reviews/stage-8-57-stage-8-closure-and-readiness-gate.md` | Stage 8.57 closure | Yes |

Backend source inspected (read-only):

| Source | Purpose |
|---|---|
| `HotelOfferProviderBoundary.kt` | Provider-agnostic fun interface |
| `FakeHotelOfferProvider.kt` | Deterministic local adapter |
| `CreateHotelSearchUseCase.kt` | Hotel search creation via provider boundary |
| `HotelSearchCriteria.kt` | Domain search criteria |
| `HotelOffer.kt` | Domain offer model |
| `Application.kt` | DI wiring (all InMemory + Fake) |

## 3. Documentation audit findings

### 3.1 Real inconsistencies (cleaned up)

| # | Document | Issue | Criteria | Action |
|---|---|---|---|---|
| 1 | `docs/architecture/README.md:15` | "Stage 7 — MVP Implementation: в работе / ожидает отдельную явную задачу" — Stage 7 завершен. Stale active wording в navigation/index документе. | Conflicts with Stage 7 completed state. Creates uncertainty about current project status. | Updated: Stage 7 → "завершен"; added Stage 8 status line; updated Code/API/DB status to reflect Stage 8 work. |
| 2 | `docs/architecture/README.md:16` | "минимальный Kotlin + Ktor backend skeleton существует; business logic... не начаты" — устарело после Stage 8. Business logic (LLM orchestration, confirmation lifecycle) существует. | Conflicts with actual backend state. Hides real MVP progress. | Updated: отражает Kotlin + Ktor backend-основу с LLM orchestration и confirmation lifecycle; сохраняет, что real provider/DB/storage/frontend polish не начаты. |
| 3 | `docs/architecture/architecture-baseline.md:25` | "Stage 8 и любая будущая интеграционная или промышленная работа начинаются только через отдельные задачи" — Stage 8 уже завершен. | Stale wording implies Stage 8 not yet completed. | Updated: Stage 8 → "завершен (backend confirmation lifecycle)"; reference to Stage 9+ for future work. |
| 4 | `docs/architecture/architecture-baseline.md:27` | "Эта архитектурная основа не активирует Stage 8 или другую будущую реализацию." — Stage 8 завершен. | Stale Stage 8 activation concern. | Updated: убрано "Stage 8", оставлено general principle statement. |

### 3.2 Findings deliberately not changed

| # | Document | Issue | Reason not changed |
|---|---|---|---|
| 5 | `docs/product/product-baseline.md` | Section 2 не упоминает Stage 8; product baseline описывает post-Stage 0-5 state. | Product baseline описывает product scope, а Stage 8 — backend-internal lifecycle. Обновление baseline до post-Stage 0-8 было бы product direction change, не documentation cleanup. |
| 6 | `docs/architecture/architecture-baseline.md:22` | "ограниченный поиск через fake provider, детерминированное ранжирование и минимальный frontend существуют" — не упоминает Stage 8 confirmation lifecycle. | Корректное statement о том, что существует; не утверждает, что только это существует. Не коллизия. |
| 7 | Historical review artifacts | "Stage 8 не начат" / "Stage 7 в работе" в pre-stage-8 и Stage 7 review artifacts. | Historical artifacts корректны на момент создания. Массовая нормализация запрещена style guide и task scope. |
| 8 | `docs/reviews/README.md` current/latest list | Длинный нумерованный список (133 entries). | Стилистический вопрос, не функциональная коллизия. |
| 9 | `docs/ROADMAP.md` | Навигационный overview без Stage 9 sub-stages. | Navigation doc; не обязан отражать sub-stage detail. Description назначения Stage 9 корректно. |

### 3.3 No production readiness claims found

Ни один active/source-of-truth документ не содержит claims production readiness.
Roadmap, baselines и README явно отмечают, что production readiness не заявлена.
Historical review artifacts упоминают "production readiness" только в контексте "не является production readiness".

### 3.4 Stage 8 completion / Stage 9 not started consistency

| Source | Stage 8 status | Stage 9 status | Consistent? |
|---|---|---|---|
| `docs/roadmap/roadmap.md` section 1 | Завершен | Не начат | Yes |
| `docs/roadmap/roadmap.md` section 7 | Завершен | Запланирован | Yes |
| `docs/roadmap/roadmap.md` Stage 8 carryover | InMemory, FakeLlmClient, FakeHotelOfferProvider, static text | — | Yes |
| `docs/product/product-baseline.md` | Не упомянут (product scope) | Не упомянут | N/A |
| `docs/architecture/architecture-baseline.md` | Завершен (после cleanup) | Не упомянут | Yes |
| `docs/architecture/README.md` | Завершен (после cleanup) | Не упомянут | Yes |
| `README.md` | Refers to roadmap | Refers to roadmap | Yes |
| `docs/ROADMAP.md` | Navigation only | Navigation only | Yes |

### 3.5 Accepted carryover consistency

Carryover описан последовательно во всех relevant sources:

| Item | Roadmap | Stage 8.57 | Architecture baseline | Consistent? |
|---|---|---|---|---|
| InMemory stores | Yes | Yes | Yes | Yes |
| FakeLlmClient | Yes | Yes | — | Yes |
| FakeHotelOfferProvider | Yes | Yes | — | Yes |
| Static message text | Yes | Yes | — | Yes |
| Local-only execution | Yes | Yes | Yes | Yes |
| No external provider calls | Yes | Yes | Yes | Yes |
| No durable persistence | Yes | Yes | Yes | Yes |

### 3.6 Documentation structure assessment

- Source-of-truth docs, historical reports и reference docs не смешиваются.
- Roles documents clearly classified в indexes (`docs/product/README.md`, `docs/architecture/README.md`, `docs/reviews/README.md`).
- Navigation docs (`README.md`, `docs/ROADMAP.md`) ссылаются на primary roadmap.
- Style guide rules соблюдаются.

## 4. Documentation cleanup performed

| # | File | Change |
|---|---|---|
| 1 | `docs/architecture/README.md` | Updated Stage 7 status ("завершен"), added Stage 8 status line, updated Code/API/DB status to reflect Stage 8 work. |
| 2 | `docs/architecture/architecture-baseline.md` | Updated Stage 8 wording ("завершен"), future work reference to Stage 9+, removed stale "не активирует Stage 8". |
| 3 | `docs/roadmap/roadmap.md` | Added Stage 9.0 completion table, updated next planned step to Stage 9.1, updated Stage 9 section status. |
| 4 | `docs/reviews/README.md` | Added Stage 9.0 entry to inventory table and current/latest list. |

## 5. Documentation cleanup deliberately not performed

| # | Category | Reason |
|---|---|---|
| 1 | Product baseline Stage 8 mention | Would be product direction change, not documentation cleanup. |
| 2 | Historical review artifacts | Correct at time of creation; mass normalization forbidden. |
| 3 | Broad documentation rewrite | Out of scope; only local safe cleanup allowed. |
| 4 | Navigation doc beautification | No verifiable goal tied to Stage 9 readiness. |
| 5 | `docs/development/**` updates | Active engineering rules remain accurate; no stale status wording found. |

## 6. Current post-Stage 8 baseline

### 6.1 Backend

- Kotlin + Ktor backend-основа в `services/backend/`.
- Provider-independent `LlmClient` boundary с `FakeLlmClient`.
- `HotelOfferProviderBoundary` fun interface с `FakeHotelOfferProvider`.
- `CreateHotelSearchUseCase` — hotel search creation через provider boundary.
- Confirmation lifecycle: pending state → reply classification → post-confirmation decision → confirmed-search execution → consume-after-success.
- `ExecuteConfirmedSearchTransitionUseCase` — attempt lifecycle с TTL/stale/retry.
- All stores process-local (InMemory).
- All providers fake/deterministic.
- Domain model: `HotelSearchCriteria`, `HotelOffer`, `HotelSearch`, `RankedHotelOffer`.

### 6.2 What does not exist

- Real hotel provider integration.
- Real LLM provider.
- Durable persistence (PostgreSQL, Redis).
- Auth/API keys.
- Production observability.
- Frontend UX polish (rich cards, inline retry).
- Booking flow.
- Generated clients.

### 6.3 Key domain contracts

| Contract | Current shape | Notes |
|---|---|---|
| `HotelOfferProviderBoundary.search(HotelSearchCriteria): List<HotelOffer>` | Single method, synchronous | Provider-agnostic; no error taxonomy, no retry, no pagination. |
| `HotelSearchCriteria` | destination, checkInDate, checkOutDate, guests, rooms | Minimal fields; no star rating, no amenity filters, no price range. |
| `HotelOffer` | id, providerReference, hotelName, city, country, totalPrice, currency, rating, reviewCount, amenities, availability, source, freshness | Domain model independent of any provider DTO. |

## 7. Accepted carryover from Stage 8

| Item | Current state | Stage 9 relevance |
|---|---|---|
| InMemory stores | `InMemoryPendingConfirmationStore`, `InMemoryConfirmedSearchExecutionAttemptStore`, `InMemoryAssistantSessionStateStore`, `InMemoryHotelSearchStateStore` | Durable persistence — future infrastructure stage. |
| FakeLlmClient | Deterministic with configurable response | Real LLM provider — Stage 9+ или отдельная задача. |
| FakeHotelOfferProvider | Deterministic fake offers | Real hotel provider — Stage 9 primary focus. |
| Static message text | Hardcoded English messages | Production UX Copy — future UX work. |
| Local-only execution | All stores in-process | No distributed requirements. |
| No external provider calls | `CreateHotelSearchUseCase` uses fake | Real provider — Stage 9. |

## 8. Candidate Stage 9 directions

| # | Direction | Description |
|---|---|---|
| A | Hotel provider integration boundary/readiness | Review и design adapter layer для real hotel provider: contract normalization, provider result mapping, error taxonomy, configuration seam. |
| B | Real LLM provider boundary/readiness | Design и wiring для real LLM provider: prompt engineering, model selection, API key management, fallback. |
| C | Durable persistence | PostgreSQL/Redis: session storage, search history, attempt store persistence. |
| D | Frontend UX polish | Rich cards для hotel results, inline retry, progress states, confirmation card UX. |
| E | Production error handling / observability | Structured logging, metrics, tracing, production error taxonomy, health checks. |
| F | Booking flow | End-to-end booking: provider booking API, payment integration, booking confirmation. |

## 9. Direction comparison

| Direction | MVP impact | Risk | Dependencies | Safe as first Stage 9 step? | Why / Why not |
|---|---|---|---|---|---|
| A — Hotel provider boundary | **High** — закрывает крупнейший MVP gap: `FakeHotelOfferProvider` → real provider data. | **Medium** — adapter design, contract normalization, error handling; но не требует production credentials для design phase. | Provider API contract; Stage 8 confirmation flow (completed). | **Yes** — builds directly on Stage 8 confirmation lifecycle; preserves existing behavior; design-first approach safe without credentials. |
| B — Real LLM provider | Medium — улучшает orchestration quality, но `FakeLlmClient` deterministic path работает. | **High** — requires API keys, model selection, prompt engineering, potential behavior changes. | LLM provider contract; prompt design; model evaluation. | **No** — mixing real LLM with provider integration in one stage is risky; behavior changes harder to validate; less direct MVP gap. |
| C — Durable persistence | Medium — улучшает reliability, но InMemory stores acceptable для current scope. | **Medium-High** — DB schema, migration strategy, data model changes; risk of scope leakage. | Infrastructure decision (PostgreSQL vs other); deployment environment. | **No** — infrastructure work orthogonal to MVP gap; requires deployment/DevOps decisions; doesn't improve user-visible behavior directly. |
| D — Frontend UX polish | Medium — улучшает UX, но Stage 7.51 shell functional. | **Low-Medium** — UI work, но может потребовать API contract changes. | Stable backend contracts; design system tokens. | **No** — frontend changes require stable API surface; backend still uses fake provider; UX polish premature before real data. |
| E — Observability | Low для MVP — важно для production, но не для MVP validation. | **Low** — additive; но требует infrastructure decisions. | Deployment environment; monitoring stack choice. | **No** — production concern; premature for MVP stage; doesn't close MVP gap. |
| F — Booking flow | High для product, но **post-MVP**. | **Very High** — payment, provider booking API, transactional safety. | Product decision; provider booking contract; payment integration. | **No** — explicitly post-MVP per product baseline; requires product decision not yet made. |

## 10. Recommended Stage 9 starting direction

**Hotel provider integration boundary/readiness** (Direction A).

Обоснование по критериям:

1. **Закрывает крупнейший MVP gap после Stage 8**: `FakeHotelOfferProvider` — главная оставшаяся fake-компонента, блокирующая реалистичность MVP.
2. **Строится на завершенном Stage 8**: confirmation lifecycle вызывает `CreateHotelSearchUseCase`, который использует `HotelOfferProviderBoundary`. Замена fake на real adapter — естественный следующий шаг.
3. **Может быть выполнен без смешения risk domains**: adapter design + contract normalization + error taxonomy — одна boundary, один risk profile.
4. **Сохраняет Stage 8 behavior**: `HotelOfferProviderBoundary` interface остается; real adapter реализует тот же контракт.
5. **Валидируется безопасно без production credentials**: design-first phase (review + adapter contract + mapping notes) не требует real API keys.
6. **Не заявляет преждевременную production readiness**: design и adapter skeleton — не production integration.
7. **Создаёт чистый путь к будущей real provider/API key/auth работе**: adapter design определяет configuration seam, error taxonomy, credential injection point.
8. **Документация теперь однозначно поддерживает это направление**: roadmap, architecture baseline и Stage 8 carryover указывают на real hotel provider как Stage 9 focus.

## 11. Proposed Stage 9 sequence

Sequence использует medium-small sizing и разделяет dangerous work.

| Sub-stage | Scope | Type | Dangerous work isolated? |
|---|---|---|---|
| **9.1** | Hotel provider boundary review и adapter design: inspect `HotelOfferProviderBoundary`, `HotelSearchCriteria`, `HotelOffer` domain model; design adapter contract для real provider; define provider result normalization rules; design error taxonomy candidate; define configuration seam; produce design report. | Review/design-only | Yes — no code changes. |
| **9.2** | Provider result contract и domain mapping: design mapping из real provider response DTO → domain `HotelOffer`; design source/freshness markers; design availability normalization; produce mapping specification. | Review/design-only | Yes — no code changes. |
| **9.3** | Provider adapter skeleton и fake-vs-real seam: implement `RealHotelOfferProviderAdapter` skeleton за `HotelOfferProviderBoundary`; implement configuration injection (provider credentials, base URL); wire `FakeHotelOfferProvider` как default, real adapter как opt-in; tests. | Implementation | Yes — skeleton only; no real API calls; fake remains default. |
| **9.4** | Provider error taxonomy и error handling: implement provider-specific error types; implement retry/fallback policy candidate; implement error mapping to domain; tests. | Implementation | Yes — error handling isolated from search flow. |
| **9.5** | Provider integration verification: verify real adapter integration with existing Stage 8 confirmation flow; verify Stage 7 strict handoff compatibility; verify fake-as-default safety; produce verification report. | Review/design-only + targeted tests | Yes — verification only; no runtime wiring changes. |
| **9.6** (future, separate) | Real LLM provider integration — отдельная задача, после provider integration stabilization. | — | Isolated from provider work. |
| **9.7** (future, separate) | Durable persistence — отдельная infrastructure задача. | — | Isolated from provider/LLM work. |
| **9.8** (future, separate) | Frontend UX polish — отдельная UX задача. | — | Isolated from backend work. |

**Separation of dangerous work:**
- Real provider calls (9.3) отделены от error handling (9.4) и verification (9.5).
- Real LLM (9.6), persistence (9.7), frontend (9.8) — отдельные будущие stages.
- Persistence, auth, booking, observability, deployment — не входят в Stage 9 provider sequence.

## 12. Stage 9.1 candidate scope

**Stage 9.1 — Hotel Provider Boundary Review and Adapter Design**

Это review/design-only sub-stage.

Scope:

1. Inspect текущую `HotelOfferProviderBoundary` и оценить, достаточно ли она для real provider integration.
2. Inspect текущий `HotelSearchCriteria` domain model и определить, нужны ли дополнительные fields для real provider (star rating, amenity filters, price range, property type).
3. Inspect текущий `HotelOffer` domain model и определить, нужны ли дополнительные fields для real provider (photos, description, policies, coordinates, cancellation policy).
4. Design adapter contract: как real provider response normalizes в domain `HotelOffer`.
5. Design provider result normalization rules: availability mapping, price normalization, source/freshness markers.
6. Design error taxonomy candidate: network errors, rate limiting, no results, partial results, authentication errors.
7. Design configuration seam: provider credentials injection, base URL, timeout, retry config.
8. Определить, какие domain model extensions нужны перед adapter implementation.
9. Produce design report с adapter contract, mapping rules, error taxonomy candidate, configuration seam design и domain model extension recommendations.

Out of scope:

- Implementation любого adapter code.
- Real API calls.
- Provider credentials или API keys.
- Domain model changes.
- Runtime wiring changes.
- Tests.
- OpenAPI/API contract changes.
- Frontend changes.
- Persistence, auth, observability, deployment.

## 13. Guardrails for Stage 9.1

- Stage 9.1 — review/design-only.
- Не создавать production code.
- Не создавать tests.
- Не менять runtime/routes/API/OpenAPI/frontend/generated clients.
- Не использовать и не хранить provider credentials/API keys.
- Не делать real API calls.
- Не менять `HotelOfferProviderBoundary` interface.
- Не менять `CreateHotelSearchUseCase` behavior.
- Не менять Stage 8 confirmation lifecycle.
- Не менять Stage 7 strict hotel-search handoff.
- Не claim production readiness.
- Не начинать Stage 9.2 implementation.
- Не смешивать provider design с LLM, persistence, frontend, auth, booking или observability work.

## 14. Validation expectations for Stage 9.1

- Review-only inspection текущего domain model и provider boundary.
- Design report в `docs/reviews/stage-9-1-hotel-provider-boundary-review-and-adapter-design.md`.
- Entry в `docs/reviews/README.md`.
- Minimal roadmap update (Stage 9.1 completion, next step).
- `git status --short` — только expected docs changed.
- `git diff --check` — no errors.
- Tests не запускаются: stage is docs/design-only.

## 15. Prompt для Stage 9.1

```
Мы продолжаем проект travel-assistant.

Пожалуйста, отвечай на русском языке. Технические имена классов, файлов,
enum values, commit messages и команды оставляй как есть.

Контекст

* Репозиторий: travel-assistant
* Branch: stage-9
* Последний завершённый sub-stage: Stage 9.0
* Stage 9 implementation ещё не начат.
* Production readiness не claimed.
* Все InMemory stores, FakeLlmClient, FakeHotelOfferProvider — accepted carryover.

Текущая provider boundary

* `HotelOfferProviderBoundary` — fun interface с единственным методом `search(HotelSearchCriteria): List<HotelOffer>`.
* `FakeHotelOfferProvider` — deterministic local adapter без external I/O.
* `CreateHotelSearchUseCase` — вызывает `hotelOfferProvider.search(criteria)` и сохраняет ranked search.
* `HotelSearchCriteria` — destination, checkInDate, checkOutDate, guests (adults/children), rooms.
* `HotelOffer` — id, providerReference, hotelName, city, country, totalPrice, currency, rating, reviewCount, amenities, availability, source, freshness.

Stage 8 carryover relevant для provider:

* Real hotel provider integration — Stage 9.
* FakeHotelOfferProvider — accepted carryover.
* Local-only search execution — accepted carryover.

Задача

Выполни:

Stage 9.1 — Hotel Provider Boundary Review and Adapter Design

Это review/design-only sub-stage.

Цели Stage 9.1

1. Inspect текущую `HotelOfferProviderBoundary` и оценить, достаточно ли она для real provider integration.
2. Inspect текущий `HotelSearchCriteria` domain model и определить, нужны ли дополнительные fields для real provider (star rating, amenity filters, price range, property type).
3. Inspect текущий `HotelOffer` domain model и определить, нужны ли дополнительные fields для real provider (photos, description, policies, coordinates, cancellation policy).
4. Design adapter contract: как real provider response normalizes в domain `HotelOffer`.
5. Design provider result normalization rules: availability mapping, price normalization, source/freshness markers.
6. Design error taxonomy candidate: network errors, rate limiting, no results, partial results, authentication errors.
7. Design configuration seam: provider credentials injection, base URL, timeout, retry config.
8. Определить, какие domain model extensions нужны перед adapter implementation.
9. Produce design report.

Required output file

* `docs/reviews/stage-9-1-hotel-provider-boundary-review-and-adapter-design.md`

Отчёт должен включить:

1. Scope
2. Sources inspected
3. Current provider boundary assessment
4. `HotelSearchCriteria` gap analysis
5. `HotelOffer` gap analysis
6. Adapter contract design
7. Provider result normalization rules
8. Error taxonomy candidate
9. Configuration seam design
10. Domain model extension recommendations
11. Impact on existing Stage 7/Stage 8 flow
12. Guardrails для Stage 9.2
13. Prompt для Stage 9.2
14. Verdict

Strict guardrails

Do not implement any production code, tests, runtime changes, domain model
changes, API/OpenAPI changes, frontend changes, or provider credential
handling.

Do not claim production readiness.

Do not start Stage 9.2.

Validation commands

git status --short
git diff --check

Final response format

1. Созданные файлы
2. Изменённые файлы
3. Краткий итог
4. Checks
5. Commit recommendation: Stage 9.1 hotel provider boundary review and adapter design
```

## 16. Stage 9.0 verdict

**Passed** — documentation audit завершен, planning readiness confirmed.

Stage 9.0 выполнил:

1. Documentation audit: inspect 14 документов и 6 backend source files.
2. Выявил 4 реальные inconsistency (stale Stage 7/8 wording в architecture docs).
3. Устранил 4 stale wording коллизии в `docs/architecture/README.md` и `docs/architecture/architecture-baseline.md`.
4. Обновил `docs/roadmap/roadmap.md` с Stage 9.0 completion и Stage 9.1 next step.
5. Добавил entry в `docs/reviews/README.md`.
6. Определил 6 candidate Stage 9 directions и классифицировал их по MVP impact, risk, dependencies.
7. Рекомендовал начать Stage 9 с hotel provider integration boundary/readiness.
8. Предложил medium-small Stage 9 sequence из 5 implementation/design sub-stages + 3 future isolated directions.
9. Определил scope, guardrails и validation для Stage 9.1.
10. Произвёл готовый prompt для Stage 9.1 на русском языке.

Production code не изменён. Runtime не изменён. Tests не запускались.
Production readiness не заявлена. Stage 9 implementation не начат.
