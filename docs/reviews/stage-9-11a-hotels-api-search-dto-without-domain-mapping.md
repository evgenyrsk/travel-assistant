# Stage 9.11a — DTO поиска Hotels API без преобразования доменных моделей

## 1. Цель и границы

Stage 9.11a добавляет минимальные внутренние DTO для формата JSON
`POST /api/v1/hotels/search`. Этап отделяет форму JSON provider от будущих
решений о `destinationId`, размещении гостей и преобразовании в
`HotelSearchCriteria`/`HotelOffer`.

DTO не подключены к `PublicHotelsApiHttpTransport`, real adapter, routes или
композиции runtime. Реальные запросы не выполняются.

## 2. DTO запроса

`HotelsApiSearchRequestDto` моделирует подтвержденные Swagger-поля:

- `destinationId`;
- `checkinDate` и `checkoutDate` как строки формата provider;
- `guests[]` с обязательным `adultsCount` и nullable `childrenAge`;
- nullable `offset` и `limit`.

DTO не проверяет часовой пояс, диапазоны дат, количество гостей, возраст детей,
распределение по комнатам или правила пагинации. Эти правила принадлежат
будущему слою преобразования и оркестрации.

## 3. DTO ответа

`HotelsApiSearchResponseDto` моделирует минимальный набор для будущего
преобразования:

- обязательную оболочку `payload`;
- `filteredHotelsCount`, `hotelsTotalCount`, `isLoadingCompleted`,
  `nextOffset`, `hotelsMinPrice`;
- идентификатор, название и звездность отеля;
- область расположения, адрес и координаты;
- nullable изображения и review;
- доступное количество комнат;
- `shownPrice`, `paymentPlace`, данные об оплате картой;
- nullable поля бесплатной отмены и питания.

`currency` и `paymentPlace` хранятся строками. Новое значение provider не
приводит к ошибке десериализации enum до отдельного продуктового решения.

Сложные filters, cashback, category и другие отложенные поля не моделируются и
безопасно пропускаются.

## 4. JSON configuration

`HotelsApiJson` использует отдельный внутренний `Json` codec:

- `ignoreUnknownKeys = true`;
- `explicitNulls = false`;
- `isLenient = false`;
- `coerceInputValues = false`;
- `encodeDefaults = false`.

Неизвестные поля не ломают совместимость, но отсутствие моделируемых
обязательных полей остается `SerializationException`. Codec не исправляет
невалидные значения и не выполняет проверку доменных правил.

## 5. Production files

Добавлены:

- `HotelsApiJson.kt`;
- `HotelsApiSearchRequestDto.kt`;
- `HotelsApiSearchResponseDto.kt`.

`PublicHotelsApiHttpTransport`, `RealHotelOfferProviderAdapter`, доменные модели,
API DTO и композиция runtime не изменены.

## 6. Тесты

`HotelsApiSearchDtoTest` проверяет:

- точные имена полей JSON-запроса;
- отсутствие nullable полей запроса при `null`;
- сохранение нескольких элементов `guests[]` и возрастов детей;
- разбор hotel, location, price, review и pagination;
- явный `null` и отсутствие review;
- пропуск filters, cashback и неизвестных полей provider;
- сохранение неизвестных значений currency и payment place;
- ошибку при отсутствии обязательных `payload`, `hotelId` или `shownPrice`.

Fixture полностью синтетический и составлен по Swagger. Он не считается
обезличенным ответом реального API и не заменяет Stage 9.14.

### 6.1 Результаты проверок

| Проверка | Результат |
|---|---|
| Targeted `HotelsApiSearchDtoTest` | Пройдена |
| Полный набор backend-тестов | Пройден |
| `git diff --check` | Пройдена |
| Реальные сетевые запросы | Не выполнялись |

## 7. Совместимость

Не изменены:

- `HotelSearchCriteria` и `HotelOffer`;
- публичный API и OpenAPI Travel Assistant;
- routes, frontend и generated clients;
- HTTP transport и provider factory;
- `FAKE` как provider по умолчанию;
- Stage 7/8 hotel-search и confirmation behavior.

## 8. Что отложено

- получение `destinationId` через autocomplete/location;
- преобразование `LocalDate` в provider date-time;
- размещение взрослых и детей по комнатам;
- проверка допустимого возраста детей;
- nullable semantics rating/review в domain;
- состав `shownPrice` и трактовка taxes/fees;
- availability policy;
- фактический fixture provider;
- вызов транспорта, сетевой движок и подключение к runtime.

## 9. Следующие этапы

Stage 9.10 остается заблокирован до получения отдельного контракта
autocomplete/location. После его получения следует реализовать
`HotelLocationResolverBoundary` и provider DTO этого endpoint.

Stage 9.11b должен добавить mapper только после решений по destination,
размещению гостей, возрасту детей, часовому поясу, nullable rating и семантике
`shownPrice`.

## 10. Verdict

Минимальная форма JSON поиска изолирована в provider DTO без изменения доменных
моделей и поведения runtime. DTO готовы к проверке реальным обезличенным fixture
и будущему mapper, но сами по себе не означают готовность интеграции real
provider.
