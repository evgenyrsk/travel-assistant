# План развития journey-инструментов MCP

**Роль документа:** рабочий план развития `tools/tbank-hotels-mcp` на ветке
`codex/tbank-hotels-mcp`. Он не меняет product roadmap, backend Travel
Assistant или границы MVP v1.

## Текущий checkpoint

| Пункт | Состояние |
| --- | --- |
| Версия MCP | Hotels `0.29.0`, Banking/broker `0.17.0`, local toolkit `0.15.0` |
| Transport | stdio, Node.js 20+, без браузера и cookie |
| Read-only search journey | Реализован и проверен fake transport tests и bounded current-worktree smoke; `breakfastIncluded` преобразуется в строгий provider filter без low-level перебора |
| Safe booking preview | Реализован без PII, booking draft и HTTP-вызова |
| Hosted payment form preview | Контракт и state machine сверены офлайн; локальный preview реализован без PII/payment credentials/HTTP, execution остаётся `NO-GO` |
| Hosted checkout handoff | Реализован безопасный переход на выбранный отель: для одной комнаты без детей сохраняются даты и число взрослых; PII, `bookHash`, token/card data, exact rate и provider write не переносятся |
| Customer reads | `GO`: `get_customer`, `list_bookings` и `get_booking_v1` прошли end-to-end read-only smoke через mobile broker; agent-facing order flow использует process-local `bookingRef` |
| Voucher handoff | Реализован безопасный local handoff: broker проверяет PDF и сохраняет owner-only файл с TTL; документ не входит в MCP JSON; проверено только fixture/fake transport после ранее зафиксированного read-only auth evidence |
| Реальные mutations | `NO-GO`; нужны подтверждённые auth/header/idempotency contracts и отдельное non-production approval |
| Load safety | Per-process concurrency `2`, bounded queue `32`, 30-second identical-search coalescing/cache; смена только локального ranking не повторяет provider search; проверено fake transport |
| Персонализация | Banking возвращает готовый typed `hotelPreferences`; `best_value` и диапазон за ночь применяются локально и мягко только при `preferencesApplied.applied=true`, без передачи банковских агрегатов или price filter провайдеру; provider total и MCP-derived цена за ночь разделены |
| Автоматические тесты | 71 Hotels + 52 Banking/broker/probe/smoke/packaging + 21 local toolkit tests и offline conformance обоих MCP; Unix-socket test выполняется вне ограниченной sandbox-среды |
| Следующий шаг | `0.29.0/0.17.0/0.15.0` опубликованы и проверены fresh install в Codex/OpenCode. Следующая отдельная задача — portability hardening; прямые booking/payment execution остаются future gate |

Checkpoint и границы сохранены в
`docs/reviews/tbank-hotels-mcp-0.8.0-progress-checkpoint.md`.

## 1. Исходная точка

Текущий MCP является API-driven stdio-сервером. Он предоставляет именованные
provider tools для поиска, фильтров, карты, карточки отеля, тарифов, кэшбэка,
отзывов, SEO/deeplink, авторизованных заказов, рассрочки, бронирования, отмены,
payment setup, промокодов и дополнительных услуг.

Низкоуровневые инструменты остаются необходимыми для полного доступа к Hotels
API. Обычный agent flow не должен требовать от MCP-клиента знания
`SearchParametersListApiRequest`, `destinationId` или provider identifiers.

## 2. Цель journey-слоя

Journey-инструменты должны уменьшить число действий клиента и скрыть лишнюю
связность API, не заменяя provider facts догадками и не убирая явное
подтверждение необратимых операций.

Целевой путь:

```text
plan_hotel_stay → compare_stay_options → create_booking_draft
→ validate_checkout → confirm_booking
```

Пользователь получает сравнимые варианты и подтверждает только финальную
операцию. Цена, доступность, условия отмены и кэшбэк должны оставаться данными
provider и обновляться перед бронированием.

## 3. Модель контекста

Первый срез использует short-lived process-local journey context:

- непрозрачный `journeyId` создаётся после поиска;
- search-контекст содержит параметры поиска и provider results; отдельный
  booking draft может временно содержать переданные контактные данные гостя
  только после подтверждённой локальной готовности booking execution;
