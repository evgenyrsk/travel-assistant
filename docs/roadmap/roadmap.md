# Roadmap Progress — Travel Assistant

Этот документ является **основным roadmap** проекта Travel Assistant. Он фиксирует статусы и границы этапов, критерии качества, перенесенные пункты и следующий разрешенный шаг.

Roadmap не является трекером задач, продуктовой или архитектурной спецификацией, реестром ADR либо активным списком реализации. Подробные продуктовая и архитектурная основы вынесены в отдельные документы и указаны ниже.

## 1. Текущий статус проекта

| Пункт | Статус |
|---|---|
| Текущий этап | Stage 9.17 завершен как REAL runtime wiring readiness gate |
| Последний завершенный этап | Stage 9.17 — выявлены обязательные блокеры асинхронной границы, typed result, location resolver и configuration |
| Следующий планируемый шаг | Stage 9.17a — review/design-only согласование async provider и typed result boundaries |
| Источник подробных статусов | Только этот документ: `docs/roadmap/roadmap.md` |

| Область | Текущее состояние |
|---|---|
| Stage 0-7 | Завершены |
| Stage 8 | Завершен (backend confirmation lifecycle) с carryover (InMemory stores, fake LLM/provider) |
| Stage 7 implementation foundation | Минимальная Kotlin + Ktor backend-основа и ограниченные assistant/conformance-tool slices завершены до Stage 7.25 включительно |
| Stage 7 documentation stabilization | Stage 7.26-7.30 завершены |
| Stage 7 resume development handoff | Stage 7.31 завершен; следующий technical task должен быть выбран отдельной явной roadmap-aligned задачей |
| Stage 7 technical context review | Stage 7.32 завершен; рекомендован Stage 7.33 manifest candidate definition без readiness claim |
| Stage 7 manifest candidate definition | Stage 7.33 завершен; non-readiness manifest candidate создан |
| Stage 7 manifest validation hardening | Stage 7.34 завершен; conformance tool блокирует преждевременные readiness promotion signals в manifest candidate |
| Stage 7 endpoint candidate review | Stage 7.35 завершен; endpoint candidates проанализированы без изменения manifest и без readiness claim |
| Stage 7 assistant endpoint candidate clarification | Stage 7.36 завершен; два assistant endpoint candidates уточнены без изменения manifest, OpenAPI/API contracts, runtime или readiness claim |
| Stage 7 assistant endpoint contract/runtime alignment notes | Stage 7.37 завершен; alignment, gaps, unknowns и carryover для двух assistant endpoint candidates зафиксированы без изменения manifest, OpenAPI/API contracts, runtime, conformance tool или readiness claim |
| Stage 7 assistant endpoint alignment cleanup decision | Stage 7.38 завершен; gaps классифицированы по documentation, OpenAPI/contract, backend/runtime tests, conformance/tooling и future-only buckets без изменений implementation/code, OpenAPI/contract, runtime, manifest или readiness state |
| Stage 7 assistant endpoint contract shape cleanup | Stage 7.39 завершен; Assistant request/response/error contract shape уточнен в OpenAPI draft и contract notes без backend runtime behavior changes, backend tests, conformance tool changes, manifest expansion, generated clients или readiness claim |
| Stage 7 assistant endpoint runtime contract test cleanup | Stage 7.40 завершен; runtime contract tests для Assistant endpoints уточнены без production backend behavior changes, OpenAPI contract changes, conformance tool changes, manifest expansion, generated clients или readiness claim |
| Stage 7 assistant endpoint conformance/tooling follow-up decision | Stage 7.41 завершен; будущие Assistant endpoint conformance/tooling checks классифицированы без conformance tool implementation, production backend changes, OpenAPI changes, manifest expansion, generated clients или readiness claim |
| Stage 7 assistant endpoint conformance candidate implementation | Stage 7.42 завершен; static/advisory Assistant endpoint candidate checks добавлены без backend runtime HTTP checks, OpenAPI changes, manifest expansion, generated clients, CI/Gradle gate или readiness claim |
| Stage 7 assistant endpoint conformance candidate verification | Stage 7.43 завершен; Stage 7.42 подтвержден как bounded/readiness-safe implementation, один Minor shape-guard hardening candidate перенесен в отдельный future stage без tool/backend/OpenAPI/manifest/generated-client/CI changes |
| Stage 7 assistant conformance shape guard hardening | Stage 7.44 завершен; property presence и required membership для `message`/`nextAction` проверяются отдельно, candidate inventory mismatch test добавлен, advisory/runtime/readiness boundaries сохранены |
| Stage 7 assistant conformance output/operator guidance | Stage 7.45 завершен; запуск и интерпретация JSON output, static/blocking/advisory semantics и Assistant checks документированы без изменения tool logic, tests или readiness state |
| Stage 7 assistant conformance documentation verification | Stage 7.46 завершен; Stage 7.45 guidance подтвержден относительно фактического tool output, source-of-truth roles и readiness boundaries без изменения README, tool logic/tests или implementation areas |
| Сверка оставшегося объёма Stage 7 | Stage 7.47 завершен; Assistant conformance подпоток признан достаточно закрытым, а обязательный остаток Stage 7 возвращён к hotel search, offers, ranking/explanation и end-to-end MVP behavior |
| Минимальный backend-поток поиска отелей | Stage 7.48 завершен; process-local search flow и deterministic `FakeHotelOfferProvider` добавлены без real provider, ranking, frontend, generated-client/manifest/CI/tool changes или readiness claims |
| Минимальное ранжирование hotel offers | Stage 7.49 завершен; provider-independent deterministic ranking и короткий `matchSummary` добавлены поверх local fake offers без LLM, real provider, frontend, OpenAPI/generated-client/manifest/CI/tool changes или readiness claims |
| Минимальная передача от Assistant к hotel search | Stage 7.50 завершен; strict explicit message format создает process-local search и возвращает `show_hotel_results` / `hotelSearchId` без LLM, real provider, frontend, generated-client/manifest/CI/tool changes или readiness claims |
| Минимальный frontend-сценарий hotel search | Stage 7.51 завершен; отдельная structured форма проверяет существующие process-local search/offers endpoints и показывает ranked offers. Это временная technical demo shell, а не целевой chat-first UI |
| Финальная сверка hotel-only MVP slice | Stage 7.52 завершен; Stage 7.48-7.51 согласованы по коду, contract shape и automated checks, а отсутствие live browser-to-backend проверки перенесено в явный carryover Stage 7.53 |
| Финальное закрытие Stage 7 | Stage 7.53 завершен; ограниченная hotel-only основа закрыта, оставшаяся работа перенесена без заявлений о готовности к промышленному использованию, generated clients, manifest или real provider и без активации Stage 8 |
| Готовность generated clients/OpenAPI | Не заявлена |
| Generated-client-ready subset / generated clients | Создан manifest-кандидат без заявления готовности; готовый subset и generated clients не созданы |
| Full conformance gate | Не реализован |
| Hotel search / broader implementation | Минимальный fake-provider backend flow, deterministic foundation ranking, bounded Assistant handoff и ручной frontend-сценарий реализованы; real provider, DB/storage, personalization и production implementation не начаты |

