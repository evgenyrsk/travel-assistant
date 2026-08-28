# Progress

## Current focus

Resumable bounded hotel-search coverage поверх опубликованного
read-only/preview-only release.

## Completed

- Предыдущий read-only/preview-only RC сохранён в dirty working tree.
- Оба Swagger-файла валидны; все локальные schema refs разрешаются.
- Подтверждены booking/LS/cancel/promocode/extra-services DTO.
- Подтверждён отдельный Hotels payment-task/PF/3DS flow; `/v1/pay` в контрактах
  отсутствует.
- Подтверждены payment task statuses и обязательный `x-real-ip` для create/PF.
- Выявлены текущие MCP drift: нет `pos`, `isBusinessTrip`, UUID validation и
  typed Hotels Payments flow.
- Booking/LS DTO исправлены: `pos`, `isBusinessTrip`, UUID card reference.
- Добавлен безопасный `tbank_hotels_create_payment_form_preview`; raw-card/3DS
  endpoints не выставлены.
- `connection_status.paymentFormExecution` и toolkit `payment-readiness` 2.0
  отделяют офлайн-подтверждённый контракт от внешних blockers.
- Mutation-флаг больше не может открыть write-path сам по себе: требуется
  отдельный non-production reviewed profile; production profile отсутствует.
- Зафиксировано покрытие 47 client-facing public v1 HotelsApi операций и
  классификация всех 24 payment paths.
- Hotels entrypoint сокращён до тонкого adapter: stdio, schemas, config,
  checkout и domain runtime разнесены по модулям.
- Добавлен `tbank_hotels_create_checkout_handoff`: безопасный внешний checkout
  без PII, `bookHash`, card data, provider write и обещания переноса тарифа.
- Checkout handoff теперь ведёт на public page выбранного отеля;
  exact rate, даты и гости по-прежнему не переносятся.
- Repeat comparison по умолчанию ограничен предыдущей показанной группой;
  полная journey требует явный scope.
- Добавлен `tbank_hotels_plan_personalized_stay` с обязательным
  `hotelPreferences`; Banking guidance запрещает лишние account/summary calls.
- Banking server/auth broker теперь получают `CuratedMobileSession` только с
  шестью allowlisted read-операциями; payment/transfer/login internals скрыты.
- Версии подняты до Hotels 0.26.0, Banking 0.16.0, toolkit 0.9.0; manifests
  обновлены офлайн.
- Полный локальный release gate пройден вне ограниченной песочницы: toolkit
  14/14, Hotels 58/58, Banking 52/52, включая Unix-socket lifecycle общего
  auth broker; manifests и conformance обоих MCP зелёные.
- Закрыты четыре P3 финального Qwen-аудита: version consistency, tool-local
  annotations, единый runtime handler registry и stale Banking editable metadata.
- Documentation gate и `git diff --check` пройдены.
- Тарифы получили стабильные `rateNumber`/`rateLabel`; rates table должна
  показываться один раз без перенумерации отфильтрованного подмножества.
- Read-only browser evidence подтвердил публичные `dateFrom`, `dateTo` и
  `guests` на странице выбранного отеля для одной комнаты без детей.
- Hosted checkout сохраняет выбранный отель, даты и число взрослых для простой
  occupancy; exact-rate handoff остаётся явно неподтверждённым.
- Версии нового checkpoint: Hotels 0.27.0, Banking 0.16.0, toolkit 0.10.0.
- Полный offline gate пройден вне restricted sandbox без пропусков: toolkit
  14/14, Hotels 59/59, Banking 52/52, manifests/conformance зелёные, provider
  requests не выполнялись.
- Documentation gate и `git diff --check` нового checkpoint пройдены.
- Banking `0.17.0` включает `tbank-banking-login` в wheel; legacy checkout
  wrapper сохранён для разработки.
- Toolkit `0.11.0` разрешает отдельно установленные runtime-команды из `PATH`
  или проверенных абсолютных overrides; repository paths — только fallback.
