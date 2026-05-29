# Stage 5 — Summary & Carryover

## Purpose

This document summarizes Stage 5 Technical Architecture / System Design for Travel Assistant and captures carryover for future stages.

Stage 5 established architecture boundaries for Hotel-Only MVP v1 without starting production implementation, API contracts, database schema, provider adapters, vendor/tool selection or implementation backlog.

## Stage 5 Completed Deliverables

Stage 5 completed:

- `architecture-scope-and-principles.md`;
- `system-context-and-boundaries.md`;
- `domain-model-and-boundaries.md`;
- `application-orchestration.md`;
- `integration-architecture.md`;
- `data-and-storage-boundaries.md`;
- `non-functional-requirements.md`;
- `architecture-decisions-draft.md`;
- `stage-5-consistency-review.md`.

This document closes the Stage 5 summary and carryover record.

## Architecture Baseline Established

Stage 5 established:

- hotel-only MVP boundary;
- system context and external/future boundaries;
- conceptual domain concepts and responsibility boundaries;
- conceptual application orchestration boundaries;
- provider, LLM and frontend/backend integration boundaries;
- data ownership, volatility and storage boundaries;
- architecture-level NFR and quality boundaries;
- draft architecture decision inventory and future ADR candidates.

## Confirmed Architecture Guardrails

- MVP v1 remains hotel-only.
- Provider facts are source-owned.
- User constraints are traceable to user input or clarification.
- Assistant assumptions are labeled and separated from facts.
- Unknown data remains unknown.
- LLM assists but does not own hotel facts.
- Current-session state is not account history.
- Current-session shortlist is not persistent saved trips, cross-device sync or full-auth account storage.
- Future expansion requires product decision and likely ADR when architecture boundaries change.
- No API/DB contracts should be created before the relevant roadmap step.
- Stage 5 architecture docs must not be read as production implementation plan.

## Deferred Decisions

The following decisions remain deferred:

- concrete hotel provider/API contract;
- concrete LLM provider/model;
- prompt/guardrail implementation;
- DB/storage technology;
- API/OpenAPI contracts;
- deployment topology;
- telemetry stack;
- auth/account model;
- booking/payment architecture;
- flight/combined itinerary architecture.

## Carryover to Next Stage

The next stage should preserve:

- facts/assumptions/unknowns separation;
- provider-agnostic hotel boundary;
- chat-first, not chat-only UX;
- Search Intent Summary as UX/domain bridge;
- Hotel Offer Card as central comparison surface;
- no hidden account history or full auth;
- no flight, combined itinerary, booking or payment in MVP v1;
- source/freshness uncertainty as a visible concept;
- current-session shortlist as session-level selection aid only.

The next stage should not treat this carryover as an implementation backlog. It is architectural context for future planning.

## Risks to Watch

- Current-session shortlist may accidentally turn into account history.
- Provider abstraction may prematurely become an API contract.
- LLM boundary may blur and start creating or rewriting provider facts.
- Future expansion may be misread as MVP scope.
- NFRs may become DevOps/security/testing backlog too early.
- Existing travel API constraints may pressure provider DTOs into domain concepts.
- Search Intent Summary editability may introduce persistence assumptions if not decided explicitly.

## Recommended Next Step

Stage 5 is complete because the consistency review found no Critical or Major blockers.

The next stage should be started only by separate explicit request.

Do not start Stage 6 in this task.

## Non-goals

This document does not define:

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