| Этап | Статус | Краткое описание |
|---|---|---|
| Stage 0 | Завершен | Product framing, первичные сценарии, предварительные MVP boundaries и открытые вопросы. |
| Stage 1 | Завершен | Business requirements, user journeys, BR/FR/NFR, assumptions, риски и consistency review. |
| Stage 2 | Завершен | Use cases, edge cases, правила поведения ассистента, data requirements и combined search levels. |
| Stage 3 | Завершен | Hotel-only MVP UX, navigation, required fields, acceptance criteria и carryover. |
| Stage 4 | Завершен | Visual direction, основы design system, component inventory, screen specs и interaction patterns. |
| Stage 4.1 | Завершен | Visual design consistency review и небольшая правка формулировок. |
| Stage 5 | Завершен | Conceptual technical architecture, границы, decision inventory, summary и completion audit. |
| Stage 6 | Завершен | API Contracts / OpenAPI / Integration Boundary; Stage 6.1 OpenAPI draft, Stage 6.2 contract review, Stage 6.3 contract fixes, Stage 6.4 post-fix review, Stage 6.5 provider boundary / mapping notes, Stage 6.6 navigation/status cleanup, Stage 6.7 completion review, Stage 6.8 pre-implementation decisions cleanup и Stage 6.9 final closure / handoff завершены. |
| Stage 7 | Завершен | Ограниченная основа hotel-only MVP закрыта Stage 7.53: backend, поиск, fake provider, ранжирование, передача от Assistant и временная frontend-оболочка завершены в заявленных границах. Целевой chat-first flow и LLM orchestration не завершены. |
| Stage 8 | Завершен | Backend confirmation lifecycle: LLM orchestration boundary, confirmation flow, local search execution, consume-after-success. Закрыт с carryover (InMemory stores, fake LLM/provider). |
| Stage 9 | В работе | Внутренний HotelsApi выбран и проанализирован; Hotels API transport/runtime integration еще не начата. |
| Stage 10 | Запланирован | Cross-platform expansion после стабилизации core product и architecture. |

## 2. Правила управления roadmap

- `docs/roadmap/roadmap.md` является источником истины по статусам и границам этапов, перенесенным пунктам и следующему разрешенному шагу.
- `docs/ROADMAP.md` является верхнеуровневым навигационным обзором, а не конкурирующим источником текущего статуса. Он содержит только карту назначения этапов и не ведет матрицу состояния, последний завершенный шаг, следующий планируемый шаг или готовность реализации.
- Документы в `docs/development/` являются активными инженерными правилами для явно ограниченных задач. `docs/development/roadmap.md` и `docs/development/implementation-strategy.md` остаются справочными материалами, а не активным списком реализации. Все документы разработки должны следовать этому roadmap.
- Запланированные и будущие этапы не являются активным списком задач. Каждый будущий этап начинается только после отдельной явной задачи roadmap.
- Рекомендации, перенесенные пункты и будущие кандидаты не должны автоматически выполняться во время задач проверки или чистки.
- Implementation, API/OpenAPI contracts, endpoint specs, DB schema, storage model, auth/security/DevOps/testing backlog и production code требуют отдельного явного roadmap step.
- Product baseline и architecture baseline кратко фиксируют текущее состояние; roadmap должен ссылаться на них, а не дублировать их полностью.
- ADR candidates, drafts и decision inventory не являются accepted ADR.
- Подтвержденный backend stack Travel Assistant — Kotlin + Ktor. Java/Spring Boot не является принятым backend stack без явного будущего ADR и задачи, согласованной с roadmap.
- Если implementation artifacts конфликтуют с architecture baseline по backend stack, дальнейшая implementation работа должна остановиться и зафиксировать архитектурное расхождение.

## 3. Baseline-документы

- `docs/product/product-baseline.md` — компактный actual product baseline после Stage 0-5.
- `docs/architecture/architecture-baseline.md` — компактный actual architecture baseline после Stage 5.
- `docs/decisions/README.md` — навигация и терминология для decisions / ADR.
- `docs/guides/documentation-style-guide.md` — правила языка, структуры, терминологии, guardrails и безопасного refactoring документации.
- `docs/reviews/documentation-refactoring-plan.md` — план controlled documentation refactoring; не active backlog.

## 4. MVP Scope

MVP v1 остается hotel-only:

- AI-assisted hotel search and selection;
- hotel request на естественном языке и уточнение запроса;
- hotel results, ranking, comparison и explanation;
- только current-session shortlist;
- явное разделение provider facts, assistant assumptions и unknowns.

Явно вне MVP v1:

- flights;
- combined itinerary / combined hotel + flight package;
- booking;
- payment;
- account history и account-level storage;
- loyalty/profile system;
- production integrations за пределами явно активированной provider work.

Provider/API data является source of truth для travel facts. LLM может интерпретировать, объяснять, ранжировать, резюмировать и уточнять, но не должна выдумывать цены, доступность, рейтинги, amenities или другие provider facts.

## 5. Открытые решения и перенесенные пункты

Эти пункты перенесены как входные данные для будущих решений и не являются активным списком задач:

