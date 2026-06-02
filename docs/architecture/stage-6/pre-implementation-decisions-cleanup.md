# Stage 6.8 — Pre-implementation Decisions Cleanup

## 1. Purpose

Этот документ фиксирует Stage 6.8 pre-implementation decisions cleanup перед backend skeleton / generated clients.

Цель — закрыть generated-client-facing и pre-implementation carryover из Stage 6.7 без расширения MVP scope и без начала Stage 7.

Stage 6.8 не создает backend/frontend implementation, DB schema, migrations, provider integration, provider-specific DTOs, ADR, booking/payment/account flows, flights или combined itinerary.

## 2. Decisions Made

| Decision | Result | Rationale |
|---|---|---|
| Nested 404 response modeling | Use shared session-scoped 404 response components with `ErrorResponse.code` examples. | OpenAPI has one response object per status code per operation. Generated clients can branch on stable error codes while keeping one response schema. |
| Offer details | Inline `HotelOffer` is sufficient for MVP. Dedicated offer details endpoint is deferred. | Stage 3/4 details needs can be served from the structured offer payload without adding a new resource flow. |
| Long-running search status | `HotelOffersResponse` result envelope is sufficient for MVP async-friendly status and terminal states. Dedicated status endpoint is deferred. | Existing `POST /hotel-searches` + `GET /hotel-searches/{searchId}/offers` covers accepted/searching/completed/failed states. |
| Session page refresh / persistence | Current-session behavior only. Page refresh may continue only if the client keeps an opaque `sessionId` and backend still has an active session. | This preserves current-session UX without DB/storage design, account history, auth, cross-device sync or long-term persistence guarantees. |

## 3. Changes Made to OpenAPI

`openapi-draft.yaml` was updated to version `0.1.2-draft`.

OpenAPI changes:

- added `SessionOrHotelOfferNotFound` response component;
- added `SessionOrShortlistItemNotFound` response component;
- changed session-scoped shortlist upsert, shortlist delete and explanation 404 responses to use shared session-or-resource response components;
- added `AssistantSession` description clarifying current-session-only behavior and lack of account/cross-device/long-term/page-refresh guarantees.

No new endpoints, schemas for provider DTOs, booking/payment/account flows, status endpoint or offer details endpoint were added.

## 4. Changes Made to Notes

`openapi-contract-notes.md` was updated to:

- record Stage 6.8 pre-implementation decisions;
- document that generated clients should branch on `ErrorResponse.code` for shared session-scoped 404 responses;
- replace the page-refresh open question with the current-session-only MVP behavior;
- remove resolved questions about dedicated status endpoint and dedicated offer details endpoint from the open questions list.

## 5. Deferred Decisions

Deferred after Stage 6.8:

- real provider/API mapping after the provider contract is available;
- exact DB/storage/session retention model if future persistence beyond current-session behavior is required;
- broad/open destination validation details;
- explanation/comparison streaming, if future UX or implementation requires it;
- stricter generated-client conventions, if future tooling requires a narrower shape than the Stage 6.8 shared 404 response components.

## 6. Why MVP Scope Remains Unchanged

Stage 6.8 keeps MVP v1 hotel-only:

- no flight endpoints;
- no combined itinerary endpoints;
- no booking or payment flow;
- no account management, account history, full auth or persistent saved trips;
- no backend/frontend implementation;
- no DB schema, migration, storage model or Redis/cache contract;
- no provider-specific DTO, provider contract, provider SDK or provider integration;
- no Stage 7 activation.

The cleanup only clarifies existing client-facing contract behavior and generated-client branching expectations.

## 7. Readiness After Cleanup

After Stage 6.8:

- nested session-scoped 404 modeling is explicit enough for generated clients;
- offer details MVP behavior is explicit: use inline `HotelOffer`;
- long-running status MVP behavior is explicit: use `HotelOffersResponse` result envelope;
- current-session behavior is explicit and does not imply persistence guarantees;
- Stage 6 package is ready for either an explicit backend skeleton preparation task or a generated-client task, subject to that task's own activation and scope.

## 8. Remaining Carryover

Remaining carryover is not blocking for backend skeleton or generated clients:

- provider/API mapping after real provider contract is available;
- future persistence/storage decisions if current-session behavior becomes insufficient;
- possible future contract review if generated tooling imposes stricter conventions;
- stale status references in historical/baseline/future-reference docs, if separate documentation cleanup is desired.
