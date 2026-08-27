# T-Bank Hotels API MCP

> Неофициальный developer preview. Пакет не заявляет одобрение или поддержку
> со стороны Т-Банка. Read-only поиск по умолчанию работает без auth-заголовка;
> mobile authorization используется только для подтверждённых операций с
> данными текущего пользователя. Booking/payment execution отключён.

Независимый от браузера MCP-сервер для Hotels API Т-Банка. Это stdio-сервер на
Node.js 20+, поэтому его можно подключить к Codex, Claude Code, OpenCode и
любому другому MCP-клиенту без расширения браузера, cookie и сохранённой
браузерной сессии.

Сервер использует только явную конфигурацию API transport. Токены и ключи
остаются в переменных окружения процесса: MCP не принимает их как аргументы
инструментов, не записывает на диск и не выводит в ответах.

## Подключение

Установите Node.js 20+. Для обычного read-only поиска достаточно публичного
Hotels endpoint — токен и JWT не нужны:

```bash
export TBANK_HOTELS_API_BASE_URL='https://hotels.tbank.ru/api'
tbank-hotels-mcp
```

Без настроенной авторизации MCP использует `authMode=anonymous`, не отправляет
`Authorization` и считает read-only search локально готовым. Это
developer-preview допущение: `connection_status` не выполняет сетевой probe, а
реальный `401/403` возвращается как terminal provider-auth result без
автоматического перебора credentials или повторов.

Static token остаётся необязательным integration override для окружений, где
он выдан владельцем API:

```bash
export TBANK_HOTELS_AUTH_TOKEN='<token>'
```

MCP передаёт такой токен как `Authorization: Bearer <token>`. Для нестандартной
схемы можно явно задать `TBANK_HOTELS_AUTH_HEADER` и
`TBANK_HOTELS_AUTH_PREFIX`. Если нужны несколько заголовков, вместо token-based
переменных используйте ровно одну:

```bash
export TBANK_HOTELS_AUTH_HEADERS_JSON='{"X-Integration-Token":"<token>","X-Client-Id":"<client-id>"}'
```

Cookie и SSO-заголовки не используются. Static token profile не обновляет токен;
service JWT profile ниже выпускает подпись локально. Если production API
потребует несколько заголовков, их можно передать в JSON без изменения MCP
tools.

### Опциональный service JWT для `HotelsApiPrivate`

Для конфигурации Hotels, предоставленной владельцем API, MCP умеет сам
подписывать service JWT алгоритмом `RS384`. В JWT записываются claims `iss`,
`aud` (массив значений через запятую) и `iat`; подпись обновляется локально раз
в 30 секунд. Это отдельный auth-profile: не сочетайте его с token или JSON
headers profile.

Service JWT остаётся необязательным integration override для окружений, где
он выдан владельцем API:

```bash
export TBANK_HOTELS_JWT_ISSUER='HOTELSSEARCHAPI'
export TBANK_HOTELS_JWT_AUDIENCE='HOTELSAPI'
export TBANK_HOTELS_JWT_PRIVATE_KEY="$(< /secure/path/hotels-qa-rsa-key.pem)"
```

Для постоянной локальной настройки предпочтителен путь к key-файлу, а не PEM
в окружении:

```bash
chmod 600 '/secure/path/hotels-qa-rsa-key.pem'
export TBANK_HOTELS_JWT_PRIVATE_KEY_FILE='/secure/path/hotels-qa-rsa-key.pem'
```

На POSIX-системах сервер отклоняет key-файл, доступный группе или другим
пользователям; требуются права `0600` или строже.

Не задавайте `TBANK_HOTELS_JWT_PRIVATE_KEY` и
`TBANK_HOTELS_JWT_PRIVATE_KEY_FILE` одновременно. Общий toolkit выполняет
одноразовый setup, doctor и генерацию подключения OpenCode/Codex/Claude без
секретов: [`../tbank-mcp-local/README.md`](../tbank-mcp-local/README.md).

Ключ может быть полным PEM PKCS#1/PKCS#8 либо телом PKCS#1 без PEM-заголовков.
Его выдаёт владелец QA-интеграции; не добавляйте ключ в конфигурацию MCP,
репозиторий или сообщение модели. В соответствии с показанным Hotels Go-client
заголовок по умолчанию формируется как `Authorization: Bearer<JWT>` — без
автоматически добавляемого пробела. Для утверждённого API-варианта можно
переопределить имя и префикс:

```bash
export TBANK_HOTELS_JWT_AUTH_HEADER='Authorization'
export TBANK_HOTELS_JWT_AUTH_PREFIX='Bearer '
```

