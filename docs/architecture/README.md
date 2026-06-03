# Архитектурная документация

Эта директория содержит архитектурную документацию Travel Assistant. Этот README является index-документом: он помогает найти актуальный architecture baseline, Stage 5 artifacts и Stage 6 contract artifacts, но не является implementation plan, architecture spec, API contract или backlog задач.

`architecture-baseline.md` является текущим architecture source of truth: он фиксирует актуальный compact architecture baseline, backend stack direction, system boundaries и границы production implementation. Этот README является navigation/index only и не должен конкурировать с `architecture-baseline.md`.

Stage 5/6/7 documents сохраняются как architecture audit trail, contract artifacts или historical/superseded artifacts в зависимости от статуса конкретного файла. OpenAPI drafts и contract reviews являются contract artifacts, а не general roadmap, active implementation backlog или provider-specific integration plan. Historical stack mentions, включая old Java/Spring Boot context, не переопределяют текущую Kotlin + Ktor direction из `architecture-baseline.md`.

Для architecture scope Codex должен сначала открыть `architecture-baseline.md`, затем primary roadmap `../roadmap/roadmap.md`, а этот README использовать только как навигацию.

## Текущий статус

- Stage 5 — Technical Architecture / System Design: Completed.
- Stage 6 — API Contracts / OpenAPI / Integration Boundary: Completed; Stage 6.1-6.9 completed.
- Stage 7 — MVP Implementation: In progress / awaiting explicit next task; Stage 7.0 stabilization and documentation cleanup завершены до Stage 7.0f-f включительно; Stage 7.2+ not activated.
- Code/API/DB/UI implementation: minimal Kotlin + Ktor backend skeleton exists; business logic, provider integration, DB/storage, frontend, generated clients and production implementation not started.

`architecture-baseline.md` фиксирует актуальный compact architecture baseline и подтверждает backend stack Kotlin + Ktor. Java/Spring Boot не является принятым Travel Assistant backend stack без явного будущего ADR и задачи, согласованной с roadmap. Документы Stage 5 сохраняются как historical architecture artifacts, deliverables и audit trail: они описывают границы, ответственности и guardrails для hotel-only MVP v1, но не являются implementation backlog, API/OpenAPI contract, endpoint specification, database schema, storage model, provider adapter design, auth/security/DevOps/testing plan или production implementation plan.

Статусы этапов, progression и следующий разрешенный шаг фиксируются в primary roadmap `../roadmap/roadmap.md`.

## Иерархия ролей

1. `architecture-baseline.md` — current architecture source of truth и backend stack authority.
2. `../roadmap/roadmap.md` — source of truth по статусам этапов, progression, carryover и следующему разрешенному шагу.
3. `README.md` — architecture navigation/index only; не competing architecture spec.
4. `stage-5/**` — historical architecture stage artifacts и detailed conceptual baseline context.
5. `stage-6/**` — API/contract drafts, contract reviews и completion/handoff artifacts; не general roadmap.
6. `stage-7/**` — Stage 7 review artifacts; old skeleton context может быть superseded текущей Kotlin + Ktor correction.

## Инвентаризация architecture docs

