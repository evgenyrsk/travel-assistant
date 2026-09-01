# Записи архитектурных решений

Эта директория предназначена для ADR — записей о значимых архитектурных решениях Travel Assistant.

`docs/roadmap/roadmap.md` остается source of truth по статусам этапов, progression и следующему разрешенному шагу. Этот раздел только объясняет, где находятся accepted ADRs, drafts, candidates и non-ADR decision inventory.

## Когда создавать ADR

ADR нужен, если решение:

- меняет архитектурные границы продукта;
- влияет на публичные контракты;
- выбирает важную технологию или провайдера;
- задает долгосрочный подход к AI/LLM-оркестрации;
- определяет способ интеграции с travel API;
- влияет на хранение данных, безопасность или кроссплатформенность.

Новый ADR создается только отдельным явным шагом. Наличие candidate или deferred decision не означает, что решение принято или что работу нужно выполнять сейчас.

## Что не является ADR

- продуктовая постановка Этапа 0;
- список открытых вопросов;
- предварительная рекомендация без принятого решения;
- обычное обновление навигации или документации;
- non-ADR decision inventory;
- future ADR candidate без отдельного принятого решения.

## Текущий статус

Приняты пять standalone ADR: о границе backend-сервиса с клиентами, о
provider-neutral semantic gateway для корпоративной инфраструктуры, об
изоляции experimental Banking MCP от Hotels MCP и об общем локальном broker
мобильной авторизации, а также о hosted-checkout границе публичных MCP.

Stage 5 создал non-ADR decision inventory в `docs/architecture/stage-5/architecture-decisions-draft.md`. Этот документ фиксирует confirmed Stage 5 architecture guardrails, deferred decisions и future ADR candidates. Он не создает accepted ADR, не активирует future decisions и не является implementation backlog.

## Accepted ADR

- [`ADR-0001 — граница backend-сервиса и внешних клиентских интеграций`](adr-0001-service-core-and-client-integration-boundary.md) — backend остается удаленным ядром, текущий web/PWA используется только как локальная demo shell, а product UI и SDK принадлежат будущим интеграционным командам.
- [`ADR-0002 — provider-neutral semantic gateway boundary`](adr-0002-provider-neutral-semantic-gateway-boundary.md) — Travel Assistant зависит от application port и versioned internal gateway contract, а model/provider остаются сменным deployment за этой границей.
- [`ADR-0003 — граница Banking MCP и Hotels MCP`](adr-0003-banking-mcp-and-hotels-composition-boundary.md) — phone auth, банковские агрегаты и будущий payment flow изолированы в отдельном experimental MCP; Hotels MCP получает агрегированный travel profile, а подтверждённые customer reads проходят через границу ADR-0004.
- [`ADR-0004 — общий локальный broker мобильной авторизации`](adr-0004-shared-mobile-auth-broker.md) — оба MCP остаются независимо подключаемыми, а при совместной работе используют одну mobile session без передачи токенов модели.
- [`ADR-0005 — граница hosted checkout для публичных MCP`](adr-0005-hosted-checkout-handoff-boundary.md) — публичный flow заканчивается безопасным переходом во внешний checkout без PII, payment credentials и прямой денежной мутации из LLM.

Accepted ADR являются отдельными ADR-файлами и применяются вместе с текущими
roadmap и baseline-документами.

## Draft ADR

Нет standalone draft ADR files.

Draft ADR — это черновик будущего ADR. Он не является accepted ADR, пока отдельная задача явно не принимает решение.

## ADR candidates

Standalone candidate files пока не созданы.

Future ADR candidates перечислены внутри `docs/architecture/stage-5/architecture-decisions-draft.md` как часть Stage 5 non-ADR inventory. Они не являются accepted decisions, текущими задачами или active backlog.

## Non-ADR decision inventory

- `docs/architecture/stage-5/architecture-decisions-draft.md` — Stage 5 non-ADR decision inventory. Это architecture baseline context, а не список accepted ADR и не implementation backlog.

Inventory может содержать confirmed architecture guardrails, deferred decisions, open questions и future ADR candidates. Он помогает ориентироваться в архитектурном контексте, но не заменяет ADR, roadmap или task tracker.

## Future decisions

Будущие ADR нужно создавать только когда отдельная явная задача принимает или меняет решение, требующее ADR.

Future ADR candidates из Stage 5 остаются candidates до такого решения. Они не должны трактоваться как разрешение создавать API/OpenAPI contracts, DB schema/storage model, auth/security/DevOps/testing backlog, implementation work или future-scope features.