- Toolkit npm artifact ограничен runtime, manifests и README; установка и
  запуск вне checkout проверены тестом.
- Полный publication offline gate: toolkit 16/16, Hotels 59/59, Banking 52/52,
  manifests/conformance зелёные, provider requests 0.
- Внутренний publication review зафиксирован; технические P1 launcher/login и
  P2 artifact allowlist закрыты.
- Hotels `0.28.0` использует anonymous read-only search по умолчанию; service
  JWT/static token остаются опциональными, customer reads — через mobile broker.
- Toolkit `0.12.0` поддерживает setup без JWT key и сохраняет мутации закрытыми.
- `tbank-hotels-mcp@0.28.0` и `tbank-mcp-local@0.12.0` опубликованы в публичном
  npm registry. Обе версии подтверждены через registry metadata и установлены
  обратно в чистое временное окружение; Hotels MCP ответил на `initialize`,
  toolkit выполнил локальный `payment-readiness` без provider requests.
- Финальный anonymous-publication gate пройден: toolkit 17/17, Hotels 60/60,
  Banking 52/52, contracts/conformance и весь repository verify зелёные;
  provider requests 0. Оба npm publish dry-run содержат ровно по 8 файлов.
- Toolkit `0.13.1` добавляет `connect --client opencode|codex --profile ...`:
  он устанавливает фиксированные Hotels/Banking/toolkit versions в owner-only
  runtime, сохраняет только executable paths/transport settings, регистрирует
  два независимых MCP и запускает terminal-only mobile login.
- `tbank-mcp-local@0.13.1` опубликован в npm и fresh-installed в изолированный
  HOME. Hotels-only launcher ответил на `initialize` без broker/provider calls.
  `0.13.0` superseded: registry smoke нашёл лишний broker requirement у
  Hotels-only, исправление закреплено тестом до combined release.
- Banking wheel `0.17.0` собран, прошёл `twine check`, опубликован в PyPI и
  установлен публичным combined installer. Временный `.pypirc` удалён.
- Чистые OpenCode и Codex CLI зарегистрировали Hotels/Banking из public
  registries. Оба MCP ответили на `initialize`; mobile login в smoke был
  отключён, provider requests 0.
- Операционный инцидент release smoke: прямой installed `login --logout` был
  запущен без изолированного HOME и удалил локальный session-файл владельца.
  Серверные данные не менялись; требуется повторный terminal login. Отдельный
  regression test теперь проверяет, что product `connect` всегда привязывает
  login к управляемым session/socket paths и игнорирует parent overrides.
- Toolkit `0.14.0` добавляет `connect cursor` и короткий positional client для
  Codex/OpenCode. Cursor config следует официальному global stdio shape,
  объединяется с существующим `~/.cursor/mcp.json` и не хранит credentials.
- `tbank-mcp-local@0.14.0` опубликован в npm. Изолированные fresh-install
  проверки из npm/PyPI прошли для Cursor и Codex; обе регистрации содержат два
  независимых MCP, login пропущен, provider requests 0.
- Устранён конфликт guidance: запрос продолжить оформление после выбранного
  тарифа теперь направляется в `tbank_hotels_create_checkout_handoff`, а
  недоступность direct execution больше не обрывает безопасный внешний handoff.
- Patch-версии подготовлены: Hotels `0.28.1`, Banking `0.17.0`, toolkit
  `0.14.1`; targeted Hotels tests 60/60 зелёные.
- Полный offline gate пройден без пропусков: toolkit 21/21, Hotels 60/60,
  Banking 52/52, contracts/conformance и repository-wide checks зелёные;
  provider requests 0.
- `tbank-hotels-mcp@0.28.1` и `tbank-mcp-local@0.14.1` опубликованы в npm,
  установлены обратно во временный runtime и подключены к локальному Codex без
  повторного mobile login. Installed Hotels initialize подтвердил версию и
  checkout-handoff instructions.
