# Development reference — Travel Assistant

Этот документ является компактным справочником для будущих задач реализации Travel Assistant.

Он не является основным roadmap, трекером задач, активным списком работ, источником статусов этапов или разрешением начинать следующий этап. Актуальный статус, критерии перехода, перенесенные пункты и следующий разрешенный шаг фиксируются только в `docs/roadmap/roadmap.md`.

## Роль документа

Используй этот файл только после того, как отдельная явная задача, согласованная с roadmap, активировала соответствующую реализацию.

Приоритет источников:

1. `docs/roadmap/roadmap.md` — статусы этапов и следующий разрешенный шаг.
2. `AGENTS.md` — правила репозитория и работы агентов.
3. `docs/product/product-baseline.md` — продуктовые границы MVP.
4. `docs/architecture/architecture-baseline.md` — архитектурная основа и стек backend.
5. Активные правила в `docs/development/` — реализация, тестирование, документация и качество для явно поставленных задач.
6. `docs/development/roadmap.md` и `docs/development/implementation-strategy.md` — справочные материалы, а не активный список задач.

Если этот документ конфликтует с основным roadmap или документами-основами, приоритет имеют основной roadmap и документы-основы.

## Статус и активация

Этот справочник не фиксирует текущий статус проекта. Последний завершенный этап и следующий разрешенный шаг всегда проверяй в `docs/roadmap/roadmap.md`.

Бизнес-логика, интеграция provider, DB/storage, frontend, generated clients и промышленная реализация не начинаются без отдельной явной задачи, согласованной с roadmap.

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
| Security / observability / local development | Использовать как справочные области будущей готовности, а не как текущий список задач. |

## Milestone vocabulary, reference only

Эти области milestones помогают обсуждать размер и направленность будущих задач реализации после явной активации через roadmap. Они не задают порядок выполнения и не являются активным списком задач.

| Milestone area | Reference value |
|---|---|
| Project/process foundation | Навигация, правила задач, базовая документация и repo workflow. |
| Product/architecture foundation | Требования, MVP boundaries, architecture boundaries и domain/API preparation на уровне документации. |
| Backend foundation | Минимальная основа на Kotlin + Ktor после явной активации; текущее состояние backend сверять с основным roadmap. |
| AI orchestration foundation | `LlmClient`, intent/slot/clarification flow и testable orchestration после отдельной задачи. |
| Hotel search foundation | Hotel-only provider abstraction, mock/fake providers and ranking-ready hotel data. |
| Web MVP | Chat UI, hotel results UI и frontend/backend integration после явной activation. |
| End-to-end MVP | Hotel-only flow from request to ranked/explained hotel offers. |
| Quality/readiness | Справочные темы тестирования, security, observability, локальной разработки и готовности к промышленному использованию. |

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
- активацию следующего этапа;
- активный список реализации;
- API/OpenAPI contracts;
- DB schema/storage model;
- auth/security/DevOps/testing backlog;
- план промышленной реализации;
- provider-specific integration design.

Для этих вопросов используй explicit roadmap task и relevant source-of-truth documents.
