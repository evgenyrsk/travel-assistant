# Roadmap Progress — Travel Assistant

Этот документ является **primary roadmap** проекта Travel Assistant. Он фиксирует статусы этапов, progression, границы этапов, quality gates, carryover и следующий разрешенный шаг.

Roadmap не является task tracker, product specification, architecture specification, ADR registry или implementation backlog. Детальные product baseline и architecture baseline вынесены в отдельные документы и указаны ниже.

## 1. Текущий статус проекта

| Пункт | Статус |
|---|---|
| Текущий этап | Stage 7 — MVP Implementation / awaiting explicit next task. Stage 7.2 backend application foundation completed by explicit roadmap-aligned task |
| Последний завершенный этап | Stage 7.2 — Kotlin + Ktor Backend Application Foundation |
| Следующий планируемый шаг | Отдельная явная roadmap-aligned задача должна выбрать следующий bounded Stage 7 implementation step или cleanup item; Stage 7.3+ не активированы |
| Code/API/DB/UI implementation | Minimal Kotlin + Ktor backend foundation exists with health endpoint, structured error handling and placeholder hotel-only routes; business logic, provider integration, DB/storage, frontend, generated clients and production implementation not started |

| Этап | Статус | Краткое описание |
|---|---|---|
| Stage 0 | Completed | Product framing, первичные сценарии, предварительные MVP boundaries и открытые вопросы. |
| Stage 1 | Completed | Business requirements, user journeys, BR/FR/NFR, assumptions, риски и consistency review. |
| Stage 2 | Completed | Use cases, edge cases, правила поведения ассистента, data requirements и combined search levels. |
| Stage 3 | Completed | Hotel-only MVP UX, navigation, required fields, acceptance criteria и carryover. |
| Stage 4 | Completed | Visual direction, основы design system, component inventory, screen specs и interaction patterns. |
| Stage 4.1 | Completed | Visual design consistency review и небольшая правка формулировок. |
| Stage 5 | Completed | Conceptual technical architecture, границы, decision inventory, summary и completion audit. |
| Stage 6 | Completed | API Contracts / OpenAPI / Integration Boundary; Stage 6.1 OpenAPI draft, Stage 6.2 contract review, Stage 6.3 contract fixes, Stage 6.4 post-fix review, Stage 6.5 provider boundary / mapping notes, Stage 6.6 navigation/status cleanup, Stage 6.7 completion review, Stage 6.8 pre-implementation decisions cleanup and Stage 6.9 final closure / handoff completed. |
| Stage 7 | In progress / awaiting explicit next task | Minimal Kotlin + Ktor backend foundation exists. Stage 7.0 stabilization/documentation cleanup and Stage 7.2 backend application foundation completed. Stage 7.3+ требуют отдельных явных задач. |
| Stage 8 | Planned | Улучшения AI/LLM orchestration после появления основы MVP implementation. |
| Stage 9 | Planned | Укрепление real provider/API integration после предоставления и активации provider/API contracts. |
| Stage 10 | Planned | Cross-platform expansion после стабилизации core product и architecture. |

## 2. Правила управления roadmap

- `docs/roadmap/roadmap.md` является source of truth по статусам этапов, progression, границам этапов, carryover и следующему разрешенному шагу.
- `docs/ROADMAP.md` является верхнеуровневым navigation overview, а не конкурирующим источником текущего статуса.
- Документы `docs/development/*` являются future/reference material. Они не являются active implementation backlog и должны следовать этому roadmap.
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
- current-session shortlist only;
- явное разделение provider facts, assistant assumptions и unknowns.

Явно вне MVP v1:

- flights;
- combined itinerary / combined hotel + flight package;
- booking;
- payment;
- account history and account-level storage;
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

## 6. Completed Stages

### Stage 0 — Product Framing

**Статус:** Completed.

**Назначение:** зафиксировать исходную продуктовую рамку, первичные сценарии, предварительные MVP boundaries и правила дальнейшей работы.