- Provider-backed open destination discovery для hotel search, если это потребуется для MVP v1.
- Сроки и формат предоставления existing travel API contract.
- Deferred technical decisions: adapter design, provider error taxonomy, reliability и production-hardening.
- Session persistence, resume behaviour, long-term history, authorization и account-level storage.
- Следующий implementation step или remaining cleanup task должны быть выбраны отдельной явной задачей.

## 6. Завершенные этапы

### Stage 0 — Product Framing

**Статус:** завершен.

**Назначение:** зафиксировать исходную продуктовую рамку, первичные сценарии, предварительные MVP boundaries и правила дальнейшей работы.

**Ключевые артефакты:**

- `docs/product/stage-0/product-framing.md`
- `docs/product/stage-0/initial-scenarios.md`
- `docs/product/stage-0/mvp-boundaries.md`
- `docs/product/stage-0/assumptions-and-open-questions.md`
- `docs/product/README.md`

**Заметка о завершении:** Stage 0 не фиксировал финальные технические решения. Приоритетные пользователи, сценарии и MVP boundaries были уточнены на следующих product stages.

### Stage 1 — Business Requirements

**Статус:** завершен.

**Назначение:** зафиксировать аудиторию, business scenarios, user journeys, BR/FR/NFR, assumptions, open questions и risks.

**Ключевые артефакты:**

- `docs/product/stage-1/target-audience.md`
- `docs/product/stage-1/business-scenarios.md`
- `docs/product/stage-1/user-journeys.md`
- `docs/product/stage-1/business-requirements.md`
- `docs/product/stage-1/functional-requirements.md`
- `docs/product/stage-1/non-functional-requirements.md`
- `docs/product/stage-1/assumptions-and-open-questions.md`
- `docs/product/stage-1/stage-1-summary.md`
- `docs/product/stage-1/stage-1-consistency-review.md`

**Заметка о завершении:** Booking и payment исключены из MVP. Более ранние flight и combined recommendations superseded для MVP v1; flight search является следующим расширением после hotel flow, а combined flow — более поздним расширением после flight flow.

**Carryover:** точный existing travel API contract, provider-backed discovery, recommendation criteria, persistence/authorization и язык uncertainty/provider-error остаются входными данными для будущих решений.

### Stage 2 — Use Cases & Edge Cases

**Статус:** завершен.

**Назначение:** развернуть Stage 1 scenarios в use cases, edge cases, assistant behaviour rules, combined search levels и product data requirements.

**Ключевые артефакты:**

- `docs/product/stage-2/use-cases.md`
- `docs/product/stage-2/edge-cases.md`
- `docs/product/stage-2/assistant-behaviour-rules.md`
- `docs/product/stage-2/combined-search-levels.md`
- `docs/product/stage-2/data-requirements.md`
- `docs/product/stage-2/stage-2-summary.md`
- `docs/product/stage-2/stage-2-consistency-review.md`

**Заметка о завершении:** Provider/API data подтверждена как source of truth для travel facts. Stage 2 flight/combined context сохранен для historical traceability, но не является разрешением реализовывать flights или combined itinerary в MVP v1.

**Carryover:** minimum required fields, open destination discovery, existing travel API contract, adapter/error/reliability decisions и session/auth questions остаются будущими входными данными.

### Stage 3 — MVP UX / Navigation

**Статус:** завершен.

**Назначение:** определить UX-структуру hotel-only MVP, navigation model, search flow boundaries, required fields и acceptance criteria без API, архитектуры или implementation work.

**Ключевые артефакты:**

- `docs/product/stage-3/screen-map.md`
- `docs/product/stage-3/required-fields-and-acceptance-criteria.md`
- `docs/product/stage-3/mvp-search-flow-details.md`
- `docs/product/stage-3/combined-search-ux-decision.md` — historical decision, superseded for MVP v1.
- `docs/product/stage-3/stage-3-hotel-only-consistency-review.md`
- `docs/product/stage-3/stage-3-summary-and-carryover.md`
- `docs/product/stage-3/stage-3-plan-reconciliation.md`

**Quality gate / заметка о завершении:** Stage 3 финализировал hotel-only MVP UX и acceptance criteria. Flight search и combined hotel+flight не требуются для MVP v1. Stage 3 не разрешает implementation, API/OpenAPI contracts, DB schema, storage model, auth/account history или UI code.

### Stage 4 — Visual Design & UX System

**Статус:** завершен.

**Назначение:** зафиксировать visual style direction, design system foundations, component inventory, screen-level specifications и interaction patterns для hotel-only MVP.

**Ключевые артефакты:**

- `docs/product/stage-4/visual-design-direction.md`
- `docs/product/stage-4/design-system-foundations.md`
- `docs/product/stage-4/component-inventory.md`
- `docs/product/stage-4/screen-specifications.md`
- `docs/product/stage-4/interaction-patterns.md`
- `docs/product/stage-4/stage-4-summary-and-carryover.md`

**Заметка о завершении:** Stage 4 создал product/design documentation, а не frontend implementation. Draft palette, design foundations и component inventory не являются финальными implementation tokens.

**Carryover:** final design tokens, hotel imagery/source markers, session persistence level и accessibility gates остаются будущими входными данными.

### Stage 4.1 — Visual Design Consistency Review

**Статус:** завершен.

**Назначение:** проверить Stage 4 documentation на согласованность со Stage 0-3, MVP scope и roadmap boundaries.

**Ключевой артефакт:**

- `docs/product/stage-4/stage-4-consistency-review.md`

**Quality gate / заметка о завершении:** Verdict: passed with minor wording fixes. Stage 4.1 не начинал frontend implementation и не расширял MVP scope.

### Stage 5 — Technical Architecture

**Статус:** завершен.

**Назначение:** зафиксировать conceptual architecture baseline, system boundaries, domain/data boundaries, orchestration, integration architecture, NFR guidance и decision inventory.

**Ключевые артефакты:**

