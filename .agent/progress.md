# Progress

## Current focus

Publication hardening локального read-only/preview-only release candidate.

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
- Registry names и public preview metadata подготовлены; npm/GitHub login на
  текущей машине отсутствует или недействителен.
- Финальный anonymous-publication gate пройден: toolkit 17/17, Hotels 60/60,
  Banking 52/52, contracts/conformance и весь repository verify зелёные;
  provider requests 0. Оба npm publish dry-run содержат ровно по 8 файлов.

## Blocker

Direct booking/payment execution и exact-rate URL по-прежнему требуют внешнего
evidence. Это не блокирует read-only/preview-only выпуск и переход в
официальный checkout.

## Next action

Выполнить npm/PyPI/GitHub login, затем upload и fresh-install matrix в
Codex/OpenCode.