- токен, auth headers, данные карты, пароль и OTP не сохраняются;
- journey и booking draft имеют независимый TTL 60 минут;
- перезапуск MCP удаляет контексты;
- persistence, cross-device resume, account history и фоновые уведомления не
  создаются.

Это намеренный компромисс для stdio MCP. Если понадобится восстановление после
перезапуска или мониторинг цен, потребуется отдельный storage/security design.

## 4. Целевой набор инструментов

| Этап | Инструмент | Результат | Статус |
| --- | --- | --- | --- |
| Локация | `tbank_hotels_resolve_destination` | Разрешает название в `destinationId`, возвращает кандидатов при неоднозначности | Реализовано |
| Поиск | `tbank_hotels_plan_stay` | Принимает typed intent, валидирует его, разрешает локацию и создаёт `journeyId` | Реализовано |
| Варианты | `tbank_hotels_get_stay_options` | Возвращает сохранённые provider options текущего поиска | Реализовано |
| Сравнение | `tbank_hotels_compare_stay_options` | Сравнивает явно выбранные или детерминированно ranked варианты без выдумывания facts | Реализовано |
| Выбор | `tbank_hotels_select_stay_option` | Связывает выбранный option с контекстом | Реализовано |
| Тарифы | `tbank_hotels_get_selected_stay_rates` | Загружает rates выбранного option без передачи `hotelId` и provider DTO | Реализовано |
| Preview | `tbank_hotels_create_booking_preview` | Показывает выбранный stay/rate и occupancy без PII, draft и HTTP-вызова | Реализовано |
| Checkout | `tbank_hotels_create_booking_draft` | Формирует черновик брони, не вызывая create endpoint | Реализовано; при недоступном execution PII не сохраняется и draft не создаётся |
| Проверка | `tbank_hotels_validate_checkout` | Повторно проверяет provider rate/availability перед действием | Реализовано |
| Оформление | `tbank_hotels_prepare_draft_booking`, `tbank_hotels_confirm_booking` | Использует отдельное явное подтверждение | Реальные мутации `NO-GO` до закрытия auth/contract gaps |
| Заказ | `tbank_hotels_get_booking_overview` | Сводит booking и optional voucher; в mobile broker-режиме принимает `bookingRef` | Реализовано |
| Отмена | `tbank_hotels_preview_cancellation` | Показывает provider данные до `prepare_cancel`; в mobile broker-режиме принимает `bookingRef` | Реализовано |
| Повтор | `tbank_hotels_repeat_stay_plan` | Создаёт новый поиск на основе текущего journey | Реализовано |
| Мониторинг | price/availability alerts | Требует durable storage и scheduler | Вне текущего MCP среза |

## 5. Правила реализации

- Journey tools используют только существующий API client и не принимают токен
  в MCP arguments.
- `plan_stay` принимает локацию, даты и комнаты в agent-facing форме; provider
  payload формируется внутри MCP и проверяется до сетевого вызова.
- Канонические поля `destination`, `rooms`, `maxOptions`, `adults` и
  `childrenAges` остаются рекомендуемыми. Ограниченные compatibility aliases
  для LLM-клиентов нормализуются локально до provider-вызова; конфликтующие
  alias/canonical пары и неизвестные поля отклоняются без network fallback.
- Обезличенный `hotelDefaults` Banking MCP передаётся как typed
  `hotelPreferences`. Диапазон цены остаётся мягким, не превращается в provider
  filter, а `best_value` отделяется от исходных provider facts как MCP-derived
  score. Варианты вне диапазона не скрываются.
- Обязательный завтрак задаётся semantic-полем `breakfastIncluded=true` и
  преобразуется внутри MCP в `meal_types=breakfast`. Клиент не должен получать
  каталог фильтров или угадывать low-level payload для этого сценария.
- Low-level search filters принимают только подтверждённые discriminator-формы
  `array`, `boolean`, `radio` и `range`; неизвестная форма отклоняется локально
  до provider-вызова.
- Ошибка обязательного provider-фильтра не приводит к unfiltered fallback:
  возвращается `requirements_unavailable` с запретом автоматического retry и
  перебора payload. Пустой результат возвращается как `no_matching_stays`.
