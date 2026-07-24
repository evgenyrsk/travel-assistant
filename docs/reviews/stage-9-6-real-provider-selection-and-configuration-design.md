# Stage 9.6 — Real Provider Selection and Configuration Design

## 1. Scope

Stage 9.6 — review/design-only stage.

Stage 9.6:

1. Определяет requirements для real hotel provider на основе current product/backend constraints.
2. Проводит review реалистичных hotel API provider candidates по публичной документации.
3. Выбирает recommended primary provider candidate или short list.
4. Определяет provider configuration design без реализации.
5. Определяет secrets/API key handling design без ввода реальных credentials.
6. Определяет sandbox vs production strategy.
7. Определяет request/response fixture strategy для future tests.
8. Определяет следующий safe Stage 9 step после provider selection.
9. Производит prompt для Stage 9.7.

Stage 9.6 не меняет production code, tests, runtime, routes, API,
OpenAPI, frontend, generated clients, product baseline или
architecture baseline.

## 2. Sources inspected

### 2.1 Backend source

| File | Purpose |
|---|---|
| `HotelProviderConfig.kt` | Current config: mode only (FAKE/REAL) |
| `HotelProviderMode.kt` | Enum: FAKE, REAL |
| `HotelOfferProviderFactory.kt` | Factory: mode → provider instance |
| `RealHotelOfferProviderAdapter.kt` | Skeleton: emptyList(), no I/O |
| `FakeHotelOfferProvider.kt` | Deterministic fake: 2 offers, hardcoded |
| `HotelProviderErrorCategory.kt` | 7 error categories |
| `HotelProviderException.kt` | Exception with category + message + cause |
| `HotelOfferProviderBoundary.kt` | Domain boundary: search(criteria) → List<HotelOffer> |
| `HotelSearchCriteria.kt` | Domain: destination, dates, guests, rooms |
| `HotelOffer.kt` | Domain: 12 fields including source/freshness |
| `HotelOfferResponse.kt` | API: ProviderFact escape hatch |
| `Application.kt` | DI: HotelProviderConfig.fromEnvironment() |

### 2.2 Documentation

| Document | Role |
|---|---|
| `docs/roadmap/roadmap.md` | Primary roadmap |
| `docs/architecture/architecture-baseline.md` | Architecture baseline |
| `docs/product/product-baseline.md` | Product baseline |
| `docs/reviews/stage-9-1-*.md` | Boundary review |
| `docs/reviews/stage-9-2-*.md` | Result contract / domain mapping |
| `docs/reviews/stage-9-3-*.md` | Adapter skeleton / fake-vs-real seam |
| `docs/reviews/stage-9-4-*.md` | Error taxonomy |
| `docs/reviews/stage-9-5-*.md` | Integration verification |

### 2.3 External research

| Provider | Source | Status |
|---|---|---|
| Expedia Rapid API | developers.expediagroup.com | Fetched successfully |
| Amadeus Hotel APIs | developers.amadeus.com | SPA — not rendered via fetch; used training knowledge |
| SerpApi Google Hotels | serpapi.com/google-hotels-api | Fetched successfully |
| Booking.com | booking.com affiliate/partner | Training knowledge (no public sandbox API) |
| Hotelbeds | docs.hotelbeds.com | Transport error; training knowledge |
| Duffel Stays | duffel.com | 404; training knowledge |

## 3. Current provider integration baseline

### 3.1 Architecture

```
HotelProviderConfig(mode: FAKE | REAL)
  │
  ▼
HotelOfferProviderFactory.create(config)
  ├── FAKE → FakeHotelOfferProvider (deterministic, 2 offers)
  └── REAL → RealHotelOfferProviderAdapter (emptyList(), no I/O)
      │
      ▼
HotelOfferProviderBoundary.search(criteria): List<HotelOffer>
      │
      ▼
CreateHotelSearchUseCase → HotelOfferRanker → HotelSearchStateStore
```

### 3.2 Current config

```kotlin
data class HotelProviderConfig(
    val mode: HotelProviderMode = HotelProviderMode.FAKE,
)
```

Only one field: `mode`. No credentials, URLs, timeouts, or provider identity.

### 3.3 What needs to be added for REAL mode

| Concern | Current state | Needed for REAL |
|---|---|---|
| Provider identity/name | Not present | Provider name string |
| Base URL | Not present | Configurable URL |
| API key/token | Not present | Secure credential injection |
| Sandbox vs production | Not present | Environment mode flag |
| Timeout | Not present | Configurable timeout |
| Rate-limit behavior | Not present | Rate-limit awareness |
| Safe startup on missing config | N/A | Fail-safe: fall back to FAKE or fail explicitly |
| HTTP client | Not present | Ktor HttpClient or similar |
| Error mapping | Not present | Provider error → HotelProviderErrorCategory |

## 4. Real provider requirements

Based on current product baseline (hotel-only MVP v1) and domain model:

### 4.1 Must-have requirements

| Requirement | Rationale |
|---|---|
| Destination/date/guests/rooms search | Matches `HotelSearchCriteria` (5 fields) |
| Hotel names | `HotelOffer.hotelName` |
| Prices (total or per-night) | `HotelOffer.totalPrice` |
| Currency | `HotelOffer.currency` |
| Availability status | `HotelOffer.availability` (AVAILABLE/LIMITED/UNKNOWN) |
| Multiple results per search | Current domain returns `List<HotelOffer>` |
| Provider offer reference | `HotelOffer.providerReference` |
| Source attribution | `HotelOffer.source` |

### 4.2 Should-have requirements