- Hotels `0.29.0` классифицирует search coverage как
  `complete`/`substantial`/`partial`, возвращает ratio и bounded continuation
  guidance.
- `tbank_hotels_continue_stay_search` продолжает тот же journey с сохранённого
  offset, сохраняет прежние `optionId` и coalesces одновременные continuation.
- Общий initial+continuation предел остаётся 20 provider requests; request
  limit, repeated offset и provider failure не образуют retry loop.
- Truncated search больше не сохраняется как final global cache; завершённый
  continuation заполняет короткий cache.
- Новый checkpoint синхронизирован как Hotels `0.29.0`, Banking `0.17.0`,
  toolkit `0.15.0`; manifest обновлён offline.
- Targeted Hotels tests 64/64, toolkit 21/21, Banking 52/52, conformance и
  repository-wide `verify.sh all` прошли без пропусков; provider requests 0.
- Независимый Qwen 3.8 Max review checkpoint `0.29.0/0.17.0/0.15.0` дал
  `READY`, P0–P2 отсутствуют.
- Review hardening добавил terminal continuation tests, stale selection reset
  и механическое `continuationRecommended=false` после первого продолжения;
  targeted Hotels tests 68/68.
- Полный post-review offline gate пройден: toolkit 21/21, Hotels 68/68,
  Banking 52/52, manifests/conformance зелёные, provider requests 0.
- Первый Codex smoke обнаружил orchestration mismatch: модель использовала
  `location`, `guests`/`rooms[].adultsCount` и `limit`, из-за чего опубликованный
  strict runtime отклонял первый вызов и модель начинала перебирать форму.
- Agent-facing `plan_stay` теперь принимает bounded compatibility aliases
  `location`, `guests`, `limit`, `adultsCount` и `childrenAge`, локально
  нормализует их в `destination`, `rooms`, `maxOptions`, `adults` и
  `childrenAges`, а конфликтующие дубли отклоняет.
- Regression test закрепляет alias normalization без повторного provider
  contract guessing; targeted Hotels suite теперь 69/69, manifest обновлён.
- Focused Qwen review compatibility boundary дал `READY`, P0–P2 отсутствуют.
  Три P3 закрыты до публикации: конфликтные/unknown aliases покрыты шестью
  fail-before-fetch сценариями, room JSON Schema требует `adults` или
  `adultsCount`, а README и journey plan объясняют локальную нормализацию.
  Targeted Hotels suite теперь 71/71; manifest обновлён.
- Финальный post-review offline gate пройден: toolkit 21/21, Hotels 71/71,
  Banking 52/52, manifests/conformance зелёные, provider requests 0. Оба npm
  publish dry-run прошли с allowlist-наборами 8/9 файлов.
- Bounded live smoke текущего Hotels `0.29.0` через ephemeral Codex прошёл:
  обычный поиск Москвы, обязательный завтрак в Санкт-Петербурге, выбор только из
  comparison, rates, booking/payment previews, selected-hotel checkout handoff,
  customer summary и Banking-personalized поиск Казани.
- Персонализированный поиск начал с 100/144 (`partial`), выполнил ровно один
  continuation, не получил новых вариантов и корректно остановил automatic
  continuation. `preferencesApplied.applied=true`, альтернативы вне мягкого
  диапазона возвращены отдельно.
- Booking/payment/cancel writes, guest PII и card data не использовались.
- Harness finding: initial ephemeral override запускал managed published runtime
  вместо working tree; current-worktree smoke закреплён явным executable
  override и подтверждён `connection_status.serverVersion=0.29.0`.

## Blocker

Direct booking/payment execution и exact-rate URL по-прежнему требуют внешнего
evidence. Это не блокирует read-only/preview-only выпуск и переход в
официальный checkout.

## Next action

Провести короткий focused review добавленного compatibility boundary,
опубликовать `0.29.0/0.15.0`, затем проверить fresh install и естественные
Codex/OpenCode requests уже из registry runtime.