- `plan_stay` использует подтверждённые `offset`, `limit`, `nextOffset` и
  `isLoadingCompleted` для bounded collection. Runtime не отправляет `sort`,
  поскольку production endpoint вернул `sorting_is_not_allowed_yet` несмотря
  на наличие enum в OpenAPI; ranking выполняется локально. `maxOptions`
  ограничивает только ответ, а не сохранённую journey-выборку.
- Неоднозначная локация не выбирается LLM автоматически: MCP возвращает
  кандидатов для пользовательского уточнения.
- Поиск конкретного отеля выполняется внутри результатов выбранной локации.
  Глобальный title-only поиск остаётся неподтверждённым до появления endpoint.
- Provider IDs и raw payload остаются доступны через provider tools; новый слой
  использует opaque `journeyId` и не делает их обязательными для обычного
  сценария.
- Сравнение показывает только поля, фактически вернувшиеся от provider. Если
  поле не найдено или неоднозначно, оно помечается как `unknown`.
- Деньги и время имеют отдельные presentation-поля. Timestamp сохраняет
  исходное UTC-смещение и не называется локальным временем отеля без timezone-
  факта; отсутствие cancellation fact показывается как «нет данных».
- Никакой journey tool не создаёт бронь, оплату, отмену или изменение тарифа
  без существующего stateless `prepare → execute` подтверждения.
- Booking draft удаляется после успешного `confirm_booking` или истечения TTL;
  его preview всегда redacted.
- Автоматизированные тесты используют только fake transport и не вызывают
  внешний Hotels API.
- Ошибки provider раскрывают только allowlisted code/request ID и не возвращают
  raw response body, токен или персональные данные.

## 6. Границы

Этот план не добавляет в core Travel Assistant новый backend API, persistent
storage, account/profile system, payment credential handling, background jobs,
generated clients или изменение `docs/roadmap/roadmap.md`.

## 7. Следующие шаги после независимого ревью

Исторический review и triage находятся в
`docs/reviews/tbank-hotels-mcp-qwen-3-8-max-review-follow-up.md`. Пункты ниже —
tool-local implementation sequence, а не этапы основного product roadmap.

### Шаг 0. Изолировать тесты от production credentials

**Статус:** завершён.

- [x] Перевести `startServer` на allowlist окружения вместо наследования всего
  `process.env`.
- [x] Добавить regression test с заведомо опасными `TBANK_HOTELS_*` в parent
  env и доказать, что child их не получает.
- [x] Гарантировать отсутствие внешнего DNS/HTTP во всём test suite.
- [x] Обновить README только после прохождения safety gate.

**Критерий готовности:** `npm test` безопасен при наличии реальных Hotels
credentials у родительского процесса; все provider calls остаются fake/local.

### Шаг 1. Закрыть agent-facing contract gaps

**Статус:** завершён для подтверждённых read/journey контрактов.

- [x] `get_selected_stay_rates` сам подставляет `checkInDate` и `checkOutDate`
  из journey; клиент передаёт только действительно дополнительные параметры.
- [x] `search_seo` использует отдельные schemas для v1, v2 и v3.
- [x] `payment_setup` не отправляет неподтверждённый request body.
- [x] Preview скрывает `firstName`, `lastName`, `middleName` и эквивалентные
  guest PII без сокрытия безопасных полей вроде `hotelName`.
- [x] Для каждого изменения добавлен focused regression test.

**Критерий готовности:** поиск → выбор → rates → draft можно пройти без знания
provider DTO; schema/runtime/tests согласованы.

### Шаг 2. Усилить MCP protocol и локальную безопасность

**Статус:** завершён для локальных границ MCP.

- [x] Реализовать `ping`.
- [x] Явно обработать JSON-RPC batch или вернуть корректную protocol error.
- [x] Добавить TTL к prepared confirmation и отклонять stale execute.
- [x] Запретить HTTP redirects для запросов с credentials.
- [x] Не сравнивать цены разных валют как одну числовую шкалу.
- [x] Ограничить крупные raw provider responses или добавить agent-facing
  summaries/pagination.

