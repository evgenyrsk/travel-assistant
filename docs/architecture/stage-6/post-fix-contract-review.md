# Stage 6.4 — Post-fix Contract Review

## Review Summary

**Verdict:** Passed for continued Stage 6 contract work after Stage 6.3 fixes.

Stage 6.3 resolved the two Major Stage 6.2 findings and improved result-state, typed assumptions and resource-specific error modeling without expanding MVP scope. The OpenAPI draft is ready for the next explicit Stage 6 task.

Movement after this review:

- Continuing with another explicit Stage 6.x contract/preparation task is acceptable.
- No Critical or Major blockers remain before the next Stage 6 task.
- Backend skeleton remains a future Stage 7 / implementation activity and is not activated by this review.
- Client generation can be prepared after one Minor 404-response clarification is either fixed or accepted as an implementation-time convention.

New findings by severity:

| Severity | Count |
|---|---:|
| Critical | 0 |
| Major | 0 |
| Minor | 1 |
| Note | 0 |

## Findings Closure Check

| Finding | Status | Evidence | Remaining action |
|---|---|---|---|
| `MA-S6.2-001` | Closed | `HotelSearchCriteria.rooms` no longer has silent `default: 1`; `rooms` states that omitted room count requires visible `DerivedAssumption`. `DerivedAssumption.category` includes `room_count` and `guest_count`. | None before the next Stage 6 task. Future implementation must enforce this business validation. |
| `MA-S6.2-002` | Closed | `HotelSearchResponse.status` and `HotelOffersResponse.status` share `accepted`, `searching`, `completed_with_offers`, `completed_no_offers`, `failed`. `GET /hotel-searches/{searchId}/offers` returns `200` result envelopes for search states and no longer uses no-offers/provider HTTP errors. | None before the next Stage 6 task. |
| `MI-S6.2-001` | Closed | `SearchResultMetadata` adds `resultCompleteness`, `freshness`, `providerState`, `refreshedAt` and `warnings`; `HotelOffersResponse.metadata` exposes result-level partial/stale/degraded state. | None before the next Stage 6 task. |
| `MI-S6.2-002` | Deferred | Stage 6.3 summary explicitly leaves a dedicated offer details endpoint as a future decision; inline `HotelOffer` remains sufficient for the current draft. | Decide in a future Stage 6.x task whether inline `HotelOffer` is enough for details or whether a session-scoped details endpoint is needed. |
| `MI-S6.2-003` | Partially closed | `ErrorResponse.code` now distinguishes `SESSION_NOT_FOUND`, `HOTEL_SEARCH_NOT_FOUND`, `HOTEL_OFFER_NOT_FOUND` and `SHORTLIST_ITEM_NOT_FOUND`; response components exist for those categories. Some nested session-scoped operations still expose only one 404 response component. | See `MI-S6.4-001`; clarify nested 404 response modeling before generated clients if strict client branching is required. |
| `NT-S6.2-001` | Closed | Stage 6.3 notes and draft exclusions preserve hotel-only scope and explicitly exclude flights, combined itinerary, booking, payment and account flows. | Keep boundary visible in future tasks. |
| `NT-S6.2-002` | Closed | `providerOfferRef` remains opaque; `ProviderFact.source` is generic and the draft avoids provider DTOs or mapping tables. | Keep provider mapping as a separate explicit task. |
| `NT-S6.2-003` | Closed | Shortlist endpoints remain session-scoped; validation errors, result-envelope failures and not-found errors are distinct. | Preserve current-session wording in future updates. |

## Search State Model Review

The Stage 6.3 search state model is coherent and client-readable:

- `accepted` represents a search request accepted for processing.
- `searching` represents work still in progress.
- `completed_with_offers` represents successful completion with one or more offers.
- `completed_no_offers` represents successful completion with no matching offers.
- `failed` represents search/provider/application-boundary failure.

HTTP status code alignment:

- `POST /hotel-searches` returns `202` with `HotelSearchResponse`, fitting async-friendly search start.
- `GET /hotel-searches/{searchId}/offers` returns `200` with `HotelOffersResponse` for in-progress and terminal search states.
- Validation remains `400`.
- Missing search remains `404`.
- Internal backend failure remains `500`.

No offers, validation errors and provider/search failure are no longer mixed:

