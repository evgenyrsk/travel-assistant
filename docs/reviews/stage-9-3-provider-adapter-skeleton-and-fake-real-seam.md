# Stage 9.3 — Provider Adapter Skeleton and Fake-vs-Real Seam

## 1. Scope

Stage 9.3 — implementation sub-stage (medium-small, bounded).

Stage 9.3:

1. Добавляет provider adapter skeleton, реализующий существующий `HotelOfferProviderBoundary`.
2. Добавляет минимальную provider configuration model для выбора fake vs real provider mode.
3. Подключает provider selection в application composition так, что `FakeHotelOfferProvider` остаётся default.
4. Добавляет real-provider skeleton path, который explicit и safe.
5. Гарантирует, что real-provider skeleton не выполняет external HTTP calls и не требует credentials.
6. Сохраняет всё существующее Stage 7 и Stage 8 behavior по умолчанию.
7. Добавляет targeted tests.
8. Производит Stage 9.3 review report и prompt для Stage 9.4.

## 2. Files changed

### 2.1 New source files

| File | Package | Purpose |
|---|---|---|
| `HotelProviderMode.kt` | `infrastructure.provider` | Enum: FAKE, REAL |
| `HotelProviderConfig.kt` | `infrastructure.provider` | Config data class с mode, safe env parsing |
| `RealHotelOfferProviderAdapter.kt` | `infrastructure.provider` | Skeleton adapter, returns `emptyList()` |
| `HotelOfferProviderFactory.kt` | `infrastructure.provider` | Factory: config → provider instance |

### 2.2 Modified source files

| File | Change |
|---|---|
| `Application.kt` | Uses `HotelProviderConfig` + `HotelOfferProviderFactory`; `moduleWithAssistantLlm` accepts `providerConfig` parameter with FAKE default |

### 2.3 New test files

| File | Tests |
|---|---|
| `RealHotelOfferProviderAdapterTest.kt` | 2 tests: returns emptyList() for different criteria |
| `HotelProviderConfigTest.kt` | 3 tests: default FAKE, explicit REAL, fromEnvironment fallback |
| `HotelOfferProviderFactoryTest.kt` | 3 tests: FAKE→FakeHotelOfferProvider, REAL→RealHotelOfferProviderAdapter, default→FAKE |

## 3. Implementation summary

### 3.1 HotelProviderMode

```kotlin
enum class HotelProviderMode {
    FAKE,
    REAL,
}
```

Simple enum with two modes. FAKE is the safe default.

### 3.2 HotelProviderConfig

```kotlin
data class HotelProviderConfig(
    val mode: HotelProviderMode = HotelProviderMode.FAKE,
)
```

- Default constructor: FAKE mode.
- `fromEnvironment()`: reads `HOTEL_PROVIDER_MODE` env var, parses case-insensitively, falls back to FAKE on any error or missing value.
- No credentials, no secrets, no HTTP config.

### 3.3 RealHotelOfferProviderAdapter

```kotlin
class RealHotelOfferProviderAdapter : HotelOfferProviderBoundary {
    override fun search(criteria: HotelSearchCriteria): List<HotelOffer> = emptyList()
}
```

- Implements `HotelOfferProviderBoundary`.
- No I/O, no network, no credentials, no HTTP client.
- Returns `emptyList()` as safe placeholder.
- Real provider execution is future work (Stage 9.4+).

### 3.4 HotelOfferProviderFactory

```kotlin
object HotelOfferProviderFactory {
    fun create(config: HotelProviderConfig): HotelOfferProviderBoundary =
        when (config.mode) {
            HotelProviderMode.FAKE -> FakeHotelOfferProvider()
            HotelProviderMode.REAL -> RealHotelOfferProviderAdapter()
        }
}
```

- Exhaustive `when` on enum — no fallback needed.
- Returns domain-typed `HotelOfferProviderBoundary`.

### 3.5 Application.kt changes

- `module()` creates `HotelProviderConfig.fromEnvironment()` and passes to `moduleWithAssistantLlm`.
- `moduleWithAssistantLlm()` accepts `providerConfig: HotelProviderConfig = HotelProviderConfig()` (FAKE default).
- `CreateHotelSearchUseCase` uses `HotelOfferProviderFactory.create(providerConfig)` instead of hardcoded `FakeHotelOfferProvider()`.
- All existing tests that call `moduleWithAssistantLlm(llmClient)` without `providerConfig` get FAKE by default — no test changes needed.

