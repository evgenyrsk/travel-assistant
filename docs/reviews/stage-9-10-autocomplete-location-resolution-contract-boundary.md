# Stage 9.10 — Контрактная граница autocomplete/location resolution

## 1. Цель и границы

Stage 9.10 фиксирует внутреннюю границу между пользовательским названием
направления и числовым `destinationId`, который нужен поиску Hotels API.

Основанием послужили предоставленные владельцем проекта фотографии актуального
контракта autocomplete. Фотографии не копировались в репозиторий и не считаются
машиночитаемым примером ответа.

Этап не вызывает autocomplete, не подключается к runtime и не меняет
`RealHotelOfferProviderAdapter`.

## 2. Подтвержденный контракт

Зафиксированы:

- путь `/api/v1/hotels/autocomplete`;
- `operationId=getSuggestions`;
- обязательный JSON-параметр `query`;
- необязательный `X-User-Language` со значениями `RU` и `EN`, значение по
  умолчанию на стороне API — `RU`;
- обязательная оболочка `payload`;
- опциональные группы `locations` и `hotels`;
- числовой `locations[].id`;
- строковый `hotels[].id`;
- обязательные `name`, `signature`, `type.code` и `type.name` для подсказок;
- допустимая длина запроса от 1 до 50 символов;
- `400` с кодом `Validation` для пустого запроса;
- ограниченная выдача без дублей, формируемая самим provider API.

Внутренние поисковые стратегии, устройство индекса и справочники отображаемых
названий не переносятся в клиентскую логику.

## 3. Добавленные файлы production-кода

Application boundary:

- `HotelLocationResolverBoundary.kt`;
- `HotelLocationResolutionRequest.kt`;
- `HotelLocationResolution.kt`.

Provider DTO и mapper:

- `HotelsApiAutocompleteRequestDto.kt`;
- `HotelsApiAutocompleteResponseDto.kt`;
- `HotelsApiAutocompleteLocationMapper.kt`.

## 4. Вход и результат boundary

`HotelLocationResolverBoundary` принимает `HotelLocationResolutionRequest`:

- исходный `query`;
- необязательный язык `RU` или `EN`.

Результат `HotelLocationResolution` содержит упорядоченный список кандидатов:

- `destinationId`;
- локализованные `name` и `signature`;
- тип с `code` и `name`.

Boundary не выбирает один результат автоматически. Политика выбора при
нескольких совпадениях относится к будущей оркестрации.

## 5. DTO и правила преобразования

`HotelsApiAutocompleteRequestDto` отражает только поле `query`.

`HotelsApiAutocompleteResponseDto` моделирует обе группы ответа, но
`HotelsApiAutocompleteLocationMapper` преобразует только `locations`:

- `locations[].id` становится `destinationId`;
- порядок provider API сохраняется;
- отсутствующий `locations` дает пустой список;
- `hotels[].id` не преобразуется в `destinationId`, потому что имеет строковый
  тип и иную семантику.

Подсказки отелей остаются provider DTO до отдельного продуктового решения.

## 6. Ограничения query и языка

DTO сохраняет `query` без обрезки и нормализации. Stage 9.10 не решает, должен
ли клиент отклонять строку длиннее 50 символов или полагаться на обрезку на
стороне сервера. Проверка пустого `query` также не добавлена в модели
application/domain слоев.

`RU`/`EN` представлены типизированным enum во входе application boundary, но
пока не преобразуются в HTTP header, поскольку transport adapter autocomplete
не входит в этот этап.

## 7. Неоднозначности контракта

До реального вызова нужно проверить машиночитаемым Swagger или обезличенным
примером JSON:

- точную форму `payload`;
- что `type` является объектом `{code, name}`, несмотря на пометку `array` в
  одной таблице документации;
- целевой host и авторизацию endpoint;
- точную JSON-форму ошибки `400`;
- политику выбора кандидата при нескольких элементах `locations`.

Текущая модель следует вложенным таблицам `payload` и `type`, но еще не
проверена реальным примером ответа.

## 8. Граница без transport/runtime wiring

Не изменены:

- `PublicHotelsApiHttpTransport`;
- `RealHotelOfferProviderAdapter` и `HotelOfferProviderFactory`;
- `Application.kt`, routes и runtime composition;
- `HotelSearchCriteria` и `HotelOffer`;
- публичный API и OpenAPI Travel Assistant;
- frontend и generated clients.

HTTP client не создается, сетевые вызовы не выполняются, `Authorization` и
credentials не используются. `FAKE` остается provider по умолчанию.

## 9. Тесты

Добавлены:

- `HotelsApiAutocompleteDtoTest` — JSON request/response, необязательные группы,
  неизвестные поля и обязательные поля;
- `HotelsApiAutocompleteLocationMapperTest` — порядок `locations`, mapping
  типов, пустой результат и запрет преобразования hotel id в destination id.

Все JSON-примеры синтетические.

Результаты проверок:

| Проверка | Результат |
|---|---|
| Targeted autocomplete DTO/mapper tests | Пройдена |
| Полный набор backend-тестов | Пройден |
| `git diff --check` | Пройдена |
| Реальные сетевые запросы | Не выполнялись |

## 10. Риски и ограничения

- Нет подтвержденного реального примера ответа.
- Boundary еще не имеет transport adapter и не вызывается оркестрацией.
- Автоматический выбор первого совпадения не разрешен.
- Подсказки отелей не участвуют в MVP location resolution.
- Валидация и нормализация `query` отложены.

## 11. Следующий этап

Stage 9.11b должен быть отдельным этапом преобразования search DTO в
`HotelSearchCriteria`/`HotelOffer` после решений по размещению гостей, возрастам
детей, timezone, nullable review/rating и составу `shownPrice`.

Подключение autocomplete к `PublicHotelsApiHttpTransport` и orchestration
допустимо только после подтверждения host, авторизации и примера ответа, без
изменения routes или выбора `REAL` по умолчанию.

## 12. Итог

Контрактная граница location resolution готова для будущего provider adapter:
числовые location ids изолированы от строковых hotel ids, provider DTO не
проникают в application model. Реальный autocomplete и runtime real provider
по-прежнему не активированы; production readiness не заявляется.