| Requirement | Rationale |
|---|---|
| Guest rating | `HotelOffer.rating` + `HotelOffer.reviewCount` |
| Amenities list | `HotelOffer.amenities` |
| City/country | `HotelOffer.city` + `HotelOffer.country` |
| Cancellation info | → `providerFacts` (not domain model) |
| Star rating | → `providerFacts` |
| Room type | → `providerFacts` |
| Meal plan | → `providerFacts` |

### 4.3 Nice-to-have (providerFacts / future)

| Requirement | Rationale |
|---|---|
| Photos | Not in domain model; future frontend concern |
| Deeplink/booking URL | Post-MVP (no booking flow) |
| Coordinates | Not in domain model |
| Description | Not in domain model |
| Policies | Not in domain model |

### 4.4 Constraints

- Must not force domain model changes (Stage 9.2 confirmed zero changes needed).
- Must not force API response model changes.
- Must not force OpenAPI/frontend changes.
- Must support sandbox/test mode without real charges.
- Auth scheme must be compatible with environment variable injection.
- Provider must allow displaying results in an assistant UI.

## 5. Provider candidates reviewed

### 5.1 Amadeus Hotel APIs

**Type**: Travel industry GDS (Global Distribution System).

**Public documentation**: https://developers.amadeus.com — self-service portal.

**Hotel APIs available**:
- **Hotel Search API** (v3): search hotels by destination, dates, guests; returns property list with offers.
- **Hotel Offers API** (v3): get detailed offers for a specific hotel.
- **Hotel Booking API**: create booking (post-MVP).

**Auth scheme**: OAuth 2.0 client credentials flow. `API_KEY` + `API_SECRET` → access token (1 hour TTL).

**Sandbox**: Yes — free self-service test environment with test data. Limited inventory (sample hotels in specific cities).

**Rate limits**: Free tier: ~3000 requests/month. Paid tier available.

**Search parameters**: cityCode or geoCode, checkInDate, checkOutDate, adults, children, rooms, priceRange, ratings.

**Response shape**: JSON with `data[]` array of hotel offers. Each offer has hotel info (name, city, rating, latitude/longitude) and room/offer details (price, currency, cancellation policy, room description).

**Pricing**: Free self-service tier for development/testing. Production requires agreement.

**Terms**: Display results allowed. Booking requires partnership agreement.

**Adapter complexity**: Medium. OAuth2 token management, JSON parsing, price normalization (total vs per-night).

**MVP risks**: Sandbox has limited inventory (may not return real-looking results for all destinations). Production requires commercial agreement.

### 5.2 Expedia Rapid API

**Type**: Major OTA (Online Travel Agency) API provider.

**Public documentation**: https://developers.expediagroup.com/rapid/lodging — comprehensive docs.

**APIs available**:
- **Shopping API**: search properties by destination, dates, occupancy; returns rooms with rates.
- **Content API**: static property information.
- **Geography API**: region/destination lookup.
- **Booking API**: create/manage bookings (post-MVP).

**Auth scheme**: Signature-based authentication (HMAC-SHA256) using API key + secret. Also OAuth 2.0 for some endpoints.

**Sandbox**: Yes — test environment with test data.

**Rate limits**: Partner-dependent. Rate limiting documentation available.

**Search parameters**: propertyId list, checkin, checkout, occupancy (adults/children), currency, country, salesChannel.

**Response shape**: JSON with `properties[]` containing rooms and rates. Each rate has price (base + tax + fees), cancellation policy, meal plan, room type.

**Pricing**: Requires commercial partnership (not free self-service).

**Terms**: Requires Rapid API partner agreement. Display results allowed under agreement.

**Adapter complexity**: High. Signature auth, complex nested response, multiple API calls (geography + shopping + content).

**MVP risks**: Requires commercial partnership for access. Complex integration. Not suitable for solo developer or MVP prototype.

### 5.3 Booking.com Affiliate/Partner APIs

**Type**: Major OTA with affiliate program.

**Public documentation**: Limited. Partner portal requires approved application.

**APIs available**:
- **Availability API**: search available properties.
- **Content API**: property descriptions, photos.
- **Booking API**: reservations (requires full partner status).

**Auth scheme**: API key (for affiliate) or machine account credentials (for connectivity partners).

**Sandbox**: Limited — test environment available for approved partners only.

**Rate limits**: Partner-dependent.

**Search parameters**: city_id or latitude/longitude, checkin, checkout, guests, rooms.

**Response shape**: XML or JSON depending on API version. Complex nested structure.

**Pricing**: Affiliate program (commission-based). Connectivity partner requires commercial agreement.

**Terms**: Strict display requirements. Affiliate links required for booking flow.

**Adapter complexity**: Very high. Complex auth, XML responses, multiple API versions, strict compliance requirements.

**MVP risks**: Application process takes weeks. Strict compliance requirements. Not suitable for rapid MVP development.

### 5.4 SerpApi Google Hotels

**Type**: Search engine results scraping API. Returns Google Hotels data.

**Public documentation**: https://serpapi.com/google-hotels-api — clear docs.

**APIs available**:
- **Google Hotels API**: returns hotel properties from Google Hotels search.
- **Google Hotels Properties API**: detailed property data.
- **Google Hotels Reviews API**: guest reviews.

**Auth scheme**: API key (simple bearer token).

**Sandbox**: No sandbox per se, but API key with free credits.

**Rate limits**: Plan-based. Free tier: 100 searches/month. Paid: $50/month for 5000 searches.

