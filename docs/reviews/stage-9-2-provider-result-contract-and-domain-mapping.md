# Stage 9.2 — Provider Result Contract and Domain Mapping

## 1. Scope

Stage 9.2 — docs/review/design-only stage.

Stage 9.2:

1. Inspect существующие `HotelSearchCriteria`, `HotelOffer`, API response models, fake provider output и search use case mapping.
2. Определяет provider-neutral result contract expectations для future real hotel provider adapters.
3. Определяет gaps между likely real provider data и current domain/API fields.
4. Определяет normalization rules для provider results до того, как они станут `HotelOffer`.
5. Определяет domain mapping rules, сохраняющие domain/provider boundary чистым.
6. Определяет source/freshness/providerFacts strategy.
7. Определяет validation expectations для future adapter skeleton.
8. Производит prompt для Stage 9.3.

Stage 9.2 не меняет production code, tests, runtime, routes, API,
OpenAPI, frontend, generated clients, product baseline или
architecture baseline.

## 2. Sources inspected

### 2.1 Backend source

| File | Layer | Purpose |
|---|---|---|
| `domain/hotel/HotelSearchCriteria.kt` | Domain | Search input parameters |
| `domain/hotel/HotelOffer.kt` | Domain | Domain offer model (12 fields) |
| `domain/hotel/HotelSearch.kt` | Domain | Search aggregate (id, session, criteria, status, offers) |
| `domain/hotel/RankedHotelOffer.kt` | Domain | Ranked offer wrapper (offer + matchSummary) |
| `domain/hotel/HotelOfferRanker.kt` | Domain | Deterministic ranking logic |
| `domain/provider/HotelOfferProviderBoundary.kt` | Domain | Provider-agnostic fun interface |
| `infrastructure/provider/FakeHotelOfferProvider.kt` | Infrastructure | Deterministic local adapter |
| `application/hotel/CreateHotelSearchUseCase.kt` | Application | Search orchestration |
| `application/hotel/CreateHotelSearchCommand.kt` | Application | Search creation command |
| `application/hotel/HotelSearchBoundary.kt` | Application | Application-level search interface |
| `api/HotelSearchRequest.kt` | API | Request model with validation |
| `api/HotelSearchCriteriaResponse.kt` | API | Criteria response mapping |
| `api/HotelSearchResponse.kt` | API | Search response with metadata |
| `api/HotelOffersResponse.kt` | API | Offers list response with aggregated providerFacts |
| `api/HotelOfferResponse.kt` | API | Offer response with Location, Price, Rating, Amenity, ProviderFact |

### 2.2 Documentation

| Document | Role |
|---|---|
| `docs/roadmap/roadmap.md` | Primary roadmap |
| `docs/architecture/architecture-baseline.md` | Architecture baseline |
| `docs/product/product-baseline.md` | Product baseline |
| `docs/reviews/stage-9-0-documentation-audit-and-stage-9-planning-readiness-review.md` | Stage 9.0 planning |
| `docs/reviews/stage-9-1-hotel-provider-boundary-review-and-adapter-design.md` | Stage 9.1 boundary review |

## 3. Current search criteria contract

### 3.1 Domain model

