# Stage 9.4 — Provider Error Taxonomy and Error Handling

## 1. Scope

Stage 9.4 — implementation sub-stage (medium-small, bounded).

Stage 9.4:

1. Определяет provider-level error taxonomy для future real hotel provider adapters.
2. Реализует minimal provider exception/error types.
3. Гарантирует совместимость provider failures с существующим `CreateHotelSearchUseCase` / Stage 8 confirmation failure behavior.
4. Добавляет targeted tests для provider error taxonomy и propagation.
5. Сохраняет default fake provider behavior.
6. Сохраняет current REAL skeleton behavior (no I/O, `emptyList()`).
7. Не добавляет real HTTP calls, credentials, provider-specific DTOs или external dependencies.

## 2. Files changed

### 2.1 New source files

| File | Package | Purpose |
|---|---|---|
| `HotelProviderErrorCategory.kt` | `infrastructure.provider` | Provider-neutral error category enum (7 values) |
| `HotelProviderException.kt` | `infrastructure.provider` | Provider exception with category + message + optional cause |

### 2.2 New test files

| File | Tests |
|---|---|
| `HotelProviderErrorCategoryTest.kt` | 2 tests: enum contains expected values, count is stable |
| `HotelProviderExceptionTest.kt` | 5 tests: category/message, category/message/cause, RuntimeException base, propagation through CreateHotelSearchUseCase, all categories usable |

### 2.3 Modified files

| File | Change |
|---|---|
| `docs/reviews/README.md` | Stage 9.4 entry |
| `docs/roadmap/roadmap.md` | Stage 9.4 completion, next step → 9.5 |

No existing source or test files were modified.

## 3. Implementation summary

### 3.1 HotelProviderErrorCategory

```kotlin
enum class HotelProviderErrorCategory {
    UNAVAILABLE,
    TIMEOUT,
    RATE_LIMITED,
    AUTHENTICATION_FAILED,
    INVALID_RESPONSE,
    MAPPING_FAILED,
    UNKNOWN,
}
```

Provider-neutral, no vendor-specific values. Safe for future adapters.

### 3.2 HotelProviderException

```kotlin
class HotelProviderException(
    val category: HotelProviderErrorCategory,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
```

- Extends `RuntimeException` (consistent with `AssistantSessionNotFoundException`, `HotelSearchNotFoundException`).
- Carries `category` (typed error classification).
- Carries `message` (safe, no raw credentials or secrets).
- Carries optional `cause` (original provider exception).
- No raw credentials, API keys, or provider-specific data.

## 4. Provider error taxonomy

| Category | Intended use |
|---|---|
| `UNAVAILABLE` | Provider service is down or unreachable |
| `TIMEOUT` | Provider request exceeded time limit |
| `RATE_LIMITED` | Provider rejected request due to rate limiting |
| `AUTHENTICATION_FAILED` | Provider rejected credentials or API key |
| `INVALID_RESPONSE` | Provider returned unparseable or structurally invalid response |
| `MAPPING_FAILED` | Provider response could not be normalized to domain model |
| `UNKNOWN` | Unclassified provider error |

Design principles:

- **Provider-neutral**: categories do not reference specific vendors.
- **Exhaustive for MVP**: covers the main failure modes expected from real hotel providers.
- **Extensible**: new categories can be added in future stages without breaking existing code (callers should use `when` with `else` or default branch).
- **Safe**: no vendor-specific error codes, no raw HTTP status codes, no credentials.

## 5. Error propagation behavior

### 5.1 Through CreateHotelSearchUseCase

`CreateHotelSearchUseCase.createSearch()` calls `hotelOfferProvider.search(criteria)`
at line 22. If the provider throws any exception (including `HotelProviderException`),
it propagates uncaught through `CreateHotelSearchUseCase`.

```
RealHotelOfferProviderAdapter.search()
  → throws HotelProviderException(category, message, cause)
  → propagates through CreateHotelSearchUseCase.createSearch()
  → propagates to caller
```

### 5.2 Through ExecuteConfirmedSearchTransitionUseCase (Stage 8)

`ExecuteConfirmedSearchTransitionUseCase` at line 109-122 catches generic
`Exception` from `hotelSearchBoundary.createSearch()`:

```kotlin
val createdSearch = try {
    hotelSearchBoundary.createSearch(commandPlan.command)
} catch (e: Exception) {
    attemptStore.markFailed(
        idempotencyKey = storedAttempt.idempotencyKey,
        reason = SEARCH_CREATION_FAILED,
        now = request.now,
    )
    return StoreRejected(...)
}
```

Since `HotelProviderException extends RuntimeException extends Exception`,
it is caught by this block. Stage 8 behavior:

- Attempt marked as `FAILED(SEARCH_CREATION_FAILED)`.
- `SEARCH_CREATION_FAILED` is retryable (`isRetryAllowed() = true`).
- Pending confirmation remains active (not consumed).
- Response is `StoreRejected` (safe fallback).

**No changes to Stage 8 code were needed.**

## 6. Stage 8 compatibility

| Stage 8 behavior | Status |
|---|---|
| Generic `Exception` catch at `createSearch()` | **Compatible** — `HotelProviderException` extends `RuntimeException` |
| `SEARCH_CREATION_FAILED` failure reason | **Compatible** — all provider exceptions map to this |
| `isRetryAllowed() = true` for `SEARCH_CREATION_FAILED` | **Compatible** — provider errors are retryable |
| Pending confirmation stays active on failure | **Compatible** — pending not consumed on `StoreRejected` |
| Existing `FailingHotelSearchBoundary` test (generic `RuntimeException`) | **Still passes** — unchanged |
| Existing `failedSearchCreationDoesNotConsumePendingConfirmation` test | **Still passes** — unchanged |

## 7. Default behavior preservation

| Behavior | Status |
|---|---|
| `FakeHotelOfferProvider` returns deterministic offers | **Preserved** — never throws |
| `RealHotelOfferProviderAdapter` returns `emptyList()` | **Preserved** — no changes |
| `HotelProviderConfig` default FAKE | **Preserved** — no changes |
| `module()` default → FAKE | **Preserved** — no changes |
| Stage 7 strict handoff | **Preserved** — no changes |
| Stage 8 confirmation lifecycle | **Preserved** — no changes |
| `show_hotel_results` response shape | **Preserved** — no changes |
| `markConsumed` behavior | **Preserved** — no changes |

## 8. Tests added/updated

### 8.1 New tests

| Test class | Test count | What is verified |
|---|---|---|
| `HotelProviderErrorCategoryTest` | 2 | Enum contains all 7 expected categories; count is stable at 7 |
| `HotelProviderExceptionTest` | 5 | Category + message preserved; category + message + cause preserved; is RuntimeException; propagation through CreateHotelSearchUseCase; all categories usable |

### 8.2 Existing tests

All existing tests pass without modification:

- `CreateHotelSearchUseCaseTest` — fake provider, unchanged.
- `ExecuteConfirmedSearchTransitionUseCaseTest` — generic `RuntimeException` failure test still passes.
- `RealHotelOfferProviderAdapterTest` — returns emptyList(), unchanged.
- `HotelProviderConfigTest` — default FAKE, unchanged.
- `HotelOfferProviderFactoryTest` — FAKE/REAL selection, unchanged.
- All route tests — unchanged.

### 8.3 Test execution

```
./gradlew test --no-daemon
BUILD SUCCESSFUL
```

## 9. Validation results

| Command | Result |
|---|---|
| `./gradlew test --no-daemon` | BUILD SUCCESSFUL |
| `git status --short` | Expected new source + test + docs files |
| `git diff --check` | No errors |

## 10. Guardrails upheld

| Guardrail | Status |
|---|---|
| No real provider calls | **Pass** |
| No external HTTP calls | **Pass** |
| No HTTP client | **Pass** |
| No API credentials | **Pass** |
| No auth/API key implementation | **Pass** |
| No real provider selection | **Pass** |
| No provider-specific DTOs | **Pass** |
| No dependency changes | **Pass** — `build.gradle.kts` unchanged |
| No persistence changes | **Pass** |
| No frontend changes | **Pass** |
| No OpenAPI changes | **Pass** |
| No generated clients | **Pass** |
| No booking flow | **Pass** |
| No production observability | **Pass** |
| No changes to `HotelSearchCriteria` | **Pass** |
| No changes to `HotelOffer` | **Pass** |
| No changes to `HotelOfferProviderBoundary` | **Pass** |
| No changes to API response models | **Pass** |
| No changes to `show_hotel_results` response shape | **Pass** |
| No changes to `markConsumed` semantics | **Pass** |
| No changes to consume-after-success | **Pass** |
| No production readiness claim | **Pass** |
| Real provider integration not marked completed | **Pass** |
| Stage 9.5 not started | **Pass** |

## 11. Known non-production limitations

| Limitation | Category | Future stage |
|---|---|---|
| `HotelProviderException` not thrown by `RealHotelOfferProviderAdapter` (skeleton returns `emptyList()`) | Implementation gap | Stage 9.5+ (when real adapter does I/O) |
| No provider-to-domain error mapping (real provider error → `HotelProviderException`) | Mapping | Stage 9.5+ |
| No retry policy based on error category | Reliability | Future |
| No error-specific user-facing messages | UX | Future |
| No structured logging of provider errors | Observability | Future |
| InMemory stores | Carryover from Stage 8 | Future infrastructure |
| FakeLlmClient | Carryover from Stage 8 | Future LLM integration |
| Static message text | Carryover from Stage 8 | Future UX work |

