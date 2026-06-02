# Stage 6.2 — OpenAPI Contract Review

## Review Summary

**Verdict:** Passed for continued Stage 6 contract work, with follow-up findings before client generation or implementation.

The Stage 6.1 OpenAPI draft is aligned with the hotel-only MVP boundary and can be used as a reviewable client-facing contract draft between the Next.js frontend and Spring Boot backend. No Critical blockers were found. The review found Major and Minor follow-ups that should be resolved before generated clients, backend/frontend implementation, or Stage 7 activation.

Findings by severity:

| Severity | Count |
|---|---:|
| Critical | 0 |
| Major | 2 |
| Minor | 3 |
| Note | 3 |

Movement after this review:

- Continuing with the next explicit Stage 6.x contract/preparation task is acceptable.
- Stage 7 implementation is not activated by this review.
- OpenAPI remediation, if needed, should be handled by a separate explicit Stage 6.x task.

## Scope Alignment

The draft remains aligned with MVP v1 hotel-only scope:

- all endpoints are scoped to assistant sessions, hotel searches, hotel offers, current-session shortlist, explanation/comparison, and health;
- no flight search endpoints are present;
- no combined itinerary or combined hotel + flight endpoints are present;
- no booking, payment, refund, ticketing, account management, account history, full auth, profile, or persistent saved trip flows are present;
- no provider-specific endpoints, schemas, SDK concepts, credentials, or DTO mapping tables are present;
- no database schema, storage model, migration, Redis/cache API, DevOps, security, telemetry, or testing backlog is introduced;
- no backend or frontend implementation is introduced.

Suspicious or watch items:

- `providerOfferRef` is acceptable because it is described as an opaque provider reference, not a provider DTO.
- Shortlist endpoints remain current-session scoped, but future contract work must avoid turning them into account-level saved trips.
- Search status and error modeling need follow-up before generated clients or implementation, but this does not expand MVP scope.

## Frontend Flow Coverage

| Frontend flow / state | Coverage | Draft location | Review note |
|---|---|---|---|
| Start assistant session | Covered | `POST /api/v1/assistant/sessions` | Supports session creation and optional initial message. |
| Send user request | Covered | `POST /api/v1/assistant/sessions/{sessionId}/messages` | Supports continuing the session with a user message. |
| Clarify destination/dates/guests/preferences | Covered | `AssistantMessageResponse`, `SearchIntentSummary`, `HotelSearchCriteria` | `nextAction`, missing fields, assumptions, and unknowns support clarification UX. |
| Launch hotel search | Covered | `POST /api/v1/hotel-searches` | Async-friendly `202` response supports provider search start. |
| Get hotel offers | Covered | `GET /api/v1/hotel-searches/{searchId}/offers` | Returns structured hotel offers and result state. |
| Empty / no offers state | Covered with consistency follow-up | `NO_OFFERS_FOUND`, `HotelOffersResponse.status` | Draft separates no offers from provider errors, but status/error representation should be unified. |
| Provider failure state | Covered with consistency follow-up | `PROVIDER_UNAVAILABLE`, `PROVIDER_FAILED`, `HotelOffersResponse.status` | Draft separates unavailable and failed states, but representation pattern needs future decision. |
| Shortlist read | Covered | `GET /api/v1/assistant/sessions/{sessionId}/shortlist` | Current-session scope is explicit. |
| Shortlist add/update | Covered | `PUT /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` | Supports save/shortlist action. |
| Shortlist remove | Covered | `DELETE /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` | Supports remove action. |
| Explanation/comparison | Covered | `POST /api/v1/assistant/sessions/{sessionId}/explanations` | Supports 1-5 hotel offers and modes `explain` / `compare`. |
| Validation feedback | Covered | `ValidationErrorResponse` | Field-level validation errors are represented. |
| Partial data state | Partially covered | `HotelOffer.unknowns`, optional fields, freshness markers | Needs explicit future representation for top-level partial/stale result states. |
| Stale result state | Partially covered | `HotelOffer.freshness`, `ShortlistItem.freshness` | Offer-level freshness exists, but result-level stale state is not explicit. |
| Offer details screen | Partially covered | inline `HotelOffer` | Inline offer can serve details, but no explicit details/read-unavailable contract exists. |

The draft covers the main Stage 3/4 UX flow: natural-language request, clarification, hotel search, structured results, no results/provider error distinction, shortlist, comparison, and validation feedback. The main gaps are representation depth, not missing MVP endpoints.

