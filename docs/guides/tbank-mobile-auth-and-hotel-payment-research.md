# Исследование mobile auth и оплаты Hotels

Документ фиксирует проверяемые факты, гипотезы и вопросы интеграции. Он не
разрешает реальные бронирования или платежи и не требует от пользователя знать
внутренние API-детали.

## Подтверждённые факты

| Область | Факт | Основание |
| --- | --- | --- |
| Mobile auth | Локальный phone/SMS/password/PIN flow создаёт refreshable mobile session | MIT-derived upstream client, локальный CLI |
| Hotel booking read | Mobile Bearer принят для `GET https://hotels.t-bank-app.ru/api/v1/hotels/bookings/{bookingId}` на собственной брони | capture-driven upstream template + live read-only broker smoke |
| Создание брони | Hotels task create возвращает `taskId`; task status после успеха содержит `orderId` | переданные Hotels OpenAPI contracts |
| Payment setup | `POST /api/v1/hotels/bookings/shevo/{orderId}/payment/setup` принимает `orderId` в path и возвращает `paymentToken` | переданный Hotels v1 contract |
| EVO payment tokens | `BookingInfoEvoApiResponse` может содержать `nfsPaymentTokens.nfsPaymentTokenAccount` и `bnplNfsPaymentToken` | переданный Hotels v1 contract |
| Общий `/v1/pay` | Upstream `/v1/pay` реализует банковские платежи/переводы, но сам по себе не доказывает оплату Hotels order | capture-driven upstream client |
| Marketplace gateway | Mobile payment-gate принимает `flow.{orderId,type,nfsPaymentToken}` и account agreement; для ticket flow подтверждены `type=marketplace` и `Pg-Api-System=t-entertainment-mb` | capture-driven upstream client |
| Mobile auth probe | Локальный CLI ограничен фиксированным Hotels origin и read-only route inventory, не читает response bodies и не раскрывает credentials/identifiers | Banking MCP `0.6.0`, probe protocol `1.1` |
| Combined MCP smoke | Отдельный CLI проверяет оба MCP, read-only customer/account routes и локальные preview без вывода PII/identifiers | Banking MCP `0.6.0`, smoke protocol `1.0` |

## Гипотезы, которые нельзя считать контрактом

- Один mobile token может подходить нескольким вертикалям супераппа.
- Защищённые Hotels endpoints могут принимать mobile Bearer.
- Hotels `paymentToken` или `nfsPaymentTokenAccount` может использоваться
  payment gateway как `nfsPaymentToken`; точное поле и момент его получения
  пока не подтверждены hotel-specific capture.
- Provider может поддерживать idempotency или внутреннюю дедупликацию.

## Endpoint matrix

Каждая строка переводится в `verified` только после contract evidence или
контролируемого read-only/non-production smoke. `401`, `403` и `404` являются
результатом проверки, а не поводом перебирать секреты или заголовки.

| Возможность | Candidate endpoint | Метод | Риск | Статус |
| --- | --- | --- | --- | --- |
| Карточка своей брони | `hotels.t-bank-app.ru/api/v1/hotels/bookings/{id}` | GET | PII/read | mobile Bearer live accepted through broker on own booking; no-auth control не повторялся |
| Customer data | `/api/v1/auth/customerdata` на утверждённом origin | GET | PII/read | auth-effect verified: no auth `401`, Bearer-only `200` |
| Список броней | `/api/v1/hotels/bookings/booking_list` | POST/read | PII/read | auth-effect verified: no auth `401`, Bearer-only `200` |
| Voucher | `/api/v1/hotels/bookings/voucher/{orderId}` | GET | document/PII | auth-effect verified: no auth `401`, Bearer-only `200 application/pdf` |
| EVO booking | `/api/v1/hotels/bookings/evo/{orderId}` | GET | PII/read | auth boundary passed: no auth `401`, Bearer-only `400 rate_not_found`; успешный контракт не подтверждён |
| Booking task status | `/api/v1/hotels/bookings/tasks/{taskId}/status` | GET | order/read | mobile auth unknown |
| Payment setup | `/api/v1/hotels/bookings/shevo/{orderId}/payment/setup` | POST | prepares money flow | не тестировать до QA gate |
| Payment execution | payment gateway candidate | POST | money | endpoint/body/auth unknown, blocked |

## Auth classification

| Группа | Методы | Текущий вывод |
| --- | --- | --- |
| Live-подтверждённый mobile Bearer | `customerdata`, `booking_list`, booking v1, voucher | Все четыре подключены через broker; voucher доступен только как owner-only local handoff без PDF/base64 в MCP JSON |
| Частично подтверждённые order reads | EVO booking | Bearer проходит auth boundary, но собственная проверенная бронь вернула `rate_not_found`; не включать в broker как рабочую функцию |
| Order reads без identifier evidence | task status | Собственный `taskId` не найден; identifiers не подбирать |
| Дополнительные customer reads | reservation, review order status, tranche/BNPL, account cashback | Customer context вероятен, точный mobile profile не подтверждён |
| Public/search reads | locations/SEO, search/map, hotel card, rates, hotel reviews | Mobile login не должен быть обязательным; используется search/service profile |
| Customer writes | booking create, cancel/update, promocode apply, extra services | Авторизация обязательна, но одного Bearer недостаточно считать execution готовым |
| Money flow | payment setup и payment gateway | Отдельный contract/security gate; не переносить банковский `/v1/pay` автоматически |