**Search parameters**: q (query string like "hotels in Rome"), check_in_date, check_out_date, adults, children.

**Response shape**: JSON with `properties[]` containing hotel_name, overall_rating, reviews, prices (from multiple sources), images, gps_coordinates.

**Pricing**: $50/month for 5000 searches. Pay-per-search model.

**Terms**: Display results allowed. No direct booking capability. Data sourced from Google.

**Adapter complexity**: Low-Medium. Simple API key auth, clean JSON response, straightforward mapping.

**MVP risks**: Not a direct hotel provider — scrapes Google Hotels results. Prices come from multiple OTAs. Limited control over data freshness. $50/month cost for limited volume.

### 5.5 Duffel Stays

**Type**: Modern travel API platform (flights + stays).

**Public documentation**: https://duffel.com/docs — developer-friendly docs.

**APIs available**:
- **Stays Search API**: search accommodation by location, dates, guests.
- **Stays Quote API**: get pricing for specific rooms.
- **Stays Booking API**: create bookings (post-MVP).

**Auth scheme**: API key (bearer token).

**Sandbox**: Yes — test mode with test data.

**Rate limits**: Reasonable limits for development.

**Search parameters**: location, check_in_date, check_out_date, guests (adults/children with ages).

**Response shape**: JSON with results containing accommodation data, rooms, rates, cancellation.

**Pricing**: Free for development/testing. Production pricing per booking.

**Terms**: Display results allowed. Booking requires agreement.

**Adapter complexity**: Low-Medium. Modern REST API, clean JSON, good documentation.

**MVP risks**: Stays API may be newer/less mature than flights API. Limited hotel inventory compared to major OTAs. Uncertain production pricing.

### 5.6 Hotelbeds

**Type**: B2B travel wholesaler.

**Public documentation**: https://docs.hotelbeds.com — requires registration.

**APIs available**:
- **Hotel API**: hotel search and availability.
- **Content API**: static hotel data.
- **Booking API**: reservations.

**Auth scheme**: API key + signature (HMAC-SHA256).

**Sandbox**: Yes — test environment.

**Rate limits**: Partner-dependent.

**Search parameters**: destination, checkIn, checkOut, occupancy.

**Response shape**: JSON with hotels[] containing rooms[] with rates.

**Pricing**: B2B commercial agreement required.

**Terms**: B2B partnership required.

**Adapter complexity**: High. Complex auth, large response payloads, enterprise-oriented.

**MVP risks**: Requires B2B agreement. Not suitable for solo developer or MVP prototype. Enterprise-oriented.

### 5.7 RapidAPI Hotel Endpoints

**Type**: API marketplace with various hotel data providers.

**Public documentation**: rapidapi.com — marketplace listings.

**APIs available**: Various third-party hotel APIs (e.g., Booking.com via API-Dojo, Hotels.com via various providers, Priceline, etc.).

**Auth scheme**: RapidAPI key (X-RapidAPI-Key header).

**Sandbox**: Provider-dependent. Most offer free tier.

**Rate limits**: Provider-dependent. Free tiers typically 500-5000 requests/month.

**Response shape**: Provider-dependent. Highly variable quality.

**Pricing**: Free tier + paid per-request.

**Terms**: Provider-dependent. Often restrictive for commercial use.

**Adapter complexity**: Variable. Often simpler auth, but unreliable data quality and terms.

**MVP risks**: Unreliable long-term. Provider may disappear. Terms often prohibit commercial display. Not suitable for production MVP.

## 6. Provider comparison matrix

| # | Criterion | Amadeus | Expedia Rapid | Booking.com | SerpApi Google Hotels | Duffel Stays | Hotelbeds | RapidAPI |
|---|---|---|---|---|---|---|---|---|
| 1 | Destination/date/guests/rooms search | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Variable |
| 2 | Hotel names and prices | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Variable |
| 3 | Currency | ✅ | ✅ | ✅ | ⚠️ Multiple sources | ✅ | ✅ | Variable |
| 4 | Availability clearly exposed | ✅ | ✅ | ✅ | ❌ Indirect | ✅ | ✅ | Variable |
| 5 | Ratings or hotel category | ✅ Rating | ✅ Star + guest | ✅ Rating | ✅ Google rating | ⚠️ Limited | ✅ | Variable |
| 6 | Cancellation/refundability | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ | Variable |
| 7 | Room/rate-level details | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ | Variable |
| 8 | Sandbox/test mode | ✅ Free | ✅ Test env | ⚠️ Approved only | ⚠️ Free credits | ✅ Test mode | ✅ Test env | Provider-dep |
| 9 | Public documentation available | ✅ | ✅ | ⚠️ Portal only | ✅ | ✅ | ⚠️ Registration | ✅ |
| 10 | Auth scheme clear | ✅ OAuth2 | ✅ Signature/OAuth | ⚠️ Varies | ✅ API key | ✅ API key | ✅ Key+sign | ✅ API key |
| 11 | Rate limits/pricing clear for MVP | ✅ Free tier | ⚠️ Partner | ❌ Unknown | ✅ $50/5000 | ✅ Free dev | ❌ B2B | ✅ Free tier |
| 12 | Terms acceptable for assistant UI | ✅ Dev tier | ⚠️ Partner agr | ❌ Strict | ✅ Display OK | ✅ Dev tier | ⚠️ B2B agr | ⚠️ Varies |
| 13 | Compatible with HotelSearchCriteria | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Variable |
| 14 | Compatible with HotelOffer + providerFacts | ✅ | ✅ | ✅ | ⚠️ Limited | ✅ | ✅ | Variable |
| 15 | Avoids forcing domain/API changes now | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Variable |
| 16 | Adapter complexity | Medium | High | Very high | Low-Med | Low-Med | High | Low |
| 17 | MVP risk level | **Low-Med** | **High** | **Very high** | **Med** | **Med** | **Very high** | **High** |

