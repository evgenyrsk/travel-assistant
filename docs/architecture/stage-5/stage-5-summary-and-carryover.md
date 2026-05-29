# Stage 5 — Summary & Carryover

## Назначение

Этот документ резюмирует Stage 5 Technical Architecture / System Design для Travel Assistant и фиксирует carryover для будущих этапов.

Stage 5 установил архитектурные границы для Hotel-Only MVP v1 без начала production implementation, API contracts, database schema, provider adapters, vendor/tool selection или implementation backlog.

## Завершенные deliverables Stage 5

Stage 5 завершил:

- `architecture-scope-and-principles.md`;
- `system-context-and-boundaries.md`;
- `domain-model-and-boundaries.md`;
- `application-orchestration.md`;
- `integration-architecture.md`;
- `data-and-storage-boundaries.md`;
- `non-functional-requirements.md`;
- `architecture-decisions-draft.md`;
- `stage-5-consistency-review.md`.

Этот документ закрывает summary и carryover record Stage 5.

## Установленный architecture baseline

Stage 5 установил:

- hotel-only MVP boundary;
- system context и external/future boundaries;
- концептуальные domain concepts и responsibility boundaries;
- концептуальные границы application orchestration;
- границы provider, LLM и frontend/backend integration;
- границы data ownership, volatility и storage;
- architecture-level NFR и quality boundaries;
- draft architecture decision inventory и future ADR candidates.

## Подтвержденные architecture guardrails

- MVP v1 остается hotel-only.
- Provider facts являются source-owned.
- User constraints трассируются к user input или clarification.
- Assistant assumptions помечены и отделены от facts.
- Unknown data остается unknown.
- LLM помогает, но не владеет hotel facts.
- Current-session state не является account history.
- Current-session shortlist не является persistent saved trips, cross-device sync или full-auth account storage.
- Future expansion требует product decision и, вероятно, ADR при изменении architecture boundaries.
- API/DB contracts не должны создаваться до соответствующего roadmap step.
- Stage 5 architecture docs не должны читаться как production implementation plan.

## Отложенные решения

Следующие решения остаются deferred:

- конкретный hotel provider/API contract;
- конкретный LLM provider/model;
- prompt/guardrail implementation;
- DB/storage technology;
- API/OpenAPI contracts;
- deployment topology;
- telemetry stack;
- auth/account model;
- booking/payment architecture;
- flight/combined itinerary architecture.

## Carryover to next stage

Следующий этап должен сохранить:

- facts/assumptions/unknowns separation;
- provider-agnostic hotel boundary;
- chat-first, not chat-only UX;
- Search Intent Summary как UX/domain bridge;
- Hotel Offer Card как central comparison surface;
- отсутствие hidden account history или full auth;
- отсутствие flight, combined itinerary, booking или payment в MVP v1;
- source/freshness uncertainty как visible concept;
- current-session shortlist только как session-level selection aid.

Следующий этап не должен трактовать этот carryover как implementation backlog. Это архитектурный контекст для будущего planning.

## Риски для контроля

- Current-session shortlist может случайно превратиться в account history.
- Provider abstraction может преждевременно превратиться в API contract.
- LLM boundary может размыться и начать создавать или переписывать provider facts.
- Future expansion может быть ошибочно прочитан как MVP scope.
- NFRs могут слишком рано превратиться в DevOps/security/testing backlog.
- Ограничения existing travel API могут продавить provider DTOs в domain concepts.
- Editability Search Intent Summary может ввести persistence assumptions, если не будет явно решена.

## Рекомендуемый следующий шаг

Stage 5 завершен, потому что consistency review не нашел Critical или Major blockers.

Следующий этап должен начинаться только по отдельному явному запросу.

Не начинай Stage 6 в этой задаче.

## Non-goals

Этот документ не определяет:

- production code;
- API contracts;
- OpenAPI;
- DB schema;
- ERD;
- DTOs/classes/interfaces/enums;
- module/package structure;
- provider adapters;
- vendor/tool selection;
- implementation backlog;
- Stage 6 work.