HTTP-метод `POST` сам по себе не означает mutation: `booking_list` является
read-only запросом. И наоборот, наличие mobile Bearer не закрывает требования
к `x-real-ip`, idempotency, antifraud и reconciliation для writes.

## Live evidence 2026-08-21

Пользовательский read-only запуск probe `1.0` на собственной mobile session
вернул HTTP 200 для `customerdata` и `booking_list` уже на `bearer_only`.
`sessionid`, cookies, device ID и `x-real-ip` не потребовались. Response bodies
не читались. Маршруты с `orderId`/`taskId` не проверялись из-за отсутствия
идентификаторов.

Повторный запуск probe `1.1` дал для обоих routes `401` без Authorization и
`200` с `bearer_only`. Auth effect подтверждён без `sessionid`, cookies,
device ID и `x-real-ip`. Этот вывод относится только к указанным origin и
routes; voucher, EVO booking и task status остаются `not_tested` без
собственных identifiers.

Последующий совместный read-only smoke получил карточку собственной брони через
`get_booking_v1` с mobile Bearer. Этот запуск подтвердил accepted auth profile,
но не повторял отдельный no-auth control, поэтому его evidence уже, чем
auth-effect probe первых двух routes.

Banking MCP `0.7.0` добавляет четыре подтверждённые операции в Hotels scope
broker, а Hotels MCP `0.14.0` направляет через него `get_customer`,
`list_bookings`, `get_booking_v1` и безопасное локальное сохранение voucher.
Broker не возвращает credentials; provider payload передаётся только в явно
вызванный customer tool и может содержать персональные данные пользователя.
Обезличенный end-to-end smoke через реальный broker подтвердил readiness
`mobile_read_only_ready`, контрактную форму customer payload и получение всех
трёх категорий booking list. Новый privacy-safe CLI передаёт provider `orderId`
только внутри MCP-процесса: agent-facing list/detail используют process-local
`bookingRef`, а итоговый smoke-report не содержит PII или identifiers.

Voucher handoff реализован после live auth evidence исключительно на
fixture/fake transport: provider `orderId` разрешается внутри MCP, PDF остаётся
в broker, проверяется по типу, сигнатуре и размеру и сохраняется в owner-only
локальный файл с TTL. MCP получает только путь и безопасные метаданные. Это не
является новым production probe и не разрешает EVO, task status или mutations.

Probe `1.2` локально обнаружил собственный `orderId` без его вывода и повторил
no-auth control. `booking_v1` и voucher подтвердили auth effect: control `401`,
Bearer-only `200`; voucher вернул `application/pdf`, содержимое документа не
читалось. EVO прошёл auth boundary (`401` → `400`), но для проверенного заказа
вернул безопасный provider code `rate_not_found`, поэтому работоспособность EVO
не подтверждена. После чтения пяти собственных booking details `taskId` не
обнаружен; task status не тестировался и identifiers не подбирались.

## Вопросы, которые закрывает команда интеграции

| Вопрос | Способ получения ответа | Gate |
| --- | --- | --- |
| Какие Hotels endpoints принимают mobile Bearer? | endpoint matrix: contracts/captures, затем read-only smoke на собственной сессии | Не расширять broker allowlist без evidence |
| Нужны ли cookies/device headers/`x-real-ip`? | сравнить mobile capture и минимальный запрос; добавлять только подтверждённые поля | Не выдумывать antifraud-атрибуты |
| Один ли mobile token используется Banking и Hotels? | broker выполняет подтверждённый hotel GET той же сессией | Успешный GET подтверждает только этот origin/path |
| Как обновляется сессия? | текущий refresh grant + silent re-login из upstream; broker остаётся единственным владельцем refresh | Тест ротации без раскрытия token values |
| Как завершается сессия? | определить фактические TTL/keepalive и официальный logout, если доступен | До выяснения предоставлять локальное удаление session как явный logout |
| Как используется `paymentToken`? | статический поиск mobile-кода/captures по `payment/setup`, `paymentToken`, `payment-gate/payments`, `Pg-Api-System` | Никаких реальных денег |
| Каковы `Pg-Api-System` и `flow.type`? | получить hotel-specific capture или внутренний контракт | Не переносить значения другой вертикали |
| Есть ли idempotency? | OpenAPI/backend contract/capture headers и повторный sandbox request только с разрешением | Execution остаётся выключенным |
| Что делать после timeout? | task/payment status reconciliation по provider ID | Не повторять write автоматически |
| Где хранить сессию? | macOS Keychain/корпоративный secret store adapter | Файл `0600` допустим только для local experimental режима |

Текущая команда локального удаления — `python login_cli.py --logout`. Она не
доказывает отзыв server-side сессии. Broker protocol v2 и advisory lock закрывают
локальные contract/race риски, но процессы одного OS-пользователя остаются в
одной границе доверия.