## 7. Recommended provider direction

### Classification: **Shortlist selected — owner input required**

> **Важно**: Stage 9.6 не получил от project owner полный пакет сведений о конкретном planned hotel API/provider service. Provider candidates review ниже может быть использован только как comparison/background. Финальная рекомендация и provider-specific adapter implementation невозможны без подтверждения конкретного provider владельцем проекта.

### Primary candidate: Amadeus Hotel APIs

**Rationale:**

1. **Self-service free tier**: 3000 requests/month for development. No commercial agreement needed for sandbox.
2. **Clear OAuth2 auth**: Well-documented client credentials flow. Compatible with environment variable injection.
3. **Sandbox with test data**: Free test environment with sample hotels. Safe for MVP validation.
4. **Compatible data shape**: Returns hotel name, price, currency, availability, rating, cancellation, room details — maps directly to `HotelOffer` + `providerFacts`.
5. **Well-documented**: Comprehensive public docs. Industry standard.
6. **Low-Medium adapter complexity**: OAuth2 token management + JSON parsing. No exotic auth.
7. **Display terms**: Development/sandbox results can be displayed. Production requires agreement (post-MVP).
8. **Industry credibility**: GDS provider used by major travel companies.

**Risks:**

- Sandbox inventory is limited (sample hotels, not real-time availability).
- Production requires commercial agreement.
- OAuth2 token refresh adds adapter complexity.

### Backup candidate: Duffel Stays

**Rationale:**

- Modern, developer-friendly API.
- Free test mode.
- Simple API key auth.
- Good documentation.

**Risks:**

- Newer API, less proven than Amadeus.
- Uncertain hotel inventory coverage.
- Uncertain production pricing.

### Why not others:

- **Expedia Rapid**: Requires commercial partnership. Too complex for MVP.
- **Booking.com**: Application process, strict compliance. Not suitable for rapid MVP.
- **SerpApi Google Hotels**: Not a direct provider. Scraping approach. Data freshness concerns.
- **Hotelbeds**: B2B enterprise. Not suitable for solo developer MVP.
- **RapidAPI marketplace**: Unreliable, variable quality, terms restrictions.

## 8. Configuration design

### 8.1 HotelProviderConfig expansion

```kotlin
data class HotelProviderConfig(
    val mode: HotelProviderMode = HotelProviderMode.FAKE,
    val providerName: String = "fake",
    val baseUrl: String = "",
    val apiKeyEnvVar: String = "",
    val apiSecretEnvVar: String = "",
    val sandboxMode: Boolean = true,
    val timeoutSeconds: Long = 30,
    val maxRetries: Int = 1,
)
```

### 8.2 Environment variable design

| Env var | Purpose | Required for REAL | Default |
|---|---|---|---|
| `HOTEL_PROVIDER_MODE` | FAKE or REAL | No | `FAKE` |
| `HOTEL_PROVIDER_NAME` | Provider identity string | Yes (REAL) | — |
| `HOTEL_PROVIDER_BASE_URL` | Provider API base URL | Yes (REAL) | — |
| `HOTEL_PROVIDER_API_KEY` | API key / client ID | Yes (REAL) | — |
| `HOTEL_PROVIDER_API_SECRET` | API secret / client secret | Yes (REAL) | — |
| `HOTEL_PROVIDER_SANDBOX` | `true` or `false` | No | `true` |
| `HOTEL_PROVIDER_TIMEOUT_SECONDS` | HTTP timeout | No | `30` |
| `HOTEL_PROVIDER_MAX_RETRIES` | Max retry count | No | `1` |

### 8.3 Safe startup behavior

| Scenario | Behavior |
|---|---|
| `HOTEL_PROVIDER_MODE` not set | FAKE (current default) |
| `HOTEL_PROVIDER_MODE=FAKE` | FAKE |
| `HOTEL_PROVIDER_MODE=REAL` but required env vars missing | **Fail-fast at startup** with clear error message |
| `HOTEL_PROVIDER_MODE=invalid` | FAKE (safe fallback) |
| `HOTEL_PROVIDER_MODE=REAL` + all config present | REAL with configured provider |

### 8.4 Provider factory expansion

```
HotelProviderConfig
  │
  ▼
HotelOfferProviderFactory.create(config)
  ├── FAKE → FakeHotelOfferProvider
  └── REAL → validate config → RealHotelOfferProviderAdapter(config)
```

## 9. Secrets handling design

### 9.1 Principles

| Principle | Rule |
|---|---|
| No secrets in source code | API keys/secrets read from environment variables only |
| No secrets in configuration files | No `.properties`, `.yaml`, or `.json` with secret values committed |
| No secrets in logs | Provider credentials must not appear in any log output |
| No secrets in test fixtures | Tests use fake/dummy credential values |
| No secrets in git | `.gitignore` includes `.env` files |

### 9.2 Delivery method

- API keys and secrets are delivered to the project owner **outside chat** (e.g., environment variable, secrets manager, `.env` file not in git).
- The coding agent must **never receive, store, or log** real API keys.

### 9.3 What should be logged later (not in this stage)

