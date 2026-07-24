# Stage 9.11c — сверка готовности преобразования search DTO в доменную модель

**Роль:** review/readiness artifact перед реализацией Stage 9.11c. Документ
сопоставляет наблюдения от 14 июля 2026 года с текущими DTO и доменными
моделями. Он не завершает Stage 9.11c и не разрешает runtime integration.

## 1. Цель и текущая точка

Цель сверки — отделить подтвержденные provider facts от проектных правил и
оставшихся неизвестных, а затем проверить, можно ли реализовать mapper без
выдуманных значений.

На текущий момент:

- выбран `POST /api/v1/hotels/search` на `https://hotels.tbank.ru/`;
- autocomplete/location boundary возвращает явный числовой `destinationId`;
- DTO запроса и ответа поиска существуют отдельно от domain;
- transport и `RealHotelOfferProviderAdapter` не подключены к поиску;
- `RealHotelOfferProviderAdapter.search()` по-прежнему возвращает пустой список;
- Stage 9.12 и последующие этапы не активированы.

## 2. Классификация подтверждений

Публичный web-интерфейс и ограниченные read-only запросы дают эмпирические
сведения о текущем поведении. Они не подтверждают официальный
server-to-server контракт, SLA, rate limits или долговременную стабильность.

| Область | Подтвержденный факт | Статус для mapper |
|---|---|---|
| Назначение | `locations[].id` принимается как `destinationId`; `hotels[].id` является opaque reference отеля | Правило закрыто |
| Комнаты | `/api/v1/hotels/search` принимает ровно один элемент `guests`; два элемента дают `invalid_rooms_count` | Правило закрыто: только одна комната |
| Возраст детей | Endpoint принял значения `0` и `17`; UI ограничивает диапазон `0..17` | Диапазон закрыт, источник возрастов в domain не определен |
| Даты | Endpoint принимает date-only и UTC midnight; web использует `YYYY-MM-DD` | Требование provider не единственно; проектное правило выбрано ниже |
| Цена | `shownPrice` меняется как стоимость всего периода | Правило total-stay закрыто |
| Налоги и сборы | В наблюдениях `taxData` пуст, отдельных fee-полей нет | Включение в `shownPrice` неизвестно |
| Пагинация | Наблюдались `nextOffset` 50/100 и offset pagination | Факт закрыт; orchestration отложена до Stage 9.13 |
| Отзывы | `review` в DTO nullable | Преобразование в domain заблокировано |
| Доступность | `availableRoomsCount > 0` подтверждает наличие доступных комнат | Порог `LIMITED` неизвестен |
| Удобства | В list response amenities отсутствуют | Это unknown, а не доказанный пустой список |
| Публичные endpoint-ы | Анонимные запросы технически прошли | Официальный server-to-server статус неизвестен |

### 2.1 Дополнительный аудит публичного UI

Read-only аудит публичного UI подтвердил дополнительные факты:

- guest rating отображается по шкале 10 отдельно от звездной категории отеля;
- `starRating` нельзя использовать вместо отсутствующего guest rating;
- amenities доступны на странице details, но не подтверждены в list response;
- среди первых 50 предложений отсутствующий review не встретился, поэтому
  nullable response shape остается контрактной возможностью, а не наблюдаемым
  примером;
- включение taxes/fees в `shownPrice` и порог `LIMITED` не установлены.

Network details, которые могли содержать session headers, не переносились в
документацию. SLA, rate limits и официальный server-to-server статус остаются
важными для live/runtime integration, но не блокируют изолированные изменения
контрактов и unit tests mapper.

## 3. Сверка текущих типов

### 3.1 Преобразование запроса

`HotelsApiSearchRequestDto` уже выражает:

- числовой `destinationId`;
- строки дат;
- `guests[]` с `adultsCount` и `childrenAge`;
- nullable `offset` и `limit`.

Текущий `HotelSearchCriteria` содержит `destination: String`, `rooms: Int?` и
только агрегат `guests.children: Int`. Поэтому безопасный request mapper не
может получить индивидуальные возраста детей и не может проверять соответствие
их количества без дополнительного типизированного входа.

Когда mapper будет разрешен, его вход должен получать выбранный
`destinationId` явно из location resolution. Нельзя парсить
`HotelSearchCriteria.destination`, использовать `hotels[].id` или автоматически
выбирать первый autocomplete result.

Правило rooms для будущего mapper:

- принимать только явно подтвержденную одну комнату;
- отклонять несколько guest groups;
- не объединять комнаты и не терять occupancy;
- не подставлять возраста детей по их количеству.

### 3.2 Преобразование ответа

`HotelsApiSearchResponseDto.Hotel.review` nullable, но `HotelOffer.rating` и
`HotelOffer.reviewCount` обязательны. Текущий API response также всегда создает
объект `rating`. Подстановка `0.0`/`0` нарушила бы действующее продуктовое
правило: отсутствующий rating должен оставаться unknown.

Кроме того, `HotelOfferResponse` безусловно указывает scale `10.0`. Наблюдения
не заменяют решения о шкале и источнике rating для присутствующего review.

Та же проблема существует для amenities. `HotelOffer.amenities` — обязательный
список, а `HotelOfferResponse` помечает каждый его элемент как
`provider_fact`. Пустой список не позволяет отличить «provider подтвердил
отсутствие» от «list endpoint не вернул данные».

Исторический Stage 9.2 предлагал подставлять `0.0`/`0` для отсутствующего
rating и пустой список для отсутствующих amenities. Эти рекомендации не
применимы к текущему mapper: явная задача и продуктовый baseline имеют более
высокий приоритет и требуют сохранять отсутствующие provider facts как unknown.
Исторический artifact при этом не переписывается.

