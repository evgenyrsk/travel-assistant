# Development reference — Travel Assistant

Этот документ является compact development reference для будущих implementation-oriented задач Travel Assistant.

Он не является primary roadmap, task tracker, active backlog, source of truth по статусам этапов или разрешением начинать следующий Stage 7 шаг. Актуальный roadmap status, stage gates, carryover и следующий разрешенный шаг фиксируются только в `docs/roadmap/roadmap.md`.

## Роль документа

Используй этот файл только после того, как отдельная явная roadmap-aligned задача активировала соответствующую implementation работу.

Приоритет источников:

1. `docs/roadmap/roadmap.md` — статусы этапов и следующий разрешенный шаг.
2. `AGENTS.md` — repository/agent governance.
3. `docs/product/product-baseline.md` — product/MVP scope.
4. `docs/architecture/architecture-baseline.md` — architecture baseline и backend stack.
5. Active engineering rules in `docs/development/` — implementation, testing, documentation and quality rules for explicit tasks.
6. `docs/development/roadmap.md` and `docs/development/implementation-strategy.md` — future/reference material, not active backlog.

Если этот документ конфликтует с primary roadmap или baseline-документами, приоритет имеют primary roadmap и baseline-документы.

## Статус и активация

Этот reference не фиксирует текущий статус проекта. Для текущего Stage 7 состояния, последнего завершенного шага и следующего разрешенного шага всегда используй `docs/roadmap/roadmap.md`.

Business logic, provider integration, DB/storage, frontend, generated clients и production implementation не начинаются без отдельной явной roadmap-aligned задачи.

## Development areas, reference only

Ниже перечислены будущие development areas. Это не backlog и не порядок выполнения.

| Area | Как читать |
|---|---|
| Backend foundation | Только как ориентир для будущих Kotlin + Ktor задач после явной активации. |
| Domain/application layers | Сохранять domain независимым от Ktor, Next.js, PostgreSQL, Redis и конкретных LLM/travel providers. |
| LLM abstraction | Любой provider должен идти через `LlmClient`; real provider integration требует отдельной задачи. |
| Hotel search abstraction | MVP v1 остается hotel-only; provider/API details должны быть за abstraction. |
| Web frontend | Next.js + React + Tailwind + shadcn/ui остаются рабочей гипотезой, но frontend work не активирован этим документом. |
| Testing / quality | Тесты добавляются вместе с явно активированными behavior/code changes. |
| Security / observability / local development | Использовать как future readiness areas, не как текущий backlog. |

## Milestone vocabulary, reference only

Эти milestone areas помогают обсуждать размер и направленность будущих implementation tasks после явной roadmap activation. Они не задают порядок выполнения и не являются active backlog.

| Milestone area | Reference value |
|---|---|
| Project/process foundation | Навигация, правила задач, базовая документация и repo workflow. |
| Product/architecture foundation | Требования, MVP boundaries, architecture boundaries и domain/API preparation на уровне документации. |
| Backend foundation | Минимальный Kotlin + Ktor foundation после явной activation; текущий backend state сверять с primary roadmap. |
| AI orchestration foundation | `LlmClient`, intent/slot/clarification flow и testable orchestration после отдельной задачи. |
| Hotel search foundation | Hotel-only provider abstraction, mock/fake providers and ranking-ready hotel data. |
| Web MVP | Chat UI, hotel results UI и frontend/backend integration после явной activation. |
| End-to-end MVP | Hotel-only flow from request to ranked/explained hotel offers. |
| Quality/readiness | Testing, security, observability, local development and production readiness references. |

## Как формулировать будущие development tasks

Каждая будущая task должна:

- ссылаться на явную roadmap activation;
- указывать product/architecture baseline, если затрагивает scope или architecture;
- иметь маленький проверяемый scope;
- перечислять expected files/modules без преждевременной детализации;
- фиксировать out-of-scope items;
- содержать validation steps;
- не выполнять соседние recommendations.

## Что намеренно не фиксируется здесь

- текущий roadmap status;
- порядок этапов;
- next Stage 7 step activation;
- active implementation backlog;
- API/OpenAPI contracts;
- DB schema/storage model;
- auth/security/DevOps/testing backlog;
- production implementation plan;
- provider-specific integration design.

Для этих вопросов используй explicit roadmap task и relevant source-of-truth documents.
