# Active task

**Статус:** resumable hotel-search checkpoint реализован, прошёл independent review и bounded live smoke; ожидается публикация

## Goal

Сделать неполную provider-выборку Hotels MCP честной и безопасно
возобновляемой: не считать усечённый результат финальным cache, продолжать тот
же journey без повторной загрузки уже собранных страниц и сохранять жёсткий
общий лимит нагрузки.

## Acceptance criteria

- [x] `searchCoverage` различает `complete`, `substantial` и `partial`, сообщает
  долю покрытия и доступность продолжения.
- [x] Частичный поиск не сохраняется как финальный global cache result.
- [x] Новый read-only tool продолжает существующий journey с сохранением
  прежних `optionId` и без повторной загрузки первых страниц.
- [x] Суммарно initial + continuation выполняют не более 20 provider search
  requests; terminal pagination anomalies не повторяются.
- [x] Tool guidance направляет естественный запрос «пять лучших» на одно
  продолжение только при действительно низком покрытии.
- [x] Hermetic tests, manifests, версии и активная документация синхронизированы.
- [x] Natural-language Codex smoke принимает устойчивые LLM-алиасы plan input
  без перебора provider contract и нормализует их в каноническую journey-форму.
- [x] Bounded live smoke подтвердил обычный поиск, обязательный завтрак,
  тарифы/preview/hosted checkout, customer summary и персонализированный поиск
  с одним continuation; production writes не выполнялись.

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
- [x] Короткие `connect cursor` и `connect codex` опубликованы и проверены.

## Constraints

- Hotels и Banking остаются раздельными MCP по ADR-0003/ADR-0004.
- После отдельного разрешения пользователя допустимы только bounded read-only и
  preview-only provider/live calls; production booking/payment/cancel/update запрещены.
- Не читать и не выводить секреты или платёжные реквизиты.
- Не переносить internal API endpoints в публичную MCP-поверхность.
- Не менять Kotlin backend и public Travel Assistant OpenAPI.
- Автоматические проверки остаются fake/offline; live evidence фиксируется
  отдельно и не подменяет hermetic regression gate.
- Не превращать `complete` в обещание полного provider-каталога: это полнота
  относительно reported filtered/total count конкретного search lifecycle.

## Out of scope

- Активация production execution.
- Direct-card flow с PAN/CVV через LLM/MCP.
- Remote transport, Docker и OS credential store.
- Утверждение фактической auth-схемы без bounded external evidence.
- Активация raw-card, Banking `/v1/pay` или direct payment execution.
- Remote transport и production mutation smoke.