- `docs/architecture/architecture-baseline.md`
- `docs/architecture/stage-5/architecture-scope-and-principles.md`
- `docs/architecture/stage-5/system-context-and-boundaries.md`
- `docs/architecture/stage-5/domain-model-and-boundaries.md`
- `docs/architecture/stage-5/application-orchestration.md`
- `docs/architecture/stage-5/integration-architecture.md`
- `docs/architecture/stage-5/data-and-storage-boundaries.md`
- `docs/architecture/stage-5/non-functional-requirements.md`
- `docs/architecture/stage-5/architecture-decisions-draft.md`
- `docs/architecture/stage-5/stage-5-consistency-review.md`
- `docs/architecture/stage-5/stage-5-summary-and-carryover.md`

**Quality gate / заметка о завершении:** Stage 5.9 не нашел Critical/Major blockers. Stage 5 является conceptual architecture documentation и system design, а не production implementation.

**Carryover:** сохранить hotel-only MVP scope, provider facts как source data, разделение facts/assumptions/unknowns, provider abstraction как conceptual boundary, а не API contract, и NFR как quality guidance, а не active DevOps/security/testing backlog.

## 7. Текущий и будущие этапы

### Stage 6 — API Contracts / OpenAPI / Integration Boundary

**Статус:** завершен. Stage 6.1, Stage 6.2, Stage 6.3, Stage 6.4, Stage 6.5, Stage 6.6, Stage 6.7, Stage 6.8 и Stage 6.9 завершены через отдельные явные roadmap-задачи.

**Назначение:** API Contracts / OpenAPI / Integration Boundary и scoped implementation-preparation planning. Stage 6 должен сохранять hotel-only MVP v1, Stage 5 architecture baseline, provider-agnostic hotel boundary и отсутствие production implementation.

**Условие активации:** Stage 6 был начат отдельной явной задачей Stage 6.1 и закрыт отдельной явной задачей Stage 6.9. Завершение Stage 6.1, Stage 6.2, Stage 6.3, Stage 6.4, Stage 6.5, Stage 6.6, Stage 6.7, Stage 6.8 или Stage 6.9 не начинает Stage 7 и не разрешает backend/frontend implementation.

**Границы scope:** Stage 6 может определять API/OpenAPI contract drafts, implementation-preparation scope, sequencing, validation approach, local workflow boundaries и conceptual boundaries для mock/fake providers и contract placeholders. Он должен сохранить hotel-only scope MVP v1 и Stage 5 architecture baseline.

**Завершенные артефакты Stage 6.1:**

- `docs/architecture/stage-6/openapi-draft.yaml` — primary OpenAPI 3.1 draft для MVP hotel-only frontend/backend API.
- `docs/architecture/stage-6/openapi-contract-notes.md` — notes по MVP endpoints, exclusions, assumptions, open questions и связи со Stage 5 baseline.

**Завершенный артефакт Stage 6.2:**

- `docs/architecture/stage-6/openapi-contract-review.md` — review OpenAPI draft относительно Stage 2-5 product, UX и architecture baselines; verdict: passed for continued Stage 6 contract work with follow-up findings before client generation or implementation.

**Завершенный артефакт Stage 6.3:**

- `docs/architecture/stage-6/openapi-fixes-summary.md` — summary of OpenAPI fixes closing Stage 6.2 Major findings and addressing allowed Minor fixes without provider-specific DTOs, implementation code or Stage 7 activation.

**Завершенный артефакт Stage 6.4:**

- `docs/architecture/stage-6/post-fix-contract-review.md` — post-fix review of Stage 6.3 contract fixes; verdict: passed for continued Stage 6 contract work with one Minor follow-up and no Critical/Major blockers.

**Завершенный артефакт Stage 6.5:**

- `docs/architecture/stage-6/provider-boundary-mapping-notes.md` — conceptual provider boundary / mapping notes for future hotel provider/source data into existing client-facing OpenAPI concepts without provider-specific DTOs, provider contracts, backend/frontend implementation or Stage 7 activation.

**Завершенный cleanup Stage 6.6:**

- Documentation navigation / status sync cleanup updated root/navigation status wording in `README.md`, `docs/ROADMAP.md`, `docs/architecture/README.md` and this roadmap without changing OpenAPI, Stage 6 contract artifacts, provider boundary notes, backend/frontend implementation, DB/storage, ADR or Stage 7 activation.

**Завершенный артефакт Stage 6.7:**

- `docs/architecture/stage-6/stage-6-completion-review.md` — Stage 6 completion review / contract package summary; verdict: завершен с carryover; рекомендует отдельный явный Stage 6.8 pre-implementation decisions cleanup до generated clients или более широкой implementation work.

**Завершенный артефакт Stage 6.8:**

- `docs/architecture/stage-6/pre-implementation-decisions-cleanup.md` — pre-implementation decisions cleanup for nested 404 modeling, inline offer details, result-envelope search status and current-session page-refresh/persistence behavior; no new resource flows, provider DTOs, DB/storage or Stage 7 activation.

**Завершенный артефакт Stage 6.9:**

- `docs/architecture/stage-6/stage-6-final-closure-and-handoff.md` — final closure / handoff to implementation; verdict: Stage 6 completed with non-blocking carryover and recorded that Stage 7 had to start only through a separate explicit task.

**Явные исключения:** Stage 6.1, Stage 6.2, Stage 6.3, Stage 6.4, Stage 6.5, Stage 6.6, Stage 6.7, Stage 6.8 и Stage 6.9 не создают backend implementation, frontend implementation, DB schema, storage model, auth/security/DevOps/testing backlog, production implementation, provider-specific integration code, generated clients или Stage 7 implementation tasks. Следующие concrete contract, storage, auth, security, DevOps, testing или implementation artifacts требуют отдельной явной future-stage задачи.

**Quality gate:** Stage 6 закрыт после того, как Stage 6 artifacts явно отделили разрешенную contract/preparation work от исключенной DB/storage/auth/DevOps/testing/production work. Любой следующий concrete contract, implementation artifact или expansion за пределы hotel-only требует отдельного явного roadmap step.

### Stage 7 — MVP Implementation

**Статус:** завершен.

**Назначение:** реализация ограниченной основы hotel-only MVP после Stage 6 через отдельные задачи. Stage 7 закрыт с явным переносом оставшихся пунктов; его завершение не означает готовность к промышленному использованию.

**Прогресс Stage 7 по областям:**