## 4. Provider seam design

```
HotelProviderConfig (mode: FAKE | REAL)
  │
  ▼
HotelOfferProviderFactory.create(config)
  │
  ├── FAKE → FakeHotelOfferProvider (existing, deterministic)
  │
  └── REAL → RealHotelOfferProviderAdapter (skeleton, emptyList())
      │
      ▼
HotelOfferProviderBoundary.search(criteria): List<HotelOffer>
      │
      ▼
CreateHotelSearchUseCase (unchanged)
      │
      ▼
HotelOfferRanker (unchanged)
      │
      ▼
HotelSearch (unchanged)
```

Key properties:

- Domain layer (`HotelOfferProviderBoundary`, `HotelOffer`, `HotelSearchCriteria`) unchanged.
- Application layer (`CreateHotelSearchUseCase`, `HotelSearchBoundary`) unchanged.
- Stage 8 `ExecuteConfirmedSearchTransitionUseCase` unchanged.
- Stage 7 `AssistantHotelSearchHandoffUseCase` unchanged.
- API response models unchanged.
- `show_hotel_results` response shape unchanged.
- `markConsumed` behavior unchanged.

## 5. Default behavior preservation

| Behavior | Status |
|---|---|
| `module()` default → FAKE provider | Preserved |
| `moduleWithAssistantLlm(llmClient)` without config → FAKE | Preserved (default parameter) |
| `FakeHotelOfferProvider` returns 2 deterministic offers | Preserved |
| Stage 7 strict handoff creates search with fake offers | Preserved |
| Stage 8 confirmation lifecycle → fake provider search | Preserved |
| `show_hotel_results` + `hotelSearchId` response | Preserved |
| `markConsumed` after success | Preserved |
| `FAILED(SEARCH_CREATION_FAILED)` on provider exception | Preserved |
| Existing tests pass without modification | Confirmed (`./gradlew test` passes) |

## 6. Real adapter skeleton behavior

| Aspect | Value |
|---|---|
| Implements | `HotelOfferProviderBoundary` |
| `search()` returns | `emptyList()` |
| External I/O | None |
| HTTP client | None |
| Credentials | None |
| Network calls | None |
| Provider-specific DTOs | None |
| Error handling | Not applicable (no calls) |
| Thread safety | Stateless, safe |

Real adapter skeleton produces `COMPLETED_NO_OFFERS` search status when used
(because `rankedOffers.isEmpty()` is true → `HotelSearch.Status.COMPLETED_NO_OFFERS`).

## 7. Configuration behavior

| Scenario | Config | Provider | Result |
|---|---|---|---|
| Default (no env) | `HotelProviderConfig()` | `FakeHotelOfferProvider` | 2 deterministic offers |
| `HOTEL_PROVIDER_MODE=fake` | FAKE | `FakeHotelOfferProvider` | 2 deterministic offers |
| `HOTEL_PROVIDER_MODE=FAKE` | FAKE | `FakeHotelOfferProvider` | 2 deterministic offers |
| `HOTEL_PROVIDER_MODE=real` | REAL | `RealHotelOfferProviderAdapter` | `emptyList()` → `COMPLETED_NO_OFFERS` |
| `HOTEL_PROVIDER_MODE=REAL` | REAL | `RealHotelOfferProviderAdapter` | `emptyList()` → `COMPLETED_NO_OFFERS` |
| `HOTEL_PROVIDER_MODE=invalid` | FAKE (fallback) | `FakeHotelOfferProvider` | 2 deterministic offers |
| `HOTEL_PROVIDER_MODE=` (empty) | FAKE (fallback) | `FakeHotelOfferProvider` | 2 deterministic offers |
| Env var not set | FAKE (fallback) | `FakeHotelOfferProvider` | 2 deterministic offers |

Safe-by-default: any unrecognized or missing configuration value results in FAKE mode.

## 8. Tests added/updated

### 8.1 New tests

| Test class | Test count | What is verified |
|---|---|---|
| `RealHotelOfferProviderAdapterTest` | 2 | Returns `emptyList()` for different criteria without external calls |
| `HotelProviderConfigTest` | 3 | Default mode is FAKE; explicit REAL preserved; `fromEnvironment()` falls back to FAKE |
| `HotelOfferProviderFactoryTest` | 3 | FAKE creates `FakeHotelOfferProvider`; REAL creates `RealHotelOfferProviderAdapter`; default creates FAKE |

