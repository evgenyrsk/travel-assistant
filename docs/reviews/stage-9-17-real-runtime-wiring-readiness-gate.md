# Stage 9.17 — gate готовности REAL runtime wiring

## Роль документа

Этот документ является review-артефактом Stage 9.17. Он проверяет готовность
реального provider flow к runtime wiring и не активирует `REAL`.
Актуальный порядок следующих шагов определяет
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

После успешного Stage 9.16 определить, можно ли безопасно подключить
`RealHotelOfferProviderAdapter` к application runtime при сохранении `FAKE` по
умолчанию.

## Подтвержденная готовность

- публичный `POST /api/v1/hotels/search` доступен без `Authorization` из
  текущей среды;
- один bounded request вернул 20 hotels, которые успешно прошли текущие DTO и
  response mapper;
- request/response mapping и single-page policy проверены;
- transport ограничивает относительный path и public host;
- provider errors не раскрывают raw body;
- `FAKE` остается default configuration mode.

Этого достаточно для продолжения изолированной интеграционной работы, но
недостаточно для runtime wiring.

## Найденные блокеры

| Область | Текущее состояние | Почему wiring небезопасно |
|---|---|---|
| Async boundary | `HotelOfferProviderBoundary.search()` и `HotelSearchBoundary.createSearch()` синхронные; transport и orchestrator используют `suspend` | Потребовался бы `runBlocking` в production adapter либо несогласованное широкое изменение application flow |
| Provider result | Boundary возвращает только `List<HotelOffer>` | Нельзя выразить `LocationNotFound`, `LocationSelectionRequired` и mapping rejection без ложного `completed_no_offers` или generic failure |
| Location resolver | Transport-backed реализация отсутствует | Runtime получает destination text, а search требует числовой `destinationId` |
| Autocomplete contract | Internal DTO использует `query`; подтвержденный public endpoint использует `input` | Нельзя молча отправить существующий DTO в другой API или объединить два provider contract |
| Selection policy | Автоматический выбор первого candidate запрещен | Текущий hotel-search result/public response не умеет вернуть список location candidates для явного выбора |
| Configuration | `REAL` требует private JWT key, хотя выбранный public search анонимный | Public-only flow нельзя включить без искусственного private credential |
| Network engine lifecycle | CIO остается только в test dependencies | Production client ownership, shutdown и timeout policy еще не определены |
| Error mapping | `HotelProviderException` попадает в общий `500 INTERNAL_ERROR` | Runtime не различает unavailable, timeout, rate limit и location clarification |

## Sync/suspend verdict

`runBlocking` внутри `RealHotelOfferProviderAdapter` не разрешен:

- он скрывает I/O за синхронной domain boundary;
- может блокировать Ktor request thread;
- усложняет timeout и cancellation;
- закрепляет временный seam перед дальнейшим assistant flow.

Безопасное направление — отдельное согласование асинхронной provider/application
границы и ее последовательное распространение через use cases и routes.

## Result contract verdict

`List<HotelOffer>` недостаточен для provider-backed location resolution.
Будущий contract должен явно различать как минимум:

- offers получены;
- location не найдена;
- требуется выбор location;
- request mapping отклонен;
- provider response отклонен;
- provider временно недоступен.

До принятия такого contract нельзя преобразовывать неоднозначное направление в
пустой список. `COMPLETED_NO_OFFERS` допустим только после выполненного поиска,
который действительно не вернул offers.

## Autocomplete verdict

Для runtime нужен отдельный transport-backed
`HotelLocationResolverBoundary`. Подтвержденный endpoint:

```text
POST /search-api/search/autocomplete
{"input":"<destination>"}
```

Он не является тем же request contract, что
`HotelsApiAutocompleteRequestDto(query)`. Если public autocomplete остается
выбранным resolver source, ему нужны отдельный request DTO, отдельные contract
tests и явное решение о допустимом переиспользовании либо дублировании response
DTO.

Автоматический выбор первого результата не добавляется.

## Configuration verdict

Текущий `HotelProviderConfig` fail-closed, но связывает public search mode с
private JWT configuration. Перед runtime wiring нужно разделить:

- обязательную public configuration для анонимного search/resolver flow;
- private target/JWT configuration, требуемую только для операций, которые
  действительно используют private API.

Ослаблять проверку private key без такого разделения нельзя.

## Что не входит в Stage 9.17

- production code и tests;
- изменение `HotelOfferProviderBoundary` или `HotelSearchBoundary`;
- autocomplete transport implementation;
- CIO в production dependencies;
- `RealHotelOfferProviderAdapter`, factory или `Application.kt` wiring;
- новые live calls;
- public API/OpenAPI/frontend changes;
- retries, pagination, polling, JWT signing или private API;
- durable storage, booking/payment и `bookHash`.

## Риски преждевременного wiring

- блокировка request threads через `runBlocking`;
- неверный `completed_no_offers` для неоднозначной location;
- автоматический выбор неверного destination;
- обязательный фиктивный private secret для public flow;
- утечка provider lifecycle в generic `500`;
- HttpClient без явного владельца и корректного shutdown;
- скрытая зависимость runtime от public web contract.

## Рекомендуемая последовательность

1. **Stage 9.17a — async provider/result contract reconciliation.**
   Зафиксировать асинхронную границу, typed outcomes и влияние на direct
   hotel-search и assistant confirmation flows. Сначала review/design-only.
2. **Stage 9.17b — autocomplete resolver transport adapter.**
   Отдельный `input` request DTO, `MockEngine` tests, без runtime wiring и live
   calls.
3. **Stage 9.17c — opt-in REAL runtime wiring.**
   Production client lifecycle, public-only configuration, adapter/factory
   composition и route/runtime tests; `FAKE` остается default.
4. **Stage 9.18 — integration closure.**
   Regression, failure behavior и документационное закрытие без заявления
   production readiness.

## Readiness verdict

`NOT_READY_FOR_REAL_RUNTIME_WIRING`.

Stage 9.16 подтвердил работоспособность public search API, но текущие
application/provider contracts не позволяют безопасно подключить его к
runtime. Следующий разрешенный шаг — Stage 9.17a как review/design-only
согласование async и typed result boundaries.

## Связанные документы

- [Stage 9.16 — первый контролируемый QA call](stage-9-16-first-controlled-hotels-api-qa-call.md)
- [Stage 9.15 — sandbox readiness gate](stage-9-15-sandbox-readiness-gate.md)
- [Stage 9.12 — search orchestration](stage-9-12-hotels-api-search-orchestration-without-runtime-wiring.md)
- [Stage 9.10 — location resolution boundary](stage-9-10-autocomplete-location-resolution-contract-boundary.md)
- [Основной roadmap](../roadmap/roadmap.md)