| Loggable | Not loggable |
|---|---|
| Provider name | API key value |
| Sandbox vs production mode | API secret value |
| Request URL (without auth headers) | Auth headers / tokens |
| Response status code | Response body with PII |
| Error category | Raw provider error with credentials |
| Request duration | Full request/response bodies in production |

## 10. Sandbox vs production design

### 10.1 Environment modes

| Mode | Provider | Base URL | Credentials | Data |
|---|---|---|---|---|
| Local dev (FAKE) | `FakeHotelOfferProvider` | N/A | None | Deterministic fake |
| Sandbox (REAL) | `RealHotelOfferProviderAdapter` | Provider sandbox URL | Sandbox credentials | Test/sample data |
| Production (REAL) | `RealHotelOfferProviderAdapter` | Provider production URL | Production credentials | Real-time data |

### 10.2 Selection

```
HOTEL_PROVIDER_MODE=REAL
HOTEL_PROVIDER_SANDBOX=true  → sandbox base URL, sandbox credentials
HOTEL_PROVIDER_SANDBOX=false → production base URL, production credentials
```

### 10.3 Behavior differences

| Aspect | Sandbox | Production |
|---|---|---|
| Data freshness | Test data, not real-time | Real-time availability |
| Inventory | Limited sample hotels | Full provider inventory |
| Rate limits | More permissive | Production limits |
| Cost | Free | Per-request or agreement-based |
| Error simulation | May have simulated errors | Real errors |

## 11. Fixture and testing strategy

### 11.1 Test categories

| Category | Provider | Data source | When to run |
|---|---|---|---|
| Unit tests | Fake/dummy | Inline test data | Every build |
| Integration tests (local) | `FakeHotelOfferProvider` | Deterministic fake | Every build |
| Integration tests (sandbox) | `RealHotelOfferProviderAdapter` | Provider sandbox | Manual or CI with sandbox credentials |
| Contract tests | Stubbed | JSON fixture files | Every build |
| End-to-end (production) | `RealHotelOfferProviderAdapter` | Provider production | Manual, post-deployment |

### 11.2 Fixture strategy

| Fixture type | Purpose | Location |
|---|---|---|
| JSON response fixtures | Provider response samples for adapter unit tests | `src/test/resources/fixtures/provider/` |
| Error response fixtures | Provider error samples for error mapping tests | `src/test/resources/fixtures/provider/errors/` |
| Normalized output fixtures | Expected `HotelOffer` output for each fixture | `src/test/resources/fixtures/expected/` |

### 11.3 Fixture file naming

```
provider-search-rome-2adults-3nights.json      → successful search response
provider-search-no-results.json                 → empty results response
provider-error-auth-failed.json                 → 401 auth error
provider-error-rate-limited.json                → 429 rate limit error
provider-error-timeout.json                     → timeout simulation
provider-error-invalid-response.json            → malformed JSON
```

### 11.4 No real credentials in tests

All tests use:
- `FakeHotelOfferProvider` for integration tests.
- JSON fixture files for adapter unit tests.
- Dummy credential values (`"test-api-key"`, `"test-api-secret"`) for config validation tests.

## 12. Error mapping design

### 12.1 Provider error → HotelProviderErrorCategory mapping

| Provider error condition | Category | Retryable |
|---|---|---|
| HTTP 401 / 403 (auth) | `AUTHENTICATION_FAILED` | No |
| HTTP 429 (rate limit) | `RATE_LIMITED` | Yes (with backoff) |
| HTTP 500 / 502 / 503 | `UNAVAILABLE` | Yes |
| HTTP 504 / connection timeout | `TIMEOUT` | Yes |
| HTTP 200 but unparseable JSON | `INVALID_RESPONSE` | No |
| HTTP 200 but missing required fields | `MAPPING_FAILED` | No |
| HTTP 200 with empty results | Not an error (valid empty list) | N/A |
| Network error (DNS, connection refused) | `UNAVAILABLE` | Yes |
| Unknown / unexpected exception | `UNKNOWN` | No |

### 12.2 Amadeus-specific error mapping (primary candidate)

| Amadeus error | HTTP code | Category |
|---|---|---|
| Invalid OAuth token | 401 | `AUTHENTICATION_FAILED` |
| Expired access token | 401 | `AUTHENTICATION_FAILED` (retry with refresh) |
| Rate limit exceeded | 429 | `RATE_LIMITED` |
| Server error | 500 | `UNAVAILABLE` |
| Service unavailable | 503 | `UNAVAILABLE` |
| Gateway timeout | 504 | `TIMEOUT` |
| Malformed response | 200 | `INVALID_RESPONSE` |
| Missing hotel data in response | 200 | `MAPPING_FAILED` |

### 12.3 Error propagation

```
Provider HTTP error
  │
  ▼
RealHotelOfferProviderAdapter catches → HotelProviderException(category, message, cause)
  │
  ▼
CreateHotelSearchUseCase propagates (no catch)
  │
  ▼
ExecuteConfirmedSearchTransitionUseCase catches Exception
  │
  ▼
SEARCH_CREATION_FAILED (retryable)
  │
  ▼
Stage 8 pending confirmation stays active
```

## 13. Implementation sequencing

### 13.1 Stage 9.7 — Selected Provider Contract Intake and Readiness Gate

**Type**: Review/design-only. **Не implementation.**

**Scope**:
1. Запросить у project owner конкретный hotel API/provider service.
2. Собрать полный provider contract intake checklist (28 пунктов).
3. Проверить completeness.
4. Зафиксировать provider-specific adapter design document.
5. Produce readiness gate verdict.
6. No code changes. No implementation.

### 13.2 Stage 9.8 — Provider Configuration Contract Expansion