### 8.2 Existing tests

All existing tests pass without modification:

- `CreateHotelSearchUseCaseTest` — uses `FakeHotelOfferProvider()` directly.
- `FakeLlmClientTest` — LLM fake, unrelated.
- All `api/` route tests — use `moduleWithAssistantLlm(llmClient)` with default FAKE provider.
- All `application/assistant/` tests — use direct use case construction.
- Stage 7/Stage 8 compatibility tests — preserved.

### 8.3 Test execution

```
./gradlew test --no-daemon
BUILD SUCCESSFUL
```

## 9. Validation results

| Command | Result |
|---|---|
| `./gradlew test --no-daemon` | BUILD SUCCESSFUL (all tests pass) |
| `git status --short` | 1 modified (Application.kt) + 4 new source + 1 new test directory |
| `git diff --check` | No errors |
| Forbidden pattern search (HttpClient, credentials, secret, apiKey, password, token) | No matches in new provider files |

## 10. Guardrails upheld

| Guardrail | Status |
|---|---|
| No external HTTP calls | **Pass** — no HTTP client, no network dependency |
| No real hotel API provider selection | **Pass** — only mode enum |
| No API credentials | **Pass** — no secrets handling |
| No environment-secret handling beyond safe mode selector | **Pass** — only `HOTEL_PROVIDER_MODE` string parsed |
| No network dependencies | **Pass** — no new dependencies added |
| No dependency changes | **Pass** — `build.gradle.kts` unchanged |
| No persistence changes | **Pass** |
| No frontend changes | **Pass** |
| No OpenAPI contract changes | **Pass** |
| No generated clients | **Pass** |
| No booking flow | **Pass** |
| No real provider response parsing | **Pass** — `emptyList()` |
| No provider-specific DTOs | **Pass** |
| No changes to `HotelSearchCriteria` | **Pass** |
| No changes to `HotelOffer` | **Pass** |
| No changes to `HotelOfferProviderBoundary` | **Pass** |
| No changes to `show_hotel_results` response shape | **Pass** |
| No changes to `markConsumed` behavior | **Pass** |
| No changes to Stage 8 attempt lifecycle | **Pass** |
| No production readiness claim | **Pass** |
| Real provider integration not marked completed | **Pass** |

## 11. Known non-production limitations

| Limitation | Category | Future stage |
|---|---|---|
| Real adapter returns `emptyList()` — no real data | Implementation gap | Stage 9.4+ |
| No HTTP client for real provider calls | Infrastructure | Stage 9.4+ |
| No provider credentials injection | Security | Future infrastructure stage |
| No provider error taxonomy | Error handling | Stage 9.4 |
| No provider-specific retry policy | Reliability | Stage 9.4+ |
| No provider response normalization | Mapping | Stage 9.4+ |
| `COMPLETED_NO_OFFERS` for real mode — no offers surfaced | Expected skeleton behavior | Stage 9.4+ |
| InMemory stores for all state | Carryover from Stage 8 | Future infrastructure |
| FakeLlmClient deterministic | Carryover from Stage 8 | Future LLM integration |
| Static message text | Carryover from Stage 8 | Future UX work |

## 12. Stage 9.4 candidate scope

**Stage 9.4 — Provider Error Taxonomy and Error Handling**

Это implementation sub-stage.

Scope:

1. Design и implement provider error types:
   - `ProviderException` sealed hierarchy (network error, authentication error, rate limited, no results, partial results, timeout, unknown).
2. Update `RealHotelOfferProviderAdapter` skeleton to throw typed errors (currently returns `emptyList()`).
3. Design error mapping from future provider-specific errors to `ProviderException`.
4. Evaluate whether `CreateHotelSearchUseCase` or `ExecuteConfirmedSearchTransitionUseCase` need error classification (currently catches generic `Exception` as `SEARCH_CREATION_FAILED`).
5. Add tests for error taxonomy.
6. Preserve Stage 8 attempt lifecycle behavior.
7. Produce Stage 9.5 prompt.

Out of scope:

- Real external API calls.
- Provider credentials or HTTP client.
- Domain model changes.
- API response model changes.
- Stage 8 lifecycle changes.
- Frontend, persistence, auth, observability, deployment.