| Документ | Классификация | Как читать |
|---|---|---|
| `architecture-baseline.md` | Current architecture source of truth | Начинать отсюда при проверке architecture scope, backend stack и system boundaries. |
| `README.md` | Architecture navigation/index | Использовать для поиска документов и понимания ролей, не как architecture spec. |
| `stage-5/architecture-scope-and-principles.md` | Historical architecture stage artifact / reference-only | Conceptual scope, guardrails и principles under current baseline. |
| `stage-5/system-context-and-boundaries.md` | Historical architecture stage artifact / reference-only | System context и boundaries; не API contract. |
| `stage-5/domain-model-and-boundaries.md` | Historical architecture stage artifact / reference-only | Conceptual domain model; не DTO/classes/schema. |
| `stage-5/application-orchestration.md` | Historical architecture stage artifact / reference-only | Conceptual orchestration; не state machine или implementation plan. |
| `stage-5/integration-architecture.md` | Historical architecture stage artifact / reference-only | Provider/LLM/frontend-backend boundaries; не provider adapter design. |
| `stage-5/data-and-storage-boundaries.md` | Historical architecture stage artifact / reference-only | Conceptual data/storage boundaries; не DB schema. |
| `stage-5/non-functional-requirements.md` | Historical architecture stage artifact / reference-only | Quality attributes; не DevOps/security/testing backlog. |
| `stage-5/architecture-decisions-draft.md` | Historical architecture stage artifact / reference-only | Non-ADR decision inventory and ADR candidates; не accepted ADR. |
| `stage-5/stage-5-consistency-review.md` | Review/audit artifact | Stage 5 quality gate; не current roadmap status. |
| `stage-5/stage-5-summary-and-carryover.md` | Historical architecture stage artifact / reference-only | Stage 5 summary and carryover context. |
| `stage-6/openapi-draft.yaml` | API/contract draft | Documentation-level OpenAPI draft; не provider contract, generated client trigger или implementation backlog. |
| `stage-6/openapi-contract-notes.md` | API/contract draft | Contract notes and assumptions; не general roadmap. |
| `stage-6/openapi-contract-review.md` | Review/audit artifact / API contract review | Stage 6.2 contract review; не active backlog. |
| `stage-6/openapi-fixes-summary.md` | API/contract draft / review follow-up | Contract fixes summary; не implementation work. |
| `stage-6/post-fix-contract-review.md` | Review/audit artifact / API contract review | Stage 6.4 contract review; не active backlog. |
| `stage-6/provider-boundary-mapping-notes.md` | API/contract draft / reference-only | Conceptual provider mapping notes; не provider-specific DTOs/adapters. |
| `stage-6/stage-6-completion-review.md` | Review/audit artifact | Stage 6.7 completion review; historical readiness context. |
| `stage-6/pre-implementation-decisions-cleanup.md` | API/contract draft / review follow-up | Pre-implementation contract cleanup; не Stage 7 activation. |
| `stage-6/stage-6-final-closure-and-handoff.md` | Review/audit artifact / reference-only | Stage 6 closure/handoff; не automatic implementation start. |
| `stage-7/stage-7-1-backend-skeleton-review.md` | Stale/superseded architecture artifact / review artifact | Old Java/Spring Boot skeleton review, superseded by Stage 7.0b Kotlin + Ktor correction. |

Unclear role: нет, все текущие файлы `docs/architecture/**` классифицированы этим index.

## Основные ссылки

- `../roadmap/roadmap.md` — primary roadmap и источник истины для текущего статуса этапов.
- `architecture-baseline.md` — актуальный compact architecture baseline после Stage 5.
- `../reviews/pre-stage-6-documentation-consistency-review.md` — review согласованности документации перед Stage 6.
- `../../services/backend/README.md` — Stage 7.0b Kotlin + Ktor backend skeleton run notes and health endpoint.
- `../reviews/stage-7-restart-readiness-review.md` — Stage 7 restart readiness review; verdict: passed with minor notes.
- `../reviews/README.md` — index review/audit artifacts и правила чтения historical/current cleanup reports.
- `../reviews/documentation-redundancy-structure-audit.md` — Stage 7.0e documentation redundancy and structure audit.
- `../reviews/stage-7-status-navigation-sync-cleanup.md` — Stage 7.0f-a status/navigation sync cleanup.
- `../reviews/stage-7-reviews-index-historical-labeling-cleanup.md` — Stage 7.0f-b reviews index / historical labeling cleanup.
- `../reviews/stage-7-prompt-governance-deduplication-cleanup.md` — Stage 7.0f-c prompt/governance deduplication cleanup.
- `../reviews/stage-7-development-docs-merge-shortening-cleanup.md` — Stage 7.0f-d development docs merge/shortening cleanup.
- `../reviews/stage-7-product-architecture-index-role-labels-cleanup.md` — Stage 7.0f-e product/architecture index role labels cleanup.
- `../reviews/stage-7-roadmap-readability-cleanup.md` — Stage 7.0f-f roadmap readability cleanup.

## Stage 7 artifacts