- no offers is `status: completed_no_offers`;
- provider/search failure is `status: failed` plus `HotelSearchFailure`;
- invalid request shape or invalid criteria remains `ValidationErrorResponse`;
- missing resources remain `ErrorResponse`.

The result envelope is understandable for frontend state rendering. It provides one place for `offers`, `metadata`, `failure`, provider facts, preferences, assumptions and unknowns.

## Facts / Assumptions / Unknowns Review

The Stage 6.3 typed schemas preserve Stage 5 boundaries:

- `ProviderFact` represents source-owned facts with generic `source` and `freshness`; it does not expose provider payload shape.
- `UserPreference` separates user-stated preferences from assistant inference.
- `AssistantAssumption` captures assistant interpretation and confirmation needs without turning it into a provider fact.
- `DerivedAssumption` captures derived values such as `room_count` and `guest_count`, including reason and confirmation need.
- `UnknownData` keeps missing or unavailable decision-critical information visible.
- `SearchResultMetadata` represents result-level completeness, freshness and provider state without introducing cache, Redis or provider-specific fields.
- `HotelSearchFailure` captures generic failure categories without becoming provider-specific taxonomy.

These schemas do not add booking, room selection, payment, account management or provider-specific DTOs. Room/guest assumptions are visible search assumptions only; they are not a booking or room-selection flow.

## Error Model Review

The error model is improved after Stage 6.3:

- session not found is represented by `SESSION_NOT_FOUND` and `SessionNotFound`;
- hotel search not found is represented by `HOTEL_SEARCH_NOT_FOUND` and `HotelSearchNotFound`;
- hotel offer not found is represented by `HOTEL_OFFER_NOT_FOUND` and `HotelOfferNotFound`;
- shortlist item not found is represented by `SHORTLIST_ITEM_NOT_FOUND` and `ShortlistItemNotFound`;
- validation errors use `ValidationErrorResponse` with `VALIDATION_ERROR` and field-level errors;
- provider/search failures are represented inside `HotelOffersResponse.failure` for `status: failed`;
- `ErrorResponse` is consistent for non-validation missing-resource/internal errors.

The remaining nuance is path-level 404 modeling for nested session-scoped endpoints. For example, shortlist upsert and explanations are under `{sessionId}` but currently document a single 404 response focused on the referenced offer or shortlist item. This is not a scope blocker, but generated clients may benefit from a shared or more explicit missing-resource response pattern.

## MVP Scope Review

Stage 6.3 fixes remain inside MVP v1 hotel-only scope:

- hotel-only API boundary is preserved;
- no flight endpoints were added;
- no combined itinerary endpoints were added;
- no booking flow was added;
- no payment flow was added;
- no account management or account history was added;
- no DB/storage implementation, schema or migration was added;
- no backend/frontend code was added;
- no provider-specific DTO, contract or mapping table was added.

The draft continues to describe a client-facing frontend/backend contract only.

## New Findings

### MI-S6.4-001

- **Severity:** Minor
- **Location:** `docs/architecture/stage-6/openapi-draft.yaml`, nested session-scoped 404 responses for shortlist and explanation operations
- **Issue:** Resource-specific not-found errors exist, but some `{sessionId}`-scoped endpoints document only one 404 response component even though a missing session and a missing nested resource are both possible.
- **Why it matters:** Generated clients and frontend recovery branches may need to distinguish missing session, missing hotel offer and missing shortlist item without relying on undocumented conventions.
- **Recommendation:** In a future Stage 6.x contract cleanup, either add a shared missing-resource 404 response with `ErrorResponse.code` examples or document operation-specific 404 code possibilities for nested resources.
- **Required before next stage:** No.

## Recommendation

Recommended next step: run an explicit Stage 6.x provider boundary / mapping notes task before provider integration or generated clients. That task should document how generic `ProviderFact`, freshness, provider limitations and failure categories map from the future hotel provider/API contract without adding provider-specific DTOs to the client-facing API.

Additional recommendations:

- Clarify nested 404 response modeling before strict generated frontend clients.
- Decide whether inline `HotelOffer` remains sufficient for the offer details screen.
- Decide whether a separate long-running search status endpoint is needed or the current result envelope is enough.
- Keep backend skeleton and frontend implementation outside scope until Stage 7 is explicitly activated.
