# Готовность booking и payment контрактов Hotels MCP

## Назначение и границы

Документ фиксирует офлайн-сверку двух предоставленных владельцем интеграции
Swagger-экспортов. Исходные файлы находятся вне репозитория и не публикуются.
Проверка не выполняла provider-запросы, бронирования, отмены или платежи.

Это источник состояния экспериментального Hotels MCP toolstream. Он не меняет
hotel-only MVP Kotlin/Ktor приложения и не активирует production mutations.

## Проверенные источники

| Контракт | Формат | Покрытие | SHA-256 |
|---|---|---|---|
| `HotelsApi` 1.0 | OpenAPI 3.0.4 | 120 paths, 511 schemas | `a2946891609aefae3d5dc1e731aa168ee7800dde8d94963d46308144bd4f5295` |
| `HotelsApi.Payments` 1.0 | OpenAPI 3.1.1 | 24 paths, 88 schemas | `3fc858c76d727b30c01f3fd562758f511fcd588725ce7f2a31a6685daec18091` |

Оба JSON корректно разбираются, все локальные `$ref` разрешаются. Хеши нужны
для воспроизводимой сверки будущих экспортов, а не для публикации исходников.

## Подтверждённый booking flow

| Шаг | Endpoint | Подтверждённый контракт |
|---|---|---|
| Создание задачи | `POST /api/v1/hotels/bookings/tasks/create` | `x-real-ip`, booking request, `taskId` UUID |
| Статус задачи | `GET /api/v1/hotels/bookings/tasks/{taskId}/status` | `in_progress`, `success`, `failed`; успешный результат содержит `orderId` |
| Отмена | `POST /api/v1/hotels/bookings/cancel` | body `{orderId}` |
| SHEVO setup | `POST /api/v1/hotels/bookings/shevo/{orderId}/payment/setup` | без body, возвращает непрозрачную payment credential |
| Промокод | `POST /api/v1/hotels/rates/{bookHash}/promocode` | body `{promocode}` |
| Дополнительные услуги | `POST /api/v1/hotels/rates/{bookHash}/extraServices` | body `{extraServiceIds}` |

Booking request требует `bookHash`, `guestContact` и `rooms[].guests[]`.
Подтверждены `paymentMeans=payment_form|on_us|off_us|dolyame|pos`, nullable
`paymentData.creditCardId` в формате UUID и nullable `isBusinessTrip`.

Публичного endpoint произвольного изменения созданного заказа в экспортированном
контракте нет. Внутренние update endpoints не выставляются как MCP tools.

### Покрытие public v1 HotelsApi

В основном Swagger найдено 55 операций под `/api/v1`. Все 47 клиентских
операций представлены текущими high-level или low-level MCP tools либо явным
safety guard для voucher. Восемь путей исключены осознанно:

- `baf/cookie` зависит от browser-cookie boundary;
- пять `hotels/internal-bookings/*` являются internal orchestration;
- `review/sync-likes` изменяет пользовательские реакции и не входит в текущий
  read-only review scope;
- `callbacks/sutochno/order` является provider callback.

Наличие endpoint в Swagger не означает готовность к execution: auth, trusted
headers, idempotency и reconciliation проверяются отдельными capability gates.

## Подтверждённый payment flow

Для публичной интеграции выбран только hosted payment form:

1. `POST /api/v1/hotels/bookings/prepay_task/pf/create` создаёт payment task.
2. Provider возвращает `taskId`, `orderId` и `paymentUrl`.
3. `GET /api/v1/hotels/bookings/payment_tasks/{taskId}/status` возвращает
   состояние задачи.
4. Swagger перечисляет `in_progress`, `give_card_data`, `waiting_booking`,
   `failed`, `completed`, `card_replacement`.
5. Переходы и terminal/action-required semantics этим enum не доказаны и
   остаются внешним contract gap.

`successUrl` и `failUrl` обязательны в provider request. Они должны поступать
из доверенной конфигурации интегратора, а не из MCP arguments или текста модели.
То же правило применяется к `x-real-ip`, device и antifraud context.