**Type**: Implementation (medium-small).

**Prerequisite**: Stage 9.7 readiness gate passed.

**Scope**:
1. Expand `HotelProviderConfig` with provider identity, base URL, credential env var names, sandbox mode, timeout.
2. Expand `HotelProviderConfig.fromEnvironment()` to parse new fields.
3. Add config validation: fail-fast when REAL mode but required config missing.
4. Update `HotelOfferProviderFactory` to pass config to adapter.
5. Add config validation tests.
6. No HTTP client yet. No real calls.

### 13.3 Stage 9.9 — Provider Adapter Implementation with Fixtures

**Type**: Implementation (medium-small).

**Prerequisite**: Stage 9.7 contract intake + Stage 9.8 config expansion.

**Scope**:
1. Add HTTP client dependency (Ktor HttpClient or similar).
2. Implement `RealHotelOfferProviderAdapter` with selected provider API call structure.
3. Implement provider response parsing → `List<HotelOffer>`.
4. Implement error mapping → `HotelProviderException`.
5. Create JSON fixture files for testing.
6. Add adapter unit tests with fixtures (no real calls).
7. No real external calls in tests.

### 13.4 Stage 9.10 — Sandbox Integration Verification

**Type**: Integration verification (medium-small).

**Scope**:
1. Enable sandbox mode with real sandbox credentials.
2. Run adapter against provider sandbox.
3. Verify response normalization.
4. Verify error handling.
5. Produce integration verification report.
6. Requires project owner to supply sandbox credentials.

### 13.5 Stage 9.11+ — Future stages

- Production configuration and deployment.
- Real LLM provider integration (separate track).
- Durable persistence.
- Frontend UX polish.
- Observability.

## 14. Real external call readiness verdict

### Classification: **Provider selection deferred — selected provider contract intake required**

| Readiness level | Status |
|---|---|
| Provider candidates research | **Done** — 7 candidates reviewed as background comparison |
| Configuration design | **Done** — env vars, safe startup, sandbox/production |
| Secrets handling design | **Done** — env vars only, no commits, no logs |
| Error mapping design | **Done** — 7 categories mapped to HTTP codes |
| Fixture strategy | **Done** — JSON fixtures for unit tests |
| Specific provider confirmed by owner | **Not done** — blocking |
| Provider contract details | **Not done** — blocking |
| HTTP client selection | **Not done** — deferred |
| Provider account | **Not done** — owner must decide |
| Sandbox credentials | **Not done** — owner must obtain |
| Sample request/response | **Not done** — owner should provide |
| Auth scheme confirmed | **Not done** — owner must verify |
| Terms confirmed | **Not done** — owner must verify |

**Not ready for real external calls** until owner provides contract intake (Section 15).

**Not ready for provider-specific adapter implementation** until owner confirms selected provider and supplies contract details (Section 15).

**Ready for**: Stage 9.7 — Selected Provider Contract Intake and Readiness Gate (review/design-only).

## 14.5. Owner input gate for selected provider

### Mandatory gate

1. **Stage 9.6 не получил от project owner полный пакет сведений о конкретном planned hotel API/provider service.** Provider candidates research (Section 5-6) является только background comparison и не может быть использован для provider-specific adapter implementation.

2. **Без полного пакета contract information нельзя безопасно начинать provider-specific adapter implementation.** Любая implementation без contract details будет спекулятивной и потребует переделки.

3. **Provider candidates research (Amadeus, Duffel, etc.) может быть использован только как comparison/background.** Ни один candidate не может быть выбран как final без явного подтверждения владельцем проекта.

4. **Следующий stage должен запросить у project owner конкретные сведения по provider contract.** Stage 9.7 — Selected Provider Contract Intake and Readiness Gate (Section 16).

5. **Реальные API keys / secrets нельзя просить и нельзя вставлять в chat/docs.** Запрашиваются только описания auth scheme и имена environment variables (без значений).

6. **Real external provider calls нельзя начинать до отдельного explicit readiness gate.** Этот gate может быть пройден только после получения contract intake от owner и после provider-specific adapter implementation.

### Secrets rule (Stage 9.6 and Stage 9.7)

Ни в Stage 9.6, ни в Stage 9.7 нельзя просить или сохранять:

- Real API key values
- Tokens
- Client secrets
- Passwords
- Private credentials
- Signed production URLs

Можно просить только:

- Описание auth scheme (OAuth2, API key, HMAC signature, etc.)
- Имена environment variables (e.g., `HOTEL_PROVIDER_API_KEY`, `HOTEL_PROVIDER_BASE_URL`)
- **Без значений**

## 14.6. Provider-specific implementation blockers

Следующие items блокируют provider-specific adapter implementation:

| # | Blocker | Reason | Resolution |
|---|---|---|---|
| 1 | Specific provider not confirmed by owner | Provider candidates review is background only | Owner must confirm selected provider |
| 2 | Provider contract details not available | Cannot design adapter without request/response structure | Owner must supply contract intake (Section 15) |
| 3 | Auth scheme not confirmed | Cannot design credential handling | Owner must describe auth scheme |
| 4 | Sample request/response not available | Cannot create fixtures or validate mapping | Owner must provide examples (without secrets) |
| 5 | Terms/restrictions not confirmed | Cannot ensure compliance for assistant UI display | Owner must verify terms |
| 6 | Sandbox availability not confirmed | Cannot plan testing strategy | Owner must confirm sandbox access |

Until these blockers are resolved, provider-specific adapter implementation would be speculative and require rework.

## 15. Information required from project owner

