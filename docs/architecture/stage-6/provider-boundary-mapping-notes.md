# Stage 6.5 — Provider Boundary / Mapping Notes

## Назначение

Этот документ фиксирует documentation-only notes по границе Backend/Application ↔ `HotelOfferProvider` для hotel-only MVP v1.

Stage 6.5 описывает, как будущие provider/source данные должны conceptually отображаться в уже существующие client-facing OpenAPI concepts из `openapi-draft.yaml`: `ProviderFact`, `SearchResultMetadata`, `UnknownData` и `HotelSearchFailure`.

Этот документ не является provider API contract, provider DTO, backend interface, adapter design, SDK design, mapping table, DB schema, storage model, generated client input или implementation plan.

## Scope

Stage 6.5 остается внутри Stage 6 API Contracts / OpenAPI / Integration Boundary:

- фиксирует conceptual mapping rules для будущей provider work;
- сохраняет provider-agnostic hotel boundary;
- сохраняет разделение provider facts, user constraints, assistant assumptions и unknown data;
- не меняет `openapi-draft.yaml`;
- не активирует Stage 7 или backend/frontend implementation.

MVP v1 остается hotel-only. Flights, combined itinerary, booking, payment, account history, full auth и production provider integration остаются вне scope этой задачи.

## Backend / HotelOfferProvider Boundary

`HotelOfferProvider` в этом документе — conceptual boundary, а не имя интерфейса или обязательная форма будущего кода.

Будущий provider boundary отвечает за получение source-owned hotel data: availability, prices, hotel attributes, location, amenities, policies, ratings, source markers, freshness markers and provider limitations, если они доступны в предоставленном provider/API contract.

Application/backend boundary отвечает за перевод provider/source результатов в client-facing concepts без утечки provider-specific payload shape:

- provider/source facts остаются provider facts;
- assistant assumptions не становятся provider facts;
- unavailable, incomplete или stale data остается visible как metadata, warnings или unknown data;
- provider/search failures не смешиваются с no-offers state;
- provider-specific errors не становятся client-facing provider taxonomy без отдельного решения.

## Conceptual Mapping Rules

| Provider/source concept | Client-facing OpenAPI concept | Rule |
|---|---|---|
| Hotel fact from provider/source | `ProviderFact` or existing `HotelOffer` field | Use `ProviderFact` for source-owned facts that need traceability or do not have a stable first-class field. Do not copy provider DTO shape into the response. |
| Provider source marker | `ProviderFact.source` or `HotelOffer.source` | Use a generic source label when available. Do not expose provider endpoint names, raw IDs, credentials, request metadata or vendor-specific payloads. |
| Offer-level freshness | `ProviderFact.freshness` and/or `HotelOffer.freshness` | Represent as `fresh`, `stale` or `unknown`. Assistant confidence must not override freshness. |
| Result-level freshness | `SearchResultMetadata.freshness`, `refreshedAt` | Use result-level metadata when freshness applies to the full search result envelope. `refreshedAt` is a source/result timestamp, not a cache or storage contract. |
| Incomplete provider data | `UnknownData`, `SearchResultMetadata.resultCompleteness`, `warnings` | Keep decision-critical missing data visible. Do not fill gaps with assistant assumptions. |
| Stale provider data | `ProviderFact.freshness`, `HotelOffer.freshness`, `SearchResultMetadata.freshness`, `warnings` | Mark stale data explicitly and let explanations reflect freshness limits. |
| Degraded provider state | `SearchResultMetadata.providerState`, `warnings` | Represent degraded source behavior without turning it into a provider-specific operational taxonomy. |
| No matching offers | `HotelOffersResponse.status: completed_no_offers` | Treat no offers as a completed search result, not as provider failure or validation error. |
| Provider/search failure | `HotelOffersResponse.status: failed`, `HotelSearchFailure` | Use generic failure categories: `provider_unavailable`, `provider_failed`, `search_failed` or `unknown`. |

## Failure and Partial Data Handling

Provider unavailable means the source could not provide usable hotel results. It should map to `HotelSearchFailure.category: provider_unavailable` when the search cannot complete.

Provider failed means the source or provider boundary failed during search processing. It should map to `provider_failed` unless a broader application/search failure is more accurate.

Search failed means the application-level hotel search could not complete but the exact cause should not be exposed as provider-specific detail.

Partial data should prefer a successful result envelope with visible limits:

- `status: completed_with_offers` if usable offers exist;
- `SearchResultMetadata.resultCompleteness: partial`;
- `SearchResultMetadata.providerState: degraded` when source behavior is degraded;
- `UnknownData` for decision-critical missing fields;
- `warnings` for user-facing caveats that are not individual facts.

Unknown freshness should stay unknown. The assistant can explain that a hotel appears to match the user constraints, but it must not imply current price, availability, policy or rating freshness when provider/source data does not support it.

## Guardrails for Future Provider Work

Future provider/API contract analysis must keep raw provider payloads behind the backend/provider boundary.

Future implementation may introduce concrete interfaces, adapters, mapping code, retries or error handling only through a separate explicit roadmap task. Stage 6.5 does not choose method names, package structure, storage approach, provider SDK, retry policy, telemetry schema or generated client strategy.

If the future provider/API contract exposes fields that do not map cleanly to current OpenAPI concepts, the next task should decide whether to:

- keep the field internal to provider mapping;
- expose it through generic `ProviderFact`;
- add a client-facing contract change through a separate Stage 6.x task;
- defer it until Stage 7 or later implementation work.

## Explicitly Deferred

The following items remain future decisions and are not executed in Stage 6.5:

- nested 404 response cleanup for session-scoped operations;
- dedicated hotel offer details endpoint;
- dedicated long-running search status endpoint;
- current-session page-refresh or persistence behavior;
- provider-specific DTOs, contracts, mappings, SDKs and integration code;
- backend/frontend implementation;
- DB schema, storage model, migrations, Redis/cache contract or auth model;
- Stage 7 activation.

## Assumptions

- Existing hotel provider/API contract has not been provided for this task.
- `providerOfferRef` remains opaque and must not expose provider payload structure.
- `ProviderFact.source` is a generic source label, not a provider DTO.
- `SearchResultMetadata` describes client-facing result state, not cache, Redis, storage or observability implementation.
- `HotelSearchFailure` remains generic until a separate task explicitly defines provider error mapping or taxonomy.