**Ключевые артефакты:**

- `docs/product/stage-0/product-framing.md`
- `docs/product/stage-0/initial-scenarios.md`
- `docs/product/stage-0/mvp-boundaries.md`
- `docs/product/stage-0/assumptions-and-open-questions.md`
- `docs/product/README.md`

**Заметка о завершении:** Stage 0 не фиксировал финальные технические решения. Приоритетные пользователи, сценарии и MVP boundaries были уточнены на следующих product stages.

### Stage 1 — Business Requirements

**Статус:** Completed.

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

**Статус:** Completed.

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

**Статус:** Completed.

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

**Статус:** Completed.

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

**Статус:** Completed.

**Назначение:** проверить Stage 4 documentation на согласованность со Stage 0-3, MVP scope и roadmap boundaries.

**Ключевой артефакт:**

- `docs/product/stage-4/stage-4-consistency-review.md`

**Quality gate / заметка о завершении:** Verdict: passed with minor wording fixes. Stage 4.1 не начинал frontend implementation и не расширял MVP scope.

### Stage 5 — Technical Architecture

**Статус:** Completed.

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

**Статус:** Completed. Stage 6.1, Stage 6.2, Stage 6.3, Stage 6.4, Stage 6.5, Stage 6.6, Stage 6.7, Stage 6.8 and Stage 6.9 completed by explicit roadmap tasks.

**Назначение:** API Contracts / OpenAPI / Integration Boundary и scoped implementation-preparation planning. Stage 6 должен сохранять hotel-only MVP v1, Stage 5 architecture baseline, provider-agnostic hotel boundary и отсутствие production implementation.

**Условие активации:** Stage 6 был начат отдельной явной задачей Stage 6.1 и закрыт отдельной явной задачей Stage 6.9. Завершение Stage 6.1, Stage 6.2, Stage 6.3, Stage 6.4, Stage 6.5, Stage 6.6, Stage 6.7, Stage 6.8 или Stage 6.9 не начинает Stage 7 и не разрешает backend/frontend implementation.

**Границы scope:** Stage 6 может определять API/OpenAPI contract drafts, implementation-preparation scope, sequencing, validation approach, local workflow boundaries и conceptual boundaries для mock/fake providers и contract placeholders. Он должен сохранить hotel-only scope MVP v1 и Stage 5 architecture baseline.

**Completed Stage 6.1 artifacts:**

- `docs/architecture/stage-6/openapi-draft.yaml` — primary OpenAPI 3.1 draft для MVP hotel-only frontend/backend API.
- `docs/architecture/stage-6/openapi-contract-notes.md` — notes по MVP endpoints, exclusions, assumptions, open questions и связи со Stage 5 baseline.

**Completed Stage 6.2 artifact:**

- `docs/architecture/stage-6/openapi-contract-review.md` — review OpenAPI draft относительно Stage 2-5 product, UX и architecture baselines; verdict: passed for continued Stage 6 contract work with follow-up findings before client generation or implementation.

**Completed Stage 6.3 artifact:**

- `docs/architecture/stage-6/openapi-fixes-summary.md` — summary of OpenAPI fixes closing Stage 6.2 Major findings and addressing allowed Minor fixes without provider-specific DTOs, implementation code or Stage 7 activation.

**Completed Stage 6.4 artifact:**

- `docs/architecture/stage-6/post-fix-contract-review.md` — post-fix review of Stage 6.3 contract fixes; verdict: passed for continued Stage 6 contract work with one Minor follow-up and no Critical/Major blockers.

**Completed Stage 6.5 artifact:**

- `docs/architecture/stage-6/provider-boundary-mapping-notes.md` — conceptual provider boundary / mapping notes for future hotel provider/source data into existing client-facing OpenAPI concepts without provider-specific DTOs, provider contracts, backend/frontend implementation or Stage 7 activation.

