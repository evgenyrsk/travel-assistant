# Stage 13.5 — платформонезависимый API деталей отеля

## Цель

Открыть bounded details flow через Travel Assistant API по паре opaque
`hotelSearchId + offerId`, не раскрывая provider identity и не подключая REAL
details adapter к runtime.

## Реализация

- Добавлены `LoadSelectedHotelDetailsUseCase` и typed result:
  `Loaded`, `SearchNotFound`, `OfferNotFound`, `DetailsNotFound`,
  `ResponseRejected`, `ProviderUnavailable`.
- Добавлен endpoint
  `GET /api/v1/hotel-searches/{searchId}/offers/{offerId}/details`.
- Выбранное предложение разрешается только внутри указанного поиска; provider
  вызывается только после успешного разрешения.
- `HotelDetailsResponse` содержит только provider-neutral facts Stage 13.2.
  Unknown optional values отсутствуют в JSON.
- Ошибки разделены на безопасные `404`, `502` и `503`; provider reference,
  raw response и внутренние причины не возвращаются.
- Default runtime provider деталей остаётся fail-closed и возвращает
  `ProviderUnavailable`. Успешный route path проверяется через внедряемый fake.

## Public contract

OpenAPI дополнен операцией `getHotelOfferDetails` и точной схемой ответа.
Subset manifest теперь содержит четыре `platform_client_candidate`, но сохраняет
`status=not_ready`, `readinessClaim=false` и пустой список generated clients.

Conformance проверяет:

- наличие endpoint в runtime и OpenAPI;
- ответы `404`, `502`, `503`;
- обязательные `hotelName` и `metadata`;
- точный набор верхнеуровневых полей;
- запрет дополнительных полей и provider identity.

## Проверки

- Response mapping проверяет полную и минимальную форму JSON.
- Route tests покрывают success, неизвестный search, неизвестный offer,
  отсутствующие provider details, rejected response и unavailable provider.
- Проверено отсутствие вызова provider до успешного selected-offer resolution.
- Targeted и полный backend suite, OpenAPI conformance, frontend tests/lint/build
  и `git diff --check` пройдены.

## Границы

REAL details runtime wiring, live calls, автоматическая загрузка, N+1, rates,
deeplink, booking, frontend flow и generated clients не добавлены.

## Verdict

`PASS_STAGE_13_5_PLATFORM_NEUTRAL_HOTEL_DETAILS_API`.

Следующий этап — Stage 13.6: opt-in REAL details runtime wiring с одним общим
Hotels API `HttpClient` и сохранением `FAKE` по умолчанию.
