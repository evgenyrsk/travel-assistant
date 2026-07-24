# Stage 9.11b3 — контракт частично известных фактов HotelOffer

**Роль:** review artifact завершенного backend-контрактного этапа. Документ
фиксирует представление неизвестных rating, review count и amenities без
provider mapper или runtime wiring.

## Цель

Не создавать фиктивные provider facts, когда list-ответ не содержит review или
amenities, и сохранить детерминированное ранжирование таких предложений.

## Изменения

- `HotelOffer.rating`, `HotelOffer.reviewCount` и `HotelOffer.amenities` стали
  nullable;
- `HotelOfferResponse.rating` и `HotelOfferResponse.amenities` стали nullable и
  не сериализуются при отсутствии;
- существующий known review по-прежнему выражается шкалой `10.0`, но
  `starRating` не используется как подмена guest rating;
- `FakeHotelOfferProvider` сохраняет прежние известные значения и прежнее
  поведение;
- ranking внутри одинаковой availability использует порядок:
  known rating, rating по убыванию, total-stay price, stable offer id;
- `matchSummary` для неизвестного rating явно говорит о его отсутствии и не
  утверждает, что предложение ранжировалось по rating.

## Семантика unknown

- `rating = null` и `reviewCount = null` означают отсутствие подтвержденного
  review fact; значения `0.0` и `0` не подставляются;
- `amenities = null` означает, что list response не предоставил данные;
- пустой список остается отдельным значением для случая, когда источник явно
  вернул известный пустой набор;
- guest rating и звездная категория отеля остаются разными фактами.

## Тесты

- known rating ранжируется выше unknown rating при одинаковой availability;
- предложение без rating получает корректный `matchSummary`;
- отсутствующие rating и amenities не попадают в JSON;
- известные rating, review count и amenities сохраняются в JSON;
- существующие fake-provider, route и application tests остаются совместимыми.

## Границы

- search DTO и provider mapper не менялись;
- transport, provider call, pagination и runtime wiring не добавлены;
- public OpenAPI и frontend отложены до Stage 9.11b4;
- `LIMITED` threshold и taxes/fees inclusion остаются неизвестными;
- REAL provider не активирован.

## Verdict

Stage 9.11b3 завершен. Domain и backend response способны сохранять unknown
provider facts без фиктивных значений. Следующий безопасный этап — Stage
9.11b4: синхронизация OpenAPI и frontend regression coverage. Stage 9.11c до
этого этапа не начинается.

## Связанные документы

- [Stage 9.11b2 — guest occupancy contract](stage-9-11b2-guest-occupancy-contract.md)
- [Readiness reconciliation Stage 9.11c](stage-9-11c-search-domain-mapping-readiness-reconciliation.md)
- [Основной roadmap](../roadmap/roadmap.md)