**Completed Stage 6.6 cleanup:**

- Documentation navigation / status sync cleanup updated root/navigation status wording in `README.md`, `docs/ROADMAP.md`, `docs/architecture/README.md` and this roadmap without changing OpenAPI, Stage 6 contract artifacts, provider boundary notes, backend/frontend implementation, DB/storage, ADR or Stage 7 activation.

**Completed Stage 6.7 artifact:**

- `docs/architecture/stage-6/stage-6-completion-review.md` — Stage 6 completion review / contract package summary; verdict: Completed with carryover; recommends explicit Stage 6.8 pre-implementation decisions cleanup before generated clients or richer implementation work.

**Completed Stage 6.8 artifact:**

- `docs/architecture/stage-6/pre-implementation-decisions-cleanup.md` — pre-implementation decisions cleanup for nested 404 modeling, inline offer details, result-envelope search status and current-session page-refresh/persistence behavior; no new resource flows, provider DTOs, DB/storage or Stage 7 activation.

**Completed Stage 6.9 artifact:**

- `docs/architecture/stage-6/stage-6-final-closure-and-handoff.md` — final closure / handoff to implementation; verdict: Stage 6 completed with non-blocking carryover and recorded that Stage 7 had to start only through a separate explicit task.

**Явные исключения:** Stage 6.1, Stage 6.2, Stage 6.3, Stage 6.4, Stage 6.5, Stage 6.6, Stage 6.7, Stage 6.8 и Stage 6.9 не создают backend implementation, frontend implementation, DB schema, storage model, auth/security/DevOps/testing backlog, production implementation, provider-specific integration code, generated clients или Stage 7 implementation tasks. Следующие concrete contract, storage, auth, security, DevOps, testing или implementation artifacts требуют отдельной явной future-stage задачи.

**Quality gate:** Stage 6 закрыт после того, как Stage 6 artifacts явно отделили разрешенную contract/preparation work от исключенной DB/storage/auth/DevOps/testing/production work. Любой следующий concrete contract, implementation artifact или expansion за пределы hotel-only требует отдельного явного roadmap step.

### Stage 7 — MVP Implementation

**Статус:** In progress / awaiting explicit next task.

Stage 7.0 stabilization завершена как bounded documentation/correction chain: backend stack drift исправлен, restart readiness review пройден, а Stage 7.0e - Stage 7.0f-f закрыли audit, navigation, governance, development docs, product/architecture index labels и roadmap readability cleanup. Stage 7.2 завершил минимальную Kotlin + Ktor backend application foundation. Подробности находятся в linked cleanup reports ниже.

**Границы:** реализация согласованного hotel-only MVP v1 после завершения Stage 6 и отдельных явных implementation tasks. Stage 7 больше не заблокирован backend stack drift или restart readiness review. Stage 7.2 завершен как foundation-only задача, но Stage 7.3 или любая последующая implementation task не активированы. Подтвержденный backend stack — Kotlin + Ktor. Flight search остается более поздним расширением после hotel flow; combined hotel+flight остается более поздним расширением после flight flow.

**Completed Stage 7 artifacts and cleanup reports:**

