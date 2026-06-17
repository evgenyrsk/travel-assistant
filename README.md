# Travel Assistant

Travel Assistant — приватный проект для разработки AI-помощника по планированию путешествий.

README является входной картой проекта: он помогает понять назначение репозитория и найти ключевые документы. README не является подробным roadmap, продуктовой спецификацией, архитектурной спецификацией, task tracker или implementation backlog.

## Цели

- Помогать пользователям планировать поездки с учетом направлений, дат, бюджета и предпочтений.
- Организовывать маршруты, места, бронирования и заметки о поездке.
- Сохранять структуру проекта гибкой, пока продуктовые решения и архитектурные контракты уточняются по roadmap.

## Структура репозитория

```text
.
├── AGENTS.md            # Правила работы Codex/AI-агентов в репозитории
├── README.md            # Входная точка и навигация
├── .env.example         # Пример локальных переменных окружения
├── .gitignore           # Правила исключения локальных и сгенерированных файлов
├── .github/             # Шаблоны GitHub issues и pull requests
├── app/                 # Будущий слой frontend/application
├── services/            # Модули backend/services; services/backend содержит базовую Kotlin + Ktor backend-основу Stage 7
├── docs/                # Продуктовая, roadmap-, архитектурная и development-документация
├── tests/               # Будущие тесты, fixtures и e2e-сценарии
└── scripts/             # Будущие скрипты разработки и автоматизации
```

## Карта документации

- `README.md` — главная входная точка проекта и краткая навигация.
- `AGENTS.md` — обязательные правила для Codex/AI-агентов внутри этого репозитория.
- `docs/product/README.md` — индекс продуктовой документации и метки ролей; только навигация.
- `docs/product/product-baseline.md` — источник истины по продукту и актуальное компактное продуктовое состояние после Stage 0-5.
- `docs/product/stage-0/` — Этап 0: продуктовая рамка, первичные сценарии, границы MVP, допущения и открытые вопросы.
- `docs/product/stage-1/` — Этап 1: бизнес-сценарии, требования, user journeys, assumptions и consistency review.
- `docs/product/stage-2/` — Этап 2: use cases, edge cases, assistant behaviour rules, combined search levels и data requirements.
- `docs/product/stage-3/` — Этап 3: MVP UX / Navigation, screen map, navigation model, UX flows, required fields и acceptance criteria.
- `docs/product/stage-4/` — Этап 4: Visual Design & UX System, visual direction, design system foundations, component inventory, screen specifications и interaction patterns.
- `docs/PROJECT_BRIEF.md` — продуктовый контекст, пользователи, ключевые сценарии и открытые продуктовые вопросы.
- `docs/ARCHITECTURE.md` — предварительные архитектурные ориентиры; текущий backend stack подтвержден как Kotlin + Ktor в architecture baseline.
- `docs/architecture/README.md` — индекс архитектурной документации и метки ролей; только навигация.
- `docs/architecture/architecture-baseline.md` — источник истины по архитектуре, authority по backend stack и актуальное компактное архитектурное состояние после Stage 5.
- `docs/roadmap/roadmap.md` — primary roadmap и source of truth по статусам этапов, progression, carryover и следующему разрешенному шагу.
- `docs/ROADMAP.md` — краткий навигационный обзор этапов, не конкурирующий roadmap и не источник текущих статусов.
- `docs/guides/documentation-style-guide.md` — единые правила языка, структуры, терминологии, guardrails и безопасного рефакторинга документации.
- `docs/reviews/README.md` — индекс review/audit artifacts и правила чтения исторических и текущих cleanup reports.
- `docs/reviews/*.md` — audit trail, cleanup reports и historical reviews; читать через `docs/reviews/README.md`.
- `docs/decisions/README.md` — правила ведения ADR; ADR пока не созданы.
- `docs/development/README.md` — индекс активных engineering rules для implementation, testing, documentation и quality work.
- `docs/development/coding-standards.md` — общие правила написания кода.
- `docs/development/kotlin-backend-style-guide.md` — Kotlin + Ktor backend style rules.
- `docs/development/testing-strategy.md` — testing strategy и coverage expectations.
- `docs/development/documentation-guidelines.md` — documentation source-of-truth, navigation и language policy rules.
- `docs/development/definition-of-done.md` — task completion criteria.
- `docs/development/quality-gates.md` — supported validation gates и reporting expectations.
- `docs/development/roadmap.md` — компактный development reference; future/reference material, не источник roadmap-статусов и не active backlog.
- `docs/development/milestones.md` — компактный словарь milestones; future/reference material, не источник статусов и не active backlog.
- `docs/development/implementation-strategy.md` — будущая стратегия реализации и правила декомпозиции задач; не primary roadmap и не active implementation backlog.
- `services/backend/README.md` — инструкция запуска Stage 7 Kotlin + Ktor backend foundation и текущих локальных endpoints.
- `docs/prompts/README.md` — индекс Codex prompt templates.
- `docs/prompts/codex-task-template.md` — практический шаблон Codex для implementation/maintenance задач.
- `docs/prompts/codex-review-template.md` — практический шаблон Codex для review-only задач.
- `docs/prompts/` — переиспользуемые правила и шаблоны Codex/opencode задач.
- `.github/` — GitHub templates для постановки задач и описания pull requests.

## Начало работы

Актуальный статус, завершенные артефакты, открытые решения и следующий шаг фиксируются только в primary roadmap: `docs/roadmap/roadmap.md`.

Текущий верхнеуровневый статус: Stage 0-6 завершены, Stage 7 находится в статусе `In progress / awaiting explicit next task`, Stage 8-10 остаются `Planned`. Подробный Stage 7 checklist, статус generated-client/OpenAPI readiness, ограничения и следующий разрешенный шаг находятся в `docs/roadmap/roadmap.md`.

## Рабочий процесс Codex

- Обязательные правила репозитория для Codex-агентов находятся в `AGENTS.md`.
- Active engineering rules находятся в `docs/development/README.md`.
- Backend layering rules находятся в `docs/architecture/backend-layering-rules.md`.
- Переиспользуемый шаблон постановки задач находится в `docs/prompts/codex-task-template.md`; legacy compact template сохранен в `docs/prompts/task-template.md`.
- Шаблон read-only review находится в `docs/prompts/codex-review-template.md`; legacy review template сохранен в `docs/prompts/review-template.md`.
- Canonical entry-point правила roadmap, scope, ADR, validation, language policy и отчетности находятся в `AGENTS.md`; подробные implementation rules живут в `docs/development/**`.
- GitHub PR checklist находится в `.github/pull_request_template.md`.
- Primary roadmap со статусами этапов находится в `docs/roadmap/roadmap.md`.
- Верхнеуровневый список этапов находится в `docs/ROADMAP.md`.
- Secondary development reference находится в `docs/development/roadmap.md`.