Из полей, уже готовых к безопасному преобразованию:

- `providerReference <- hotelId` без разбора или конструирования provider id;
- `hotelName <- hotelName`;
- `city <- areaLocation.destinationName`;
- `country <- areaLocation.countryName`;
- `totalPrice <- rateForHotelsFeed.shownPrice.amount`;
- `currency <- rateForHotelsFeed.shownPrice.currency`;
- `freshness <- UNKNOWN` при отсутствии provider timestamp;
- `availability <- AVAILABLE` при `availableRoomsCount > 0`, иначе `UNKNOWN`.

`LIMITED` нельзя выводить из произвольного количества комнат. До отдельного
решения о пороге будущий mapper не должен создавать этот статус.

## 4. Канонический формат исходящих дат

Для будущего Stage 9.11c выбирается проектная policy:

```text
ISO_LOCAL_DATE: YYYY-MM-DD
```

Причины:

- исходный тип — `LocalDate`, без времени и часового пояса;
- этот формат последовательно наблюдался в web runtime;
- `/api/v1/hotels/search` принял date-only;
- преобразование в UTC midnight добавило бы несуществующую временную семантику.

Это решение не утверждает, что provider требует только date-only. DTO остается
строковым, а будущий unit test должен проверять точное значение, например
`2026-07-18`, без `T00:00:00Z`.

## 5. Закрытые и оставшиеся вопросы

### Закрыто для будущего mapper

- одна комната и один элемент `guests`;
- допустимый детский возраст `0..17` включительно;
- location id и hotel id имеют разные назначения;
- `shownPrice` и currency переносятся как total за весь stay без пересчета;
- cashback, discount, taxes и fees не прибавляются и не вычитаются;
- даты сериализуются как `YYYY-MM-DD` по проектной policy;
- положительное `availableRoomsCount` дает `AVAILABLE`, остальные значения —
  `UNKNOWN`; `LIMITED` не создается;
- nullable pagination fields не запускают pagination orchestration.

### Принятые решения перед реализацией

1. **Вход детских возрастов.** `childrenAges` становится каноническим
   типизированным значением. Каждый явно указанный ребенок требует возраста;
   без полного списка возрастов поиск не запускается, а Assistant запрашивает
   недостающие значения. Переходное поле `children` сохраняется в публичном
   контракте и должно согласовываться с размером списка.
2. **Unknown review/rating.** `rating` и `reviewCount` становятся nullable.
   Отсутствующие значения не заменяются через `starRating`, `0.0` или `0` и не
   отправляются в JSON. Внутри одинаковой availability предложения с известным
   rating ранжируются выше предложений без rating.
3. **Unknown amenities.** `amenities` становится nullable. Отсутствие данных в
   list response выражается как `null` и не отправляется в JSON; пустой список
   остается отдельным значением, если provider действительно вернул пустой
   набор.

Эти решения требуют отдельных контрактных этапов до mapper: Stage 9.11b2 для
guest occupancy, Stage 9.11b3 для partial `HotelOffer` facts и Stage 9.11b4 для
выравнивания публичного контракта.

### Открыто, но не требует выдуманного значения в mapper

- включение taxes/fees в `shownPrice`: сохранять `unknown`;
- порог для `LIMITED`: не использовать этот статус до отдельного правила;
- официальный server-to-server статус, rate limits и стабильность endpoint-ов:
  блокируют live/runtime integration, но не изолированные unit tests mapper;
- pagination budget и polling: относятся к Stage 9.13 и более поздним этапам.

## 6. Решения владельца и оставшиеся неизвестные

Владелец подтвердил:

- запрашивать возраст каждого ребенка и не запускать поиск без полного списка;
- добавить `childrenAges` совместимо с текущим `children`;
- выражать отсутствующие rating, review count и amenities через nullable поля;
- не отправлять unknown поля в JSON;
- ранжировать неизвестный rating после известного внутри одинаковой
  availability.

Остаются неизвестными, но не требуют выдуманного значения в mapper:

- включение taxes/fees в `shownPrice`;
- порог `LIMITED`, поэтому mapper не должен создавать этот статус;
- официальный server-to-server статус, SLA и rate limits публичных endpoint-ов.

## 7. Граница этапа

В рамках этой сверки не выполнены:

- production mapper и unit tests mapper;
- изменения DTO, domain, ranking или API response;
- transport calls, fixtures и provider orchestration;
- pagination, polling, `etag` и `waitLoadingCompleted`;
- runtime wiring и активация `RealHotelOfferProviderAdapter`;
- public API, OpenAPI Travel Assistant, frontend или generated clients;
- booking/payment, `bookHash`, storage, auth или infrastructure changes.

Исторические review artifacts не переписывались.

## 8. Verdict

Readiness reconciliation завершена: обязательные product policies приняты, но
Stage 9.11c **пока не готов к implementation**, потому что действующие модели и
публичный контракт еще не выражают эти решения.

Следующие безопасные шаги — отдельные Stage 9.11b2, Stage 9.11b3 и Stage
9.11b4. После их успешного завершения Stage 9.11c может остаться узким
mapper-only этапом с точечными unit tests, без transport/runtime/pagination.

## 9. Связанные документы

- [Основной roadmap](../roadmap/roadmap.md)
- [Stage 9.11b readiness gate](stage-9-11b-provider-target-and-mapping-policy-readiness-gate.md)
- [Stage 9.11a search DTO](stage-9-11a-hotels-api-search-dto-without-domain-mapping.md)
- [Stage 9.10 location resolution](stage-9-10-autocomplete-location-resolution-contract-boundary.md)
- [Наблюдение публичного web-flow](stage-9-tbank-web-hotel-search-contract-observation.md)
