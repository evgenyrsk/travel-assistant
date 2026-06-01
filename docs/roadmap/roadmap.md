# Roadmap Progress — Travel Assistant

Этот документ является **primary roadmap** проекта Travel Assistant. Он фиксирует статусы этапов, progression, границы этапов, quality gates, carryover и следующий разрешенный шаг.

Roadmap не является task tracker, product specification, architecture specification, ADR registry или implementation backlog. Детальные product baseline и architecture baseline вынесены в отдельные документы и указаны ниже.

## 1. Текущий статус проекта

| Пункт | Статус |
|---|---|
| Текущий этап | Pre-Stage 6 roadmap cleanup завершен; Stage 6 остается Planned / not started |
| Последний завершенный этап | Stage 5.9 — Stage 5 Consistency Review / Completion Audit |
| Следующий планируемый шаг | Следующую задачу нужно выбрать явно; Stage 6 остается Planned / not started до отдельной активации |
| Code/API/DB/UI implementation | Not started |

| Этап | Статус | Краткое описание |
|---|---|---|
| Stage 0 | Completed | Product framing, первичные сценарии, предварительные MVP boundaries и открытые вопросы. |
| Stage 1 | Completed | Business requirements, user journeys, BR/FR/NFR, assumptions, риски и consistency review. |
| Stage 2 | Completed | Use cases, edge cases, правила поведения ассистента, data requirements и combined search levels. |
| Stage 3 | Completed | Hotel-only MVP UX, navigation, required fields, acceptance criteria и carryover. |
| Stage 4 | Completed | Visual direction, основы design system, component inventory, screen specs и interaction patterns. |
| Stage 4.1 | Completed | Visual design consistency review и небольшая правка формулировок. |
| Stage 5 | Completed | Conceptual technical architecture, границы, decision inventory, summary и completion audit. |
| Stage 6 | Planned / not started | Планирование подготовки к реализации; любые работы Stage 6 требуют отдельной явной активации. |
| Stage 7 | Planned | Реализация hotel-only MVP после нужного планирования и отдельной явной активации. |
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
- Следующий этап или cleanup task должны быть выбраны отдельной явной задачей.

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

## 7. Планируемые и будущие этапы

### Stage 6 — Implementation Preparation

**Статус:** Planned / not started.

**Назначение:** scoped implementation-preparation planning, task framing, validation strategy, mock/fake provider approach, contract-placeholder boundaries и local workflow boundaries.

**Условие активации:** Stage 6 начинается только после отдельной явной задачи, которая активирует Stage 6 planning/scope definition. Завершение reviews, documentation cleanup или controlled refactoring до Stage 6 не начинает Stage 6.

**Границы scope:** Stage 6 может определить implementation-preparation scope, sequencing, validation approach, local workflow boundaries и conceptual boundaries для mock/fake providers и contract placeholders. Он должен сохранить hotel-only scope MVP v1 и Stage 5 architecture baseline.

**Явные исключения:** Stage 6 не создает автоматически API/OpenAPI contracts, endpoint specs, DB schema, storage model, auth/security/DevOps/testing backlog, production implementation, provider-specific integration code или Stage 7 implementation tasks.

**Quality gate:** Stage 6 может закрыться только после того, как planning artifacts явно отделят разрешенную implementation-preparation work от исключенной API/DB/storage/auth/DevOps/testing/production work. Любой конкретный contract или implementation artifact требует отдельного явного roadmap step.

### Stage 7 — MVP Implementation

**Статус:** Planned.

**Границы:** реализация согласованного hotel-only MVP v1 после завершения Stage 6 и отдельной явной активации implementation. Flight search остается более поздним расширением после hotel flow; combined hotel+flight остается более поздним расширением после flight flow.

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
- `docs/development/roadmap.md` — secondary development roadmap; только future/reference material.
- `docs/development/milestones.md` — development milestones; только future/reference checkpoints.
- `docs/development/implementation-strategy.md` — implementation strategy; future/reference material до активации.
- `docs/reviews/pre-stage-6-documentation-consistency-review.md` — documentation consistency review перед Stage 6.
- `docs/reviews/roadmap-structure-and-process-fitness-review.md` — review структуры roadmap и process fitness.
- `docs/reviews/global-documentation-quality-review.md` — глобальный review качества документации.
- `docs/reviews/documentation-refactoring-plan.md` — план controlled documentation refactoring.
