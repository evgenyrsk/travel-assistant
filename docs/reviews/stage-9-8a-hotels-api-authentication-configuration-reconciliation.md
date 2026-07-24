# Stage 9.8a — Hotels API Authentication Configuration Reconciliation

## 1. Цель

Stage 9.8a корректирует OAuth-предположение Stage 9.8 по подтвержденной runtime
структуре Hotels API. Этап остается configuration-only: JWT signing, HTTP client,
network calls и runtime wiring не добавлены.

## 2. Configuration model

`HotelsApiConfig` теперь объединяет три internal configuration boundary:

| Boundary | Settings | Defaults / requirement |
|---|---|---|
| `publicTarget` | base URL, timeout | `https://hotels.tcsbank.ru/`, `60s` |
| `privateTarget` | base URI, timeout | `https://hotels-private.tcsbank.ru/`, `10s` |
| `jwtAuth` | issuer, audience, private key | `HOTELSSEARCHAPI`, `HOTELSAPI`, key required in REAL |

Target values можно переопределить через `HOTELS_API_PUBLIC_BASE_URL`,
`HOTELS_API_PUBLIC_TIMEOUT_MS`, `HOTELS_API_PRIVATE_BASE_URI` и
`HOTELS_API_PRIVATE_TIMEOUT_MS`. Issuer/audience допускают overrides через
`HOTELS_API_JWT_ISSUER` и `HOTELS_API_JWT_AUDIENCE`. Private key читается только
из `HOTELS_API_JWT_PRIVATE_KEY` и хранится как `RedactedSecret`.

Неподтвержденные OAuth settings `tokenUrl`, `clientId`, `clientSecret` и `scope`
удалены из active configuration model. Старые OAuth environment keys больше не
требуются.

## 3. Validation и security boundary

- public/private URI должны быть absolute HTTP(S) без credentials, query и
  fragment;
- оба timeout должны быть положительными;
- issuer/audience должны быть непустыми;
- REAL требует private key и fail closed при его отсутствии;
- FAKE остается default и не читает Hotels API settings;
- private key не попадает в `toString` или configuration exceptions.

Defaults содержат только подтвержденные endpoint metadata, issuer и audience.
Credentials, signing algorithm, key format, TTL, clock skew и `kid` не
hardcode-ятся.

## 4. Endpoint auth matrix

Endpoint-level auth routing остается нерешенным:

- selected OpenAPI наследует root `SiamBearer` для
  `POST /api/v1/hotels/search`;
- runtime settings описывают JWT только рядом с private target;
- anonymous public search не подтвержден фактическим request contract.

До устранения конфликта live calls запрещены. Ожидаемый
`Authorization: Bearer <jwt>` зафиксирован только как candidate wire format и не
реализован.

## 5. Production files и tests

Добавлены `HotelsApiTargetConfig` и `HotelsApiJwtAuthConfig`. Обновлены
`HotelsApiConfig`, `HotelProviderConfig` и generic validation `RedactedSecret`.

Targeted tests покрывают defaults, overrides, URL/timeout validation,
issuer/audience validation, обязательный private key, redaction, отсутствие
зависимости от старых OAuth keys и сохранение FAKE/REAL provider seam.

## 6. Не входит в Stage 9.8a

- JWT generation/signing или crypto dependency;
- HTTP transport и token/header injection;
- endpoint-to-target/auth routing;
- provider DTO, mapper, autocomplete или search orchestration;
- реальные запросы, credentials и fixtures с секретами;
- public API, OpenAPI Travel Assistant, frontend или generated clients;
- durable storage и production readiness.

## 7. Совместимость и ограничения

`Application.kt`, routes и `RealHotelOfferProviderAdapter` не изменены. REAL
adapter остается no-I/O seam; FAKE остается default. Stage 7/8 hotel-search и
confirmation behavior не затронуты.

Подтвержденные defaults не доказывают, какой target обслуживает MVP search и
какая auth применяется к endpoint. Для signing boundary нужны algorithm,
private-key format, TTL, clock skew и optional `kid`.

## 8. Рекомендуемый Stage 9.9

Stage 9.9 — mock-only HTTP/JWT transport boundary, но фактическая JWT header
injection должна оставаться blocked до подтверждения:

1. host для `POST /api/v1/hotels/search`;
2. endpoint-level auth matrix;
3. signing contract;
4. sanitized headers успешного search request.

Transport не должен подключаться к runtime factory и не должен выполнять live
calls.

## 9. Verdict

Stage 9.8a согласует active configuration с предоставленной public/private JWT
runtime structure. Configuration готова к проектированию mock transport, но не
к JWT signing или реальному Hotels API request.