**Критерий готовности:** protocol edge cases и локальные safety boundaries
покрыты тестами; повторный execute после stale confirmation отклоняется.

### Шаг 3. Получить недостающие API-контракты

**Статус:** частично выполнен; оставшиеся пункты требуют владельца Hotels API.

- [ ] Закрыть auth/customer-context вопросы для mutations; mobile customer
  reads уже подтверждены для allowlist операций.
- [x] Типизировать доступные request schemas booking create, LS create, booking
  list, cancel, payment setup, promocode, extra services, tranches и BNPL.
- [x] Сверить отдельный `HotelsApi.Payments`: hosted payment form create,
  payment task status, response states и raw-card/3DS boundary; добавить
  preview-only tool без PII и payment credentials.
- [ ] Подтвердить происхождение обязательного `x-real-ip` и остальных customer
  headers для booking create вне browser session.
- [ ] Подтвердить idempotency и timeout recovery для booking/payment create,
  включая timeout до получения provider taskId.
- [ ] Подтвердить абсолютный payment origin, customer auth и owner-bound
  payment-link handoff; полная матрица — в
  [`booking-payment-contract-readiness.md`](booking-payment-contract-readiness.md).
- [ ] Подтвердить JWT lifetime/rotation claims, dates, location pagination и
  public/private routing.

**Критерий готовности:** каждый выставленный mutating tool имеет typed schema,
подтверждённые auth/request headers, retry policy и contract test.

### Шаг 3a. Устранить неоднозначность выборки и ranking

**Статус:** завершён для доступного search-контракта.

- [x] Проверить provider sort: production вернул
  `sorting_is_not_allowed_yet`; не отправлять поле до фактической активации.
- [x] Следовать `nextOffset` в пределах 20 provider-запросов.
- [x] Ограниченно опрашивать partial response с
  `isLoadingCompleted=false` и удалять повторные отели.
- [x] Ограничить search collection бюджетом 11 секунд и возвращать накопленный
  partial result вместо общего MCP timeout.
- [x] Возвращать `searchCoverage` с completeness/truncation metadata.
- [x] Классифицировать покрытие как `complete`/`substantial`/`partial` и
  возвращать долю относительно provider reported count.
- [x] Продолжать partial journey с сохранённого offset без повторной загрузки
  первой страницы и со стабильными `optionId`.
- [x] Сохранять общий предел 20 provider search requests на initial и
  continuation; не продолжать terminal pagination anomalies.
- [x] Не сохранять truncated search как финальный global cache result.
- [x] Покрыть terminal continuation states: repeated offset, provider failure,
  request limit и bounded loading poll recovery.
- [x] После первого continuation механически отключать автоматическую
  рекомендацию повторения, сохраняя явное user-driven продолжение.
- [x] Сбрасывать stale hotel/rate selection, если выбранный provider-вариант
  исчез из обновлённой выборки.
- [x] Наследовать ranking `plan_stay` в `get_stay_options` и
  `compare_stay_options`, если клиент не передал override.
- [x] При явном ranking сравнивать всю journey-выборку, даже если LLM также
  передала `optionIds`; explicit IDs предназначены только для выбора человека.
- [x] Покрыть pagination, partial loading, deduplication и ranking inheritance
  fake-transport regression tests.

**Критерий готовности:** формулировка «пять лучших» относится ко всей bounded
собранной выборке, а не только к первой странице; при низком покрытии agent
один раз продолжает тот же journey, а оставшаяся неполнота видна через
`searchCoverage` и не маскируется cache.

### Шаг 3c. Стабилизировать обязательные search conditions

**Статус:** реализован локально в Hotels MCP `0.18.0`; Qwen review дал `READY`,
P3 hardening закрыт локально, естественные breakfast/control smoke успешно
проверили orchestration. По их UX evidence добавлены трёхсостоянийный
displayed-price breakfast fact и обязательный presentation scope.

- [x] Добавить semantic `breakfastIncluded` в `plan_stay`.
- [x] Применять `meal_types=breakfast` до создания journey и сравнения.
- [x] Сохранять `requiredConditions` и `conditionsApplied` в plan/get/compare.
- [x] Типизировать четыре OpenAPI discriminator-формы low-level filters и
  валидировать их до HTTP.