| Область | Завершено до | Статус |
|---|---|---|
| Stabilization and restart readiness | Stage 7.0f-f | Backend stack drift исправлен; restart readiness пройден; cleanup читаемости navigation/governance/development/product/architecture/roadmap завершен. |
| Backend foundation and assistant boundaries | Stage 7.12 | Минимальная Kotlin + Ktor backend-основа, assistant session/message boundaries, local session/clarification/slot metadata и internal slot update boundary завершены. |
| API/runtime alignment and readiness reviews | Stage 7.15b | API/contract alignment checkpoint, assistant runtime contract alignment cleanup, generated-client/OpenAPI readiness checkpoint и response semantics/status sync завершены. |
| Generated-client/OpenAPI conformance planning and tooling | Stage 7.25 | Conformance planning, subset policy, skeleton planning/tooling decision, standalone read-only conformance tool, reporting tests и manifest detection/validation завершены. |
| Documentation stabilization track | Stage 7.30 | Documentation quality audit, governance rules cleanup, roadmap structure refactor, active documentation language normalization и final quality gate завершены. |
| Resume development handoff | Stage 7.31 | Documentation stabilization closure and guardrails for resumed bounded Stage 7 technical work completed; no technical implementation started. |
| Resume technical context review | Stage 7.32 | Technical context restored after documentation stabilization; next recommended bounded task is Stage 7.33 manifest candidate definition without readiness claim. |
| Ready subset manifest candidate definition | Stage 7.33 | Non-readiness `generated-client-ready-subset.yaml` candidate created for skeleton validation; no generated-client readiness claim. |
| Manifest candidate validation hardening | Stage 7.34 | Tool-local validation now blocks premature readiness promotion signals in the manifest candidate while keeping the report advisory/not_ready. |
| Endpoint candidate review | Stage 7.35 | Current OpenAPI/backend endpoint candidates reviewed for future manifest expansion; manifest and readiness state unchanged. |
| Assistant endpoint candidate clarification | Stage 7.36 | Two assistant endpoint candidates clarified for contract/runtime/security/product conditions; manifest and readiness state unchanged. |
| Assistant endpoint contract/runtime alignment notes | Stage 7.37 | Alignment, gaps, unknowns и carryover для двух assistant endpoint candidates зафиксированы; manifest, OpenAPI/API contracts, runtime, conformance tool и readiness state не изменены. |
| Assistant endpoint alignment cleanup decision | Stage 7.38 | Gaps классифицированы по documentation, OpenAPI/contract, backend/runtime tests, conformance/tooling и future-only buckets; implementation, contract, runtime, manifest и readiness state не изменены. |
| Assistant endpoint contract shape cleanup | Stage 7.39 | Assistant request/response/error contract shape уточнен в OpenAPI draft и contract notes; backend runtime behavior, backend tests, conformance tool, manifest, generated clients и readiness state не изменены. |
| Assistant endpoint runtime contract test cleanup | Stage 7.40 | Runtime contract tests для Assistant endpoints уточнены; production backend behavior, OpenAPI contracts, conformance tool, manifest, generated clients и readiness state не изменены. |
| Assistant endpoint conformance/tooling follow-up decision | Stage 7.41 | Будущие Assistant endpoint conformance/tooling checks классифицированы; conformance tool implementation, production backend behavior, OpenAPI contracts, manifest, generated clients и readiness state не изменены. |
| Assistant endpoint conformance candidate implementation | Stage 7.42 | Static/advisory Assistant endpoint candidate checks добавлены; backend runtime HTTP checks, OpenAPI contracts, manifest, generated clients, CI/Gradle gate и readiness state не изменены. |
| Assistant endpoint conformance candidate verification | Stage 7.43 | Stage 7.42 проверен на Stage 7.41 alignment, bounded scope, static/advisory distinction и readiness safety; Critical/Major findings отсутствуют, один Minor hardening candidate оставлен отдельному future stage. |
| Assistant conformance shape guard hardening | Stage 7.44 | Property presence и required membership для `message`/`nextAction` проверяются отдельно; добавлен targeted negative inventory test; runtime semantics остаются advisory-only, readiness state не изменен. |
| Assistant conformance output/operator guidance | Stage 7.45 | Документированы команды запуска, интерпретация `not_ready`/`readinessClaim`, blocking/advisory findings и Assistant checks; tool logic, tests и readiness state не изменены. |
| Assistant conformance documentation verification | Stage 7.46 | Operator guidance проверен относительно фактического JSON output, static/advisory behavior и source-of-truth boundaries; factual inconsistencies и readiness overclaims не найдены. |
| Сверка оставшегося объёма Stage 7 | Stage 7.47 | Подтверждено, что conformance подпоток не требует дальнейшего дробления; следующий практический шаг возвращён к минимальному hotel search flow с `fake provider`. |
| Минимальный backend-поток поиска отелей | Stage 7.48 | Existing hotel search contract реализован как process-local flow с deterministic `FakeHotelOfferProvider`, targeted validation/tests и нормализованными offers без ranking/readiness claims. |
| Минимальное ранжирование hotel offers | Stage 7.49 | Offers ранжируются по availability, rating, total stay price и stable offer ID; существующий `matchSummary` используется для короткого deterministic объяснения без LLM или readiness claims. |
| Минимальная передача от Assistant к hotel search | Stage 7.50 | Strict explicit Assistant message format вызывает существующий search boundary; response возвращает `show_hotel_results` и opaque `hotelSearchId`, а ordinary/incomplete messages сохраняют clarification behavior. |
| Минимальный frontend-сценарий hotel search | Stage 7.51 | Отдельная structured форма создаёт process-local session/search, загружает ranked offers и показывает `matchSummary` через ручной local API client без generated clients. |
| Финальная сверка hotel-only MVP slice | Stage 7.52 | Backend и frontend slices согласованы по request/response shape и проходят раздельные automated gates; live browser-to-backend E2E не заявлен. |
| Финальное закрытие и carryover | Stage 7.53 | Stage 7 закрыт в bounded foundation scope; future integration, production и AI/LLM work перенесены без автоматической активации. |

**Documentation stabilization track:**

