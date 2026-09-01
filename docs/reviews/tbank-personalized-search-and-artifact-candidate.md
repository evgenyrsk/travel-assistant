# Персонализированный поиск и локальные artifact candidates T-Bank MCP

**Статус:** завершённый локальный implementation checkpoint.

**Версии:** Hotels MCP `0.23.0`, Banking/broker `0.14.0`, local toolkit
`0.6.1`.

## Scope

- privacy-safe передача `hotelDefaults` Banking MCP в `hotelPreferences`
  Hotels MCP;
- детерминированный мягкий `best_value` без provider price filter;
- presentation-поля для денег, времени и отмены;
- исключение повторного provider search при смене только локального ranking;
- воспроизводимые npm/wheel candidates с установкой вне checkout;
- fake/local tests, manifests, documentation и offline conformance.

В scope не входят provider smoke, публикация пакетов, remote transport,
Keychain, Docker и любые booking/payment/cancel mutations.

## Реализованный контракт

Banking portfolio tool по-прежнему возвращает только обезличенный
`hotelDefaults`. Hotels `plan_stay` принимает тот же объект как
`hotelPreferences`: ценовой диапазон, `best_value` и разрешение показывать
альтернативы. Идентификаторы счетов, категории, абсолютные суммы и операции не
передаются между MCP.

Диапазон остаётся мягким: все provider options сохраняются, а попадание в него
показывается отдельно. `best_value_v1` использует только доступные provider
facts: rating — 60%, логарифмический review evidence — 20%, price utility —
20%. Score явно помечен как MCP-derived и не отправляется провайдеру.

Деньги отображаются без дополнительного округления. Timestamp сохраняет
исходное смещение и не называется локальным временем отеля без timezone-факта.
Отсутствующая информация об отмене отображается как «нет данных».

## Load и packaging safety

Search cache больше не включает локальную стратегию ranking в ключ. Повтор того
же поиска с другим локальным ranking переиспользует результат и меняет только
`rankingAppliedLocally`.

Hotels package имеет allowlist `files` и realpath-safe bin entrypoint. Offline
test собирает npm tarball, устанавливает его во временный каталог вне checkout
и выполняет `initialize`. Banking test аналогично собирает wheel из
изолированной копии, проверяет отсутствие session/env/test files, устанавливает
его вне checkout и выполняет `initialize`.

Эти проверки создают artifact candidates, но не являются публикацией. Hotels
package остаётся `private`; registry upload, checksums, SBOM и provenance не
добавлены.

## Проверки

- `npm --prefix tools/tbank-mcp-local run verify` вне sandbox: `13/13` toolkit,
  `51/51` Hotels, `49/49` Banking; manifests match; conformance обоих MCP
  прошёл `initialize`, `tools/list`, `ping`, framing, EOF shutdown и clean
  restart.
- Unix-socket lifecycle test общего broker прошёл вне sandbox.
- Provider requests: `0`; родительские `TBANK_*` credentials изолированы.

## Следующий gate

1. Независимый Qwen 3.8 Max review по обновлённому prompt.
2. Закрытие только release-blocking findings.
3. Шесть последовательных естественных smoke-кейсов в OpenCode и Codex CLI.
4. Отдельный fresh-machine/client/OS packaging этап с лицензированием,
   checksums, SBOM, provenance и secure storage.

Booking/payment execution и remote transport остаются отдельными будущими
этапами и не активируются этим checkpoint.
