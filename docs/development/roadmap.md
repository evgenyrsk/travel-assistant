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
6. `docs/development/roadmap.md`, `docs/development/milestones.md` and `docs/development/implementation-strategy.md` — future/reference material, not active backlog.

Если этот документ конфликтует с primary roadmap или baseline-документами, приоритет имеют primary roadmap и baseline-документы.

## Текущий статус

На момент Stage 7.0f-f:

- Stage 7 corrective stabilization and documentation cleanup завершены до Stage 7.0f-f включительно.
- Следующий Stage 7 implementation step не активирован этим документом.
- Business logic, provider integration, DB/storage, frontend, generated clients и production implementation не начинаются без отдельной явной roadmap-aligned задачи.

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
- Stage 7.2 start;
- active implementation backlog;
- API/OpenAPI contracts;
- DB schema/storage model;
- auth/security/DevOps/testing backlog;
- production implementation plan;
- provider-specific integration design.

Для этих вопросов используй explicit roadmap task и relevant source-of-truth documents.
