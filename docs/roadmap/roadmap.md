# Roadmap Progress — Travel Assistant

Этот документ является **primary roadmap** проекта Travel Assistant. Он фиксирует статусы этапов, progression, границы этапов, quality gates, carryover и следующий разрешенный шаг.

Roadmap не является task tracker, продуктовой спецификацией, архитектурной спецификацией, ADR registry или implementation backlog. Детальные product baseline и architecture baseline вынесены в отдельные документы и указаны ниже.

## 1. Текущий статус проекта

| Пункт | Статус |
|---|---|
| Текущий этап | Stage 7 — реализация MVP / ожидает отдельную явную задачу |
| Последний завершенный этап | Stage 7.42 — Assistant Endpoint Conformance Candidate Implementation |
| Следующий планируемый шаг | Stage 7.43 — Assistant Endpoint Conformance Candidate Verification, только через отдельную явную roadmap-aligned задачу |
| Подробный roadmap/status source of truth | Только этот документ: `docs/roadmap/roadmap.md` |

| Область | Текущее состояние |
|---|---|
| Stage 0-6 | Завершены |
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
| Generated-client/OpenAPI readiness | Не заявлена |
| Generated-client-ready subset / generated clients | Non-readiness manifest candidate создан; generated-client-ready subset/readiness и generated clients не созданы |
| Full conformance gate | Не реализован |
| Business logic / provider integration / DB-storage / frontend / production implementation | Не начаты |

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
| Stage 7 | В работе / ожидает отдельную явную задачу | Bounded Kotlin + Ktor backend foundation, assistant boundaries, conformance-tool work, documentation stabilization, resume-development handoff, technical context review, manifest candidate definition, manifest validation hardening, endpoint candidate review, assistant endpoint candidate clarification, contract/runtime alignment notes, cleanup decision, contract shape cleanup, runtime contract test cleanup, conformance/tooling follow-up decision и conformance candidate implementation завершены до Stage 7.42 включительно. Подробности см. в Stage 7 checklist ниже. |
| Stage 8 | Запланирован | Улучшения AI/LLM orchestration после появления основы MVP implementation. |
| Stage 9 | Запланирован | Укрепление real provider/API integration после предоставления и активации provider/API contracts. |
| Stage 10 | Запланирован | Cross-platform expansion после стабилизации core product и architecture. |

## 2. Правила управления roadmap

- `docs/roadmap/roadmap.md` является source of truth по статусам этапов, progression, границам этапов, carryover и следующему разрешенному шагу.
- `docs/ROADMAP.md` является верхнеуровневым navigation overview, а не конкурирующим источником текущего статуса. Он должен содержать только stage-purpose map и не должен включать матрицу текущего состояния, last completed step, next planned step или implementation readiness.
- Документы правил в `docs/development/` являются активными engineering rules для явно ограниченных задач. `docs/development/roadmap.md` и `docs/development/implementation-strategy.md` остаются future/reference material, а не active implementation backlog. Все development docs должны следовать этому roadmap.
- Planned и future stages не являются active backlog. Каждый будущий этап начинается только после отдельной явной roadmap-задачи.
- Recommendations, carryover и future candidates не должны автоматически выполняться во время review или cleanup задач.
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

## 5. Open Decisions and Carryover

Эти пункты являются carryover и входными данными для будущих решений, а не active backlog:

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

**Статус:** В работе / ожидает отдельную явную задачу.

**Назначение:** реализация согласованного hotel-only MVP v1 после Stage 6 через отдельные bounded tasks. Stage 7 больше не заблокирован backend stack drift или restart readiness review.

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

**Текущие исключения Stage 7 и неначатые работы:**

| Category | Status |
|---|---|
| Generated-client/OpenAPI readiness | Не заявлена |
| Generated-client-ready subset | Non-readiness candidate manifest создан; ready subset не заявлен |
| Generated clients | Не созданы |
| Full conformance gate | Не реализован |
| Real hotel search business logic, ranking and recommendation behavior | Не начаты |
| Provider integration, provider-specific DTO/contracts and production integrations | Не начаты |
| DB/storage, Redis/cache, auth/account flows and persistent account history | Не начаты |
| Frontend, booking, payment, flights and combined itinerary | Не начаты / вне текущего MVP v1 scope, пока не активированы отдельно |

**Ключевой guardrail Stage 7:** завершенные Stage 7 slices не означают generated-client readiness, OpenAPI finalization, DB/storage activation, real provider integration, frontend implementation или Stage 8 activation. Любая следующая implementation, cleanup или expansion work требует отдельной явной roadmap-aligned задачи.

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

**Следующий шаг:** Stage 7.43 — Assistant Endpoint Conformance Candidate Verification, только через отдельную явную roadmap-aligned задачу. Перед стартом читать current roadmap/status, `docs/reviews/stage-7-41-assistant-endpoint-conformance-tooling-follow-up-decision.md`, `docs/reviews/stage-7-42-assistant-endpoint-conformance-candidate-implementation.md`, `tools/openapi-conformance/README.md`, relevant files under `tools/openapi-conformance/src/` и `docs/architecture/stage-7/generated-client-ready-subset.yaml`. Stage 7.43 должен быть review-only verification этапом и не должен менять manifest, OpenAPI contracts, backend runtime, generated clients, подключать CI/Gradle gate или заявлять readiness без отдельного явно ограниченного scope. Stage 8+ остаются Planned и не активированы. Generated-client/OpenAPI readiness не заявлена.

### Stage 8 — AI/LLM Orchestration Improvements

**Статус:** Запланирован.

**Границы:** улучшение уточнений, объяснений, сравнения и устойчивости AI behaviour без привязки продукта к одному LLM provider.

### Stage 9 — Real Provider/API Integration Hardening

**Статус:** Запланирован.

**Границы:** adapter design, provider-specific error handling, reliability и production-hardening вокруг реального provider/API после предоставления и активации нужных контрактов.

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
