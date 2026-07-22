# Stage 13.1 — контролируемая проверка hotel details contract

## Цель

Проверить, принимает ли публичный Hotels API `hotelId` из поисковой выдачи в
`GET /api/v1/hotels/{hotelId}`, и получить обезличенный success fixture до
создания production DTO, mapper или runtime wiring.

## Выполненные запросы

22 июля 2026 года выполнены ровно два анонимных запроса без redirect и retry:

1. `POST /api/v1/hotels/search` с известным синтетическим контекстом, чтобы
   получить актуальный opaque `hotelId`;
2. `GET /api/v1/hotels/{hotelId}` для первого предложения из ответа.

Оба запроса вернули `200 application/json`. Search response содержал 20
предложений и строковый `hotelId`. Тот же идентификатор был принят details
endpoint. `Authorization`, cookies и session/device headers не передавались.

## Наблюдаемая структура details

Ответ содержит единственный top-level `payload` object. В одном наблюдении
присутствовали:

- identity/display: `hotelId`, `hotelName`, `hotelChain`, `categoryCode`,
  `starRating`;
- location: `areaLocation`, address и coordinates;
- content: description sections и images;
- facilities: группы с provider codes и display names;
- operational facts: check-in/check-out, payment methods, structured rules;
- certification и owner/register fields;
- `badgeSlugs`, `isClosed`, `viewsCount`.

Наличие поля в одном success response не доказывает его обязательность для
всех отелей. Certification, contacts, owner/register data и сложные
`structuredRules` не разрешены к автоматическому переносу в будущий public
contract только на основании этого fixture.

## Обезличивание

В репозиторий добавлен сокращённый репрезентативный fixture. Сохранены field
names, JSON types, nesting, пустые rule arrays и безопасные enum-like values.
Заменены:

- hotel/location/room identifiers;
- названия, адреса, coordinates и description;
- phone/email и image URLs;
- facility display names;
- certification registry и owner data;
- response headers, включая trace identifier.

Raw body и headers использовались только во временной директории `/tmp` и не
добавлялись в Git.

## Contract verdict

- Фактический успешный path: `GET /api/v1/hotels/{hotelId}`.
- Один анонимный запрос принят без auth; это наблюдение, а не официальная S2S
  гарантия.
- Search `hotelId` совместим с details path и остаётся internal opaque
  provider reference.
- Response envelope совместим с будущим fixture-driven дизайном adapter.
- Details должны загружаться только для явно выбранного offer, без N+1.

## Оставшиеся ограничения

- `404` и provider error body намеренно не проверялись из-за лимита одного
  details probe; будущая error mapping сначала проверяется через `MockEngine`.
- Nullability и requiredness полей по одному отелю не установлены.
- Rates, realtime availability, `bookHash`, deeplink и booking lifecycle не
  входят в этот contract.
- Официальные SLA, rate limits и S2S support остаются rollout gates.

Эти ограничения не блокируют Stage 13.2, если provider DTO будет устойчив к
отсутствующим optional fields, а provider-specific contacts/certification не
попадут в первую provider-neutral model.

## Проверки

- fixture и manifest являются валидным JSON;
- hash fixture совпадает с manifest;
- contract test проверяет envelope, идентификаторы, representative arrays и
  отсутствие provider host/tracing data;
- чувствительные исходные значения отсутствуют в sanitized fixture;
- автоматические повторы и проверки альтернативных endpoint отсутствовали.

## Вне Stage 13.1

Не добавлены production DTO, mapper, transport, domain model, public endpoint,
assistant flow, runtime wiring, UI, rates, deeplink, booking или payment.

## Verdict

`PASS_STAGE_13_1_HOTEL_DETAILS_SUCCESS_CONTRACT_VERIFIED`.

Следующий безопасный этап — Stage 13.2: provider-neutral details model и
fixture-driven mapping без transport/runtime wiring.