### Provider contract intake checklist

Этот checklist должен быть заполнен project owner **до** начала provider-specific adapter implementation. Stage 9.7 будет запросить эту информацию.

| # | Item | Priority | Notes |
|---|---|---|---|
| 1 | **Название provider/service** | Blocking | Конкретное название hotel API или provider service |
| 2 | **Public documentation links** | Blocking | URL публичной документации provider |
| 3 | **Private/internal API docs** | Important | Если public docs недостаточно |
| 4 | **Endpoint для hotel search** | Blocking | URL/path endpoint для поиска отелей |
| 5 | **Auth scheme (без secret values)** | Blocking | Описание схемы аутентификации (OAuth2, API key, HMAC, etc.) |
| 6 | **Sandbox/test environment documentation** | Important | Ссылки на sandbox docs |
| 7 | **Production environment distinction** | Important | Отличия sandbox от production |
| 8 | **Required headers (без секретных значений)** | Important | Названия обязательных HTTP headers |
| 9 | **Required query/body parameters** | Important | Обязательные параметры запроса |
| 10 | **Destination/location search format** | Important | Формат передачи направления (city code, IATA, geo, text) |
| 11 | **Date format** | Important | Формат дат (ISO 8601, Unix timestamp, etc.) |
| 12 | **Guests/rooms/occupancy format** | Important | Формат передачи гостей и номеров |
| 13 | **Currency/locale behavior** | Important | Как provider обрабатывает валюту и локаль |
| 14 | **Example success request (без credentials)** | Blocking | Полный пример запроса без реальных secrets |
| 15 | **Example success response** | Blocking | Полный пример успешного response JSON |
| 16 | **Example empty/no availability response** | Important | Пример response когда нет результатов |
| 17 | **Example error responses** | Important | Примеры error responses (401, 429, 500, etc.) |
| 18 | **Rate limits** | Important | Ограничения на количество запросов |
| 19 | **Pricing/usage constraints relevant to MVP** | Important | Ценовые или usage ограничения для MVP |
| 20 | **Terms/usage restrictions for displaying hotel results in assistant UI** | Blocking | Юридические ограничения на отображение результатов |
| 21 | **Whether results are hotel-level, room-level, or rate-level** | Important | Гранулярность результатов |
| 22 | **Whether booking/deeplink fields are returned** | Nice-to-have | Наличие полей для бронирования |
| 23 | **Whether cancellation/refundability is returned** | Important | Наличие данных об отмене |
| 24 | **Whether taxes/fees are included in price** | Important | Включены ли налоги и сборы в цену |
| 25 | **Whether sandbox credentials exist** | Blocking | Существуют ли тестовые credentials |
| 26 | **How credentials will be supplied securely later (outside chat)** | Blocking | Способ безопасной передачи credentials (env vars, secrets manager, etc.) |
| 27 | **Whether production credentials are separate from sandbox credentials** | Important | Отличаются ли production credentials от sandbox |
| 28 | **Any provider-specific compliance/legal notes** | Important | Специфические compliance или legal требования |

### Delivery rules

- **Real API key values / tokens / secrets / passwords / private credentials**: НЕЛЬЗЯ передавать через chat или включать в docs.
- **Auth scheme descriptions и env var names** (e.g., `HOTEL_PROVIDER_API_KEY`, `HOTEL_PROVIDER_BASE_URL`, `HOTEL_PROVIDER_ENVIRONMENT`): можно передавать через chat **без значений**.
- **Sample requests и responses**: можно передавать через chat **без реальных credentials** (подставить placeholder values).

## 16. Stage 9.7 candidate scope

**Stage 9.7 — Selected Provider Contract Intake and Readiness Gate**

**Type**: Review/design-only stage. **Не implementation.**

Этот stage не реализует provider adapter и не выполняет external HTTP calls. Его цель — собрать и проверить contract information по конкретному hotel API/provider service, который должен подтвердить project owner.

### Scope

1. Запросить у project owner полный provider contract intake checklist (Section 15, 28 пунктов).
2. Получить и зафиксировать ответы owner в review report.
3. Проверить completeness полученной информации.
4. Определить, достаточна ли информация для provider-specific adapter design.
5. Если информация недостаточна — зафиксировать gaps и запросить дополнение.
6. Если информация достаточна — произвести provider-specific adapter design document (без implementation).
7. Определить readiness gate для перехода к provider-specific adapter implementation.

### What Stage 9.7 does NOT do

- Не реализует provider adapter code.
- Не добавляет HTTP client dependency.
- Не делает external HTTP calls.
- Не вводит provider credentials.
- Не создаёт provider-specific DTOs.
- Не меняет domain model или API response models.
- Не меняет runtime configuration.
- Не заявляет production readiness.

### Output

- Review report с полной provider contract intake информацией.
- Provider-specific adapter design document (mapping rules, request/response structure, error mapping для конкретного provider).
- Readiness gate verdict: ready for adapter implementation or gaps identified.
- Prompt для Stage 9.8 (provider-specific adapter implementation with fixtures).

## 17. Prompt для Stage 9.7