`service_jwt` удостоверяет MCP как сервис, но не заменяет пользовательскую
авторизацию, если конкретный endpoint требует customer context.
В этом режиме `connection_status` возвращает `canReadCustomer=false`, а
`get_customer` завершается локальной диагностикой без заведомо бесполезного
HTTP-запроса и ответа `401`.

### Опциональная общая mobile session

Hotels MCP не требует Banking MCP и сохраняет существующие service-JWT/static
auth profiles. Для подтверждённых customer endpoints можно дополнительно
настроить локальный auth broker из `tools/tbank-banking-mcp`:

```bash
export TBANK_AUTH_BROKER_SOCKET="$HOME/.local/share/tbank-auth-broker/auth.sock"
```

При такой настройке через общую mobile session выполняются
`tbank_hotels_get_customer`, `tbank_hotels_list_bookings`,
`tbank_hotels_get_booking` с `apiVersion=v1` и безопасная локальная выдача
ваучера через `tbank_hotels_save_voucher`. Для первых двух routes live probe
подтвердил минимальный профиль: без Authorization — `401`, с Bearer-only —
`200`; карточка собственной брони также прошла live read-only smoke с тем же
Bearer-only профилем. Cookies, `sessionid`, device query и `x-real-ip` не
передаются. Токен не передаётся в Hotels MCP или LLM. Если broker не настроен,
MCP полностью сохраняет прежний режим работы.

В broker-режиме `tbank_hotels_list_bookings` заменяет provider `orderId` на
короткоживущий process-local `bookingRef`. Последующий
`tbank_hotels_get_booking`, `tbank_hotels_get_booking_overview` и
`tbank_hotels_preview_cancellation` принимают этот `bookingRef`, разрешают raw
identifier только внутри MCP и удаляют provider identifiers из ответа. Для
прямого static/service API-профиля прежний `orderId` остаётся доступен как
низкоуровневый integration contract.

`connection_status` выполняет только локальный `status`-probe broker без
provider-вызова и различает `broker_unavailable`, `mobile_login_required`,
`mobile_read_only_ready` и `partial_read_only_unverified`. Наличие пути socket само по себе больше не
считается готовностью customer read. Только явный массив `verifiedOperations`
может включить customer reads; legacy broker с одним `supportedOperations`
остаётся `partial_read_only_unverified`. Основные read-only broker-операции ждут до
45 секунд, поскольку могут включать refresh сессии; диапазон настраивается через
`TBANK_AUTH_BROKER_TIMEOUT_MS=1000..120000`.

`searchReady=true` означает только готовность локального transport и выбранной
конфигурации авторизации (`anonymous`, static или service JWT).
`connection_status` намеренно не обращается к provider и возвращает
`networkReachability=not_checked`. Фактический DNS/network failure обрабатывает
`plan_stay`: он возвращает terminal `search_unavailable` с
`retryAllowed=false`, после чего агент может один раз показать
`connection_status` и обязан остановиться без повторов и обхода MCP.

В combined-конфигурации local toolkit broker автоматически обеспечивается
launcher-ом как Hotels, так и Banking MCP. Поэтому Hotels-first запуск после
рестарта клиента не требует ручной команды и не зависит от предварительного
обращения к Banking MCP. Закрытие одного MCP не завершает общую mobile session;
её явно завершает toolkit `logout` или диагностическая команда `stop-broker`.

Broker protocol v2 ограничивает Hotels-клиент операциями
`hotels.get_customer`, `hotels.list_bookings`, `hotels.get_booking_v1` и
`hotels.save_voucher_v1`, а также локальным
`hotels.create_payment_handoff`, который выполняет один bounded booking v1 read
внутри broker boundary и связывает наблюдаемые payment facts с capability. Это
контрактная изоляция между двумя MCP, но не защита
от произвольного процесса того же OS-пользователя: owner-only socket и
session-файл находятся в единой локальной границе доверия.

### Безопасная локальная выдача ваучера

По явному запросу пользователя сначала получите `bookingRef` через
`tbank_hotels_list_bookings`, затем вызовите `tbank_hotels_save_voucher`.
Provider `orderId` разрешается только внутри Hotels MCP, а PDF скачивается и
проверяется внутри auth broker. В MCP JSON возвращаются только безопасные
метаданные и локальный путь; байты PDF, base64, PII документа и credentials в
контекст модели не попадают.

Файл и каталог создаются с правами `0600`/`0700`, максимальный размер PDF —
5 MiB, обязательны `application/pdf` и сигнатура `%PDF-`. По умолчанию файл
удаляется через 15 минут. Настройки локального хранения:

```bash
export TBANK_HOTELS_VOUCHER_DIRECTORY='/absolute/owner-only/path/tbank-vouchers'
export TBANK_HOTELS_VOUCHER_TTL_SECONDS=900
```

Legacy-инструмент `tbank_hotels_get_voucher` оставлен только как явный safety
guard и всегда отклоняет inline-выдачу. `tbank_hotels_get_booking_overview`
также не загружает и не встраивает PDF.

`tbank_hotels_create_booking_draft` принимает guest PII только после локальной
проверки готовности booking execution. Если execution не активирован или не
настроен, tool не создаёт `bookingDraftId`, не сохраняет переданные данные и
направляет клиента в `tbank_hotels_create_booking_preview` без PII.

### Безопасный payment handoff

`tbank_hotels_create_payment_handoff_preview(bookingRef)` передаёт provider
`orderId` только внутрь owner-only broker boundary и получает короткоживущий
`paymentHandoffRef`. Banking MCP проверяет этот capability у того же broker.
Между MCP не передаются `orderId`, `paymentToken` или credentials; provider
booking v1 read выполняется внутри broker, а payment setup и оплата не
выполняются. Capability одноразовый: первый Banking preview атомарно поглощает
его, а для повторного preview требуется новый handoff. Capability связывает наблюдаемые
`rateData.paymentData.paymentPrice` и raw `paymentStatus`. Статус не
интерпретируется как разрешение оплаты, а `paymentPrice` не называется
непогашенным остатком или суммой к списанию.
Сумма передаётся дальше как каноническая decimal-строка `amountDecimal`, а
capability содержит локальное время наблюдения и ограниченное окно свежести.
Это защищает последующие вычисления от дополнительной потери точности, но не
доказывает официальный scale денежного поля и не восстанавливает исходное
лексическое представление уже разобранного provider JSON.

Отдельный `tbank_hotels_create_payment_form_preview(journeyId)` описывает
подтверждённый hosted payment form flow для выбранного тарифа. Он не принимает
PII, данные карты, OTP, device/browser context или redirect URL, не вызывает
provider и не возвращает `paymentUrl`. В `connection_status.paymentFormExecution`
видны офлайн-подтверждённые части контракта и оставшиеся внешние гейты.
Подробная матрица зафиксирована в
[`docs/booking-payment-contract-readiness.md`](docs/booking-payment-contract-readiness.md).

Для публичного завершения сценария используйте
`tbank_hotels_create_checkout_handoff(journeyId)`. Он возвращает HTTPS-ссылку
на публичную страницу выбранного отеля и безопасную сводку stay/rate, но не
переносит `bookHash`, PII, токены или платёжные credentials, не создаёт бронь и
не запускает оплату. Для одной комнаты без детей ссылка сохраняет выбранный
отель, даты и число взрослых через подтверждённые публичные параметры
`dateFrom`, `dateTo` и `guests`. Для нескольких комнат или детей сохраняются
только даты: MCP не угадывает неподтверждённый формат состава гостей. Точный
тариф не резервируется и не переносится — цену и доступность нужно подтвердить
во внешнем интерфейсе.

После выбранного тарифа естественные запросы «оформить», «забронировать»,
«продолжить» или «перейти к оформлению» должны завершаться именно этим handoff,
а не остановкой на `tbank_hotels_create_booking_preview`. Handoff доступен даже
когда прямой booking execution выключен: модель показывает `hostedCheckoutUrl`,
а пользователь вводит персональные и платёжные данные только на доверенной
внешней странице.
По умолчанию используется официальный hotel-page route. Интегратор может задать свой
шаблон с одним `{hotelId}` без query/секретов:

```bash
export TBANK_HOTELS_HOTEL_PAGE_URL_TEMPLATE='https://www.tbank.ru/travel/hotels/new/hotels/{hotelId}/'
```

Архитектурная граница закреплена в
[`ADR-0005`](../../docs/decisions/adr-0005-hosted-checkout-handoff-boundary.md).

Переданный OpenAPI помечает HTTP-header `x-real-ip` обязательным для обычного и
LS-создания брони. Это исходный IP клиента, обычно используемый antifraud и
аудитом. MCP не спрашивает его у пользователя, не вычисляет самостоятельно и
не принимает как tool argument. Источник должен определить владелец API или
доверенный gateway. Для `payment/setup` доступный v1-контракт `x-real-ip` не
требует. Пока источник не подтверждён, один `service_jwt` не считается готовым
booking execution profile.

Необязательная настройка таймаута:

```bash
export TBANK_HOTELS_TIMEOUT_MS=15000
```

### Локальная защита provider от нагрузки

MCP `0.28.0` ограничивает один процесс двумя параллельными provider-запросами.
Значение можно уменьшить или увеличить в безопасном диапазоне `1..8`:

```bash
export TBANK_HOTELS_MAX_CONCURRENT_REQUESTS=2
```

Очередь ограничена 32 запросами: при переполнении MCP отклоняет новый вызов
локально, не обращаясь к provider. Одинаковые одновременно запущенные
`plan_stay` объединяются в один hotel search, а успешный результат такого же
поиска переиспользуется 30 секунд внутри процесса. Кэш не хранит credentials и
исчезает при перезапуске MCP. Стратегия локального ранжирования не входит в
ключ provider-кэша: переключение между `provider_order`, `highest_rating`,
`lowest_price` и `best_value` не создаёт повторный HTTP-поиск для тех же
параметров.

`connection_status.loadProtection` показывает настройку, текущую очередь и
число cached/in-flight searches. Вызов остаётся локальным и не обращается к
Hotels API.

Публичная установка developer preview выполняется одной командой:

```bash
npm install --global tbank-hotels-mcp
```

Пример stdio-конфигурации установленного пакета для любого MCP-клиента:

```json
{
  "command": "tbank-hotels-mcp",
  "args": [],
  "env": {
    "TBANK_HOTELS_API_BASE_URL": "https://hotels.tbank.ru/api"
  }
}
```

Для combined-профиля и мобильной авторизации используйте общий toolkit: он
генерирует client config и не помещает токены в него.

## Контракт MCP

Низкоуровневые инструменты возвращают ответ provider как `{ status, data }` и
предназначены для интеграторов, которым нужен точный v1–v3 контракт. Основной
journey-инструмент `tbank_hotels_plan_stay` принимает пользовательские параметры,
сам разрешает название локации в `destinationId`, валидирует даты и гостей и
создаёт корректный `SearchParametersListApiRequest`.

Это скрывает provider DTO на шагах поиска, сравнения и получения тарифов:
`get_selected_stay_rates` сам переносит даты и состав гостей из journey.
Подтверждённые контракты обычной и LS-брони, списка броней, отмены, промокода,
дополнительных услуг и рассрочки также представлены typed-полями. Auth-модель,
обязательные request headers и idempotency реальных mutations пока имеют
contract gaps, перечисленные в tool-local плане развития.

| Группа | Инструменты |
| --- | --- |
| Проверка transport/auth | `tbank_hotels_connection_status`, `tbank_hotels_get_customer` |
| Поиск и разрешение локации | `tbank_hotels_resolve_destination`, `tbank_hotels_plan_stay`, `tbank_hotels_search`, `tbank_hotels_get_search_filters`, `tbank_hotels_get_filter_availability`, `tbank_hotels_search_map`, `tbank_hotels_get_map_hotels`, `tbank_hotels_search_points_of_interest` |
| Карточка, номера и тарифы | `tbank_hotels_get_hotel`, `tbank_hotels_get_hotel_rates`, `tbank_hotels_get_rate`, `tbank_hotels_get_cashback_percent`, `tbank_hotels_get_max_cashback`, `tbank_hotels_validate_promocode`, `tbank_hotels_get_rate_upgrade` |
| Отзывы, SEO и deeplink | `tbank_hotels_get_reviews`, `tbank_hotels_get_review_order_status`, `tbank_hotels_search_seo`, `tbank_hotels_search_urls`, `tbank_hotels_get_seo_resource`, `tbank_hotels_get_deeplink_token`, `tbank_hotels_get_partner_redirect_url` |
| Рассрочка | `tbank_hotels_get_available_tranche_amount`, `tbank_hotels_get_bnpl_offer` |
| Авторизованные заказы | `tbank_hotels_get_booking`, `tbank_hotels_list_bookings`, `tbank_hotels_save_voucher`, `tbank_hotels_create_payment_handoff_preview`; `tbank_hotels_get_voucher` — safety guard; остальные low-level reads: `tbank_hotels_get_reservation`, `tbank_hotels_get_evo_booking`, `tbank_hotels_get_bnpl_offer`, `tbank_hotels_get_booking_task_status`, `tbank_hotels_check_ls_order` |
| Изменяющие операции | `tbank_hotels_prepare_*` и `tbank_hotels_execute_*` для обычной и LS-брони, отмены, setup оплаты, промокода и дополнительных услуг |
| Journey-сценарий | `tbank_hotels_plan_stay`, `tbank_hotels_get_stay_options`, `tbank_hotels_compare_stay_options`, `tbank_hotels_select_stay_option`, `tbank_hotels_get_selected_stay_rates`, `tbank_hotels_select_stay_rate`, `tbank_hotels_repeat_stay_plan` |
| Journey checkout и заказ | `tbank_hotels_create_booking_preview`, `tbank_hotels_create_payment_form_preview`, `tbank_hotels_create_checkout_handoff`, `tbank_hotels_create_booking_draft`, `tbank_hotels_validate_checkout`, `tbank_hotels_prepare_draft_booking`, `tbank_hotels_confirm_booking`, `tbank_hotels_get_booking_overview`, `tbank_hotels_preview_cancellation` |