- `../../services/backend/` — minimal Kotlin + Ktor backend skeleton with health endpoint and health test coverage for Stage 7.0b only.
- `stage-7/stage-7-1-backend-skeleton-review.md` — historical Stage 7.1 backend skeleton scope audit for the old Java/Spring Boot skeleton; superseded by Stage 7.0b Kotlin + Ktor correction.
- `../reviews/backend-stack-decision-sync.md` — Stage 7.0a backend stack decision and documentation sync handoff.
- `../reviews/backend-skeleton-correction.md` — Stage 7.0b backend skeleton correction report.

## Stage 6 artifacts

- `stage-6/openapi-draft.yaml` — Stage 6.1 primary OpenAPI 3.1 draft для MVP hotel-only frontend/backend API.
- `stage-6/openapi-contract-notes.md` — notes к OpenAPI draft: MVP endpoints, exclusions, assumptions, open questions и связь со Stage 5 baseline.
- `stage-6/openapi-contract-review.md` — Stage 6.2 review OpenAPI draft относительно Stage 2-5 product, UX и architecture baselines.
- `stage-6/openapi-fixes-summary.md` — Stage 6.3 summary of OpenAPI fixes по Major findings Stage 6.2 и allowed Minor fixes.
- `stage-6/post-fix-contract-review.md` — Stage 6.4 post-fix review of Stage 6.3 contract fixes and remaining readiness notes.
- `stage-6/provider-boundary-mapping-notes.md` — Stage 6.5 provider boundary / mapping notes для future hotel provider/source data into existing client-facing OpenAPI concepts.
- Stage 6.6 — documentation navigation / status sync cleanup; обновлены только navigation/status documents без изменения Stage 6 contract artifacts.
- `stage-6/stage-6-completion-review.md` — Stage 6.7 completion review / contract package summary and carryover record.
- `stage-6/pre-implementation-decisions-cleanup.md` — Stage 6.8 pre-implementation decisions cleanup for generated-client-facing carryover.
- `stage-6/stage-6-final-closure-and-handoff.md` — Stage 6.9 final closure / handoff to implementation.

## Stage 5 artifacts

- `stage-5/architecture-scope-and-principles.md` — scope Stage 5, guardrails и архитектурные принципы.
- `stage-5/system-context-and-boundaries.md` — system context, акторы и boundary rules.
- `stage-5/domain-model-and-boundaries.md` — концептуальная доменная модель и границы ответственности.
- `stage-5/application-orchestration.md` — концептуальные границы application orchestration.
- `stage-5/integration-architecture.md` — границы provider, LLM и frontend/backend integrations.
- `stage-5/data-and-storage-boundaries.md` — концептуальные границы владения данными, изменчивости и хранения.
- `stage-5/non-functional-requirements.md` — architecture-level quality attributes и NFR boundaries.
- `stage-5/architecture-decisions-draft.md` — non-ADR inventory архитектурных решений, deferred decisions и future ADR candidates.
- `stage-5/stage-5-consistency-review.md` — consistency review / completion audit Stage 5.
- `stage-5/stage-5-summary-and-carryover.md` — summary Stage 5 и carryover для будущих этапов.

## Guardrails

- MVP v1 остается hotel-only.
- Flights, combined itinerary, booking, payment, account history и full auth остаются вне MVP v1.
- Provider facts должны приходить из providers/source data, а не из LLM output.
- LLM может уточнять, резюмировать, объяснять, сравнивать и ранжировать, но не должен выдумывать provider facts.
- User-provided constraints, provider facts, assistant assumptions и unknown data должны оставаться разделенными.
- Provider abstraction не является API contract.
- Current-session shortlist не является account history или persistent saved trips.
- Future ADR candidates не являются accepted ADR.

## Граница Stage 6

Stage 6 завершен как contract/documentation phase. Созданный и уточненный OpenAPI draft является documentation-level API contract draft, а не provider-specific contract, DB schema, storage model, auth/security/DevOps/testing backlog или production code. Stage 7 больше не заблокирован backend stack drift или restart readiness review, но Stage 7.2+ не активированы и требуют отдельной явной roadmap-aligned задачи. Remaining documentation cleanup является bounded future cleanup candidate, а не open-ended blocker.