## Намеренно исключённые endpoints

Payment Swagger также содержит ввод карты, сохранение карты, fingerprint и
3-D Secure шаги. Они не должны становиться MCP tools: модель и обычный CLI не
являются PCI/3DS boundary. MCP никогда не принимает PAN, срок действия карты,
CVV/CVC, PIN, OTP, challenge data или browser fingerprint.

Banking `/v1/pay` отсутствует в обоих Hotels Swagger. Он не считается способом
оплаты отеля, пока владелец API не предоставит отдельный linkage contract.

### Классификация всех 24 payment paths

| Класс | Paths | Решение MCP |
|---|---|---|
| Hosted PF | `prepay_task/pf/create`, `payment_tasks/{taskId}/status` | Typed preview; execution gated |
| Agreement/account | `payment_tasks/agreement_data` | Не выставлять до auth и flow-sequence evidence |
| Saved-card inventory | `prepay_task/cards_list` | Не нужен hosted PF; customer auth не подтверждён |
| Raw-card/3DS | `prepay_task/create`, `prepay_task/card_data`, `prepay_task/remove_card`, `payment_tasks/credit_card_data` | Никогда не принимать card/3DS data через LLM-facing tools |
| Provider callbacks | `mapi_callback/*`, `pf_callback/payments/status` | Server-to-server callbacks, не клиентские tools |
| Internal processing | все `/internal_api/*`, `jobs/db_test`, `internal/{taskId}/cancel` | Не выставлять публично |

Таким образом, ни один путь экспорта не оставлен без явного решения. Возможная
будущая поддержка agreement/saved-card сценария требует отдельного security и
auth review и не блокирует hosted payment form.

## Реализованная безопасная поверхность

- booking DTO приведён к Swagger: `pos`, `isBusinessTrip`, UUID card reference;
- `tbank_hotels_create_payment_form_preview` показывает выбранный stay/rate,
  подтверждённую state machine и readiness без PII, credentials и HTTP;
- `connection_status.paymentFormExecution` машинно показывает подтверждённые
  офлайн-гейты и внешние blockers;
- `payment-readiness` local toolkit использует ту же fail-closed границу;
- raw-card endpoints не представлены в MCP tool manifest;
- `tbank_hotels_create_checkout_handoff` завершает публичный сценарий через
  внешний hosted checkout без PII, payment credentials, provider request или
  обещания переноса точного тарифа;
- `tbank_hotels_inspect_checkout` получает актуальный выбранный rate,
  нормализует цены/отмену/cashback, скрывает provider identifiers и может
  отдельно валидировать промокод либо запросить upgrade без применения;
- `tbank_hotels_preview_checkout_changes` локально показывает выбор
  проверенного промокода и opaque дополнительных услуг, но не вызывает
  stateful apply endpoints и не вычисляет неподтверждённую итоговую цену;
- checkout inspection действует пять минут и после истечения требует повторной
  provider-проверки; удаление применённого промокода не выставлено в public
  journey до подтверждения источника promo-состояния в read-контракте;
- booking/payment execution по умолчанию и в release candidate остаётся `NO-GO`.

Применение/удаление промокода и замена дополнительных услуг являются
изменением provider checkout quote, хотя ещё не создают бронь и не запускают
оплату. Error taxonomy содержит lifecycle-коды promo operation, поэтому эти
два POST нельзя классифицировать как read-only только по отсутствию списания.
Обычный journey ограничен inspection/validation/local preview; существующие
low-level `prepare → execute` остаются интеграционным `NO-GO` до отдельного
live evidence о TTL, повторе, rollback и потреблении промокода.

## Внешние blockers