```kotlin
data class HotelSearchCriteria(
    val destination: String,
    val checkInDate: LocalDate,
    val checkOutDate: LocalDate,
    val guests: Guests,
    val rooms: Int?,
)
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `destination` | `String` | Yes | Free-text destination string |
| `checkInDate` | `LocalDate` | Yes | ISO-8601 date |
| `checkOutDate` | `LocalDate` | Yes | Must be after checkInDate |
| `guests.adults` | `Int` | Yes (≥1) | Adult count |
| `guests.children` | `Int` | Yes (≥0) | Children count |
| `rooms` | `Int?` | Nullable | Nullable; rooms count when known |

### 3.2 API request model

`HotelSearchRequest.Criteria` extends domain criteria with additional optional fields:

| Field | Type | Used in search? |
|---|---|---|
| `budget` | `JsonElement?` | No — accepted but not passed to domain |
| `preferences` | `List<JsonElement>` | No — accepted but not passed to domain |
| `requiredAmenities` | `List<String>` | No — accepted but not passed to domain |
| `assistantAssumptions` | `List<JsonElement>` | No — accepted but not passed to domain |
| `derivedAssumptions` | `List<JsonElement>` | Partial — `room_count` assumption can substitute rooms |
| `unknowns` | `List<JsonElement>` | No — accepted but not passed to domain |

**Key observation**: API request model is wider than domain model. Fields like
`budget`, `preferences`, `requiredAmenities` are accepted at API level but
silently dropped during validation. They do not reach `HotelSearchCriteria`.

### 3.3 Criteria gap analysis for real providers

| Likely real provider field | Current domain model | Status |
|---|---|---|
| Destination (city/region) | `destination: String` | Present |
| Check-in date | `checkInDate: LocalDate` | Present |
| Check-out date | `checkOutDate: LocalDate` | Present |
| Adults count | `guests.adults: Int` | Present |
| Children count | `guests.children: Int` | Present |
| Rooms count | `rooms: Int?` | Present (nullable) |
| Star rating filter | — | **Missing** |
| Property type filter | — | **Missing** |
| Amenity filter | — | **Missing** (accepted at API, not in domain) |
| Price range filter | — | **Missing** (accepted at API, not in domain) |
| Meal plan filter | — | **Missing** |
| Coordinates/geographic filter | — | **Missing** |
| Nationality (for pricing) | — | **Missing** |
| Children ages | — | **Missing** (only count, not individual ages) |
| Language/currency preference | — | **Missing** |

**Assessment**: current criteria covers the core mandatory fields that virtually
all hotel providers require. Filter fields (star rating, amenities, price range,
property type) are deferred — they can be added to domain model later when
a specific real provider contract demands them, or handled as adapter-level
post-filtering. **No domain model changes needed for Stage 9.3.**

## 4. Current hotel offer contract

### 4.1 Domain model

```kotlin
data class HotelOffer(
    val id: String,
    val providerReference: String,
    val hotelName: String,
    val city: String,
    val country: String,
    val totalPrice: Double,
    val currency: String,
    val rating: Double,
    val reviewCount: Int,
    val amenities: List<String>,
    val availability: Availability,
    val source: String,
    val freshness: Freshness,
)
```

| Field | Type | Required | Current fake value |
|---|---|---|---|
| `id` | `String` | Yes | `"fake-offer-$slug-001"` |
| `providerReference` | `String` | Yes | `"local-fake-$slug-001"` |
| `hotelName` | `String` | Yes | `"$destination Central Hotel"` |
| `city` | `String` | Yes | destination string |
| `country` | `String` | Yes | mapped from destination |
| `totalPrice` | `Double` | Yes | 420.0 / 510.0 |
| `currency` | `String` | Yes | `"EUR"` |
| `rating` | `Double` | Yes | 8.6 / 9.0 |
| `reviewCount` | `Int` | Yes | 1240 / 860 |
| `amenities` | `List<String>` | Yes | `["Wi-Fi", "Breakfast"]` / `["Wi-Fi", "Gym"]` |
| `availability` | `Availability` | Yes | AVAILABLE / LIMITED |
| `source` | `String` | Yes | `"local_fake_provider"` |
| `freshness` | `Freshness` | Yes | FRESH |

### 4.2 Enum contracts

`Availability`: AVAILABLE, LIMITED, UNKNOWN — all with `apiValue` mapping.
`Freshness`: FRESH, STALE, UNKNOWN — all with `apiValue` mapping.

## 5. Current API response exposure

### 5.1 HotelOfferResponse

| Field | Source | Notes |
|---|---|---|
| `offerId` | `offer.id` | Direct mapping |
| `providerOfferRef` | `offer.providerReference` | Direct mapping |
| `hotelName` | `offer.hotelName` | Direct mapping |
| `location.city` | `offer.city` | Nested in Location |
| `location.country` | `offer.country` | Nested in Location |
| `price.amount` | `offer.totalPrice` | Direct mapping |
| `price.currency` | `offer.currency` | Direct mapping |
| `price.basis` | `"total_stay"` | **Hardcoded** |
| `price.includesTaxesAndFees` | `"unknown"` | **Hardcoded** |
| `price.providerFreshness` | `offer.freshness.apiValue` | From domain freshness |
| `rating.value` | `offer.rating` | Direct mapping |
| `rating.scale` | `10.0` | **Hardcoded** |
| `rating.reviewCount` | `offer.reviewCount` | Direct mapping |
| `rating.source` | `offer.source` | From domain source |
| `amenities[]` | `offer.amenities` | Each with `source = "provider_fact"` |
| `availability` | `offer.availability.apiValue` | From enum |
| `source` | `offer.source` | Direct mapping |
| `freshness` | `offer.freshness.apiValue` | From enum |
| `matchSummary` | `rankedOffer.matchSummary` | From ranker |
| `providerFacts[]` | Hardcoded single fact | Only availability fact currently |

### 5.2 HotelSearchResponse

| Field | Source | Notes |
|---|---|---|
| `searchId` | `search.id.value` | Direct mapping |
| `sessionId` | `search.sessionId.value` | Direct mapping |
| `status` | `search.status.apiValue` | From enum |
| `criteria` | `HotelSearchCriteriaResponse.from()` | Direct mapping |
| `metadata.resultCompleteness` | `"complete"` | **Hardcoded** |
| `metadata.freshness` | `"fresh"` | **Hardcoded** |
| `metadata.providerState` | `"available"` | **Hardcoded** |
| `metadata.warnings` | Hardcoded list | Fake provider warning |

### 5.3 HotelOffersResponse

| Field | Source | Notes |
|---|---|---|
| `searchId` | `search.id.value` | Direct mapping |
| `status` | `search.status.apiValue` | From enum |
| `offers` | `search.offers.map(HotelOfferResponse::from)` | Per-offer mapping |
| `metadata` | Same hardcoded structure | Same as search response |
| `providerFacts` | `offers.flatMap { it.providerFacts }` | Aggregated from all offers |

### 5.4 API response readiness assessment

API response models are **ahead of domain model** in several dimensions:

- `Price.basis`, `Price.includesTaxesAndFees` — already present but hardcoded.
- `Rating.scale` — already present but hardcoded.
- `ProviderFact(field, value, source, freshness)` — flexible structure ready for real provider data.
- `Metadata(resultCompleteness, freshness, providerState, warnings)` — ready for real provider state.

**Key finding**: API layer already has the structural capacity to expose
provider-specific metadata without domain model changes. The `ProviderFact`
list is the designated escape hatch for provider data that does not belong
in domain model.

## 6. Current fake provider output

`FakeHotelOfferProvider` generates 2 offers per destination:

| Property | Offer 1 | Offer 2 |
|---|---|---|
| `id` | `fake-offer-{slug}-001` | `fake-offer-{slug}-002` |
| `providerReference` | `local-fake-{slug}-001` | `local-fake-{slug}-002` |
| `hotelName` | `{destination} Central Hotel` | `{destination} Riverside Stay` |
| `city` | destination | destination |
| `country` | Mapped (Rome→Italy, Paris→France, Berlin→Germany, else→Demo) | Same |
| `totalPrice` | 420.0 | 510.0 |
| `currency` | EUR | EUR |
| `rating` | 8.6 | 9.0 |
| `reviewCount` | 1240 | 860 |
| `amenities` | Wi-Fi, Breakfast | Wi-Fi, Gym |
| `availability` | AVAILABLE | LIMITED |
| `source` | `local_fake_provider` | `local_fake_provider` |
| `freshness` | FRESH | FRESH |

**Fake provider compatibility requirements for Stage 9.3:**

- Must produce at least 2 offers per destination.
- Must produce deterministic IDs (tests depend on them).
- Must set `source = "local_fake_provider"`.
- Must set `freshness = FRESH`.
- Must set valid `availability` (AVAILABLE or LIMITED).
- Must produce non-empty `amenities` list.

## 7. Provider result gap analysis

### 7.1 Gap table

| # | Provider data category | Current domain `HotelOffer` | Current API response | Gap severity | MVP relevance |
|---|---|---|---|---|---|
| 1 | Provider hotel identifier | `id`, `providerReference` | `offerId`, `providerOfferRef` | **Low** — fields exist; real provider IDs map naturally. | Required |
| 2 | Room/rate identifier | — | — | **Deferred** — MVP does not expose individual room/rate selection. | Post-MVP |
| 3 | Price structure | `totalPrice: Double` | `Price(amount, currency, basis, includesTaxesAndFees, providerFreshness)` | **Medium** — domain has single `totalPrice`; real providers return per-night, base+tax breakdown. API already has `basis` and `includesTaxesAndFees`. | MVP: total stay price sufficient |
| 4 | Taxes/fees | — | `includesTaxesAndFees = "unknown"` | **Low** — API already has placeholder; domain does not need tax breakdown for MVP. | Deferred |
| 5 | Currency | `currency: String` | `Price.currency` | **Low** — field exists; real provider currencies map naturally. | Required |
| 6 | Availability | `Availability` enum (AVAILABLE, LIMITED, UNKNOWN) | `availability: String` | **Low** — enum sufficient; adapter normalizes provider-specific availability. | Required |
| 7 | Cancellation/refundability | — | — | **Deferred** — cancellation policy is important but MVP does not expose it as domain field; belongs in providerFacts. | Deferred |
| 8 | Board/meal plan | — | — | **Deferred** — real providers return meal plan (RO, BB, HB, FB); not in MVP domain. | Deferred |
| 9 | Room name/type | — | — | **Deferred** — real providers return room type (Standard, Deluxe, Suite); not in MVP domain. | Deferred |
| 10 | Occupancy | — | — | **Deferred** — real providers return max occupancy; derived from criteria. | Deferred |
| 11 | Amenities | `amenities: List<String>` | `List<Amenity(name, source)>` | **Low** — field exists; real provider amenities map to string list. | Required |
| 12 | Hotel location/address | `city`, `country` | `Location(city, country)` | **Medium** — no street address, coordinates, postal code, district. | MVP: city/country sufficient |
| 13 | Star rating vs guest rating | `rating: Double`, `reviewCount: Int` | `Rating(value, scale, reviewCount, source)` | **Medium** — domain has single rating; real providers distinguish official star rating from guest review score. API has `scale` and `source`. | MVP: single rating sufficient |
| 14 | Images | — | — | **Deferred** — real providers return photo URLs; not in MVP domain. | Deferred |
| 15 | Booking/deeplink fields | — | — | **Deferred** — booking is post-MVP. | Post-MVP |
| 16 | Provider terms/disclaimers | — | — | **Deferred** — real providers include legal text; can go in providerFacts. | Deferred |
| 17 | Freshness/timestamp | `freshness: Freshness` | `freshness: String`, `Price.providerFreshness` | **Low** — enum exists; adapter sets FRESH on fetch, STALE for cached. | Required |
| 18 | Provider source attribution | `source: String` | `source: String` | **Low** — field exists; adapter sets provider name. | Required |
| 19 | Partial/missing data | — | `Metadata(resultCompleteness, warnings)` | **Low** — API already has completeness and warnings metadata. | Required |

### 7.2 Gap severity summary

| Severity | Count | Action |
|---|---|---|
| Low (field exists, mapping straightforward) | 9 | Adapter normalizes; no domain/API change needed |
| Medium (field exists but limited, adapter compensates) | 3 | Adapter enriches via existing fields or providerFacts; no domain change for MVP |
| Deferred (field missing, post-MVP concern) | 7 | providerFacts or future domain extension |
| Post-MVP (booking, room-rate IDs) | 2 | Explicitly out of scope |

**Finding**: zero domain model changes are needed for MVP real provider integration.
All gaps are addressed by adapter normalization, existing API response capacity,
or deferred to post-MVP.

## 8. Provider-neutral normalization rules

### 8.1 Required vs optional fields

| Field | Required in `HotelOffer` | Normalization rule |
|---|---|---|
| `id` | Yes | Adapter generates stable ID from provider hotel+rate identifiers. Must be unique per offer within a search. |
| `providerReference` | Yes | Raw provider offer identifier. Opaque string. |
| `hotelName` | Yes | Provider hotel name; trimmed, non-empty. |
| `city` | Yes | Provider city or location name; trimmed, non-empty. |
| `country` | Yes | Provider country code or name; trimmed, non-empty. |
| `totalPrice` | Yes | Total stay price in provider currency. Must be ≥ 0. Adapter computes from per-night × nights if provider returns nightly rates. |
| `currency` | Yes | ISO 4217 currency code (3 letters uppercase). Adapter validates format. |
| `rating` | Yes | Numeric rating value. Adapter normalizes to 0-10 scale. Default 0.0 if provider does not provide rating. |
| `reviewCount` | Yes | Number of reviews. Default 0 if provider does not provide. |
| `amenities` | Yes | List of amenity name strings. Empty list if provider returns none. Deduplicated, trimmed. |
| `availability` | Yes | Enum normalized from provider-specific values (see 8.3). |
| `source` | Yes | Provider name identifier string. |
| `freshness` | Yes | Enum: FRESH on initial fetch, STALE for cached/expired data. |

### 8.2 Price/currency handling

| Rule | Description |
|---|---|
| **Total stay price** | `totalPrice` is always total for entire stay, not per-night. If provider returns nightly rate, adapter multiplies by number of nights from criteria. |
| **Currency preservation** | `currency` preserves provider currency as-is. No cross-currency conversion in adapter. |
| **Tax inclusion unknown** | If provider does not specify tax inclusion, adapter does not assume. API exposes `includesTaxesAndFees = "unknown"`. |
| **Zero price valid** | Price of 0.0 is valid (e.g., loyalty points redemption). Negative prices invalid → offer excluded. |
| **Double precision** | `totalPrice` uses `Double`. For MVP, floating-point precision is acceptable. Future: `BigDecimal` if needed. |

### 8.3 Availability mapping

| Provider concept | Normalized `Availability` |
|---|---|
| Available, in stock, confirmed | `AVAILABLE` |
| Limited availability, few rooms left, last room | `LIMITED` |
| Unknown, on request, pending confirmation | `UNKNOWN` |
| Sold out, unavailable, closed | **Offer excluded** from results |

### 8.4 Missing rating handling

| Provider scenario | `rating` | `reviewCount` | `Rating.scale` (API) |
|---|---|---|---|
| Provider returns guest rating 0-10 | Direct value | Direct count | `10.0` |
| Provider returns star rating 1-5 | `starRating * 2.0` (normalize to 0-10) | Direct or 0 | `10.0` |
| Provider returns no rating | `0.0` | `0` | `10.0` |
| Provider returns rating on custom scale | `value * (10.0 / providerScale)` | Direct or 0 | `10.0` |

### 8.5 Source/freshness handling

| Field | Rule |
|---|---|
| `source` | Adapter sets to provider identifier string (e.g., `"amadeus"`, `"booking_com"`, `"expedia"`). Must be consistent for same provider across calls. |
| `freshness` | `FRESH` when data was fetched within current request. `STALE` when data comes from cache or is older than configurable threshold. `UNKNOWN` when freshness cannot be determined. |

### 8.6 providerFacts population

`ProviderFact` is the designated escape hatch for provider-specific data that
does not belong in domain `HotelOffer`. Adapter should populate `providerFacts`
with:

| Fact | When to include |
|---|---|
| `cancellation_policy` | If provider returns cancellation terms |
| `meal_plan` | If provider returns board basis (RO, BB, HB, FB) |
| `room_type` | If provider returns room name/type |
| `star_rating` | If provider returns official star classification |
| `address` | If provider returns street address |
| `coordinates` | If provider returns lat/lon |
| `images` | If provider returns photo URLs (comma-separated or first URL) |
| `deeplink` | If provider returns booking URL |
| `provider_terms` | If provider includes legal/disclaimer text |
| `last_price_check` | Timestamp of provider price validation |

These facts appear in API response `providerFacts[]` without requiring domain
model changes.

### 8.7 Invalid/partial offer exclusion

| Condition | Action |
|---|---|
| Missing `hotelName` (empty/null) | Exclude offer from results |
| Missing `totalPrice` (null/NaN) | Exclude offer from results |
| Negative `totalPrice` | Exclude offer from results |
| Missing `currency` | Exclude offer from results |
| Invalid `currency` format | Exclude offer from results |
| Provider returned "sold out" | Exclude offer from results |
| Missing `city` and `country` | Include with `city = "Unknown"`, `country = "Unknown"` |
| Missing `amenities` | Include with empty list |
| Missing `rating` | Include with `rating = 0.0`, `reviewCount = 0` |

### 8.8 Sorting expectations

Adapter returns offers in **provider order** (unsorted). Ranking is performed
by `HotelOfferRanker` after adapter returns. Adapter does not sort.

### 8.9 Duplicate handling

| Scenario | Action |
|---|---|
| Same `providerReference` returned twice | Adapter deduplicates; keeps first occurrence |
| Same hotel, different room/rate | Keep both — different `id` and `providerReference` |
| Same hotel, same room/rate, different price | Adapter keeps lowest price |

### 8.10 Stable IDs

| Field | ID strategy |
|---|---|
| `id` | Adapter generates from provider reference. Pattern: `"{providerSource}-{providerHotelId}-{providerRateId}"` or similar. Must be deterministic for same provider data. |
| `providerReference` | Raw provider identifier. Opaque to domain. |

### 8.11 Error vs empty result distinction

| Provider response | `HotelSearch.Status` | Behavior |
|---|---|---|
| Provider returns offers | `COMPLETED_WITH_OFFERS` | Normal flow |
| Provider returns empty list (no hotels match) | `COMPLETED_NO_OFFERS` | Valid empty result |
| Provider returns error (exception) | Exception propagated | Caught by `ExecuteConfirmedSearchTransitionUseCase` as `SEARCH_CREATION_FAILED` |
| Provider returns partial results | `COMPLETED_WITH_OFFERS` | Valid partial; `Metadata.warnings` should note partial |

**Critical**: provider error (exception) is fundamentally different from
empty result (no offers). Empty result is a valid business outcome.
Error is a failure that Stage 8 attempt lifecycle handles.

### 8.12 No-booking-flow boundary

Adapter does not include:

- Booking URLs or deeplinks in domain model.
- Payment information.
- Reservation confirmation data.
- Guest personal data.

Booking/deeplink data may appear in `providerFacts` as informational
metadata but must not drive any application behavior.

## 9. Domain mapping rules

### 9.1 Field classification

#### A. Fields safe to map into current `HotelOffer`

These fields map directly from normalized provider data into existing
domain fields without model changes:

| Domain field | Provider source | Mapping |
|---|---|---|
| `id` | Generated from provider refs | Deterministic adapter-generated string |
| `providerReference` | Provider offer ID | Direct string |
| `hotelName` | Provider hotel name | Direct string, trimmed |
| `city` | Provider city/location | Direct string, trimmed |
| `country` | Provider country | Direct string, trimmed |
| `totalPrice` | Provider total price | Computed: nightly × nights or direct total |
| `currency` | Provider currency code | ISO 4217 string |
| `rating` | Provider guest rating | Normalized to 0-10 scale |
| `reviewCount` | Provider review count | Direct int, default 0 |
| `amenities` | Provider amenity list | String list, deduplicated |
| `availability` | Provider availability status | Normalized to AVAILABLE/LIMITED/UNKNOWN |
| `source` | Provider identifier | Adapter-configured string |
| `freshness` | Data fetch timestamp | FRESH or STALE based on age |

#### B. Fields that should remain in `providerFacts`

These fields are provider-specific and do not belong in domain model.
They appear in API response via `ProviderFact`:

| Fact | Reason not in domain |
|---|---|
| Cancellation policy | Varies wildly between providers; not used in ranking or domain logic |
| Meal plan / board basis | Not in MVP domain; informational only |
| Room type / room name | MVP treats hotel as unit, not individual rooms |
| Star rating (official) | Domain uses guest rating; star rating is supplementary |
| Street address | Not needed for MVP ranking or selection |
| Coordinates (lat/lon) | Not needed for MVP |
| Photo URLs | Not in domain; frontend concern |
| Booking deeplink | Post-MVP; must not drive behavior |
| Provider legal terms | Informational only |
| Last price check timestamp | Operational metadata |
| Provider-specific tags | Varies per provider; opaque to domain |

#### C. Fields that should remain future-only

| Field | Reason deferred |
|---|---|
| Room/rate identifier | MVP treats hotel as unit; room selection is post-MVP |
| Booking reference | Post-MVP; no booking flow |
| Guest personal data | Post-MVP; no booking flow |
| Payment information | Post-MVP |
| Loyalty/pricing tier | Post-MVP |
| Multi-currency converted price | Post-MVP; adapter preserves provider currency |

#### D. Fields that would require explicit later domain/API change

| Field | Current status | Future trigger |
|---|---|---|
| `HotelSearchCriteria.starRatingFilter` | Not in domain | When real provider contract demands filter |
| `HotelSearchCriteria.amenityFilters` | Not in domain (API accepts, drops) | When MVP expands filter support |
| `HotelSearchCriteria.priceRange` | Not in domain (API accepts, drops) | When MVP expands budget filtering |
| `HotelSearchCriteria.nationality` | Not in domain | When provider requires nationality for pricing |
| `HotelSearchCriteria.childrenAges` | Not in domain | When provider requires individual child ages |
| `HotelOffer.cancellationPolicy` | Not in domain | When cancellation becomes first-class MVP feature |
| `HotelOffer.starRating` | Not in domain (guest rating used) | When official star classification needed |
| `HotelOffer.address` | Not in domain | When location detail becomes MVP requirement |
| `HotelOffer.photoUrls` | Not in domain | When frontend needs images |
| `HotelOffer.roomType` | Not in domain | When room-level selection enters MVP |

#### E. Fields that must not leak into Stage 8 confirmation lifecycle

The Stage 8 confirmation lifecycle operates on `ProceedWithCandidateCriteria`
→ `HotelSearchCriteria` → `CreateHotelSearchCommand` → `HotelSearchBoundary.createSearch()`.

Fields that must NOT appear in this chain:

- Provider credentials, API keys, configuration.
- Provider-specific error details.
- Provider rate limiting or throttling signals.
- Booking/payment data.
- Provider internal identifiers beyond `providerReference`.

Stage 8 sees only `HotelSearchCriteria` (destination, dates, guests, rooms)
and receives `HotelSearch` (id, session, criteria, status, ranked offers).
Provider-specific knowledge stays in infrastructure adapter.

### 9.2 Mapping flow diagram

```
Real Provider API Response
  │
  ▼