## 13. Prompt для Stage 9.4

```
Мы продолжаем проект travel-assistant.

Пожалуйста, отвечай на русском языке. Технические имена классов, файлов,
enum values, commit messages и команды оставляй как есть.

Контекст

* Репозиторий: travel-assistant
* Branch: stage-9
* Последний завершённый sub-stage: Stage 9.3
* Stage 9 runtime implementation ещё не завершён.
* Production readiness не claimed.

Текущая provider seam (после Stage 9.3)

* `HotelProviderMode` — enum: FAKE, REAL.
* `HotelProviderConfig` — data class с mode, safe env parsing, default FAKE.
* `HotelOfferProviderFactory` — создаёт provider по config.
* `FakeHotelOfferProvider` — deterministic, default, accepted carryover.
* `RealHotelOfferProviderAdapter` — skeleton, returns emptyList(), no I/O.
* `HotelOfferProviderBoundary` — fun interface: search(HotelSearchCriteria): List<HotelOffer>.
* `CreateHotelSearchUseCase` — session check → provider.search() → rank → save.
* `ExecuteConfirmedSearchTransitionUseCase` — catches Exception as SEARCH_CREATION_FAILED.

Stage 9.2 provider-neutral result contract

* Empty result = emptyList() (valid business outcome).
* Error = exception (caught by Stage 8 attempt lifecycle).
* Provider-specific data → providerFacts.
* Adapter normalizes: price → total stay, rating → 0-10, availability → AVAILABLE/LIMITED/UNKNOWN.

Задача

Выполни:

Stage 9.4 — Provider Error Taxonomy and Error Handling

Это implementation sub-stage.

Цели Stage 9.4

1. Design и implement provider error types:
   - ProviderException sealed hierarchy (network error, authentication error, rate limited, no results, partial results, timeout, unknown).
2. Update RealHotelOfferProviderAdapter skeleton to throw typed errors.
3. Design error mapping from future provider-specific errors to ProviderException.
4. Evaluate whether CreateHotelSearchUseCase or ExecuteConfirmedSearchTransitionUseCase need error classification beyond current generic Exception catch.
5. Add tests for error taxonomy.
6. Preserve Stage 8 attempt lifecycle behavior.
7. Produce Stage 9.5 prompt.

Required output files

Source files (new or modified):
* Provider exception hierarchy in infrastructure/provider/
* Updated RealHotelOfferProviderAdapter (if safe)
* Tests for error taxonomy

Review report:
* docs/reviews/stage-9-4-provider-error-taxonomy-and-error-handling.md

Strict guardrails

* No real external API calls.
* No provider credentials or HTTP client.
* No domain model changes (HotelOffer, HotelSearchCriteria).
* No API response model changes.
* No changes to HotelOfferProviderBoundary interface.
* Preserve Stage 8 attempt lifecycle: generic Exception still caught as SEARCH_CREATION_FAILED.
* Preserve Stage 7 strict handoff.
* All existing tests must pass.
* No production readiness claim.

Validation

* `./gradlew test --no-daemon` passes.
* `git status --short` — expected files only.
* `git diff --check` — no errors.

Final response format

1. Созданные файлы
2. Изменённые файлы
3. Краткий итог
4. Checks
5. Commit recommendation: Stage 9.4 provider error taxonomy and error handling
```

## 14. Stage 9.3 verdict

**Passed** — provider adapter skeleton and fake-vs-real seam implemented.

Stage 9.3:

1. Создал `HotelProviderMode` enum (FAKE, REAL).
2. Создал `HotelProviderConfig` data class (safe env parsing, FAKE default).
3. Создал `RealHotelOfferProviderAdapter` skeleton (implements `HotelOfferProviderBoundary`, returns `emptyList()`, no I/O).
4. Создал `HotelOfferProviderFactory` (exhaustive `when` on mode enum).
5. Обновил `Application.kt`: `moduleWithAssistantLlm` принимает `providerConfig` parameter; default FAKE.
6. Добавил 8 targeted tests (2 adapter + 3 config + 3 factory).
7. Все existing tests pass без модификаций (`./gradlew test` — BUILD SUCCESSFUL).
8. Domain model, API response, Stage 7/Stage 8 behavior — unchanged.
9. No external HTTP, no credentials, no network, no dependency changes.
10. Production readiness не claimed.
