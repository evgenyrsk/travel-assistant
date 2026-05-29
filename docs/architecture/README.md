# Архитектурная документация

Эта директория содержит архитектурную документацию Travel Assistant.

## Текущий статус

- Stage 5 — Technical Architecture / System Design: Completed.
- Stage 6 — Implementation Preparation: Planned / not started.
- Code/API/DB/UI implementation: Not started.

Документы Stage 5 являются текущим architecture baseline. Они описывают границы, ответственности и guardrails для hotel-only MVP v1. Они не являются implementation backlog, API/OpenAPI contract, endpoint specification, database schema, storage model, provider adapter design, auth/security/DevOps/testing plan или production implementation plan.

## Основные ссылки

- `../roadmap/roadmap.md` — primary roadmap и источник истины для текущего статуса этапов.
- `../reviews/pre-stage-6-documentation-consistency-review.md` — review согласованности документации перед Stage 6.

## Deliverables Stage 5

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

Stage 6 не начат. Не создавай `docs/architecture/stage-6/`, API/OpenAPI contracts, endpoint specs, DB schema, storage model, auth/security/DevOps/testing backlog или production code без отдельной явной roadmap-задачи, которая разрешает такую работу.