[Adapter: parse + validate]
  │
  ├── Invalid offer → excluded
  ├── Sold out → excluded
  ├── Valid offer ↓
  │
  ▼
[Adapter: normalize]
  │
  ├── Price → total stay price (compute if nightly)
  ├── Currency → ISO 4217 validate
  ├── Availability → AVAILABLE/LIMITED/UNKNOWN
  ├── Rating → 0-10 scale normalize
  ├── ReviewCount → direct or 0
  ├── Amenities → deduplicated string list
  ├── Source → provider identifier
  ├── Freshness → FRESH/STALE/UNKNOWN
  │
  ▼
[Adapter: enrich providerFacts]
  │
  ├── Cancellation policy
  ├── Meal plan
  ├── Room type
  ├── Star rating
  ├── Address, coordinates
  ├── Photos, deeplinks
  │
  ▼
Domain: HotelOffer (unchanged model)
  │
  ▼
Domain: HotelOfferRanker (unchanged logic)
  │
  ▼
Domain: RankedHotelOffer (unchanged)
  │
  ▼
API: HotelOfferResponse + providerFacts (unchanged model, richer data)
```

## 10. Recommended contract direction

### 10.1 Domain model

**Keep `HotelSearchCriteria`, `HotelOffer`, `HotelOfferProviderBoundary` unchanged for Stage 9.3.**

Rationale:

1. All 12 `HotelOffer` fields are sufficient for MVP real provider integration.
2. Gaps (cancellation, room type, meal plan, photos, coordinates) belong in `providerFacts`.
3. Criteria gaps (star rating filter, amenity filter, price range) are deferred — API already accepts them as optional JSON.
4. Changing domain model now would break Stage 7/Stage 8 tests unnecessarily.
5. Domain model should change only when a specific real provider contract demands it.

### 10.2 API response model

**Keep `HotelOfferResponse`, `HotelSearchResponse`, `HotelOffersResponse` unchanged for Stage 9.3.**

Rationale:

1. `Price.basis`, `Price.includesTaxesAndFees` already exist — adapter populates via domain data.
2. `ProviderFact` flexible structure ready for real provider metadata.
3. `Metadata(resultCompleteness, freshness, providerState, warnings)` ready for real provider state.
4. `Rating.scale`, `Rating.source` already exist.

### 10.3 Provider-neutral result contract

Define the following contract for any future real adapter:

| Contract item | Expectation |
|---|---|
| **Input** | `HotelSearchCriteria` (destination, dates, guests, rooms) |
| **Output** | `List<HotelOffer>` (provider-normalized domain offers) |
| **Empty result** | Return `emptyList()`, not exception |
| **Error** | Throw exception (caught as `SEARCH_CREATION_FAILED` by Stage 8) |
| **Offer validity** | All returned offers pass validation rules (section 8.1, 8.7) |
| **No duplicates** | Adapter deduplicates by `providerReference` |
| **No sorting** | Adapter returns provider order; `HotelOfferRanker` sorts |
| **Deterministic IDs** | Same provider data → same `id` |
| **Source tagging** | Every offer tagged with provider source |
| **Freshness** | Every offer tagged with FRESH/STALE/UNKNOWN |
| **ProviderFacts** | Adapter populates via `HotelOfferResponse.ProviderFact` for extra data |

### 10.4 Fake provider compatibility

`FakeHotelOfferProvider` must remain compatible:

- Produces `List<HotelOffer>` with valid fields.
- Deterministic IDs.
- No exceptions.
- Source = `"local_fake_provider"`.
- Freshness = FRESH.
- No changes to fake provider needed for Stage 9.3.

## 11. Stage 9.3 candidate scope

**Stage 9.3 — Provider Adapter Skeleton and Fake-vs-Real Seam**

Это implementation sub-stage (medium-small, bounded).

Scope:

1. Implement `RealHotelOfferProviderAdapter : HotelOfferProviderBoundary` skeleton в `infrastructure/provider/`.
2. Adapter skeleton returns `emptyList()` by default (no real calls).
3. Implement configuration seam: provider type selection via constructor/config.
4. Wire fake-vs-real seam in `Application.kt`: `FakeHotelOfferProvider` remains default; real adapter opt-in.
5. Add provider configuration data class (provider type, base URL placeholder, timeout placeholder).
6. Add targeted tests:
   - Default wiring uses fake provider.
   - Real adapter skeleton returns empty list.
   - Configuration selects provider type.
   - Fake provider behavior unchanged (Stage 7/8 compatibility).
7. Update `HotelSearchResponse.Metadata` to reflect actual provider state (fake vs real).

Out of scope:

- Real external API calls.
- Provider credentials or API keys.
- Network/HTTP client setup.
- Domain model changes.
- API response model changes.
- Stage 8 confirmation lifecycle changes.
- Stage 7 strict handoff changes.
- Persistence, auth, observability, deployment.
- Frontend changes.
- OpenAPI changes.

## 12. Guardrails for Stage 9.3

- Real provider adapter skeleton returns `emptyList()` by default — no real calls.
- `FakeHotelOfferProvider` remains default in `Application.kt`.
- No provider credentials, API keys, or secrets in source code or configuration defaults.
- No external HTTP client dependency.
- No domain model changes (`HotelOffer`, `HotelSearchCriteria`, `HotelOfferProviderBoundary`).
- No API response model changes.
- No Stage 8 confirmation lifecycle changes.
- No Stage 7 strict handoff changes.
- `show_hotel_results` response behavior preserved.
- All existing tests must pass without modification.
- New tests verify skeleton behavior only.
- No production readiness claim.
- No runtime behavior change when fake provider is selected (default).

## 13. Validation expectations for Stage 9.3

- `./gradlew build` passes (compile + tests).
- Existing Stage 7/Stage 8 tests pass without modification.
- New tests verify:
  - Default wiring uses `FakeHotelOfferProvider`.
  - Real adapter skeleton implements `HotelOfferProviderBoundary`.
  - Real adapter returns `emptyList()` without configuration.
  - Configuration selects provider type.
  - Fake provider output unchanged (2 offers, deterministic IDs, correct fields).
- `git status --short` — only expected source/test/docs files.
- `git diff --check` — no errors.

## 14. Prompt для Stage 9.3

```
Мы продолжаем проект travel-assistant.