Runtime разделён на тонкую stdio-точку входа (`src/server.mjs`), MCP framing
(`src/stdio-server.mjs`), tool contracts (`src/tool-contracts.mjs`),
конфигурацию (`src/config.mjs`), checkout boundary
(`src/checkout-handoff.mjs`) и доменный runtime (`src/runtime.mjs`). Большой
runtime остаётся внутренним implementation module, а публичный контракт
фиксируется versioned manifest и не зависит от расположения кода.

По умолчанию для чтения заказа, тарифов, SEO и URL выбран v3; аргумент
`apiVersion` позволяет явно выбрать поддерживаемую версию (`v1`, `v2`, `v3`) в
тех случаях, где она есть в полученных контрактах.

Не включены browser-cookie endpoint, internal endpoints, callbacks и операции
изменения отзывов: они не являются переносимой клиентской Hotels-функцией либо
требуют отдельного официального контракта и security review.

### Journey-сценарий

Обычный поиск не требует знания provider DTO:

```json
{
  "destination": "Москва",
  "checkinDate": "2026-09-15",
  "checkoutDate": "2026-09-16",
  "rooms": [{ "adults": 2, "childrenAges": [] }],
  "language": "ru-RU",
  "ranking": "provider_order",
  "maxOptions": 20
}
```

Для обязательного завтрака добавьте semantic-поле:

```json
{
  "destination": "Санкт-Петербург",
  "checkinDate": "2026-09-15",
  "checkoutDate": "2026-09-16",
  "rooms": [{ "adults": 2, "childrenAges": [] }],
  "breakfastIncluded": true,
  "ranking": "highest_rating",
  "maxOptions": 20
}
```

Banking MCP возвращает готовый объект `hotelPreferences`; его нужно передать
без преобразований как мягкие предпочтения без счетов, категорий и абсолютных
сумм:

```json
{
  "destination": "Казань",
  "checkinDate": "2026-09-15",
  "checkoutDate": "2026-09-17",
  "rooms": [{ "adults": 2, "childrenAges": [] }],
  "hotelPreferences": {
    "pricePerNight": { "min": 6000, "max": 13000, "currency": "RUB" },
    "ranking": "best_value",
    "showAlternativesOutsideBand": true
  }
}
```

`hotelPreferences` не превращается в provider price filter и не отсекает
варианты. `best_value_v2` детерминированно сочетает provider rating (60%),
логарифмический вес числа отзывов (20%) и band-aware полезность цены (20%).
Цена внутри диапазона получает наибольшую полезность; варианты немного ниже
или выше сохраняются как альтернативы, а сильное отклонение от диапазона
понижает score и больше не награждается как «самая выгодная» цена. Это
MCP-derived score, а не оценка provider; ответ отдельно показывает попадание в
предпочтительный диапазон. Верхнеуровневый `ranking` при наличии имеет
приоритет над `hotelPreferences.ranking`. Один `ranking=best_value` без объекта
`hotelPreferences` не означает применение профиля: это можно утверждать только
при `preferencesApplied.applied=true` в ответе Hotels.

Provider `shownPrice` является полной ценой за выбранный период. Journey-ответы
возвращают её как `totalPriceDisplay`, число ночей как `stayNights` и отдельно
вычисленный MCP `pricePerNightDisplay`. Мягкий `pricePerNight`-диапазон и
`best_value` сравниваются именно с ценой за ночь. Пользовательские таблицы
должны использовать готовые колонки «За поездку» и «За ночь», не делить и не
перемножать цену повторно.

