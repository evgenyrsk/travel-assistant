# Stage 5.1 — Architecture Scope & Principles

## Purpose

Stage 5 translates the product, requirements, UX and visual-design decisions from Stage 0-4 into architecture boundaries and principles for Travel Assistant.

This stage defines how future system design should protect the hotel-only MVP v1 scope, separate domain/application responsibilities, keep provider and LLM integrations replaceable, and preserve the distinction between user-provided constraints, provider facts, assistant assumptions and unknown data.

Stage 5 is not an implementation stage. It does not create production code, API contracts, database schema, UI components, provider adapters or implementation tasks.

## Stage 5 Scope

Stage 5 may define:

- system architecture;
- domain/application boundaries;
- AI/LLM orchestration boundaries;
- provider integration boundaries;
- data ownership boundaries;
- MVP non-functional architecture considerations;
- architecture decisions that affect future implementation.

Stage 5 should turn prior product and UX commitments into stable architectural constraints without starting Stage 6 implementation preparation or Stage 7 production development.

## Explicitly Out of Scope

The following are explicitly out of scope for Stage 5.1 and must not be introduced by this document:

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

## MVP v1 Scope Guardrails

MVP v1 is hotel-only.

The assistant helps clarify user intent, explain options, compare hotel offers and guide decision-making. It must not imply that booking, payment, flight search, combined itinerary planning or account history are available in MVP v1.

Future expansion sections may mention flights, combined itinerary, booking, payments, full authorization or account history only as future context. They must remain explicitly marked as future expansion and must not become active MVP v1 requirements.

## Required Constraints from Stage 0-4

Stage 5 architecture work must preserve these constraints from Stage 0-4:

- The product is chat-first, not chat-only.
- A results view exists alongside the assistant conversation.
- Search Intent Summary is an explicit UX/domain concept.
- Hotel Offer Card is central to MVP hotel comparison.
- Provider facts must be separated from assistant assumptions.
- User-provided constraints must be separated from provider facts.
- Unknown data must not be fabricated.
- The assistant may reason, but must distinguish assumptions from facts.

The architecture should make these distinctions visible in future domain and application boundaries so that provider data, user input and assistant reasoning do not collapse into one ambiguous model.

## Architecture Principles

### Domain-first boundaries

Domain concepts should be described before framework, storage, transport or provider details. Future implementation should keep domain logic independent from web framework, database, UI and concrete LLM/provider SDKs.

### Provider-agnostic integrations

Hotel search must be represented behind provider-agnostic boundaries. Provider-specific facts, errors and limitations should be translated at integration boundaries rather than leaking into domain or UX concepts.

### AI-assisted but not AI-owned facts

The assistant can interpret, summarize, compare and explain, but provider/API data remains the source of truth for hotel facts. LLM output must not become the authoritative source for availability, price, hotel attributes or provider-originated travel facts.

### Explicit uncertainty handling

Architecture must preserve uncertainty as a first-class concern. Unknown data, missing fields, unavailable provider facts and assistant assumptions should remain distinguishable in future models and flows.

### MVP-first, expansion-ready

Architecture should serve the hotel-only MVP v1 first while leaving clear extension points for later flight search, combined itinerary, booking, account history and deeper authorization. Extension readiness must not pull future scope into MVP v1.

### No hidden scope expansion

Architecture documents must not smuggle future product features into current requirements. Any expansion beyond hotel-only MVP v1 requires a later product decision and, where the choice affects long-term architecture, likely an ADR.

### Architecture before implementation

Stage 5 documents architectural boundaries and decisions before implementation preparation. It must not define production classes, migrations, endpoint contracts, UI tasks or delivery backlog.

### Readable documentation for future coding agents

Architecture documents should be concise, explicit and easy for future coding agents to follow. They should make scope limits, source-of-truth rules and future-expansion boundaries hard to miss.

## Future Expansion Policy

Future features may be mentioned only as context for extensibility and risk management.

Future expansion notes must not define detailed MVP requirements, detailed UX requirements, provider contracts, database models or implementation tasks for future areas.

Activating flight search, booking, payment flows, account history, full authorization or combined itinerary planning requires a later product decision. If the activation changes architecture boundaries, public contracts, provider strategy, data ownership or security posture, it likely also requires an ADR.

## Stage 5 Deliverables Preview

Future Stage 5 work may create these documents as separate tasks. This list is a planning preview, not an instruction to create them now:

- `system-context-and-boundaries.md`
- `domain-model-and-boundaries.md`
- `application-orchestration.md`
- `integration-architecture.md`
- `data-and-storage-boundaries.md`
- `non-functional-requirements.md`
- `architecture-decisions-draft.md`
- `stage-5-summary-and-carryover.md`
- `stage-5-consistency-review.md`

## Open Questions

- Which architecture decisions in Stage 5 require ADRs rather than ordinary architecture notes?
- What minimum hotel provider capabilities are required by the existing travel API contract once it is provided?
- How should future domain/application boundaries represent user-provided constraints, provider facts, assistant assumptions and unknown data without prematurely defining implementation classes?
- What data ownership boundaries are needed for save/shortlist in MVP v1 without introducing account history or full authorization?
- Which MVP non-functional constraints are architectural requirements, and which should remain implementation-stage acceptance criteria?

## Non-goals

This document does not select production-level implementation details, introduce code, define API contracts, design a database schema, create UI implementation work or start development.

It establishes Stage 5 scope, guardrails and principles so that later architecture documents can proceed without changing roadmap order or expanding hotel-only MVP v1.
