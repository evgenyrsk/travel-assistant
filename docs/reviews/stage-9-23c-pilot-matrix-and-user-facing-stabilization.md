# Stage 9.23c — pilot-матрица и стабилизация пользовательского текста

## Роль документа

Это отчет о завершении ограниченной pilot-матрицы chat-first MVP и исправлении
наблюдений Stage 9.23b. Текущий статус проекта задает
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

Проверить оставшиеся сценарии внутреннего пилота без лишних внешних вызовов,
устранить три наблюдения успешного сценария и определить, достаточно ли
доказательств для закрытия Stage 9.

## Исправленные наблюдения

| Наблюдение | Решение | Проверка |
|---|---|---|
| Финальный ответ и `matchSummary` были на английском | Встроенные пользовательские сообщения и объяснения ранжирования переведены на русский; инструкция LLM требует уточняющий вопрос на русском | Backend и frontend tests |
| В выдаче встретилось имя `МА` | Короткое непустое имя сохранено как provider fact; эвристическая замена запрещена | Mapper и frontend regression tests |
| `/favicon.ico` возвращал `404` | Добавлен локальный `favicon.svg`, подключенный к основной и диагностической страницам | Static test, frontend build и локальный browser smoke |

Длина имени сама по себе не доказывает ошибку данных. Mapper не должен
придумывать название, раскрывать другой идентификатор или удалять предложение
без отдельного контрактного правила.

Frontend также не показывает произвольный нерусский текст технической ошибки:
он заменяется безопасным русским сообщением. Русский текст, уже подготовленный
application/frontend слоем, сохраняется без изменений.

## Pilot-матрица

| Сценарий | Доказательство | Результат |
|---|---|---|
| Направление → уточнение дат → гости | `AssistantHotelConstraintsConversationIntegrationTest` | Контекст сохраняется, поиск начинается только после подтверждения |
| Ребенок без возраста | `AssistantHotelConstraintsConversationIntegrationTest` | Запрашивается возраст, `hotelSearchId` до ответа отсутствует |
| Неоднозначная location | `ProviderSeamIntegrationTest` | Первый candidate не выбирается, search endpoint не вызывается |
| Отказ от confirmation | `AssistantHotelConstraintsConversationIntegrationTest` | Pending state очищается, поиск и `hotelSearchId` не создаются |
| Flight/unsupported request | `ProviderSeamIntegrationTest` и frontend flow test | Возвращается безопасная граница, Hotels API не вызывается |
| Отказ OpenRouter | `OpenRouterRuntimeIntegrationTest` | Внутренние данные скрыты, Hotels API не вызывается |
| Отказ Hotels API | `ProviderSeamIntegrationTest` и `RealHotelsRuntimeFailureIntegrationTest` | Нет `hotelSearchId`, pending state не потребляется, provider data не раскрываются |
| Неизвестные rating/amenities | Mapper, fixture и frontend tests | Не подставляются нули и выдуманные amenities |
| Price/currency | `HotelsApiProviderFixtureContractTest` | Значения переносятся без пересчета |

Отказные ветки используют `MockEngine` и application test seams. Дополнительные
live-вызовы OpenRouter или Hotels API не потребовались: Stage 9.23b уже
подтвердил один контролируемый успешный сквозной сценарий.

## Актуализация baseline

`product-baseline.md` и `architecture-baseline.md` минимально приведены к
фактическому состоянию Stage 8–9. Удалены утверждения, что chat-first frontend,
`LlmClient`, OpenRouter adapter и REAL Hotels adapter еще только предстоит
создать. Исторические stage/review artifacts не переписывались.

## Проверки

- точечные backend tests pilot-матрицы и `OpenRouterLlmClientTest` пройдены;
- полный backend `./gradlew test` пройден;
- frontend `npm test`, `npm run lint` и `npm run build` пройдены;
- локальный browser smoke получил `200` для страницы, скриптов, стилей и
  `/favicon.svg`; console/page errors отсутствовали;
- `git diff --check` пройден;
- новые и измененные локальные ссылки проверены вручную.

## Оставшиеся ограничения

Закрытие pilot-матрицы не устраняет внешние и будущие ограничения:

- официальный server-to-server статус и долгосрочная стабильность публичных
  Hotels endpoints не подтверждены;
- SLA и rate limits не зафиксированы;
- включение taxes/fees в `shownPrice` остается неизвестным;
- `LIMITED` не создается без подтвержденного правила;
- stores остаются process-local;
- auth, booking, payment, flights и production hardening не входят в MVP.

Эти пункты не блокируют завершенную внутреннюю проверку, но запрещают заявление
о готовности к промышленному использованию.

## Границы

Не изменены public API, OpenAPI, DTO, provider mapping, provider transport,
runtime modes, pagination и persistence. `FAKE` остается режимом по умолчанию
для LLM и Hotels provider. Новые внешние вызовы и зависимости не добавлены.

## Verdict

`PASS_INTERNAL_PILOT_MATRIX_COMPLETE_STAGE_9_CLOSED`.

Stage 9.23 и Stage 9 завершены в ограниченных границах внутреннего chat-first
MVP. Это не production readiness. Stage 10 не начинается автоматически;
следующий безопасный шаг — отдельная planning/readiness задача для выбора
направления дальнейшего развития.
