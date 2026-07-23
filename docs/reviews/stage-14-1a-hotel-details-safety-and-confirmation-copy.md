# Stage 14.1a — безопасность hotel details и понятное подтверждение

## Цель

Закрыть обнаруженную после Stage 14.0 утечку служебных provider-разделов из
hotel details, подготовить безопасное первое изображение предложения и
заменить технический текст подтверждения на понятное пользователю описание
параметров.

## Что изменено

- description sections проходят fail-closed allowlist по заголовкам;
- безымянное описание и разделы `Описание`, `Об отеле`,
  `Важная информация` с английскими эквивалентами разрешены;
- неизвестные заголовки и paragraphs с certification, registry, owner,
  contact, ИНН, ОГРН, КПП, URL, email или телефоноподобными значениями
  исключаются;
- проверка безопасных HTTPS image URL вынесена в общий infrastructure helper;
- внутренние `HotelOfferCandidate` и `HotelOffer` получили optional
  `imageUrl`; search mapper выбирает первый безопасный URL;
- confirmation показывает направление, даты, гостей, номера и активные
  условия на русском языке, затем отдельно задаёт вопрос о запуске поиска.

## Границы безопасности

- `structuredRules`, contacts, provider codes и certification data не
  моделируются;
- неизвестный description title скрывается целиком;
- HTTP URL, URL с credentials или fragment и malformed URL не попадают в
  offer/details;
- отсутствие изображения не отклоняет предложение;
- до отдельного ответа «Да» поиск не выполняется и `hotelSearchId` не
  создаётся.

## Проверки

- targeted tests mapper-ов, confirmation use case, assistant routes и
  runtime details integration;
- полный backend suite: 507 тестов;
- runtime fixture с ИНН, ОГРН, КПП, owner и registry URL не раскрывается в
  публичном details JSON;
- `git diff --check`.

## Не входит в этап

- публичное поле `imageUrl` и изменение OpenAPI;
- переработка demo-карточек;
- новые provider calls, image proxy/cache, rates, deeplink или booking;
- изменение confirmation lifecycle.

## Verdict

`PASS_STAGE_14_1A_HOTEL_DETAILS_SAFETY_AND_CONFIRMATION_COPY`.

Следующий атомарный этап — Stage 14.1b: optional `imageUrl` в публичном offer
contract без generated clients и без изменения provider runtime.