- [x] Stage 7.26 — Documentation Quality Calibration Audit
- [x] Stage 7.27 — Documentation Governance Rules Cleanup
- [x] Stage 7.28 — Roadmap Structure Refactor
- [x] Stage 7.29 — Active Documentation Language Normalization
- [x] Stage 7.30 — Documentation Final Quality Gate

**Resume development handoff:**

- [x] Stage 7.31 — Resume Development Handoff

**Resume technical context review:**

- [x] Stage 7.32 — Resume Stage 7 Technical Context Review

**Manifest candidate definition:**

- [x] Stage 7.33 — Ready Subset Manifest Candidate Definition

**Manifest candidate validation hardening:**

- [x] Stage 7.34 — Manifest Candidate Validation Hardening

**Endpoint candidate review:**

- [x] Stage 7.35 — Endpoint Candidate Review

**Assistant endpoint candidate clarification:**

- [x] Stage 7.36 — Assistant Endpoint Candidate Clarification

**Assistant endpoint contract/runtime alignment notes:**

- [x] Stage 7.37 — Assistant Endpoint Contract/Runtime Alignment Notes

**Assistant endpoint alignment cleanup decision:**

- [x] Stage 7.38 — Assistant Endpoint Contract/Runtime Alignment Cleanup Decision

**Assistant endpoint contract shape cleanup:**

- [x] Stage 7.39 — Assistant Endpoint Contract Shape Cleanup

**Assistant endpoint runtime contract test cleanup:**

- [x] Stage 7.40 — Assistant Endpoint Runtime Contract Test Cleanup

**Assistant endpoint conformance/tooling follow-up decision:**

- [x] Stage 7.41 — Assistant Endpoint Conformance/Tooling Follow-up Decision

**Assistant endpoint conformance candidate implementation:**

- [x] Stage 7.42 — Assistant Endpoint Conformance Candidate Implementation

**Assistant endpoint conformance candidate verification:**

- [x] Stage 7.43 — Assistant Endpoint Conformance Candidate Verification

**Assistant conformance shape guard hardening:**

- [x] Stage 7.44 — Assistant Conformance Shape Guard Hardening

**Assistant conformance output/operator guidance:**

- [x] Stage 7.45 — Assistant Conformance Output Documentation / Operator Guidance

**Assistant conformance documentation verification:**

- [x] Stage 7.46 — Assistant Conformance Documentation Verification

**Сверка оставшегося объёма Stage 7:**

- [x] Stage 7.47 — Сверка оставшегося объёма Stage 7

**Минимальный backend-поток поиска отелей:**

- [x] Stage 7.48 — Минимальный backend-поток поиска отелей с `fake provider`

**Минимальное ранжирование hotel offers:**

- [x] Stage 7.49 — Минимальное ранжирование hotel offers

**Минимальная передача от Assistant к поиску отелей:**

- [x] Stage 7.50 — Минимальная передача от Assistant к поиску отелей

**Минимальный frontend-сценарий hotel search:**

- [x] Stage 7.51 — Минимальный frontend-сценарий hotel search

**Финальная сверка hotel-only MVP slice:**

- [x] Stage 7.52 — Финальная сверка hotel-only MVP slice

**Финальное закрытие Stage 7:**

- [x] Stage 7.53 — Финальное закрытие Stage 7 и перенос оставшихся пунктов

**Перенесенные пункты после Stage 7:**

| Категория | Статус после закрытия |
|---|---|
| Полная browser-to-backend E2E и визуальная проверка | Перенесены в отдельную задачу интеграции/стабилизации; не являются выполненными |
| Generated clients и готовый subset | Не созданы; manifest остается кандидатом без заявления готовности или расширения |
| Интеграция CI/Gradle и runtime conformance gate | Не реализованы; требуют отдельного решения по инструментам |
| Интеграция real provider | Перенесена в Stage 9 или отдельную задачу, согласованную с roadmap, после появления provider contract |
| LLM orchestration и расширенный Assistant UI | Перенесены в Stage 8; Stage 8 не активирован |
| DB/storage, Redis/cache, auth/account flows | Перенесены до отдельного продуктового или архитектурного решения |
| Booking, payment, flights и combined itinerary | Не входят в закрытые границы hotel-only Stage 7 |
| Production UI, security, observability и deployment hardening | Не заявлены и остаются будущей работой |

**Ключевое ограничение закрытия:** завершение Stage 7 не означает готовность generated clients, завершение OpenAPI, активацию DB/storage, интеграцию real provider, промышленную готовность frontend, готовность CI gate или автоматическую активацию Stage 8.

**Stage 7 linked artifacts by group:**