Пожалуйста, отвечай на русском языке. Технические имена классов, файлов,
enum values, commit messages и команды оставляй как есть.

Контекст

* Репозиторий: travel-assistant
* Branch: stage-9
* Последний завершённый sub-stage: Stage 9.2
* Stage 9 runtime implementation ещё не начат.
* Production readiness не claimed.
* Все InMemory stores, FakeLlmClient, FakeHotelOfferProvider — accepted carryover.

Текущая provider boundary (после Stage 9.1-9.2)

* `HotelOfferProviderBoundary` — fun interface: `search(HotelSearchCriteria): List<HotelOffer>`.
* `FakeHotelOfferProvider` — deterministic local adapter, default.
* `HotelSearchCriteria` — destination, checkInDate, checkOutDate, guests (adults/children), rooms.
* `HotelOffer` — 12 fields (id, providerReference, hotelName, city, country, totalPrice, currency, rating, reviewCount, amenities, availability, source, freshness).
* `HotelOffer.Availability` — AVAILABLE, LIMITED, UNKNOWN.
* `HotelOffer.Freshness` — FRESH, STALE, UNKNOWN.

Рекомендации из Stage 9.2

* Keep HotelSearchCriteria, HotelOffer, HotelOfferProviderBoundary unchanged.
* Real adapter skeleton implements HotelOfferProviderBoundary.
* Adapter skeleton returns emptyList() by default — no real calls.
* Configuration seam: provider type selection via constructor/config.
* FakeHotelOfferProvider remains default.
* Provider-neutral result contract: valid offers, no duplicates, no sorting, deterministic IDs, source tagging, freshness tagging.
* Empty result = emptyList(); error = exception.