## 12. Stage 9.5 candidate scope

**Stage 9.5 — Provider Integration Verification**

Это review/design + targeted test sub-stage.

Scope:

1. Verify `RealHotelOfferProviderAdapter` integration with existing `CreateHotelSearchUseCase` — real adapter returns `emptyList()` → `COMPLETED_NO_OFFERS` status.
2. Verify Stage 7 strict handoff compatibility with both FAKE and REAL modes.
3. Verify Stage 8 confirmation lifecycle compatibility with both FAKE and REAL modes.
4. Verify `HotelProviderException` propagation through full confirmation path (end-to-end).
5. Produce integration verification report.
6. Define Stage 9.6+ direction (real provider contract, HTTP client, or pivot).

Out of scope:

- Real external API calls.
- Provider credentials or HTTP client.
- Domain model changes.
- API response model changes.
- Frontend, persistence, auth, observability, deployment.

## 13. Prompt для Stage 9.5

```
Мы продолжаем проект travel-assistant.

Пожалуйста, отвечай на русском языке. Технические имена классов, файлов,
enum values, commit messages и команды оставляй как есть.

Контекст

* Репозиторий: travel-assistant
* Branch: stage-9
* Последний завершённый sub-stage: Stage 9.4
* Stage 9 runtime implementation ещё не завершён.
* Production readiness не claimed.

Текущая provider seam (после Stage 9.3-9.4)

* `HotelProviderMode` — enum: FAKE, REAL.
* `HotelProviderConfig` — data class с mode, safe env parsing, default FAKE.
* `HotelOfferProviderFactory` — создаёт provider по config.
* `FakeHotelOfferProvider` — deterministic, default, accepted carryover.
* `RealHotelOfferProviderAdapter` — skeleton, returns emptyList(), no I/O.
* `HotelProviderErrorCategory` — enum: UNAVAILABLE, TIMEOUT, RATE_LIMITED, AUTHENTICATION_FAILED, INVALID_RESPONSE, MAPPING_FAILED, UNKNOWN.
* `HotelProviderException` — RuntimeException с category, message, optional cause.
* `HotelOfferProviderBoundary` — fun interface: search(HotelSearchCriteria): List<HotelOffer>.
* `CreateHotelSearchUseCase` — session check → provider.search() → rank → save.
* `ExecuteConfirmedSearchTransitionUseCase` — catches Exception as SEARCH_CREATION_FAILED (retryable).

Задача

Выполни:

Stage 9.5 — Provider Integration Verification

Это review/design + targeted test sub-stage.

Цели Stage 9.5

1. Verify RealHotelOfferProviderAdapter integration с CreateHotelSearchUseCase:
   - real adapter returns emptyList() → COMPLETED_NO_OFFERS status.
2. Verify Stage 7 strict handoff compatibility с FAKE и REAL modes.
3. Verify Stage 8 confirmation lifecycle compatibility с FAKE и REAL modes.
4. Verify HotelProviderException propagation через full confirmation path (end-to-end test).
5. Produce integration verification report.
6. Define Stage 9.6+ direction.

Required output files

Test files (new):
* Integration tests verifying FAKE and REAL mode end-to-end behavior.

Review report:
* docs/reviews/stage-9-5-provider-integration-verification.md

Strict guardrails

* No real external API calls.
* No provider credentials or HTTP client.
* No domain model changes (HotelOffer, HotelSearchCriteria).
* No API response model changes.
* No changes to HotelOfferProviderBoundary interface.
* Preserve Stage 8 attempt lifecycle.
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
5. Commit recommendation: Stage 9.5 provider integration verification
```

## 14. Stage 9.4 verdict

**Passed** — provider error taxonomy and error handling implemented.

Stage 9.4:

1. Создал `HotelProviderErrorCategory` enum (7 provider-neutral categories).
2. Создал `HotelProviderException` (RuntimeException с category + message + optional cause).
3. Подтвердил propagation через `CreateHotelSearchUseCase` (exception bubbles up).
4. Подтвердил Stage 8 compatibility (generic `Exception` catch maps to `SEARCH_CREATION_FAILED`, retryable, pending stays active).
5. Добавил 7 targeted tests (2 taxonomy + 5 exception/propagation).
6. Все existing tests pass без модификаций (`./gradlew test` — BUILD SUCCESSFUL).
7. No changes to domain model, API response, Stage 7/Stage 8 behavior.
8. No external HTTP, no credentials, no dependency changes.
9. Production readiness не claimed.