| Group | Key artifacts |
|---|---|
| Backend foundation | `services/backend/`, `services/backend/README.md` |
| Full review/audit trail | `docs/reviews/README.md` |
| Stack correction and restart readiness | `project-consistency-audit.md`, `backend-stack-decision-sync.md`, `backend-skeleton-correction.md`, `stage-7-restart-readiness-review.md` |
| Stage 7.2-7.15b implementation/readiness/status reports | См. `docs/reviews/README.md` |
| Stage 7.16-7.25 generated-client/OpenAPI conformance reports | См. `docs/reviews/README.md` |
| Stage 7.26-7.30 documentation stabilization reports | `stage-7-26-documentation-quality-calibration-audit.md`, `stage-7-27-documentation-governance-rules-cleanup.md`, `stage-7-28-roadmap-structure-refactor.md`, `stage-7-29-active-documentation-language-normalization.md`, `stage-7-30-documentation-final-quality-gate.md` |
| Stage 7.31 resume development handoff | `stage-7-31-resume-development-handoff.md` |
| Stage 7.32 resume technical context review | `stage-7-32-resume-stage-7-technical-context-review.md` |
| Stage 7.33 ready subset manifest candidate | `docs/architecture/stage-7/generated-client-ready-subset.yaml`, `stage-7-33-ready-subset-manifest-candidate-definition.md` |
| Stage 7.34 manifest candidate validation hardening | `stage-7-34-manifest-candidate-validation-hardening.md` |
| Stage 7.35 endpoint candidate review | `stage-7-35-endpoint-candidate-review.md` |
| Stage 7.36 assistant endpoint candidate clarification | `stage-7-36-assistant-endpoint-candidate-clarification.md` |
| Stage 7.37 assistant endpoint contract/runtime alignment notes | `stage-7-37-assistant-endpoint-contract-runtime-alignment-notes.md` |
| Stage 7.38 assistant endpoint alignment cleanup decision | `stage-7-38-assistant-endpoint-alignment-cleanup-decision.md` |
| Stage 7.39 assistant endpoint contract shape cleanup | `stage-7-39-assistant-endpoint-contract-shape-cleanup.md` |
| Stage 7.40 assistant endpoint runtime contract test cleanup | `stage-7-40-assistant-endpoint-runtime-contract-test-cleanup.md` |
| Stage 7.41 assistant endpoint conformance/tooling follow-up decision | `stage-7-41-assistant-endpoint-conformance-tooling-follow-up-decision.md` |
| Stage 7.42 assistant endpoint conformance candidate implementation | `stage-7-42-assistant-endpoint-conformance-candidate-implementation.md` |
| Stage 7.43 assistant endpoint conformance candidate verification | `stage-7-43-assistant-endpoint-conformance-candidate-verification.md` |
| Stage 7.44 assistant conformance shape guard hardening | `stage-7-44-assistant-conformance-shape-guard-hardening.md` |
| Stage 7.45 assistant conformance output/operator guidance | `stage-7-45-assistant-conformance-output-operator-guidance.md` |
| Stage 7.46 assistant conformance documentation verification | `stage-7-46-assistant-conformance-documentation-verification.md` |
| Stage 7.47 remaining scope review | `stage-7-47-stage-7-remaining-scope-review.md` |
| Stage 7.48 minimal backend hotel search with fake provider | `stage-7-48-minimal-backend-hotel-search-fake-provider.md` |
| Stage 7.49 minimal hotel offer ranking | `stage-7-49-minimal-hotel-offer-ranking.md` |
| Stage 7.50 minimal Assistant-to-hotel-search handoff | `stage-7-50-minimal-assistant-to-hotel-search-handoff.md` |
| Stage 7.51 minimal frontend hotel search scenario | `stage-7-51-minimal-frontend-hotel-search-scenario.md` |
| Stage 7.52 hotel-only MVP slice final review | `stage-7-52-hotel-only-mvp-slice-final-review.md` |
| Stage 7.53 final closure and carryover | `stage-7-53-final-stage-7-closure-and-carryover.md` |

**Следующий шаг:** Stage 8.1 — LLM Orchestration Boundary and Safety Plan, только через отдельную явную planning-only задачу. Закрытие Stage 7 и эта Pre-Stage 8 синхронизация не активируют Stage 8 автоматически.

### Stage 8 — AI/LLM Orchestration Improvements

**Статус:** завершен.

**Назначение:** развитие chat-first AI/LLM orchestration: provider-independent `LlmClient` boundary, confirmation lifecycle, local search execution после user confirmation и consume-after-success policy.

**Прогресс Stage 8 по областям:**

| Область | Завершено до | Статус |
|---|---|---|
| Entry review and planning | Stage 8.0 | Carryover classification и Stage 8.1 planning завершены. |
| LLM boundary design | Stage 8.1–8.5 | `LlmClient` boundary, skeleton, orchestration, decision planning, pipeline composition завершены. |
| Handoff planning and minimal wiring | Stage 8.6–8.8 | Readiness gate, clarification/fallback wiring завершены; `ProceedWithCandidate` deferred. |
| Criteria validation and confirmation | Stage 8.9–8.15 | Proceed candidate validator, confirmation proposal, prompt wiring завершены. |
| Pending confirmation lifecycle | Stage 8.16–8.24 | Pending state, reply classifier, decision composition, consuming wiring завершены. |
| Confirmed-search pipeline | Stage 8.25–8.39 | Criteria mapper, creation plan, command builder, execution result, guard, attempt store, orchestration завершены. |
| Response mapping and integration | Stage 8.40–8.50 | Lifecycle policy, TTL/stale, retry, response mapping, integration composition, non-results wiring завершены. |
| Actual execution and consume | Stage 8.51–8.55 | Actual `CreateHotelSearchUseCase` call, SUCCEEDED recording, consume-after-success завершены. |
| Lifecycle verification and closure | Stage 8.56–8.57 | End-to-end lifecycle verified; Stage 8 formally closed with carryover. |

**Перенесенные пункты после Stage 8:**

| Категория | Статус после закрытия |
|---|---|
| InMemory stores (pending, attempt, session, search) | Accepted carryover; durable persistence — future infrastructure work |
| FakeLlmClient (deterministic) | Accepted carryover; real LLM provider — Stage 9+ |
| FakeHotelOfferProvider | Accepted carryover; real hotel provider — Stage 9 |
| Static message text | Accepted carryover; production UX copy — future work |
| Real provider/API integration | Перенесена в Stage 9 |
| Durable persistence (PostgreSQL, Redis) | Перенесена до отдельного infrastructure решения |
| Frontend UX polish (rich cards, inline retry) | Перенесена в отдельную UX задачу |
| Auth/API keys, production observability | Перенесены до отдельных security/operations решений |
| Booking, payment, flights и combined itinerary | Не входят в закрытые границы hotel-only Stage 8 |

**Ключевое ограничение закрытия:** завершение Stage 8 не означает ready real provider, real LLM, durable persistence, production readiness или автоматическую активацию Stage 9.

**Stage 8 linked artifacts by group:**

| Group | Key artifacts |
|---|---|
| Full review/audit trail | `docs/reviews/README.md` (Stage 8.0–8.57) |
| Closure and lifecycle verification | `stage-8-57-stage-8-closure-and-readiness-gate.md`, `stage-8-56-end-to-end-confirmation-lifecycle-verification.md` |

**Следующий шаг:** Stage 9 planning/readiness review, только через отдельную явную planning-only задачу. Закрытие Stage 8 не активирует Stage 9 автоматически.

### Stage 9 — Real Provider/API Integration Hardening

**Статус:** Stage 9.17 завершил REAL runtime wiring readiness gate с verdict `NOT_READY_FOR_REAL_RUNTIME_WIRING`. Публичный search endpoint технически подтвержден, но sync/suspend seam, typed location outcomes, transport-backed resolver, public-only configuration и production lifecycle `HttpClient` не готовы. Runtime wiring не добавлено, `FAKE` остается provider по умолчанию.