## Порядок безопасной проверки

1. Локальный вход и проверка session status без provider-вызова.
2. Остановить auth broker и запустить
   `tbank-hotels-mobile-auth-probe --acknowledge-read-own-data`: сначала без
   identifiers, затем при наличии только с собственными `orderId`/`taskId`.
3. Зафиксировать для каждого route первый принятый вариант: `bearer_only`,
   `bearer_session` или `capture_compatible`. Не переносить результат между
   routes и не трактовать `400/404/5xx` как auth evidence.
4. Только после evidence расширять Hotels broker allowlist и выполнять
   соответствующий customer-read smoke через Hotels MCP.
5. Статический анализ hotel payment setup и payment-gate linkage.
6. Fixture/contract tests.
7. Только отдельное non-production разрешение на prepare/setup без списания.
8. Payment execution остаётся отдельным этапом с human confirmation,
   idempotency и reconciliation.

## Офлайн-контракт передачи payment preview

Hotels MCP `0.22.0` и Banking MCP `0.13.1` реализуют локальный capability
handoff. Основной Banking-инструмент
`tbank_banking_prepare_hotel_payment_handoff_preview`. Он принимает только:

- одноразовый короткоживущий `paymentHandoffRef`, выпущенный общим broker после
  `tbank_hotels_create_payment_handoff_preview(bookingRef)`;
- process-local `accountRef` от Banking MCP.

Provider `orderId`, `paymentToken`, mobile token, cookies и antifraud-заголовки
через эту границу не передаются. Broker выполняет booking v1 read и связывает
наблюдаемые `paymentPrice` и raw `paymentStatus`; Banking не принимает сумму от
модели. Статус остаётся неинтерпретированным, а `paymentPrice` не считается
автоматически суммой к списанию. Старый experimental tool с
сырым `booking_order_id` удалён из публичного MCP-контракта.

Первый Banking preview атомарно поглощает capability; повторный preview требует
нового handoff. Readiness сообщает `bookingBindingSupported`, а подтверждение
связи относится только к конкретному выпущенному capability. Raw status
возвращается как `paymentStatusObservation`, чтобы не смешивать наблюдаемый
provider fact с решением о возможности оплаты.

Сумма проходит границу как `amountDecimal`, а не binary floating-point число.
Broker добавляет локальное время наблюдения и окно свежести; Banking отклоняет
протухшие facts и проверяет, что `accountRef` принадлежит текущему процессу, до
поглощения capability. Это не подтверждает официальный decimal scale: исходный
provider JSON уже разобран upstream-клиентом.
Возвращаемый `payloadHash` защищён случайным per-process pepper и не позволяет
проверять предполагаемые provider account ID простым offline SHA-256 перебором.
Если validation отклоняет уже атомарно поглощённый capability, ошибка прямо
указывает создать новый handoff вместо повторного использования старого ref.

Это закрывает безопасную статическую цепочку только до локального preview. Для
связывания с настоящей оплатой всё ещё нужны официальные контракты Hotels
payment setup/payment gateway, idempotency, reconciliation и доверенный канал
подтверждения.

## Structure-only intake собственных booking fixtures

Local toolkit `0.5.0` предоставляет команды `inspect-booking-fixture` и
`payment-readiness`. Первая принимает
только явно указанный локальный JSON-файл, не обращается к provider и формирует
отдельный отчёт без исходных значений. Динамические object keys, похожие на UUID,
короткие смешанные identifiers, числовые identifiers, email или длинные
hex-значения, заменяются на
`<dynamic-key>`.

`payment-readiness` полностью офлайн перечисляет закрытые локальные гарантии и
неподтверждённые gates. До получения официальных Hotels payment setup/gateway,
status, idempotency, reconciliation и antifraud contracts readiness остаётся
fail-closed; банковский `/v1/pay` не считается hotel payment endpoint.

Этот отчёт разрешено использовать только как evidence наблюдаемой формы. Он не
доказывает required-поля, не определяет payment semantics и не активирует
payment setup. Следующий gate — получить такой structure-only отчёт по уже
имеющемуся ответу собственной брони и отдельно сверить потенциальные amount,
currency и payment-state поля с переданными контрактами. Исходный booking JSON
остаётся вне репозитория и не передаётся модели.

Если raw fixture заранее не сохранён, `capture-booking-shape` выполняет только
после явного `--acknowledge-read-own-data` два bounded read собственной брони:
`hotels.list_bookings` для одной выбранной категории и
`hotels.get_booking_v1` для первой записи provider order. Raw ответы остаются в
памяти локального процесса; на диск записываются только две structure-only
формы. Возможный refresh mobile session принадлежит broker. Команда не доступна
модели и не выполняет payment setup или иные writes.

Пробник не отправляет `x-real-ip`, не читает response bodies и не содержит
payment/mutation routes. `200` на `bearer_only` доказывает минимальный профиль
только для конкретного route. Успех лишь на `capture_compatible` не доказывает,
что одного Bearer достаточно. Отсутствие тестового identifier фиксируется как
`not_tested_missing_identifier`, без подбора значений.