Задача

Выполни:

Stage 9.3 — Provider Adapter Skeleton and Fake-vs-Real Seam

Это implementation sub-stage (medium-small, bounded).

Цели Stage 9.3

1. Implement `RealHotelOfferProviderAdapter : HotelOfferProviderBoundary` в `infrastructure/provider/`.
2. Adapter skeleton returns `emptyList()` by default (no real calls).
3. Implement provider configuration data class (provider type enum, base URL placeholder, timeout placeholder).
4. Wire fake-vs-real seam in `Application.kt`: `FakeHotelOfferProvider` remains default; real adapter opt-in via config.
5. Add targeted tests.
6. Update `HotelSearchResponse.Metadata` to reflect actual provider state (optional; if safe).

Required source files to create

* `services/backend/src/main/kotlin/com/travelassistant/backend/infrastructure/provider/RealHotelOfferProviderAdapter.kt`
* `services/backend/src/main/kotlin/com/travelassistant/backend/infrastructure/provider/HotelProviderConfig.kt`

Required source files to modify

* `services/backend/src/main/kotlin/com/travelassistant/backend/Application.kt` — configuration-driven provider selection.

Required test files to create

* `services/backend/src/test/kotlin/com/travelassistant/backend/infrastructure/provider/RealHotelOfferProviderAdapterTest.kt`

