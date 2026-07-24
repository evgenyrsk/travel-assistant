# Stage 13.3 — opaque offer identity и разрешение выбранного предложения

## Цель

Исключить provider `hotelId` из публичной модели предложения и добавить
search-bound разрешение выбранного `offerId` для будущей загрузки деталей.

## Реализация

- Provider boundary теперь возвращает `HotelOfferCandidate` без client-facing
  идентификатора.
- `CreateHotelSearchUseCase` назначает каждому кандидату process-local
  `offerId` через application-owned `HotelOfferIdGenerator` до ранжирования и
  сохранения поиска.
- `LocalHotelOfferIdGenerator` не получает provider reference и не включает её
  в результат.
- `ResolveSelectedHotelOfferUseCase` принимает `hotelSearchId + offerId` и
  возвращает typed outcomes `Resolved`, `SearchNotFound` или `OfferNotFound`.
- Разрешение выполняется только внутри указанного сохранённого поиска.
- `providerOfferRef` удалён из `HotelOfferResponse` и активного OpenAPI draft.

## Граница идентификаторов

`offerId` является application-owned opaque identifier. Provider `hotelId`
сохраняется только во внутреннем `providerReference` соответствующего offer и
может быть получен лишь после успешного search-bound resolution. Клиент не
получает и не конструирует provider reference.

Неизвестный поиск отличается от неизвестного предложения typed result. Offer
из другого поиска не разрешается даже при корректном `offerId`.

## Проверки

- Application-owned IDs генерируются без provider input.
- Пустой и неуспешный provider result не создаёт offer IDs.
- Provider candidates не имеют поля `id`.
- Search-bound resolve различает unknown search и unknown offer.
- Public JSON не содержит `providerOfferRef` или provider `hotelId`.
- OpenAPI/conformance expectations согласованы с runtime shape.
- Targeted и полный backend test suite пройдены.
- OpenAPI conformance tests/check и frontend regression gates пройдены.
- `git diff --check` пройден.

## Границы этапа

Не добавлены details transport, provider details call, route, runtime wiring,
demo UI или live request. Rates, deeplink, shortlist, comparison, booking и
payment остаются вне MVP.

## Verdict

`PASS_STAGE_13_3_OPAQUE_OFFER_IDENTITY_AND_SELECTION`.

Следующий этап — Stage 13.4: безопасный hotel details transport и provider
adapter через `MockEngine`, без route/runtime wiring.