- [x] Запретить автоматический unfiltered fallback и альтернативный перебор
  payload после отказа обязательного фильтра.
- [x] Покрыть happy path, malformed filters и provider failure герметичными
  regression tests.
- [x] Провести Qwen 3.8 Max review working tree и follow-up после P3 fixes.
- [x] Разделить provider auth rejection (`401/403`) и обычный отказ фильтра,
  проверить network fail-closed и condition metadata в `get_stay_options`.
- [x] Зафиксировать structured-success семантику filtered low-level errors и
  неподтверждённый pass-through статус `rates.filters`.
- [x] Выполнить естественный breakfast journey smoke без технических подсказок:
  ровно `plan_stay` + `compare_stay_options`, без low-level перебора и writes.
- [x] Добавить консервативный `displayedPriceBreakfastEvidence` для search-feed
  options и rates, чтобы condition filter не подменял тарифный provider fact.
- [x] Выполнить control search без breakfast condition; orchestration прошёл,
  но таблица скрыла сравнительные факты и добавила отель вне top-5 в выводах.
- [x] Разделить `confirmed_by_meal_name`, `excluded_by_meal_name` и
  `not_confirmed_for_displayed_price`; добавить `presentationGuidance` с
  обязательными полями и запретом подмешивать варианты вне comparison.
- [x] Повторить control на `0.15.3`: scope соблюдён, но сложный provider price
  снова не попал в таблицу.
- [x] Добавить плоские `comparisonRows` с числовой ценой/валютой, локацией,
  рейтингом, отзывами, отменой и meal evidence.
- [x] Добавить `comparisonRows`, versioned manifests и offline conformance,
  чтобы дальнейшие проверки выполнялись пакетно, а не микрофиксами.
- [x] Privacy-кейсы 4 и 5 прошли на Hotels `0.18.0` / Banking `0.8.1`; booking/payment execution
  не вызывать.

**Критерий готовности:** natural-language запрос с обязательным завтраком
использует `plan_stay` и `compare_stay_options`, не вызывает filter-discovery и
low-level search, а при недоступности фильтра честно останавливается.

### Шаг 3b. Стабилизировать длительный booking journey

**Статус:** завершён для process-local preview flow.

- [x] Увеличить TTL journey до 60 минут и выдавать booking draft собственный
  60-минутный TTL.
- [x] Увеличить freshness checkout до 5 минут и повторять один timeout внутри
  13-секундного бюджета одного tool-вызова.
- [x] При выключенных mutations возвращать `preview_only` без confirmation
  phrase/hash и не просить пользователя подтвердить невозможное действие.
- [x] Проверять mutation guard до checkout freshness в `confirm_booking`.
- [x] Для `service_jwt` отклонять `get_customer` локально без HTTP `401`.
- [x] Возвращать `guestCoverage` без неподтверждённого OpenAPI требования о
  точном количестве имён гостей.

**Критерий готовности:** обычный человеческий диалог сохраняет контекст до
60 минут, preview не выглядит исполнимой бронью при выключенных mutations, а временный сетевой
timeout не заставляет LLM вручную повторять весь checkout flow.

### Шаг 3c. Устранить ложную execution readiness

**Статус:** завершён для локально подтверждаемых границ; provider contracts
остаются внешним gate.

- [x] Разделить `searchReady`, `customerReadiness` и `bookingExecution` в
  `connection_status`.
- [x] Для booking/LS учитывать обязательный trusted header `x-real-ip`, не
  принимая его через LLM arguments и не выдумывая источник.
- [x] Возвращать `preview_only` без hash/confirmation во всех generic prepare,
  если execution profile не готов.
- [x] Проверять execution readiness до TTL/hash во всех execute-tools.
- [x] Убрать из tool outputs инструкции модели менять runtime environment.
- [x] Добавить single-flight для journey confirm и состояние `outcome_unknown`
  после timeout/network/5xx вместо автоматического повторного POST.
- [x] Отслеживать generic execute по `requestHash`, блокируя concurrent call,
  replay после успеха и повтор после неизвестного исхода.