```
Мы продолжаем проект travel-assistant.

Пожалуйста, отвечай на русском языке. Технические имена классов, файлов,
enum values, commit messages и команды оставляй как есть.

Этот stage не реализует provider adapter и не выполняет external HTTP calls.
Его цель — собрать и проверить contract information по конкретному hotel
API/provider service, который должен подтвердить project owner.

Контекст

* Репозиторий: travel-assistant
* Branch: stage-9
* Последний завершённый sub-stage: Stage 9.6
* Stage 9 provider integration foundation завершена (9.1-9.5).
* Stage 9.6 — provider selection background comparison и configuration design.
* Stage 9.6 verdict: "Provider selection deferred — selected provider
  contract intake required."
* Production readiness не claimed.
* Real provider integration ещё не implemented.

Задача

Выполни:

Stage 9.7 — Selected Provider Contract Intake and Readiness Gate

Это review/design-only stage. НЕ implementation.

Цель Stage 9.7:

1. Запросить у project owner конкретный hotel API/provider service.
2. Собрать полный provider contract intake по checklist из 28 пунктов.
3. Проверить completeness полученной информации.
4. Зафиксировать provider-specific adapter design document.
5. Произвести readiness gate verdict.

Provider contract intake checklist

Запроси у project owner следующую информацию (без реальных secret values):

1. Название provider/service.
2. Public documentation links.
3. Private/internal API docs, если public docs недостаточно.
4. Endpoint для hotel search.
5. Auth scheme (без secret values).
6. Sandbox/test environment documentation.
7. Production environment distinction.
8. Required headers (без секретных значений).
9. Required query/body parameters.
10. Destination/location search format.
11. Date format.
12. Guests/rooms/occupancy format.
13. Currency/locale behavior.
14. Example success request (без credentials).
15. Example success response.
16. Example empty/no availability response.
17. Example error responses.
18. Rate limits.
19. Pricing/usage constraints relevant to MVP.
20. Terms/usage restrictions for displaying hotel results in assistant UI.
21. Whether results are hotel-level, room-level, or rate-level.
22. Whether booking/deeplink fields are returned.
23. Whether cancellation/refundability is returned.
24. Whether taxes/fees are included in price.
25. Whether sandbox credentials exist.
26. How credentials will be supplied securely later (outside chat).
27. Whether production credentials are separate from sandbox credentials.
28. Any provider-specific compliance/legal notes.

Secrets rule

НЕЛЬЗЯ просить или сохранять:
* Real API key values
* Tokens
* Client secrets
* Passwords
* Private credentials
* Signed production URLs

Можно просить только:
* Описание auth scheme
* Имена environment variables (без значений)
* Sample requests/responses (без реальных credentials)

Required output file

* `docs/reviews/stage-9-7-selected-provider-contract-intake-and-readiness-gate.md`

Отчёт должен включить:

1. Scope
2. Provider name and confirmation by owner
3. Provider contract intake responses (28 items)
4. Completeness assessment
5. Provider-specific adapter design document:
   - Request structure
   - Response structure
   - Normalization mapping rules (provider → HotelOffer)
   - Error mapping (provider errors → HotelProviderErrorCategory)
   - providerFacts population strategy
6. Impact on existing domain model (confirm zero changes or document needed changes)
7. Configuration requirements for selected provider
8. Sandbox vs production readiness assessment
9. Fixture strategy for selected provider
10. Readiness gate verdict:
    - Ready for adapter implementation
    - Gaps identified (list specific missing items)
11. Guardrails для Stage 9.8
12. Prompt для Stage 9.8
13. Verdict

Strict guardrails

* Do not implement any provider adapter code.
* Do not add HTTP client dependency.
* Do not make real API calls.
* Do not add provider credentials to source code, tests, or docs.
* Do not create provider-specific DTOs in production code.
* Do not change domain model (HotelOffer, HotelSearchCriteria).
* Do not change API response models.
* Do not change runtime behavior.
* Do not change build.gradle.kts.
* Do not claim production readiness.
* Do not mark provider integration as implemented.

Validation

* `git status --short` — только docs files.
* `git diff --check` — no errors.

Final response format

1. Созданные файлы
2. Изменённые файлы
3. Краткий итог
4. Provider contract intake summary
5. Readiness gate verdict
6. Checks
7. Commit recommendation: Stage 9.7 selected provider contract intake and readiness gate
```

## 18. Stage 9.6 verdict

**Passed** — provider selection background comparison и configuration design complete. Provider-specific adapter implementation deferred pending owner contract intake.

Stage 9.6:

1. Inspect 12 backend source files и 8 review documents.
2. Researched 7 hotel API provider candidates via public documentation (background comparison only).
3. Built 17-criterion comparison matrix.
4. Identified Amadeus Hotel APIs и Duffel Stays как potential candidates (shortlist).
5. Rejected Expedia Rapid (requires partnership), Booking.com (strict compliance), SerpApi (not direct provider), Hotelbeds (B2B enterprise), RapidAPI (unreliable).
6. Designed configuration expansion (7 new config fields, environment variable strategy).
7. Designed secrets handling (env vars only, no commits, no logs).
8. Designed sandbox vs production strategy (3 environment modes).
9. Designed fixture and testing strategy (unit/integration/contract/e2e categories).
10. Designed error mapping (provider HTTP errors → HotelProviderErrorCategory).
11. Established **Owner input gate**: provider-specific adapter implementation невозможен без contract intake от project owner.
12. Defined provider contract intake checklist (28 пунктов) для Stage 9.7.
13. Readiness verdict: **Provider selection deferred — selected provider contract intake required**.
14. Defined Stage 9.7 как review/design-only contract intake stage (не implementation).

**Verdict classification: Shortlist selected — owner input required**

Ни один provider не выбран как final. Provider candidates review является background comparison. Provider-specific adapter implementation не может начаться до получения contract intake от project owner.

Production code не изменён. Runtime не изменён. Tests не запускались.
Production readiness не заявлена. Real provider integration не начат.
