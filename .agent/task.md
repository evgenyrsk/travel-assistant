# Active task

**Статус:** combined developer-preview published; local login recovery pending

## Goal

Подготовить experimental Hotels/Banking MCP toolstream к публичному
read-only/preview-only выпуску: разделить крупные runtime-обязанности, сузить
поставляемую Banking mobile-поверхность и добавить безопасный переход из
выбранного journey в официальный checkout без передачи card data модели и без
provider mutations.

## Acceptance criteria

- [x] Hotels stdio entrypoint, tool schemas и runtime orchestration разделены
  без изменения существующих tool contracts.
- [x] Новый checkout handoff не принимает PII, PAN, CVC/CVV, OTP, PIN, mobile
  tokens, provider `paymentUrl` или доверенные headers как tool arguments.
- [x] Checkout handoff ведёт на public page выбранного отеля; для одной комнаты
  без детей переносит только подтверждённые даты и число взрослых, не переносит
  exact rate, secrets или PII.
- [x] Номера тарифов стабильны во всём journey, а готовая таблица имеет
  однократную presentation-семантику.
- [x] Follow-up «среди показанных» не вводит отель из-за пределов предыдущей comparison-группы.
- [x] Персонализированный hotel search требует явный privacy-safe `hotelPreferences`.
- [x] Публичный Banking MCP не включает денежные, marketplace, grocery и
  messenger tools как MCP-поверхность; packaged-code риск имеет
  явный минимальный boundary и regression gate.
- [x] Прямое booking/payment execution остаётся fail-closed и не блокирует
  read-only/preview-only release.
- [x] Manifests, документация и offline release gate нового checkpoint синхронизированы.
- [x] Portable launcher, packaged phone login и artifact allowlists проверены вне checkout.
- [x] Внутренний publication review завершён.
- [x] Независимый fresh-context review публикационного checkpoint завершён.
- [x] Anonymous read-only search не требует JWT и не отправляет Authorization.
- [x] Toolkit `0.13.1` загружен в npm и проверен fresh-install.
- [x] Banking package `0.17.0` загружен в PyPI.
- [x] One-command combined install проверен из public registries вне checkout.

## Constraints

- Hotels и Banking остаются раздельными MCP по ADR-0003/ADR-0004.
- Не выполнять provider/live calls, production booking/payment/cancel/update.
- Не читать и не выводить секреты или платёжные реквизиты.
- Не переносить internal API endpoints в публичную MCP-поверхность.
- Не менять Kotlin backend и public Travel Assistant OpenAPI.

## Out of scope

- Активация production execution.
- Direct-card flow с PAN/CVV через LLM/MCP.
- Remote transport, Docker и OS credential store.
- Утверждение фактической auth-схемы без bounded external evidence.
- Активация raw-card, Banking `/v1/pay` или direct payment execution.