- [x] Ограничить cold location catalog общим бюджетом 10 секунд.
- [x] Запретить price ranking при неизвестной валюте всех вариантов.
- [x] Покрыть fake-transport тестами required header, concurrent confirm,
  unknown outcome, независимый draft TTL, checkout retry boundaries и generic
  preview-only.

**Критерий готовности:** модель не получает финальное подтверждение для
неисполнимого действия, один draft не создаёт два параллельных booking POST, а
неизвестный исход не повторяется автоматически. Реальная активация остаётся
`NO-GO` до подтверждения источника `x-real-ip`, customer auth и provider
idempotency.

### Шаг 3d. Упростить безопасный booking preview

**Статус:** завершён для process-local preview и read-only rates retry.

- [x] Добавить `tbank_hotels_create_booking_preview` без PII, booking draft и
  HTTP-вызова.
- [x] Направлять preview-only сценарий в новый tool и не запрашивать guest PII,
  когда `bookingExecution.available=false`.
- [x] Повторять один timeout загрузки тарифов внутри общего 13-секундного
  бюджета.
- [x] После исчерпания бюджета возвращать
  `rates_temporarily_unavailable` с `attempts`, `durationMs` и `failureKind`
  вместо ошибки, провоцирующей автоматические повторы LLM.
- [x] Покрыть оба поведения fake-transport regression tests.

**Критерий готовности:** preview можно показать без персональных данных, а один
временный timeout rates не требует ручного повторения tool-вызова моделью.

### Шаг 4. Провести только read-only QA smoke

**Статус:** завершён для совместного read-only/preview-only smoke; provider
timeout остаётся покрыт автоматическими fake-transport тестами.

- [x] Полностью перезапустить MCP-клиент и проверить server versions Hotels
  `0.11.0` / Banking `0.5.0` на момент live-smoke.
- [x] Выполнить `connection_status`, `resolve_destination`, `plan_stay`,
  `get_stay_options` и `compare_stay_options`.
- [x] Для отеля с пустыми rates подтвердить `no_bookable_rates`, отсутствие
  запроса PII и невозможность создать preview/draft по search-feed цене.
- [x] Для отеля с доступным тарифом выбрать `rateOptionId` и вызвать
  `create_booking_preview`; подтвердить `personalDataCollected=false`,
  `httpRequestPerformed=false` и отсутствие запроса PII/final confirmation.
- [x] При контролируемом fake-transport timeout rates подтвердить один
  внутренний retry и
  структурированный `rates_temporarily_unavailable` без ручного повтора LLM.
- [x] Проверить customer reads только после подтверждения auth scope и на
  разрешённых тестовых данных.
- [x] После локального phone login остановить auth broker и запустить отдельный
  `tbank-hotels-mobile-auth-probe`; отсутствие собственного `orderId`/`taskId`
  фиксируется как `not_tested_missing_identifier`, без подбора значений.
- [x] Проверить `get_booking_v1` на собственной брони через broker и после
  live-smoke заменить provider `orderId` в agent-facing flow на process-local
  `bookingRef`.
- [x] Через bounded own-order discovery проверить voucher: no-auth `401`,
  Bearer-only `200 application/pdf`, без чтения PDF и вывода identifier.
- [x] Реализовать fixture-only безопасную выдачу voucher: process-local
  `bookingRef`, broker-side type/signature/size validation, owner-only local
  file с TTL и отсутствие PDF/base64/provider `orderId` в MCP JSON.
- [x] Зафиксировать EVO как partial evidence (`401` → `400 rate_not_found`) и
  не объявлять рабочим endpoint; task status не тестировать без собственного
  `taskId`.
- [x] Расширять broker allowlist и проверять Hotels customer tools только для
  routes с отдельно зафиксированным mobile auth evidence.
- [x] Убедиться, что provider errors и privacy-safe smoke report не раскрывают
  raw body, PII, credentials или identifiers.

**Критерий готовности:** read-only flow проходит на утверждённом QA endpoint;
ни один prepare/execute mutating tool не вызывается.

### Шаг 5. Провести независимый review и локальный hardening

