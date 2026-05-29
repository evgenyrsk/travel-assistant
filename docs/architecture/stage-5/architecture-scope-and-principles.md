# Stage 5.1 — Architecture Scope & Principles

## Назначение

Stage 5 переводит продуктовые решения, требования, UX и visual-design decisions из Stage 0-4 в архитектурные границы и принципы Travel Assistant.

Этот этап определяет, как future system design должен защищать hotel-only MVP v1 scope, разделять domain/application responsibilities, сохранять provider и LLM integrations заменяемыми и поддерживать различие между user-provided constraints, provider facts, assistant assumptions и unknown data.

Stage 5 не является implementation stage. Он не создает production code, API contracts, database schema, UI components, provider adapters или implementation tasks.

## Scope Stage 5

Stage 5 может определять:

- system architecture;
- domain/application boundaries;
- AI/LLM orchestration boundaries;
- provider integration boundaries;
- data ownership boundaries;
- MVP non-functional architecture considerations;
- architecture decisions, влияющие на будущую implementation.

Stage 5 должен превратить предыдущие product и UX commitments в стабильные architectural constraints без старта Stage 6 implementation preparation или Stage 7 production development.

## Explicitly out of scope

Следующее явно находится вне scope Stage 5.1 и не должно вводиться этим документом:

- production code;
- database migrations;
- real provider integrations;
- OpenAPI contracts;
- UI implementation;
- auth implementation;
- booking/payment flows;
- flight search;
- combined itinerary;
- account history;
- implementation backlog.

## MVP v1 scope guardrails

MVP v1 является hotel-only.

Assistant помогает уточнять user intent, объяснять options, сравнивать hotel offers и поддерживать decision-making. Он не должен подразумевать, что booking, payment, flight search, combined itinerary planning или account history доступны в MVP v1.

Разделы future expansion могут упоминать flights, combined itinerary, booking, payments, full authorization или account history только как future context. Они должны оставаться явно помеченными как future expansion и не должны становиться active MVP v1 requirements.

## Required constraints from Stage 0-4

Архитектурная работа Stage 5 должна сохранять эти constraints из Stage 0-4:

- Продукт является chat-first, not chat-only.
- Results view существует рядом с assistant conversation.
- Search Intent Summary является явным UX/domain concept.
- Hotel Offer Card является central для MVP hotel comparison.
- Provider facts должны быть отделены от assistant assumptions.
- User-provided constraints должны быть отделены от provider facts.
- Unknown data не должна выдумываться.
- Assistant может рассуждать, но должен отличать assumptions от facts.

Architecture должна делать эти distinctions видимыми в будущих domain и application boundaries, чтобы provider data, user input и assistant reasoning не схлопывались в одну неоднозначную модель.

## Архитектурные принципы

### Domain-first boundaries

Domain concepts должны описываться до framework, storage, transport или provider details. Будущая implementation должна сохранять domain logic независимой от web framework, database, UI и concrete LLM/provider SDKs.

### Provider-agnostic integrations

Hotel search должен быть представлен за provider-agnostic boundaries. Provider-specific facts, errors и limitations должны переводиться на integration boundaries, а не протекать в domain или UX concepts.

### AI-assisted but not AI-owned facts

Assistant может interpret, summarize, compare и explain, но provider/API data остается source of truth для hotel facts. LLM output не должен становиться authoritative source для availability, price, hotel attributes или provider-originated travel facts.

### Explicit uncertainty handling

Architecture должна сохранять uncertainty как first-class concern. Unknown data, missing fields, unavailable provider facts и assistant assumptions должны оставаться различимыми в будущих models и flows.

### MVP-first, expansion-ready

Architecture должна сначала обслуживать hotel-only MVP v1, оставляя clear extension points для будущего flight search, combined itinerary, booking, account history и deeper authorization. Extension readiness не должна втягивать future scope в MVP v1.

### No hidden scope expansion

Architecture documents не должны протаскивать future product features в текущие requirements. Любое expansion за пределы hotel-only MVP v1 требует будущего product decision и, если выбор влияет на long-term architecture, вероятно ADR.

### Architecture before implementation

Stage 5 документирует architectural boundaries и decisions до implementation preparation. Он не должен определять production classes, migrations, endpoint contracts, UI tasks или delivery backlog.

### Readable documentation for future coding agents

Architecture documents должны быть краткими, явными и простыми для future coding agents. Они должны делать scope limits, source-of-truth rules и future-expansion boundaries трудно пропускаемыми.

## Политика future expansion

Future features могут упоминаться только как context for extensibility и risk management.

Future expansion notes не должны определять detailed MVP requirements, detailed UX requirements, provider contracts, database models или implementation tasks для future areas.

Активация flight search, booking, payment flows, account history, full authorization или combined itinerary planning требует будущего product decision. Если активация меняет architecture boundaries, public contracts, provider strategy, data ownership или security posture, вероятно также требуется ADR.

## Stage 5 deliverables preview

Будущая работа Stage 5 может создать эти документы отдельными задачами. Этот список является planning preview, а не инструкцией создавать их сейчас:

- `system-context-and-boundaries.md`
- `domain-model-and-boundaries.md`
- `application-orchestration.md`
- `integration-architecture.md`
- `data-and-storage-boundaries.md`
- `non-functional-requirements.md`
- `architecture-decisions-draft.md`
- `stage-5-summary-and-carryover.md`
- `stage-5-consistency-review.md`

## Open questions

- Какие architecture decisions в Stage 5 требуют ADR, а не обычных architecture notes?
- Какие минимальные hotel provider capabilities требуются existing travel API contract, когда он будет предоставлен?
- Как future domain/application boundaries должны представлять user-provided constraints, provider facts, assistant assumptions и unknown data без преждевременного определения implementation classes?
- Какие data ownership boundaries нужны для save/shortlist в MVP v1 без введения account history или full authorization?
- Какие MVP non-functional constraints являются architectural requirements, а какие должны оставаться implementation-stage acceptance criteria?

## Non-goals / что не входит

Этот документ не выбирает production-level implementation details, не вводит code, не определяет API contracts, не проектирует database schema, не создает UI implementation work и не начинает development.

Он устанавливает Stage 5 scope, guardrails и principles, чтобы последующие architecture documents могли двигаться дальше без изменения roadmap order или расширения hotel-only MVP v1.
