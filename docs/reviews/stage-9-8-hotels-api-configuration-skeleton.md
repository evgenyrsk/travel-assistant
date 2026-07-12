# Stage 9.8 — Hotels API Configuration Skeleton

## 1. Цель и scope

Stage 9.8 добавляет typed backend configuration boundary для будущего
внутреннего Hotels API. Этап не создает HTTP client, OAuth call, provider DTO,
mapper, network call или runtime execution реального API.

`FAKE` остается default. `REAL` теперь требует полную Hotels API конфигурацию и
fail closed при missing/invalid values. Silent fallback из явно выбранного REAL
в FAKE отсутствует.

## 2. Что добавлено

### 2.1 Configuration model

Добавлен `HotelsApiConfig` со следующими typed settings:

| Property | Environment key | Requirement |
|---|---|---|
| `baseUrl` | `HOTELS_API_BASE_URL` | Required in REAL; absolute HTTP(S) URL |
| `tokenUrl` | `HOTELS_API_TOKEN_URL` | Required in REAL; absolute HTTP(S) URL |
| `clientId` | `HOTELS_API_CLIENT_ID` | Required in REAL; non-blank |
| `clientSecret` | `HOTELS_API_CLIENT_SECRET` | Required in REAL; redacted |
| `scope` | `HOTELS_API_SCOPE` | Required in REAL; non-blank |
| `connectTimeoutMillis` | `HOTELS_API_CONNECT_TIMEOUT_MS` | Required in REAL; positive integer |
| `requestTimeoutMillis` | `HOTELS_API_REQUEST_TIMEOUT_MS` | Required in REAL; positive integer |
| `userLanguage` | `HOTELS_API_USER_LANGUAGE` | Optional non-blank header value |
| `sourcePlatform` | `HOTELS_API_SOURCE_PLATFORM` | Optional non-blank header value |
| `appVersion` | `HOTELS_API_APP_VERSION` | Optional non-blank header value |

URL validation запрещает relative/non-HTTP URLs, embedded credentials, query и
fragment. Реальные URL, scope и credentials не имеют defaults и не добавлены в
репозиторий.

### 2.2 Provider mode behavior

`HotelProviderConfig` теперь содержит optional `hotelsApi`:

- FAKE не требует и не читает Hotels API settings;
- REAL без `HotelsApiConfig` отклоняется через typed
  `HotelProviderConfigurationException`;
- `fromEnvironment` принимает environment map для deterministic tests и по
  умолчанию читает `System.getenv()`;
- unknown/blank provider mode сохраняет прежний safe fallback в FAKE;
- explicit REAL не получает fallback при ошибке конфигурации.

### 2.3 Secret redaction

`RedactedSecret` хранит client secret как opaque internal value и всегда
возвращает `[REDACTED]` из `toString`. Configuration exceptions содержат только
имя config key и безопасную причину; raw secret в message не включается.

## 3. Production files

Созданы:

- `HotelProviderConfigurationException.kt`;
- `HotelsApiConfig.kt`;
- `RedactedSecret.kt`.

Изменен:

- `HotelProviderConfig.kt`.

`Application.kt`, `HotelOfferProviderFactory`, `RealHotelOfferProviderAdapter`,
routes, API DTO и domain models не изменялись.

## 4. Tests

Обновлены:

- `HotelProviderConfigTest`;
- `HotelOfferProviderFactoryTest`;
- `ProviderSeamIntegrationTest`.

Покрыто:

- FAKE без Hotels API settings;
- игнорирование incomplete API environment в FAKE;
- complete explicit и environment REAL config;
- direct REAL без config;
- отсутствие каждого required env value;
- invalid URL schemes/shapes;
- zero/non-numeric timeout;
- secret redaction в config/error text;
- прежний REAL adapter seam с synthetic complete config;
- прежний default FAKE route behavior.

Все test URLs, ids, scope и secrets синтетические и используют `.test` hosts.

## 5. Runtime boundary

Stage 9.8 не передает `HotelsApiConfig` в adapter и не создает transport. Factory
по-прежнему выбирает:

```text
FAKE -> FakeHotelOfferProvider
REAL -> RealHotelOfferProviderAdapter (no I/O, empty result)
```

Изменение runtime ограничено startup/config validation: явно выбранный REAL с
неполной конфигурацией теперь завершается typed configuration error до provider
execution. Search/Assistant routes и public response behavior не менялись.

## 6. Security decisions

- Credentials не хранятся в repository files или defaults.
- Secret не выводится через config `toString` или exception messages.
- URLs с embedded user info отклоняются.
- REAL требует явные token URL и scope; подозрительный scope из selected OpenAPI
  не hardcode-ится.
- `x-travel-session-id` не добавлен в config или headers.

Redaction снижает риск случайного вывода, но не заменяет secret manager,
environment hardening или log-scrubbing policy. Эти concerns остаются future.

## 7. Public contracts and compatibility

Не изменены:

- Travel Assistant public API request/response shape;
- OpenAPI contracts;
- frontend и generated clients;
- Stage 7 strict `hotel-search;` handoff;
- Stage 8 confirmation lifecycle;
- provider/domain boundaries;
- fake-provider default behavior.

Новые types являются internal infrastructure configuration types.

## 8. Что не входит в Stage 9.8

- Ktor `HttpClient` или другой transport;
- OAuth token acquisition/cache;
- autocomplete/location resolution;
- provider request/response DTO;
- search mapper/orchestration/pagination;
- real network/QA calls;
- runtime wiring config в real adapter;
- `.env` values, credentials или secrets;
- booking/payment/cancellation;
- durable storage;
- production readiness.

## 9. Remaining owner inputs

До Stage 9.9 нужны подтвержденные QA/sandbox base URL, OAuth token URL,
scope/audience и service identity. До Stage 9.10–9.11 дополнительно нужны
autocomplete contract, occupancy/child-age rules, nullable review semantics,
shown-price semantics, pagination limits и sanitized fixtures.

## 10. Risks and limitations

- `RedactedSecret` остается process-memory value; secure injection/storage не
  решены.
- Config validation не доказывает доступность URL или корректность OAuth scope.
- REAL adapter пока не использует config и возвращает empty offers.
- Unknown non-REAL provider mode сохраняет historical fallback в FAKE; отдельное
  ужесточение mode parsing не входило в этап.
- No live call означает, что header/auth compatibility пока не проверена.

## 11. Validation

| Check | Result |
|---|---|
| Targeted config/factory/provider-seam tests | Passed |
| Full backend tests | Passed |
| `git diff --check` | Passed |
| HTTP/network calls | Not performed |

## 12. Рекомендуемый Stage 9.9

Stage 9.9 — Hotels API HTTP/OAuth transport boundary с injected/mock transport:

- принимать validated `HotelsApiConfig`;
- моделировать token acquisition и authenticated JSON request без live calls;
- не добавлять autocomplete/search DTO или adapter orchestration;
- переводить transport/auth failures в existing provider error taxonomy;
- использовать synthetic responses и не подключать transport к runtime factory.

Stage 9.9 нельзя начинать с hardcoded token URL/scope. Сначала должны быть
получены owner-confirmed auth values или этап должен оставаться полностью
parameterized/mock-only.

## 13. Verdict

Stage 9.8 завершен как bounded backend configuration skeleton. Safe next step —
Stage 9.9 только как mock-only HTTP/OAuth transport boundary после подтверждения
auth contract. Real API execution и production readiness не заявлены.