- `services/backend/` — minimal Kotlin + Ktor backend skeleton with application entrypoint, health route and health endpoint test.
- `services/backend/README.md` — local backend run notes, health endpoint list and explicit non-implemented scope.
- `docs/reviews/project-consistency-audit.md` — global consistency audit that identified the backend stack blocker.
- `docs/reviews/backend-stack-decision-sync.md` — Stage 7.0a backend stack decision and documentation sync handoff.
- `docs/reviews/backend-skeleton-correction.md` — Stage 7.0b correction report for replacing Java/Spring Boot skeleton with Kotlin + Ktor.
- `docs/reviews/stage-7-restart-readiness-review.md` — Stage 7 restart readiness review; verdict: passed with minor notes.
- `docs/reviews/product-baseline-status-cleanup.md` — status cleanup after restart readiness review.
- `docs/reviews/documentation-redundancy-structure-audit.md` — Stage 7.0e documentation redundancy and structure audit; found remaining cleanup needs.
- `docs/reviews/stage-7-status-navigation-sync-cleanup.md` — Stage 7.0f-a narrow status/navigation sync cleanup.
- `docs/reviews/README.md` — reviews index and role labels for current, historical and superseded audit artifacts.
- `docs/reviews/stage-7-reviews-index-historical-labeling-cleanup.md` — Stage 7.0f-b cleanup report for reviews index / historical labeling.
- `docs/reviews/stage-7-prompt-governance-deduplication-cleanup.md` — Stage 7.0f-c cleanup report for prompt/governance deduplication.
- `docs/reviews/stage-7-development-docs-merge-shortening-cleanup.md` — Stage 7.0f-d cleanup report for development docs merge/shortening.
- `docs/reviews/stage-7-product-architecture-index-role-labels-cleanup.md` — Stage 7.0f-e cleanup report for product/architecture index role labels.
- `docs/reviews/stage-7-roadmap-readability-cleanup.md` — Stage 7.0f-f cleanup report for roadmap readability.
- `docs/reviews/stage-7-2-backend-application-foundation.md` — Stage 7.2 implementation report for minimal Kotlin + Ktor backend foundation.

**Явные исключения Stage 7.0b:** Stage 7.0b не реализует search business logic, provider integrations, provider-specific DTO/contracts, DB migrations/entities/repositories, Redis/cache, LLM integration, frontend code, generated clients, booking, payment, flights, combined itinerary или account flows.

**Явные исключения Stage 7.2:** Stage 7.2 не реализует production assistant sessions, hotel search business logic, ranking, provider integrations, provider-specific DTO/contracts, DB migrations/entities/repositories, Redis/cache, LLM integration, frontend code, generated clients, booking, payment, flights, combined itinerary, auth или account flows.

**Следующий шаг:** отдельная явная roadmap-aligned задача должна выбрать следующий bounded Stage 7 implementation step или конкретный bounded documentation cleanup item. Stage 7.3 или любая последующая implementation task не активированы Stage 7.2. Remaining documentation cleanup не является open-ended blocker; это набор отдельных candidates, которые выполняются только через явные задачи. Stage 8+ остаются Planned.

### Stage 8 — AI/LLM Orchestration Improvements

**Статус:** Planned.

**Границы:** улучшение уточнений, объяснений, сравнения и устойчивости AI behaviour без привязки продукта к одному LLM provider.

### Stage 9 — Real Provider/API Integration Hardening

**Статус:** Planned.

**Границы:** adapter design, provider-specific error handling, reliability и production-hardening вокруг реального provider/API после предоставления и активации нужных контрактов.

### Stage 10 — Cross-platform Expansion

**Статус:** Planned.

**Границы:** расширение за пределы первой платформы без переписывания product и domain logic.

**Правило активации будущих этапов:** planned stages не являются active backlog. Каждый будущий этап начинается только после отдельной явной roadmap-задачи, которая активирует этап и подтверждает нужные предыдущие решения.

## 8. Связанные документы и audit trail

- `docs/ROADMAP.md` — верхнеуровневый overview roadmap.
- `docs/product/README.md` — индекс продуктовой документации.
- `docs/architecture/README.md` — индекс архитектурной документации.
- `docs/development/roadmap.md` — compact development reference; только future/reference material.
- `docs/development/milestones.md` — compact milestone vocabulary; только future/reference material.
- `docs/development/implementation-strategy.md` — implementation strategy; future/reference material до активации.
- `docs/reviews/README.md` — index review/audit artifacts и правила чтения historical/current cleanup reports.
- `docs/reviews/stage-7-roadmap-readability-cleanup.md` — latest roadmap readability cleanup report.