## API Consistency

Naming consistency:

- Path naming is mostly consistent: assistant session resources are under `/assistant/sessions`, and hotel search resources are under `/hotel-searches`.
- Operation IDs are readable and action-oriented.
- Schema names are consistent with Stage 5 concepts: `AssistantSession`, `HotelSearchRequest`, `HotelOffer`, `ShortlistItem`, `ErrorResponse`.

Status codes:

- `GET /health` uses `200`.
- Assistant session creation uses `201`.
- Hotel search creation uses `202`, which fits async-friendly search.
- Shortlist delete uses `204`.
- Validation uses `400`; session not found uses `404`; provider failures use `502`/`503`; internal errors use `500`.
- The main consistency issue is that no offers/provider terminal states are modeled both as response statuses and HTTP error responses.

Error response consistency:

- `ErrorResponse` and `ValidationErrorResponse` provide a consistent baseline.
- Validation, provider unavailable/failed, no offers found, session not found, and internal error categories are explicit.
- Search, offer, and shortlist item not-found categories are not yet distinct.

ID naming:

- `sessionId`, `searchId`, `offerId`, `itemId`, and `providerOfferRef` are understandable and opaque.
- `providerOfferRef` should remain an opaque source reference and must not become a provider DTO shape.

OpenAPI 3.1 optional / nullable style:

- Optional fields are generally omitted from `required` instead of using legacy nullable style.
- The draft uses `type: ["string", "number", "boolean", "null"]` for `rejectedValue`, which is valid OpenAPI 3.1 / JSON Schema style.

Request/response symmetry:

- `HotelSearchRequest.criteria` maps to `HotelSearchResponse.criteria`.
- `AssistantMessageResponse.hotelSearchRequest` gives the frontend a bridge from clarification to search.
- Shortlist read/add/remove are coherent, but add/update returns only one item while read returns the collection.

Async-friendly search flow:

- `POST /hotel-searches` with `202` and `GET /hotel-searches/{searchId}/offers` gives a viable async-friendly shape.
- A separate status endpoint is not required at this draft stage, but the result/error state pattern needs follow-up.

Session boundary:

- Assistant messages, shortlist, and explanations are scoped under `sessionId`.
- Search creation includes `sessionId`.
- The draft does not imply account history, cross-device persistence, booking, or payment.

## Architecture Alignment

The draft aligns with the Stage 5 architecture baseline:

- Backend remains the orchestration boundary: assistant session, hotel search, provider search result handling, explanation, comparison, and shortlist are coordinated through client-facing backend endpoints.
- `HotelOfferProvider` remains an abstraction: hotel search endpoints do not expose provider-specific contracts, endpoints, or DTOs.
- LLM/provider separation is preserved: assistant responses and explanations are separate from provider-owned hotel facts.
- Current-session shortlist is preserved: shortlist endpoints are session-scoped and do not introduce account history, full auth, persistent saved trips, booking, or payment.
- Provider facts, assistant assumptions, unknown data, and user-provided constraints remain visible through `SearchIntentSummary`, `HotelOffer`, `AssistantExplanationResponse`, and error/validation responses.
- DB/storage implementation does not leak: there are identifiers and session concepts, but no tables, migrations, persistence technology, retention policy, or Redis/cache design.
- Provider/search errors are separated from validation errors and no-offers states at the category level.

The draft should remain a client-facing contract. It must not become a provider API contract, provider DTO map, DB schema, or implementation backlog.

## Findings

### MA-S6.2-001

- **Severity:** Major
- **Location:** `docs/architecture/stage-6/openapi-draft.yaml`, `HotelSearchCriteria.rooms`
- **Issue:** `rooms` is optional with default `1`, while Stage 3 requires rooms count or a visible room assumption.
- **Why it matters:** A silent default can hide an assistant assumption from the user and weaken the Stage 3/5 requirement that assumptions affecting search remain visible and correctable.
- **Recommendation:** In a future OpenAPI remediation task, make room count explicit or represent the default as a visible assistant assumption before hotel search.
- **Required before next stage:** Yes, before client generation or implementation.

### MA-S6.2-002