При `breakfastIncluded=true` `plan_stay` сам преобразует условие в
подтверждённый provider-фильтр
`{"$objectType":"array","filterId":"meal_types","values":["breakfast"]}`.
Для такого запроса не нужно вызывать `get_search_filters`,
`get_filter_availability` или низкоуровневый `search`. Если provider отклоняет
обязательный фильтр или не отвечает, tool возвращает
`requirements_unavailable` с `retryAllowed=false` и не подменяет результат
обычной неотфильтрованной выдачей. Пустой отфильтрованный результат возвращается
как `no_matching_stays`; ослабить условие можно только по явному решению
пользователя. Ответы provider `401/403` выделяются как
`provider_auth_rejected`: это требует восстановления search auth profile вне
диалога с моделью, а не повторного поиска или изменения фильтра.

Низкоуровневые search-tools принимают только четыре discriminator-формы из
OpenAPI: `array`, `boolean`, `radio` и `range`. Неизвестные поля, отсутствие
`$objectType` и неподходящий тип значения отклоняются локально до HTTP-вызова.
При отказе filtered request эти low-level tools намеренно возвращают обычный
структурированный результат со `status=requirements_unavailable`; MCP-клиент
должен проверять `status`, а не только transport-флаг `isError`.
`conditionsApplied` в journey-ответах подтверждает применение фильтра ко всей
выборке. При этом утверждать, что завтрак входит именно в показанную feed-цену,
можно только при
`displayedPriceBreakfastEvidence=confirmed_by_meal_name` у конкретного
варианта. `excluded_by_meal_name` означает, что provider явно исключил завтрак
или питание из показанной цены. Значение `not_confirmed_for_displayed_price` не
означает отсутствие завтрака: оно запрещает связывать завтрак с показанной
ценой без явного provider fact. Тот же признак возвращается для бронируемых
rates. `compare_stay_options` дополнительно возвращает
готовые плоские `comparisonRows` и `presentationGuidance`: пользовательское
сравнение должно использовать только эти строки и показывать локацию, рейтинг,
число отзывов, числовую цену/валюту, отмену и питание, не подмешивая другие
отели без запроса. Исходный массив `comparison` сохраняется для совместимости.

`filters` в `get_selected_stay_rates` остаются неподтверждённым untyped
pass-through: rates endpoint отсутствует в доступном локальном OpenAPI evidence.
Агент должен опускать это поле и не переносить в него search filters, пока
владелец API не предоставит точный rates-контракт.

MCP загружает каталог `/api/v1/seo/locations`, выбирает однозначную локацию и
передаёт в поиск `destinationId`, даты и `guests[].adultsCount`. Для
неоднозначного названия `plan_stay` возвращает `clarification_required` с
кандидатами и не вызывает hotel search. Для конкретного отеля можно добавить
`hotelName`: совпадение выполняется только среди provider results выбранной
локации. Глобальный поиск отеля по одному названию не заявлен, поскольку в
полученных контрактах нет подтверждённого endpoint.

`plan_stay` собирает выдачу по `nextOffset` и повторно опрашивает текущую
страницу, если provider вернул `isLoadingCompleted=false` без нового offset.
Каталог локаций имеет отдельный общий бюджет 10 секунд, чтобы cold-cache lookup
не занял десятки последовательных provider timeouts. Сбор отелей ограничен 20 HTTP-запросами, тремя дополнительными опросами одной
частично загруженной страницы и общим бюджетом 11 секунд. Если дополнительная
страница не успевает ответить, MCP возвращает уже собранные варианты с
`truncated=true`, а не заставляет весь tool-вызов упасть по таймауту. Дубли
удаляются по `hotelId`, а при его отсутствии — по хэшу полного provider-объекта.
Ответ содержит `searchCoverage`: число собранных отелей и запросов, provider
counts, признак завершённой загрузки, применённый `providerSort` и флаг
`truncated`. Поэтому агент может явно сообщить, сравнивает ли он полную
подтверждённую выдачу или ограниченное окно результатов. `maxOptions` влияет
только на число вариантов непосредственно в ответе `plan_stay`, но не обрезает
сохраняемую в journey выборку.

Успешный вызов возвращает непрозрачный `journeyId`. В пределах 60 минут можно
получить варианты, детерминированно выбрать `provider_order`, `lowest_price`
или `highest_rating`/`best_value`, сравнить 2–5 вариантов, выбрать один и
загрузить тарифы
без передачи provider `hotelId` между вызовами. После выбора тарифа MCP может
сформировать безопасный локальный `booking preview` без ФИО, email и телефона.
Такой preview не создаёт draft, не проверяет checkout и не вызывает Hotels API.
Реальные guest PII нужны только после явного намерения оформить бронь и при
готовом execution profile. Затем MCP может сформировать черновик брони без
`bookHash` в аргументах клиента, повторно
проверить checkout и принять одно явное подтверждение перед созданием брони.
Контекст не включает токен или auth headers и удаляется при перезапуске MCP.
Booking draft получает собственный 60-минутный TTL, поэтому длительный диалог
не делает свежий черновик зависимым от остатка жизни исходного journey.

