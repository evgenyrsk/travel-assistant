# Stage 9.17c — подключение REAL Hotels API к runtime

## Роль документа

Это implementation review Stage 9.17c. Источник текущего статуса и порядка
этапов — [`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

Подключить уже проверенную цепочку public autocomplete → один hotel search →
domain mapper к backend runtime в явно выбранном режиме `REAL`, сохранив
`FAKE` режимом по умолчанию.

## Реализовано

- активная `HotelsApiConfig` содержит только public target и необязательный
  `X-User-Language` (`RU` или `EN`);
- private target и JWT больше не требуются для public search flow;
- неизвестное значение `HOTEL_PROVIDER_MODE` отклоняется, а не включает
  `FAKE` неявно;
- production `HttpClient` использует Ktor CIO и `HttpTimeout`;
- Ktor application владеет lifecycle provider runtime и закрывает client по
  событию `ApplicationStopped`;
- `HotelOfferProviderFactory` композирует
  `PublicHotelsApiLocationResolverAdapter` → `HotelsApiSearchOrchestrator` →
  `RealHotelOfferProviderAdapter` только для `REAL`;
- `RealHotelOfferProviderAdapter` преобразует orchestration и transport
  outcomes в `HotelOfferProviderResult`;
- cancellation пробрасывается без преобразования в provider failure.

## Конфигурация

| Переменная | Семантика |
|---|---|
| `HOTEL_PROVIDER_MODE` | `FAKE` по умолчанию; `REAL` включает public flow |
| `HOTELS_API_PUBLIC_BASE_URL` | public base URL; default `https://hotels.tbank.ru/` |
| `HOTELS_API_PUBLIC_TIMEOUT_MS` | положительный timeout; default `60000` |
| `HOTELS_API_USER_LANGUAGE` | необязательное `RU` или `EN` |

JWT/private settings не читаются активным public flow. `Authorization` не
добавляется.

## Typed outcomes

- успешный orchestration дает `SearchCompleted`;
- отсутствие или неоднозначность location сохраняются отдельными outcomes;
- числовые `destinationId` не включаются в безопасные suggestions;
- request/response mapping errors сворачиваются в application-owned причины;
- timeout, rate limit, auth rejection и unavailable сохраняют безопасную
  категорию без raw body, URL или provider message.

Hotel search state и `hotelSearchId` по-прежнему создаются только после
`SearchCompleted` существующим application use case.

## Проверки

Targeted tests покрывают:

- public-only `REAL` config без private key;
- fail-closed неизвестный provider mode;
- factory composition и закрываемый runtime;
- все основные typed mappings adapter;
- отсутствие `destinationId` в location suggestions;
- propagation coroutine cancellation;
- route-level REAL success через `MockEngine`;
- безопасный unavailable outcome без создания search;
- неизменный deterministic `FAKE` flow.

## Границы

Не добавлены:

- live calls и retries;
- pagination или polling;
- новые public API/OpenAPI/frontend fields;
- private API, JWT signing, secrets или auth headers;
- durable storage, booking/payment и `bookHash`;
- OpenRouter или другой real LLM.

Публичные Hotels endpoints подтверждены технически, но это не заявление об их
официальном server-to-server SLA или production readiness.

## Следующий этап

Stage 9.18 — integration closure: полный regression, route failure matrix,
контролируемый opt-in runtime smoke и проверка существующей diagnostic frontend
формы.

## Verdict

`REAL_RUNTIME_WIRED_OPT_IN_READY_FOR_STAGE_9_18`.

`FAKE` остается default. До Stage 9.18 интеграция не считается закрытой.