- **Severity:** Major
- **Location:** `GET /hotel-searches/{searchId}/offers`, `HotelOffersResponse.status`, shared error responses
- **Issue:** Search terminal states are represented both as `HotelOffersResponse.status` values and HTTP error responses for no offers/provider failure.
- **Why it matters:** Frontend clients need one predictable pattern for rendering no results, provider unavailable, provider failed, and completed states. Mixed success/error representation can create duplicated UI branches and inconsistent recovery behavior.
- **Recommendation:** In a future contract task, choose a consistent pattern for search terminal states: either return a search result envelope for terminal search states or reserve HTTP errors for transport/application failures.
- **Required before next stage:** Yes, before client generation or implementation.

### MI-S6.2-001

- **Severity:** Minor
- **Location:** `HotelOffersResponse`, `HotelOffer.freshness`, `HotelOffer.unknowns`, `ShortlistItem.freshness`
- **Issue:** Partial and stale result states are represented indirectly through offer-level `unknowns` and `freshness`, not as top-level search/result states.
- **Why it matters:** Stage 3/4 treat no results, partial data, provider error, and stale data as distinct UX states. A top-level state would help the frontend render clear result-level banners and recovery actions.
- **Recommendation:** Add explicit future representation for `partial` and `stale` result states or a result-level warnings/caveats collection.
- **Required before next stage:** No.

### MI-S6.2-002

- **Severity:** Minor
- **Location:** `HotelOffer`, missing dedicated offer details endpoint
- **Issue:** The offer details screen can be served from inline `HotelOffer`, but no explicit details/read-unavailable contract exists.
- **Why it matters:** Stage 4 includes an offer details screen with provider facts, assumptions, unknowns, freshness, and stale/unavailable handling. Inline offer data may be enough for MVP, but this should be an explicit contract decision.
- **Recommendation:** Decide in a future Stage 6.x task whether MVP uses inline `HotelOffer` for details or needs `GET /hotel-offers/{offerId}` / session-scoped equivalent.
- **Required before next stage:** No.

### MI-S6.2-003

- **Severity:** Minor
- **Location:** `ErrorResponse`, shared `SESSION_NOT_FOUND`, path operations for searches/offers/shortlist
- **Issue:** The draft does not distinguish session not found, search not found, offer not found, and shortlist item not found.
- **Why it matters:** Frontend recovery differs by missing resource: a missing session may start a new session, missing search may return to chat, missing offer may show stale/unavailable details, and missing shortlist item may simply refresh saved state.
- **Recommendation:** Refine error taxonomy before frontend client work.
- **Required before next stage:** No.

### NT-S6.2-001

- **Severity:** Note
- **Location:** Scope-wide
- **Issue:** Hotel-only MVP scope is preserved.
- **Why it matters:** The draft avoids flight, combined itinerary, booking, payment, account management, and future expansion leakage.
- **Recommendation:** Keep this boundary visible in future contract remediation tasks.
- **Required before next stage:** No.

### NT-S6.2-002

- **Severity:** Note
- **Location:** `HotelOffer.providerOfferRef`, provider-related descriptions
- **Issue:** Provider DTOs do not leak into the client-facing contract.
- **Why it matters:** Stage 5 requires provider-agnostic boundaries and source-owned provider facts without coupling product/domain concepts to provider payloads.
- **Recommendation:** Keep `providerOfferRef` opaque and avoid provider-specific fields until a separate provider/API mapping task.
- **Required before next stage:** No.

### NT-S6.2-003

- **Severity:** Note
- **Location:** `ShortlistItem`, shortlist endpoints, error schemas
- **Issue:** Current-session shortlist and validation/provider/search error separation are represented at draft level.
- **Why it matters:** This supports Stage 3/4 UX states and Stage 5 data boundaries without creating account history or implementation leakage.
- **Recommendation:** Preserve current-session wording in future contract updates.
- **Required before next stage:** No.

## Follow-up Recommendations

- Resolve room assumption representation before client generation or implementation.
- Choose one consistent search terminal-state pattern for no offers, provider unavailable, provider failed, and completed states.
- Add explicit result-level representation for partial and stale states, or document why offer-level markers are sufficient.
- Decide whether offer details are served from inline `HotelOffer` or require a separate details endpoint.
- Refine resource-specific not-found errors before frontend client work.
- Keep provider-specific mapping separate until the existing hotel provider/API contract is provided and explicitly activated.
- Do not start Stage 7 until the required Stage 6 contract remediation and preparation tasks are explicitly completed.