Ответ rates различает `ready`, `no_bookable_rates` и
`rates_temporarily_unavailable`. Если provider вернул пустой массив `rates`,
search-feed цена остаётся только информационным фактом: без
`rateOptionId`/`bookHash` MCP не позволяет выбирать тариф, запрашивать
персональные данные гостей или создавать booking draft. Если обе timeout-попытки
исчерпали общий бюджет, MCP возвращает структурированный результат и запрещает
LLM автоматически повторять тот же вызов.

Каждый тариф получает стабильные `rateNumber` и `rateLabel`, которые не
меняются после фильтрации подмножества или выбора тарифа. Готовая
`ratesTableMarkdown` предназначена для однократного показа: если критерий
выбора уже задан пользователем, агент завершает `select_stay_rate` и
`create_booking_preview`, а затем даёт один итоговый ответ без промежуточной
повторной таблицы.

Хотя OpenAPI перечисляет `review_rating` и `price` в search sort, проверенный
production endpoint отвечает `sorting_is_not_allowed_yet`. Поэтому MCP не
отправляет `sort`: `highest_rating` и `lowest_price` применяются локально ко
всей собранной выборке. Локальный `highest_rating` сравнивает только числовой
рейтинг и сохраняет исходный порядок при равенстве; число отзывов не
используется как дополнительный вес. `provider_order` сохраняет порядок
собранной provider-выдачи. Если `get_stay_options` или
`compare_stay_options` вызываются без `ranking`, они наследуют стратегию из
исходного `plan_stay`.

Поля `priceDisplay`, `freeCancellationUntilDisplay`, `cancellationDisplay` и
готовые Markdown-таблицы нормализуют показ денег и времени без изменения raw
provider facts. Смещение времени сохраняется явно (`UTC`, `UTC+03:00` и т. п.)
и не называется локальным временем отеля без отдельного timezone-факта.
Отсутствующий cancellation fact показывается как «нет данных», а не как
«невозвратный» или «бесплатная отмена».

Первый `compare_stay_options` ранжирует всю сохранённую journey-выборку. Следующий
вызов без `scope` ранжирует только предыдущую показанную comparison-группу. Фраза «выбери
среди этих» больше не может незаметно ввести новый отель. Полный пересчёт выборки требует
`scope=all_journey_options`. Явные `optionIds` задают подмножество, а `ranking` применяется только внутри него.
При ценовом профиле `preferenceAlternatives` отдельно возвращает хорошие варианты ниже и выше мягкого
диапазона.

Локальная валидация отклоняет неправильный диапазон дат, пустые комнаты,
некорректное число взрослых и возраст детей до HTTP-запроса. Provider error
возвращает только HTTP status, безопасный error code и request ID; произвольное
тело ошибки не раскрывается модели. `tbank_hotels_connection_status` локально
проверяет не только наличие настроек, но и формат URL/auth profile и способность
создать service JWT. `ready`/`searchReady` относятся к read-only transport;
`customerReadiness` и `bookingExecution` возвращаются отдельно. Вызов не
обращается к Hotels API.

MCP metadata помечает фактически изменяющие инструменты как mutating, а
read-only и prepare-инструменты — как безопасные для предварительного вызова.
Число одновременно хранимых journey, booking drafts и location caches
ограничено; при переполнении удаляется контекст с самым ранним сроком жизни.
Каталог локаций читается ограниченными страницами. Redirect с credentialed
запросов запрещён, provider response ограничен 2 MiB, а price ranking не
смешивает неизвестные или разные валюты.

## Бронь, отмена и оплата

Для каждой изменяющей операции есть пара `prepare` → `execute`. Полный набор
данных подтверждения формируется только когда соответствующий execution profile
готов:

1. `prepare` не вызывает API, ничего не бронирует и не списывает. Он возвращает
   endpoint, безопасный preview payload, `requestHash`, `preparedAt`,
   `expiresAt` и точную фразу подтверждения. Подтверждение живёт 5 минут.
2. Покажите пользователю цену, условия, последствия и preview. Только после
   непосредственного явного согласия вызовите соответствующий `execute` с тем
   же typed-набором полей, `preparedRequestHash`, временными метками и фразой
   подтверждения.

