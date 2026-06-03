# Travel Assistant

Travel Assistant — приватный проект для разработки AI-помощника по планированию путешествий.

README является входной картой проекта: он помогает понять назначение репозитория и найти ключевые документы. README не является roadmap, product spec, architecture spec, task tracker или implementation backlog.

## Цели

- Помогать пользователям планировать поездки с учетом направлений, дат, бюджета и предпочтений.
- Организовывать маршруты, места, бронирования и travel notes.
- Сохранять структуру проекта гибкой, пока продуктовые решения и архитектурные контракты уточняются по roadmap.

## Структура репозитория

```text
.
├── AGENTS.md            # Правила работы Codex/AI-агентов в репозитории
├── README.md            # Входная точка и навигация
├── .env.example         # Пример локальных переменных окружения
├── .gitignore           # Правила исключения локальных и generated-файлов
├── .github/             # GitHub issue и pull request templates
├── app/                 # Будущий frontend/application слой
├── services/            # Backend/services модули; services/backend содержит Stage 7.0b Kotlin + Ktor skeleton
├── docs/                # Продуктовая, roadmap, архитектурная и development-документация
├── tests/               # Будущие тесты, fixtures и e2e-сценарии
└── scripts/             # Будущие скрипты разработки и автоматизации
```

## Карта документации

- `README.md` — главная входная точка проекта и краткая навигация.
- `AGENTS.md` — обязательные правила для Codex/AI-агентов внутри этого репозитория.
- `docs/product/README.md` — index product-документации и role labels; navigation only.
- `docs/product/product-baseline.md` — product source of truth и актуальное компактное продуктовое состояние после Stage 0-5.
- `docs/product/stage-0/` — Этап 0: продуктовая рамка, первичные сценарии, границы MVP, допущения и открытые вопросы.
- `docs/product/stage-1/` — Этап 1: бизнес-сценарии, требования, user journeys, assumptions и consistency review.
- `docs/product/stage-2/` — Этап 2: use cases, edge cases, assistant behaviour rules, combined search levels и data requirements.
- `docs/product/stage-3/` — Этап 3: MVP UX / Navigation, screen map, navigation model, UX flows, required fields и acceptance criteria.
- `docs/product/stage-4/` — Этап 4: Visual Design & UX System, visual direction, design system foundations, component inventory, screen specifications и interaction patterns.
- `docs/PROJECT_BRIEF.md` — продуктовый контекст, пользователи, ключевые сценарии и открытые продуктовые вопросы.
- `docs/ARCHITECTURE.md` — предварительные архитектурные ориентиры; текущий backend stack подтвержден как Kotlin + Ktor в architecture baseline.
- `docs/architecture/README.md` — index архитектурной документации и role labels; navigation only.
- `docs/architecture/architecture-baseline.md` — architecture source of truth, backend stack authority и актуальное компактное архитектурное состояние после Stage 5.
- `docs/roadmap/roadmap.md` — primary roadmap и source of truth по статусам этапов, progression, carryover и следующему разрешенному шагу.
- `docs/ROADMAP.md` — краткий navigation overview этапов, не competing roadmap и не источник текущих статусов.
- `docs/guides/documentation-style-guide.md` — единые правила языка, структуры, терминологии, guardrails и безопасного рефакторинга документации.
- `docs/reviews/README.md` — индекс review/audit artifacts и правила чтения historical/current cleanup reports.
- `docs/reviews/*.md` — audit trail, cleanup reports и historical reviews; читать через `docs/reviews/README.md`.
- `docs/decisions/README.md` — правила ведения ADR; ADR пока не созданы.
- `docs/development/roadmap.md` — compact development reference; future/reference material, не roadmap status source и не active backlog.
- `docs/development/milestones.md` — compact milestone vocabulary; future/reference material, не источник статусов и не active backlog.
- `docs/development/implementation-strategy.md` — будущая стратегия реализации и правила декомпозиции задач; не primary roadmap и не active implementation backlog.
- `services/backend/README.md` — инструкция запуска Stage 7.0b Kotlin + Ktor backend skeleton и health endpoint.
- `docs/prompts/` — переиспользуемые правила и шаблоны Codex/opencode задач.
- `.github/` — GitHub templates для постановки задач и описания pull requests.

## Начало работы

Актуальный статус, завершенные артефакты, открытые решения и следующий шаг фиксируются только в primary roadmap: `docs/roadmap/roadmap.md`.

Текущий baseline: Stage 0-6 завершены, Stage 7 находится в статусе `In progress / awaiting explicit next task`, а Stage 7.0 stabilization and documentation cleanup завершены до Stage 7.0f-f включительно. Stage 7.2+ не активированы и требуют отдельной явной roadmap-aligned задачи. Business logic, provider integration, frontend implementation, DB schema/storage model, auth/security/DevOps/testing backlog, generated clients и production code не создаются без отдельной явной roadmap activation.

## Рабочий процесс Codex

- Обязательные правила репозитория для Codex-агентов находятся в `AGENTS.md`.
- Переиспользуемый шаблон постановки задач находится в `docs/prompts/task-template.md`.
- Canonical правила roadmap, scope, ADR, validation, documentation language и отчетности находятся в `AGENTS.md`; `docs/prompts/codex-rules.md` является prompt companion, а не competing governance source.
- Шаблон задач на ревью с проверками отклонения от roadmap, разрастания границ задачи и преждевременной реализации будущих этапов находится в `docs/prompts/review-template.md`.
- GitHub PR checklist находится в `.github/pull_request_template.md`.
- Primary roadmap со статусами этапов находится в `docs/roadmap/roadmap.md`.
- Верхнеуровневый список этапов находится в `docs/ROADMAP.md`.
- Secondary development reference находится в `docs/development/roadmap.md`.
