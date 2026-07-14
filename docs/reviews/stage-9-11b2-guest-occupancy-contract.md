# Stage 9.11b2 — контракт состава гостей и возрастов детей

**Роль:** review artifact завершенного backend-контрактного этапа. Документ
фиксирует каноническое представление детей без provider call и runtime wiring.

## Цель

Выразить подтвержденное правило: возраст требуется для каждого явно указанного
ребенка, а поиск не начинается до получения полного и валидного списка.

## Изменения

- `HotelSearchCriteria.Guests.childrenAges` и
  `ProceedWithCandidateCriteria.Guests.childrenAges` стали каноническими
  значениями; количество детей вычисляется из размера списка;
- публичный `HotelSearchRequest` сохраняет переходное nullable-поле `children`
  и принимает nullable `childrenAges`;
- `HotelSearchCriteriaResponse` возвращает `childrenAges` вместе с вычисленным
  количеством детей;
- internal constraints поддерживают `children-ages` в формате `0,17`;
- confirmation proposal показывает количество и возраста детей;
- idempotency basis включает отсортированную копию возрастов, поэтому
  перестановка одного и того же набора не создает новое основание попытки.

## Правила нормализации и проверки

- дети не указаны — канонический список пуст;
- положительный `children` без полного `childrenAges` отклоняется;
- при наличии обоих полей размер списка обязан совпадать с `children`;
- каждый возраст должен находиться в диапазоне `0..17` включительно;
- порядок возрастов сохраняется при обычном mapping;
- Assistant формирует `AskClarification`, если количество детей известно, а
  возраста отсутствуют; defensive validation не разрешает
  `ProceedWithCandidate` с неполными данными.

## Тесты

Добавлены и обновлены точечные тесты для:

- граничных возрастов `0` и `17`, а также отклонения значений вне диапазона;
- отсутствующего или неполного списка возрастов;
- переходной совместимости `children` и `childrenAges`;
- clarification, confirmation summary и internal parser;
- сохранения порядка при mapping и нормализации порядка для idempotency;
- JSON-десериализации `HotelSearchRequest` и request validation boundary.

## Границы

- provider, transport и `RealHotelOfferProviderAdapter` не вызываются;
- route composition и runtime wiring не менялись;
- OpenAPI и frontend намеренно отложены до Stage 9.11b4;
- HotelOffer, ranking и search DTO mapping не входят в этап;
- taxes/fees, `LIMITED`, SLA и server-to-server статус не определяются.

## Verdict

Stage 9.11b2 завершен. Контракт occupancy готов для последующего mapper, но
Stage 9.11c остается заблокированным до Stage 9.11b3 и Stage 9.11b4.
Следующий безопасный этап — Stage 9.11b3: nullable provider facts в
`HotelOffer` и ranking без выдуманных rating/amenities.

## Связанные документы

- [Readiness reconciliation Stage 9.11c](stage-9-11c-search-domain-mapping-readiness-reconciliation.md)
- [Основной roadmap](../roadmap/roadmap.md)