Сервер повторно вычисляет хэш полного запроса. Изменение даже одного поля делает
подтверждение недействительным. Для journey-брони draft хранит только локальное
состояние `ready` / `confirming` / `outcome_unknown`: параллельный confirm не
отправляет второй POST, а после timeout/network/5xx с неизвестным исходом
автоматический retry блокируется до внешней сверки task/order status.
Generic execute-tools аналогично отслеживаются по `requestHash`: параллельный
вызов, replay завершённого действия и повтор после неизвестного исхода
отклоняются до истечения локального окна. Пользовательская сессия и платёжное
состояние не хранятся. MCP не принимает PAN, срок действия карты, CVV/CVC, PIN,
OTP, 3-D Secure challenge data или browser fingerprint. Provider raw-card/3DS
endpoints намеренно не выставлены как MCP tools; основной публичный маршрут —
hosted payment form через owner-bound handoff, который ещё не активирован.

Реальные mutations дополнительно заблокированы по умолчанию. Даже точное
подтверждение не отправит HTTP write-запрос без отдельной переменной:

```bash
export TBANK_HOTELS_ENABLE_MUTATIONS=true
```

Одного флага недостаточно: booking/LS execution дополнительно требует валидный
transport/auth profile, настроенный доверенный `x-real-ip` и отдельный
`TBANK_HOTELS_MUTATION_EXECUTION_PROFILE=non_production_v1_reviewed`. Toolkit
никогда не наследует этот profile из родительского CLI. Значение предназначено
только для отдельно одобренного non-production contract test; production
profile текущая версия намеренно не поддерживает. Не включайте execution, пока
владелец Hotels API не подтвердит customer auth, источник headers, idempotency
и timeout recovery. `connection_status` показывает раздельную готовность, но
не раскрывает значения headers или секреты.

При неготовом execution profile journey и низкоуровневые prepare-tools
возвращают `executionAvailable=false` и `status=preview_only` без вычислимой
фразы подтверждения и без предложения пользователю оформить действие.
Execute-tools проверяют readiness до TTL/hash, поэтому не запускают бесполезную
повторную подготовку. Checkout после успешной проверки считается
свежим 5 минут; один сетевой timeout повторяется внутри tool в общем бюджете
13 секунд. Загрузка тарифов также безопасно повторяет один timeout внутри
`get_selected_stay_rates`: первая попытка ограничена 5 секундами, обе попытки —
общим бюджетом 13 секунд. Ответ сообщает `attempts`, `durationMs` и
`failureKind`, поэтому LLM не должна самостоятельно перебирать одинаковые
вызовы.

Черновик также возвращает `guestCoverage`: сопоставление числа указанных имён с
составом гостей поиска. Это предупреждение, а не выдуманное ограничение:
доступный OpenAPI не задаёт обязательного равенства этих количеств.

Если `bookingExecution.available=false`, агент должен использовать
`tbank_hotels_create_booking_preview`, показать выбранный тариф и состав гостей,
а затем остановиться без запроса PII и финального подтверждения. Инструмент не
возвращает `bookHash` и не подставляет вымышленные персональные данные.

## Проверка

```bash
cd tools/tbank-hotels-mcp
npm test
```

59 тестов проверяют MCP-протокол, конфигурационные границы, строгие search filters,
semantic breakfast journey, мягкую персонализацию `best_value`, presentation-
поля, безопасную упаковку npm-артефакта, no-retry guard, auth
rejection/network fail-closed, общий auth broker, безопасную локальную выдачу
voucher и stateless confirmation
без сетевых вызовов к Т-Банку. Test subprocess получает allowlist окружения и
не наследует `TBANK_HOTELS_*` родительского shell; отдельный regression test
фиксирует эту границу. Provider-вызовы в тестах перехватываются локальными
fake transport implementations.

Порядок развития высокоуровневых journey-инструментов находится в
[`docs/journey-tools-plan.md`](docs/journey-tools-plan.md). План переносимости
для CLI, локальных приложений и будущего Streamable HTTP
находится в
[`docs/portability-and-distribution-roadmap.md`](docs/portability-and-distribution-roadmap.md).
Готовый промпт для
независимого review-only аудита всего MCP находится в
[`docs/mcp-review-prompt.md`](docs/mcp-review-prompt.md). Результат и triage
Qwen 3.8 Max review зафиксированы в
[`../../docs/reviews/tbank-hotels-mcp-qwen-3-8-max-review-follow-up.md`](../../docs/reviews/tbank-hotels-mcp-qwen-3-8-max-review-follow-up.md).
Предыдущий checkpoint версии `0.8.0`, safety gates и handoff сохранены в
[`../../docs/reviews/tbank-hotels-mcp-0.8.0-progress-checkpoint.md`](../../docs/reviews/tbank-hotels-mcp-0.8.0-progress-checkpoint.md).