| ID | Что должен подтвердить владелец интеграции |
|---|---|
| `payment_task_lifecycle_semantics_unverified` | Переходы, terminal и action-required semantics documented states |
| `non_production_payment_origin_unavailable` | Одобренный non-production origin, доступный из среды проверки |
| `payment_customer_auth_unverified` | Реальный customer auth profile для booking и hosted PF endpoints |
| `trusted_client_ip_source_unverified` | Доверенный источник `x-real-ip`, не контролируемый моделью |
| `provider_idempotency_unverified` | Idempotency key и duplicate-request semantics |
| `timeout_reconciliation_unverified` | Recovery, если create завершился timeout до получения `taskId` |
| `provider_payment_url_handoff_unverified` | Exact provider `paymentUrl` handoff остаётся неподтверждённым; публичный generic hosted checkout реализован отдельно |
| `non_production_execution_not_approved` | Явное разрешение и credentials для bounded non-production smoke |

Отдельно остаётся неизвестным consumer и lifecycle непрозрачной credential из
SHEVO payment setup. Этот маршрут не объединяется с hosted payment form по
предположению.

Владелец интеграции указал production origin `https://hotels.tbank.ru/api`.
Этот факт фиксирует routing, но не разрешает production smoke или mutations.
Доступный с текущей машины non-production origin отсутствует.

## Обязательные гейты перед execution

- [x] Booking request/response и task status сверены офлайн.
- [x] Hosted payment form request/response и task status сверены офлайн.
- [x] Raw-card/3DS поверхность исключена из MCP.
- [x] Typed schema, checkout inspection и local preview покрыты hermetic tests.
- [x] Зафиксирован owner-provided production origin без выполнения запросов.
- [ ] Получен одобренный и доступный non-production payment origin.
- [ ] Подтверждены переходы и terminal/action-required semantics payment task.
- [ ] Подтверждён customer auth на non-production endpoint.
- [ ] Настроен доверенный IP/device context вне модели.
- [ ] Подтверждены idempotency и unknown-outcome recovery.
- [x] Реализован безопасный generic hosted-checkout handoff без PII и writes.
- [ ] Подтверждён exact owner-bound provider `paymentUrl` handoff для будущего direct execution.
- [ ] Получено явное разрешение на bounded non-production smoke.
- [ ] Пройден smoke: create task → status polling → hosted form, без списания.
- [ ] Отдельно разрешён и пройден минимальный платёж с reconciliation.

До закрытия всех пунктов MCP может выполнять только read-only и preview-only
сценарии. Значение `TBANK_HOTELS_ENABLE_MUTATIONS=true` само по себе не является
достаточным основанием для booking или payment execution. Runtime также требует
явный `non_production_v1_reviewed` execution profile; production profile в
текущей версии отсутствует и не может быть активирован конфигурацией.

## Bounded non-production probe plan

План выполняется только на одобренном non-production контуре с тестовым
отелем/тарифом/пользователем и отдельным письменным разрешением. Каждый шаг
имеет жёсткий лимит один запрос, кроме bounded status polling.

1. Проверить customer access на `cards_list` и `agreement_data` с официально
   выданным профилем. Не перебирать headers после первого отказа.
2. Подтвердить, какие trusted headers gateway добавляет сам и какие принимает
   от интегратора. Значения не передавать через MCP arguments.
3. Создать одну тестовую booking task с уникальным correlation marker и
   опрашивать status не чаще одного раза в секунду, максимум 30 раз.
4. Сверить полученный test order через booking read. При timeout до `taskId`
   остановиться и выполнить owner-provided reconciliation, не повторять create.
5. Создать один hosted PF task с доверенными test success/fail redirects.
   Проверить только получение task metadata и owner-bound handoff; форму не
   подтверждать и оплату не выполнять.
6. Проверить payment task status тем же bounded polling. Не интерпретировать
   неизвестное состояние самостоятельно.
7. Отменить/очистить тестовый объект только официальным sandbox cleanup flow и
   зафиксировать результат reconciliation.
8. Idempotency/duplicate и искусственный timeout тестировать отдельно через
   управляемый proxy/fault injection, а не повторным ручным вызовом create.

Stop conditions: любой `401/403`, неизвестный state, отсутствие `taskId`,
network timeout, redirect вне allowlist или несоответствие суммы/валюты.
После stop запрещены автоматический retry и переход к следующему шагу.
