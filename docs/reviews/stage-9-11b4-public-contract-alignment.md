# Stage 9.11b4 — синхронизация публичного контракта

**Роль:** review artifact завершенного contract/frontend этапа. Документ
фиксирует синхронизацию OpenAPI с уже реализованными backend-моделями без
добавления нового UI flow.

## Цель

Отразить канонические возраста детей и частично известные provider facts в
публичном OpenAPI, а также подтвердить безопасное отображение предложения без
rating во frontend.

## Изменения

- в `HotelSearchCriteria.guests` добавлен optional `childrenAges` с возрастами
  `0..17`;
- переходное поле `children` сохранено и документирует требование полного
  списка возрастов при положительном количестве;
- `HotelOffer.rating` и `HotelOffer.amenities` остаются optional, а описание
  фиксирует отсутствие поля как unknown provider data;
- frontend regression test подтверждает текст `Нет рейтинга` при отсутствии
  объекта rating и запрещает отображение фиктивного `0.0 / 10`.

## Совместимость

- текущий frontend продолжает отправлять `children=0`;
- family UI, ввод возрастов и новый пользовательский flow не добавлены;
- generated clients в репозитории для этого контракта отсутствуют, поэтому
  обновление generated artifacts не требовалось;
- backend route и runtime behavior в этапе не менялись.

## Проверки

- frontend tests и syntax lint;
- read-only OpenAPI conformance check: YAML распознан и отчет сформирован;
- `git diff --check`.

Статус `not_ready` в conformance report ожидаем для существующего Stage 7
skeleton и не является заявлением OpenAPI/generated-client readiness.

## Границы

- transport, provider call, mapper и pagination не добавлены;
- `RealHotelOfferProviderAdapter` не активирован;
- taxes/fees inclusion и `LIMITED` threshold остаются неизвестными;
- официальный server-to-server статус не меняется.

## Verdict

Stage 9.11b4 завершен. Контрактные prerequisites Stage 9.11b2–9.11b4 закрыты,
поэтому Stage 9.11c можно начать как узкий mapper-only этап без transport,
pagination и runtime wiring.

## Связанные документы

- [Stage 9.11b3 — partial HotelOffer facts](stage-9-11b3-partial-hotel-offer-facts-contract.md)
- [Readiness reconciliation Stage 9.11c](stage-9-11c-search-domain-mapping-readiness-reconciliation.md)
- [Основной roadmap](../roadmap/roadmap.md)
