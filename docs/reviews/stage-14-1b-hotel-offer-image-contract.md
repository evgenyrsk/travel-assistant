# Stage 14.1b — optional image в hotel offer contract

## Цель

Передать demo-клиенту первое безопасное изображение уже найденного offer без
details lookup, N+1-запросов и раскрытия provider identity.

## Изменение контракта

- `HotelOfferResponse` получил optional `imageUrl`;
- поле отсутствует, если search response не содержит безопасного изображения;
- в поле передаётся только первый HTTPS URL, прошедший общую infrastructure
  policy Stage 14.1a;
- OpenAPI описывает поле как optional URI с HTTPS pattern;
- `offerId` остаётся opaque, provider `hotelId` и `providerReference` не
  раскрываются.

## Проверки

- сериализация известного `imageUrl` и отсутствие unknown значения;
- runtime integration переносит первый fixture image через search → store →
  public offers response;
- targeted backend contract tests;
- OpenAPI conformance `npm test` и `npm run check`;
- conformance report сохранил `status=not_ready`, `readinessClaim=false`, четыре
  `platform_client_candidate` и отсутствие blocking findings;
- subset manifest не изменён, `generatedClientTargets` остаётся пустым.

## Не входит в этап

- image proxy, cache, preloading или проверка доступности URL;
- details request для заполнения карточки;
- изменение ранжирования, provider transport или runtime mode;
- generated clients и readiness promotion;
- frontend redesign.

## Verdict

`PASS_STAGE_14_1B_HOTEL_OFFER_IMAGE_CONTRACT`.

Следующий этап — Stage 14.1c: компактные demo-карточки, единое объяснение
ранжирования, image fallback и responsive browser QA.