**Статус:** внешний Qwen-review версии `0.9.0` выполнен раньше предыдущего
smoke; Qwen 3.8 Max review `0.15.0` завершён с verdict `READY`, а его безопасные
P3 findings закрыты в `0.15.1`. Новый live breakfast/control smoke ещё не
выполнен: preflight поднял `0.15.0`, но новый OpenCode-процесс не унаследовал
search transport/auth.

- [x] Передать ревьюеру код, tool schemas, тесты, README и этот план; smoke-report
  отсутствовал и остаётся входом для повторной проверки.
- [x] Проверить, что LLM может пройти natural-language search → compare → rates
  → preview без знания provider DTO и без лишнего PII.
- [x] Проверить timeout/retry, state TTL, secret redaction, mutation guards и
  portability stdio contract.
- [x] Разделить findings на подтверждённые дефекты, contract gaps и future
  recommendations; не активировать реальные mutations по результатам review.

**Критерий готовности:** локальный triage и UX hardening закрыты в `0.18.0`;
для live evidence остаются два обезличенных read-only smoke-кейса. Внешние
contract gaps не маскируются догадками MCP.

### Шаг 6. Закрыть внешние Hotels API contract gaps

**Статус:** заблокирован ответами владельца API.

- [ ] Подтвердить customer auth profile для customer reads и mutations.
- [ ] Подтвердить доверенный источник и семантику `x-real-ip`.
- [ ] Подтвердить idempotency key/deduplication и recovery после неизвестного
  исхода booking create.
- [ ] Подтвердить JWT lifetime/rotation claims, date/timezone semantics,
  `sessionId` и public/private routing.
- [ ] Получить sandbox/test identifiers и безопасные negative/positive
  сценарии без денежных последствий.

**Критерий готовности:** каждый execution tool имеет подтверждённые auth,
headers, body, idempotency и recovery semantics; решения отражены в typed
contract tests и документации.

### Шаг 7. Проверить переносимость и подготовить controlled activation

**Статус:** детальный план зафиксирован, реализация не активирована.
Read-only переносимость можно начинать после повторного review Шага 5; закрытие
внешних contract gaps Шага 6 обязательно только для execution tier.

Полный порядок этапов, профили `stdio`/sidecar/Streamable HTTP и release gates
описаны в
[`portability-and-distribution-roadmap.md`](portability-and-distribution-roadmap.md).
Принятое решение: сначала стабилизировать локальные read-only/preview flows и
выпустить воспроизводимый `stdio` профиль; remote transport следует после
фиксации контракта и client matrix, а execution tier остаётся отдельным gate.

- [ ] Проверить stdio-подключение в OpenCode и Codex CLI с одинаковой моделью
  environment variables и без browser dependency.
- [x] Combined profile прошёл в OpenCode на естественных search/customer/banking
  кейсах и в Codex CLI через встроенный tool discovery и два локальных
  `connection_status`; provider requests в Codex smoke не выполнялись.
- [x] Claude Code не входит в текущую acceptance matrix по решению владельца;
  генератор конфигурации сохраняется как необязательная возможность.
- [ ] Зафиксировать минимальные конфигурационные примеры без секретов.
- [x] Зафиксировать целевые способы распространения: native package/stdio как
  первый профиль, Docker/sidecar как опциональный и Streamable HTTP как
  отдельный remote security track.
- [ ] Подготовить отдельный non-production mutation smoke plan с точными
  targets, rollback/reconciliation и явным разрешением владельца API.

**Критерий готовности:** read-only MCP воспроизводимо подключается к целевым
CLI без checkout репозитория, а real mutation activation остаётся отдельным
контролируемым решением.

### Отдельный activation gate для реальных мутаций

Booking, payment, cancel, promocode и extra services остаются `NO-GO`, пока не
закрыт Шаг 3, не подготовлен изолированный sandbox и владелец API не дал
отдельное явное разрешение на конкретный non-production сценарий. Runtime
дополнительно требует `TBANK_HOTELS_ENABLE_MUTATIONS=true` и отдельный
`non_production_v1_reviewed` execution profile; production profile отсутствует.
По умолчанию оба гейта выключены.