Strict guardrails

* Real adapter skeleton returns emptyList() — no real external calls.
* FakeHotelOfferProvider remains default.
* No provider credentials or API keys in code.
* No external HTTP client dependency.
* No domain model changes.
* No API response model changes (unless trivially safe metadata update).
* No Stage 8 confirmation lifecycle changes.
* No Stage 7 strict handoff changes.
* All existing tests must pass without modification.
* No production readiness claim.

Validation

* `./gradlew build` passes.
* All existing tests pass.
* New tests verify skeleton and configuration behavior.
* `git status --short` — expected files only.
* `git diff --check` — no errors.

Final response format

1. Созданные файлы
2. Изменённые файлы
3. Краткий итог
4. Checks
5. Commit recommendation: Stage 9.3 provider adapter skeleton and fake-vs-real seam
```

## 15. Stage 9.2 verdict

**Passed** — provider result contract and domain mapping defined.

Stage 9.2:

1. Inspect 15 backend source files и 5 документов.
2. Задокументировал current search criteria contract: 5 domain fields + 6 API-only fields (dropped during validation).
3. Задокументировал current hotel offer contract: 12 domain fields с Availability/Freshness enums.
4. Задокументировал current API response exposure: API layer опережает domain model (Price.basis, Price.includesTaxesAndFees, Rating.scale, ProviderFact, Metadata).
5. Задокументировал fake provider output: 2 deterministic offers, compatible constraints.
6. Провёл gap analysis по 19 категориям: 9 low (mapping straightforward), 3 medium (adapter compensates), 7 deferred (providerFacts or future), 2 post-MVP.
7. Определил 12 provider-neutral normalization rules (required/optional, price/currency, availability mapping, rating normalization, source/freshness, providerFacts, validation, sorting, duplicates, stable IDs, error vs empty, no-booking boundary).
8. Классифицировал domain mapping rules: 13 fields safe to map, 11 fields for providerFacts, 6 future-only, 10 requiring later domain/API change, 5 categories must not leak into Stage 8.
9. Рекомендовал сохранить `HotelSearchCriteria`, `HotelOffer`, `HotelOfferProviderBoundary` без изменений для Stage 9.3.
10. Определил scope, guardrails и validation для Stage 9.3.
11. Произвёл готовый prompt для Stage 9.3 на русском языке.

**Key conclusion**: zero domain model changes, zero API response model changes,
and zero provider boundary changes are needed for MVP real provider integration.
The existing domain model, API response structure (especially `ProviderFact`
and `Metadata`), and provider boundary are sufficient. All provider-specific
gaps are handled by adapter normalization and `providerFacts` escape hatch.

Production code не изменён. Runtime не изменён. Tests не запускались.
Production readiness не заявлена. Stage 9 implementation не начат.
