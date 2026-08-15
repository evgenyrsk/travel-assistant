# Travel Assistant

Travel Assistant — приватный проект для разработки AI-помощника по планированию путешествий.

README является входной картой проекта: он помогает понять назначение репозитория и найти ключевые документы. README не является подробным roadmap, продуктовой или архитектурной спецификацией, трекером задач либо активным списком реализации.

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
├── app/                 # Локальная demo shell MVP и отдельная диагностическая страница API
├── services/            # Backend-модули; services/backend содержит основу на Kotlin + Ktor
├── docs/                # Продуктовая, архитектурная и инженерная документация, а также roadmap
├── tests/               # Будущие тесты, fixtures и E2E-сценарии
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
- `docs/roadmap/roadmap.md` — основной roadmap и единственный источник текущих статусов этапов, перенесенных пунктов и следующего разрешенного шага.
- `docs/ROADMAP.md` — краткая навигационная карта этапов без текущих статусов; не конкурирует с основным roadmap.
- `docs/guides/documentation-style-guide.md` — единые правила языка, структуры, терминологии, ограничений и безопасного рефакторинга документации.
- `docs/guides/local-mvp-demo.md` — воспроизводимый локальный запуск demo shell и backend в явных `FAKE`/`REAL` профилях.
- `docs/guides/backend-operations-runbook.md` — deployment-neutral запуск Java 17 backend, локальная проверка логов и практическая интеграция collector, log storage, Prometheus, dashboards и alerts.
- `docs/guides/corporate-transfer-readiness.md` — checklist переноса репозитория и выбора внутреннего semantic deployment без внешних model calls.
- `tools/semantic-evaluation/README.md` — rights-safe harness для агрегированной проверки качества semantic `GLAMPING` без хранения provider content в репозитории.
- `tools/tbank-hotels-mcp/README.md` — API-driven MCP для поиска, тарифов, авторизованных заказов, бронирования и оплаты в разделе «Отели» Т-Банка; браузер не требуется.
- `docs/reviews/README.md` — индекс отчетов о проверках и правила чтения исторических и текущих отчетов.
- `docs/reviews/*.md` — исторический журнал проверок и чисток; читать через `docs/reviews/README.md`.
- `docs/decisions/README.md` — индекс принятых ADR и правила ведения архитектурных решений.
- `docs/development/README.md` — индекс активных инженерных правил для реализации, тестирования, документации и проверок качества.
- `docs/development/coding-standards.md` — общие правила написания кода.
- `docs/development/kotlin-backend-style-guide.md` — Kotlin + Ktor backend style rules.
- `docs/development/testing-strategy.md` — testing strategy и coverage expectations.
- `docs/development/documentation-guidelines.md` — documentation source-of-truth, navigation и language policy rules.
- `docs/development/definition-of-done.md` — task completion criteria.
- `docs/development/quality-gates.md` — supported validation gates и reporting expectations.
- `docs/development/roadmap.md` — компактный справочник по будущим направлениям разработки; не источник статусов и не активный список задач.
- `docs/development/implementation-strategy.md` — справочная стратегия реализации и правила декомпозиции задач; не основной roadmap и не активный список реализации.
- `services/backend/README.md` — инструкция по запуску backend на Kotlin + Ktor и описание текущих локальных endpoints.
- `app/README.md` — запуск локальной demo shell и отдельной диагностической страницы.
- `scripts/local-demo.mjs` — единый launcher локальной демонстрации MVP.
- `docs/prompts/README.md` — индекс Codex prompt templates.
- `docs/prompts/codex-task-template.md` — практический шаблон Codex для задач реализации и сопровождения.
- `docs/prompts/codex-review-template.md` — практический шаблон Codex для задач только на проверку.
- `docs/prompts/` — переиспользуемые правила и шаблоны Codex/opencode задач.
- `.github/` — GitHub templates для постановки задач и описания pull requests.

## Начало работы

Актуальный статус, завершенные артефакты, открытые решения, готовность generated clients/OpenAPI и следующий разрешенный шаг фиксируются только в основном roadmap: `docs/roadmap/roadmap.md`.

## Рабочий процесс Codex

- Обязательные правила репозитория для Codex-агентов находятся в `AGENTS.md`.
- Активные инженерные правила находятся в `docs/development/README.md`.
- Правила слоев backend находятся в `docs/architecture/backend-layering-rules.md`.
- Переиспользуемый шаблон постановки задач находится в `docs/prompts/codex-task-template.md`; устаревший компактный шаблон сохранен только как совместимая переадресация.
- Шаблон проверки без изменений кода находится в `docs/prompts/codex-review-template.md`; устаревший шаблон сохранен только как совместимая переадресация.
- Основные правила работы с roadmap, границами задач, ADR, проверками, языком и отчетностью находятся в `AGENTS.md`; подробные правила реализации живут в `docs/development/**`.
- GitHub PR checklist находится в `.github/pull_request_template.md`.
- Основной roadmap со статусами этапов находится в `docs/roadmap/roadmap.md`.
- Верхнеуровневый список этапов находится в `docs/ROADMAP.md`.
- Дополнительный справочник по разработке находится в `docs/development/roadmap.md`.
