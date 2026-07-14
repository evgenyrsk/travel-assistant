# Stage 9.13 — один ограниченный пул hotel candidates

**Роль:** review artifact завершенного provider orchestration этапа. Документ
фиксирует single-page policy для hotel-only MVP и осознанный перенос pagination
до появления подтвержденного пользовательского сценария.

## Цель и исходная точка

Stage 9.12 связал location resolution, один вызов
`POST /api/v1/hotels/search` и domain mapping без runtime wiring. Следующим
кандидатом roadmap была bounded pagination, однако продуктовый baseline
ориентирован на ассистентскую подборку и сравнение 2-5 вариантов, а сценарий
«показать еще» не определен.

Цель Stage 9.13 — ограничить первичный provider candidate pool без добавления
повторных запросов и не превращать travel assistant в каталожный поиск.

## Принятая policy

- выполняется ровно один Hotels API search call;
- request содержит `offset=0` и `limit=20`;
- после provider mapping возвращается не более 20 уникальных предложений;
- дедупликация выполняется существующим response mapper по opaque
  `providerReference` до применения ограничения;
- первое стабильное вхождение сохраняется;
- `nextOffset` и `isLoadingCompleted` остаются частью provider DTO, но не
  запускают pagination, polling или дополнительный запрос;
- assistant presentation 2-5 вариантов не реализуется в provider layer.

Если provider вернет больше запрошенного limit, orchestration детерминированно
сохраняет первые 20 уникальных mapped offers. Это bounded safety policy, а не
утверждение о provider ranking semantics.

## Изменения production code

Изменен только internal `HotelsApiSearchOrchestrator`:

1. к request DTO после domain mapping добавляются `offset=0` и `limit=20`;
2. mapped offers ограничиваются первыми 20 элементами;
3. количество HTTP calls остается равным одному.

`HotelsApiSearchRequestMapper` остается независимым от pagination policy и
продолжает возвращать nullable `offset`/`limit`. Это сохраняет разделение между
domain-to-provider mapping и orchestration request window.

## Тесты

Targeted `HotelsApiSearchOrchestratorTest` подтверждает:

- точные `offset=0` и `limit=20` в JSON request;
- `isLoadingCompleted=false` и `nextOffset` не создают второй HTTP call;
- ответ меньше 20 уникальных элементов сохраняется полностью;
- дубликаты удаляются до применения limit;
- ответ больше 20 уникальных элементов ограничивается первыми 20;
- result содержит весь bounded candidate pool, а не presentation top 5;
- все HTTP-проверки используют только `MockEngine`.

## Product и runtime boundary

Product baseline уточнен:

- provider-backed candidate pool — до 20;
- assistant presentation — 2-5 вариантов;
- automatic pagination и «показать еще» не входят в текущий MVP flow.

Stage 9.13 не подключает orchestrator к `RealHotelOfferProviderAdapter`,
`HotelOfferProviderFactory`, `Application.kt`, routes или runtime storage.
`CreateHotelSearchUseCase` и `HotelOfferRanker` не изменены. После будущего
runtime wiring они смогут получить весь bounded pool, но такое wiring не входит
в этот этап.

## Документация и статусы

- primary roadmap отмечает Stage 9.13 завершенным;
- следующим отдельным этапом указан Stage 9.14 fixture verification;
- pagination перенесена до отдельного продуктового решения;
- `docs/ROADMAP.md` и root `README.md` не изменены, поскольку остаются
  корректными navigation documents;
- исторические Stage 9.7 и Stage 9.12 review artifacts не переписывались.

## Что не входит в этап

- pagination, polling, retry, `etag` или повторные provider calls;
- ограничение public/assistant response до top 5;
- live API calls и fixture capture;
- autocomplete transport implementation;
- REAL adapter и runtime wiring;
- routes, public API, OpenAPI, frontend или generated clients;
- auth/JWT, secrets, durable storage, booking/payment и `bookHash`.

## Риски и ограничения

- качество provider ordering не подтверждено официальным contract;
- отсутствие pagination означает, что кандидаты за пределами первой страницы
  не рассматриваются;
- taxes/fees semantics и threshold для `LIMITED` остаются неизвестными;
- официальный server-to-server статус публичных endpoints не подтвержден.

Эти ограничения допустимы для текущего isolated MVP integration slice и не
являются заявлением production readiness.

## Verdict

Stage 9.13 завершен как bounded single-page candidate window: один запрос,
до 20 уникальных offers, без pagination и runtime wiring. Следующий
roadmap-кандидат — Stage 9.14: проверка DTO и mapper-ов на обезличенных provider
fixtures по отдельной явной задаче.

## Связанные документы

- [Stage 9.12 — search orchestration](stage-9-12-hotels-api-search-orchestration-without-runtime-wiring.md)
- [Stage 9.11c — search domain mapping](stage-9-11c-hotels-api-search-domain-mapping.md)
- [Product baseline](../product/product-baseline.md)
- [Основной roadmap](../roadmap/roadmap.md)
