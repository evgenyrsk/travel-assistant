# Архитектурная документация

Эта директория содержит архитектурную документацию Travel Assistant. Этот README является index-документом: он помогает найти актуальный architecture baseline, Stage 5 artifacts и Stage 6 contract artifacts, но не является implementation plan, architecture spec, API contract или backlog задач.

## Текущий статус

- Stage 5 — Technical Architecture / System Design: Completed.
- Stage 6 — API Contracts / OpenAPI / Integration Boundary: Completed; Stage 6.1-6.9 completed.
- Stage 7 — MVP Implementation: Planned / not activated.
- Code/API/DB/UI implementation: Not started.

`architecture-baseline.md` фиксирует актуальный compact architecture baseline после Stage 5. Документы Stage 5 сохраняются как historical architecture artifacts, deliverables и audit trail: они описывают границы, ответственности и guardrails для hotel-only MVP v1, но не являются implementation backlog, API/OpenAPI contract, endpoint specification, database schema, storage model, provider adapter design, auth/security/DevOps/testing plan или production implementation plan.

Статусы этапов, progression и следующий разрешенный шаг фиксируются в primary roadmap `../roadmap/roadmap.md`.

## Основные ссылки

- `../roadmap/roadmap.md` — primary roadmap и источник истины для текущего статуса этапов.
- `architecture-baseline.md` — актуальный compact architecture baseline после Stage 5.
- `../reviews/pre-stage-6-documentation-consistency-review.md` — review согласованности документации перед Stage 6.

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

Stage 6 завершен как contract/documentation phase. Созданный и уточненный OpenAPI draft является documentation-level API contract draft, а не backend/frontend implementation, provider-specific contract, DB schema, storage model, auth/security/DevOps/testing backlog или production code. Stage 7 остается Planned / not activated до отдельной явной задачи.