**Planning:**

| Sub-stage | Scope | Статус |
|---|---|---|
| Stage 9.0 | Documentation audit и Stage 9 planning readiness review | Завершен |
| Stage 9.1 | Hotel provider boundary review и adapter design | Завершен |
| Stage 9.2 | Provider result contract и domain mapping | Завершен |
| Stage 9.3 | Provider adapter skeleton и fake-vs-real seam | Завершен |
| Stage 9.4 | Provider error taxonomy и error handling | Завершен |
| Stage 9.5 | Provider integration verification | Завершен |
| Stage 9.6 | Real provider selection background comparison и configuration design | Завершен |
| Stage 9.7 | Selected Hotels API contract reconciliation и implementation plan | Завершен |
| Stage 9.8 | Hotels API configuration skeleton без HTTP | Завершен |
| Stage 9.8a | Hotels API authentication configuration reconciliation без HTTP/JWT signing | Завершен |
| Stage 9.9 | HTTP-транспорт публичного Hotels API без авторизации, проверяемый через `MockEngine` | Завершен; расхождение с auth в Swagger остается риском sandbox |
| Stage 9.10 | Autocomplete/location contract boundary, provider DTO и location mapper | Завершен без transport/runtime wiring; response fixture проверен в Stage 9.14 |
| Stage 9.11a | DTO поиска по Swagger без преобразования доменных моделей | Завершен; использован синтетический fixture |
| Stage 9.11b | Provider target and mapping policy readiness gate | Завершен; внутренний API и `https://hotels.tbank.ru/` подтверждены, public web orchestration не активирована |
| Stage 9.11b1 | Configuration-only public base URL reconciliation | Завершен; default URL изменен на `https://hotels.tbank.ru/` без transport/runtime wiring |
| Stage 9.11b2 | Guest occupancy contract | Завершен: канонический `childrenAges`, совместимость с `children`, clarification и idempotency rules |
| Stage 9.11b3 | Partial `HotelOffer` facts contract | Завершен: nullable rating/review/amenities и ranking без выдуманных provider facts |
| Stage 9.11b4 | Public contract alignment | Завершен: OpenAPI `childrenAges`, optional facts и frontend regression test |
| Stage 9.11c | Преобразование search DTO выбранного API в доменные модели | Завершен как mapper-only implementation без transport/runtime wiring |
| Stage 9.12 | Internal orchestration resolver → один search call → mapper без runtime wiring | Завершен; только `MockEngine`, REAL adapter не подключен |
| Stage 9.13 | Single-page candidate window без pagination | Завершен; один запрос, до 20 уникальных кандидатов, только `MockEngine` |
| Stage 9.14 | Sanitized fixture contract verification | Завершен; provider-derived responses совместимы с текущими response DTO и mapper policy |
| Stage 9.15 | Sandbox readiness gate | Завершен; выбран direct search call, обязательный preflight вынесен в Stage 9.15a |
| Stage 9.15a | Mock-only QA transport preflight | Завершен; test-scoped engine и opt-in harness проверены только через `MockEngine`, live call отсутствует |
| Stage 9.16 | Первый контролируемый QA call через проектный transport | Завершен; один request, `200`, 20 hotels/offers, без runtime wiring |
| Stage 9.17 | REAL runtime wiring readiness gate | Завершен; фактическое подключение заблокировано границами async/result/resolver/configuration |
| Stage 9.17a | Async provider/result contract reconciliation | Запланирован как review/design-only этап |
| Stage 9.17b | Autocomplete resolver transport adapter | Запланирован после Stage 9.17a; `MockEngine`, без runtime wiring |
| Stage 9.17c | Opt-in REAL runtime wiring с FAKE default | Заблокирован до Stage 9.17a–9.17b |
| Stage 9.18 | Integration closure | Запланирован |

**Границы:** выбранный контракт поиска — внутренний HotelsApi OpenAPI 1.0/2.0/3.0 на `https://hotels.tbank.ru/`. Для MVP выбран `POST /api/v1/hotels/search`; его анонимный вызов подтвержден технически, но официальный server-to-server статус и долгосрочная стабильность не подтверждены. Autocomplete технически проверен через отдельный `/search-api/search/autocomplete`, но не объединяется с внутренними DTO и не подключен к transport/runtime. Booking/payment/cancellation, durable storage и production-hardening не входят в текущий slice.

**Следующий шаг:** Stage 9.17a должен в режиме review/design-only определить асинхронный provider/application contract и typed outcomes для offers, location not found, location selection required и provider failure. Нельзя использовать `runBlocking`, превращать неоднозначную location в пустой список или менять public API без отдельного решения. После этого Stage 9.17b может добавить transport-backed autocomplete resolver с отдельным `input` request DTO и тестами на `MockEngine`. Фактическое opt-in `REAL` runtime wiring остается Stage 9.17c; `FAKE` должен остаться default. Pagination возвращается в roadmap только после отдельного продуктового решения. Включение taxes/fees и threshold для `LIMITED` остаются неизвестными и не должны заполняться догадками.

### Stage 10 — Cross-platform Expansion

**Статус:** Запланирован.

**Границы:** расширение за пределы первой платформы без переписывания product и domain logic.

**Правило активации будущих этапов:** planned stages не являются active backlog. Каждый будущий этап начинается только после отдельной явной roadmap-задачи, которая активирует этап и подтверждает нужные предыдущие решения.

## 8. Связанные документы и audit trail

- `docs/ROADMAP.md` — верхнеуровневый обзор roadmap.
- `docs/product/README.md` — индекс продуктовой документации.
- `docs/architecture/README.md` — индекс архитектурной документации.
- `docs/development/roadmap.md` — компактный development reference и milestone vocabulary; только future/reference material.
- `docs/development/implementation-strategy.md` — implementation strategy; future/reference material до активации.
- `docs/reviews/README.md` — индекс review/audit artifacts и правила чтения historical/current cleanup reports.
- `docs/reviews/stage-7-33-ready-subset-manifest-candidate-definition.md` — latest ready subset manifest candidate definition report.
