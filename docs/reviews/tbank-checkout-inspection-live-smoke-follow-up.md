# Checkout inspection: implementation и bounded live smoke

**Статус:** completed implementation follow-up / Hotels `0.30.0`, Banking
`0.17.0`, toolkit `0.16.0`.

## Scope

- journey-level чтение актуального checkout выбранного тарифа;
- promo validation без применения;
- opaque дополнительные услуги и optional upgrade;
- локальный preview желаемых изменений без provider write;
- hermetic regression gate и один bounded production read-only smoke.

Вне scope: применение/удаление промокода, изменение дополнительных услуг,
создание брони, оплата, отмена и activation execution profiles.

## Реализация

- `tbank_hotels_inspect_checkout` сам связывает выбранный journey rate с
  provider `bookHash` и не принимает provider DTO от модели.
- Ответ исключает `bookHash`, `checkOutId`, hotel/room/service IDs и cashback
  account numbers.
- Дополнительные услуги представлены process-local
  `extraServiceOptionRef`; provider IDs не сохраняются в checkout inspection.
- `tbank_hotels_preview_checkout_changes` выполняется локально, не применяет
  изменения и не вычисляет неподтверждённую итоговую цену.
- Смена option/rates/rate инвалидирует предыдущую inspection.

## Найденный и закрытый дефект

Первый smoke дошёл до выбранного тарифа, но `inspect_checkout` завершился
ошибкой. Единственный последующий `connection_status` подтвердил локальную
готовность `0.30.0`, configured transport/auth и scope
`local_configuration_only`; provider retry или low-level обход не выполнялся.

Офлайн-сверка `message (5).txt` показала причину: GET
`/api/v3/rates/{bookHash}` возвращает выбранный rate внутри
`payload.roomsForBooking.rooms[].rates[]`, а первоначальный normalizer ожидал
v1 checkout wrapper `payload.rate`. Normalizer исправлен: поддерживает обе
формы и fail-closed выбирает только rate с ожидаемым `bookHash`. Fake fixture
переведён на фактическую v3-форму и содержит decoy rate для regression-проверки.

## Bounded live evidence

Повторный smoke после исправления выполнил последовательность: один hotel
search, выбор первого доступного варианта, один rates fetch, выбор первого
тарифа, один checkout inspection с заведомо недействительным smoke-промокодом
и optional upgrade, затем один локальный preview.

Результат:

| Проверка | Результат |
| --- | --- |
| Server version | `0.30.0` |
| Checkout | `ready` |
| Цена и отмена | присутствуют |
| Promo validation | `invalid`, без применения |
| Upgrade | `ready` |
| Provider identifiers в ответе | отсутствуют |
| Local preview | `preview_only`, provider write `false` |
| Booking/payment | не создавались и не запускались |
| stderr | пусто |

Значения отеля, тарифа, цены и provider identifiers намеренно не сохранены в
этом evidence.

## Checks

- Hotels tests: `72/72`.
- Banking tests: `52/52`.
- Toolkit tests: `21/21`, включая Unix socket вне sandbox.
- Contract manifests и MCP conformance: passed.
- Provider requests в automated tests: `0`.

## Остаточная граница

Read-only checkout inspection готов к внешнему review. Stateful promo и
extra-services apply остаются `NO-GO`: live smoke их не вызывал и не даёт
evidence по TTL, повтору, rollback или возможному потреблению промокода.
