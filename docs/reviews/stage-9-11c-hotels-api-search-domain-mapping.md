# Stage 9.11c — преобразование Hotels API search в доменные модели

**Роль:** review artifact завершенного mapper-only backend-этапа. Документ
фиксирует изолированное преобразование request/response без transport,
pagination orchestration или runtime wiring.

## Цель

Связать подтвержденные Hotels API search DTO с `HotelSearchCriteria` и
`HotelOffer`, не смешивая location resolution, сетевой вызов и выполнение
реального provider flow.

## Добавленные компоненты

- `HotelsApiSearchRequestMapper` преобразует явно выбранный
  `HotelLocationResolution.Candidate` и `HotelSearchCriteria` в
  `HotelsApiSearchRequestDto`;
- `HotelsApiSearchResponseMapper` преобразует `HotelsApiSearchResponseDto` в
  список `HotelOffer`;
- `HotelsApiSearchMappingError` задает typed issues для невалидных destination,
  dates, rooms, occupancy, provider facts, price, currency, review и
  availability;
- оба mapper-а возвращают typed `Mapped`/`Rejected` result.

## Request mapping

- `destinationId` берется только из переданного location candidate;
- свободный `criteria.destination` и hotel suggestion id не используются для
  построения `destinationId`;
- автоматический выбор первого autocomplete result отсутствует;
- даты сериализуются как `YYYY-MM-DD`;
- разрешена ровно одна комната и создается ровно один элемент `guests`;
- adults и порядок `childrenAge` сохраняются;
- возраста проверяются в диапазоне `0..17`;
- `offset` и `limit` остаются `null`.

## Response mapping

- `hotelId` сохраняется без разбора как opaque `providerReference`;
- stable internal offer id строится из фиксированного source prefix и
  `hotelId`;
- дубликаты provider reference сохраняют первое корректное предложение;
- `shownPrice.amount` и currency переносятся без пересчета как total-stay
  price;
- cashback, discount, taxes и fees не прибавляются и не вычитаются;
- guest review маппится только при наличии и не подменяется `starRating`;
- amenities остаются `null`, потому что list response их не предоставляет;
- положительное `availableRoomsCount` дает `AVAILABLE`, ноль — `UNKNOWN`;
- `LIMITED` не создается;
- pagination metadata не меняет количество вызовов и не запускает следующие
  страницы.

## Тесты

- точный date-only формат, explicit location id, одна guest group и nullable
  pagination;
- комнаты и детские возраста, включая границы `0`/`17`;
- opaque provider reference и stable offer id;
- total-stay price/currency без пересчета;
- review present/absent и запрет fallback на `starRating`;
- `AVAILABLE`/`UNKNOWN` без `LIMITED`;
- typed errors для price, currency, review и availability;
- deduplication и отсутствие pagination behavior.

## Границы

- mapper-ы не подключены к `PublicHotelsApiHttpTransport` или
  `RealHotelOfferProviderAdapter`;
- HTTP/network calls и live fixtures не выполнялись;
- route, `Application.kt`, provider factory и runtime behavior не менялись;
- polling, `etag`, pagination orchestration и `waitLoadingCompleted` не
  добавлены;
- OpenAPI/frontend не менялись в этом этапе;
- booking/payment, `bookHash`, auth, secrets и storage не входят в scope.

## Остаточные неизвестные

- включение taxes/fees в `shownPrice` остается `unknown`;
- threshold для `LIMITED` не определен, поэтому mapper его не создает;
- официальный server-to-server статус, SLA и rate limits относятся к будущему
  live/runtime gate.

## Verdict

Stage 9.11c завершен как mapper-only implementation. Следующий планируемый этап
по roadmap — Stage 9.12, но он требует отдельной явной задачи: orchestration
location resolver → search transport → mapper без runtime wiring.

## Связанные документы

- [Stage 9.11b4 — public contract alignment](stage-9-11b4-public-contract-alignment.md)
- [Stage 9.11a — search DTO](stage-9-11a-hotels-api-search-dto-without-domain-mapping.md)
- [Stage 9.10 — location resolution](stage-9-10-autocomplete-location-resolution-contract-boundary.md)
- [Основной roadmap](../roadmap/roadmap.md)
