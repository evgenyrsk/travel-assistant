# Stage 6.3 — OpenAPI Fixes Summary

## 1. Назначение

Этот документ фиксирует точечные OpenAPI contract fixes после Stage 6.2 review.

Stage 6.3 исправляет contract-level findings в `openapi-draft.yaml` и уточняет `openapi-contract-notes.md`. Эта задача не создает backend/frontend implementation, DB schema, migrations, provider-specific DTO, provider mapping, generated clients или Stage 7 work.

## 2. Закрытые findings Stage 6.2

| Finding | Status | Как закрыто |
|---|---|---|
| `MA-S6.2-001` | Closed | Silent `rooms.default: 1` удален. Room/guest assumptions представлены через typed `DerivedAssumption`, включая category `room_count` и `guest_count`. |
| `MA-S6.2-002` | Closed | Search terminal states сведены к единому result envelope pattern с lifecycle status `accepted`, `searching`, `completed_with_offers`, `completed_no_offers`, `failed`. |
| `MI-S6.2-001` | Addressed | Добавлен `SearchResultMetadata` для result-level partial/stale/freshness/provider degraded states. |
| `MI-S6.2-003` | Addressed | Добавлены resource-specific not-found errors: session, hotel search, hotel offer, shortlist item. |

`MI-S6.2-002` остается future decision: MVP может использовать inline `HotelOffer` для details screen, а отдельный details endpoint должен быть решен отдельной Stage 6.x задачей, если потребуется.

## 3. Изменения в OpenAPI

В `openapi-draft.yaml` внесены изменения:

- обновлена draft version до `0.1.1-draft`;
- removed silent `rooms.default: 1`;
- added typed schemas:
  - `ProviderFact`;
  - `UserPreference`;
  - `AssistantAssumption`;
  - `DerivedAssumption`;
  - `UnknownData`;
  - `SearchResultMetadata`;
  - `HotelSearchFailure`;
- updated `SearchIntentSummary`, `HotelSearchCriteria`, `HotelOffersResponse`, `HotelOffer` and `AssistantExplanationResponse` to use typed facts/assumptions/preferences/unknowns;
- unified `HotelSearchResponse.status` and `HotelOffersResponse.status`;
- changed hotel search results retrieval to return terminal search states through `HotelOffersResponse`;
- added result-level metadata for completeness, freshness, provider state, `refreshedAt` and warnings;
- added `HOTEL_SEARCH_NOT_FOUND`, `HOTEL_OFFER_NOT_FOUND`, `SHORTLIST_ITEM_NOT_FOUND` to `ErrorResponse.code`;
- added response components for `HotelSearchNotFound`, `HotelOfferNotFound` and `ShortlistItemNotFound`.

## 4. Изменения в notes

В `openapi-contract-notes.md` уточнены:

- роль Stage 6.3 contract fixes;
- единый search state pattern;
- typed separation of provider facts, user preferences, assistant assumptions, derived assumptions and unknown data;
- room/guest assumptions через `DerivedAssumption`;
- no offers vs failed search distinction;
- refined not-found errors;
- future questions around details endpoint, long-running status and provider mapping.

## 5. Принятые решения

- Search result retrieval uses one result envelope pattern. No offers and failed provider/search states are represented by `HotelOffersResponse.status`, not by separate no-offers/provider HTTP errors.
- `rooms` can remain optional only when visible `DerivedAssumption(category: room_count)` explains the assumption before search.
- Partial/stale/degraded state is represented at result level through `SearchResultMetadata`, while offer-level freshness remains available for cards/details/shortlist.
- Provider facts remain generic source-owned data. Stage 6.3 does not add provider-specific fields, provider DTOs or mapping tables.
- Dedicated offer details endpoint is not added in Stage 6.3.

## 6. Future provider mapping / implementation questions

- Which provider facts and source/freshness markers will be available after the existing hotel provider/API contract is provided?
- Whether inline `HotelOffer` is enough for MVP offer details or a separate details endpoint is needed.
- Whether long-running provider/LLM operations need a separate status endpoint.
- How generated frontend clients should model search result envelopes and typed assumptions.
- How current-session state behaves across page refresh before any DB/storage model.

These are not executed in Stage 6.3.

## 7. MVP scope control

Stage 6.3 keeps MVP v1 hotel-only:

- no flight endpoints;
- no combined itinerary endpoints;
- no booking or payment flow;
- no account management or account history;
- no provider-specific DTO or integration code;
- no DB schema, migrations, Redis/cache API or storage model;
- no backend/frontend implementation;
- no Stage 7 activation.
