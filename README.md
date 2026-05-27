# Travel Assistant

Travel Assistant — приватный проект для разработки AI-помощника по планированию путешествий.

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
├── services/            # Будущие backend/services модули
├── docs/                # Продуктовая, roadmap, архитектурная и development-документация
├── tests/               # Будущие тесты, fixtures и e2e-сценарии
└── scripts/             # Будущие скрипты разработки и автоматизации
```

## Карта документации

- `README.md` — главная входная точка проекта и краткая навигация.
- `AGENTS.md` — обязательные правила для Codex/AI-агентов внутри этого репозитория.
- `docs/product/README.md` — входная точка в продуктовую документацию.
- `docs/product/stage-0/` — Этап 0: продуктовая рамка, первичные сценарии, границы MVP, допущения и открытые вопросы.
- `docs/product/stage-1/` — Этап 1: бизнес-сценарии, требования, user journeys, assumptions и consistency review.
- `docs/product/stage-2/` — Этап 2: use cases, edge cases, assistant behaviour rules, combined search levels и data requirements.
- `docs/product/stage-3/` — Этап 3: MVP UX / Navigation, screen map, navigation model, UX flows, required fields и acceptance criteria.
- `docs/PROJECT_BRIEF.md` — продуктовый контекст, пользователи, ключевые сценарии и открытые продуктовые вопросы.
- `docs/ARCHITECTURE.md` — целевая архитектура, границы слоев и технические принципы.
- `docs/roadmap/roadmap.md` — главный roadmap проекта: статусы этапов, чеклисты артефактов, open questions, carryover и следующий шаг.
- `docs/ROADMAP.md` — краткий верхнеуровневый список этапов, не источник текущих статусов.
- `docs/decisions/README.md` — правила ведения ADR; ADR пока не созданы.
- `docs/development/roadmap.md` — secondary roadmap разработки; следует primary roadmap и не заменяет статусы этапов.
- `docs/development/milestones.md` — вехи, контрольные точки, границы задачи и критерии приемки; не источник статусов продуктовых этапов.
- `docs/development/implementation-strategy.md` — практическая стратегия реализации и правила декомпозиции задач; не primary roadmap.
- `docs/prompts/` — переиспользуемые правила и шаблоны Codex/opencode задач.
- `.github/` — GitHub templates для постановки задач и описания pull requests.

## Начало работы

Текущий статус: Этап 0, Этап 1 и Этап 2 завершены, Stage 2 Consistency Review и Stage 2 Minor Cleanup выполнены. Stage 3 — MVP UX / Navigation начат; Stage 3.1 Screen Map, Stage 3.2 Required Fields & Acceptance Criteria и Stage 3.3 MVP Search Flow Details выполнены. Следующий шаг — Combined Search UX Decision в рамках Stage 3. Техническая архитектура, визуальный дизайн, каркас приложения и сервисов добавляются только на соответствующих будущих этапах roadmap.

## Рабочий процесс Codex

- Обязательные правила репозитория для Codex-агентов находятся в `AGENTS.md`.
- Переиспользуемый шаблон постановки задач находится в `docs/prompts/task-template.md`.
- Общие правила roadmap, границ задачи, ADR и отчетности находятся в `docs/prompts/codex-rules.md`.
- Шаблон задач на ревью с проверками отклонения от roadmap, разрастания границ задачи и преждевременной реализации будущих этапов находится в `docs/prompts/review-template.md`.
- GitHub PR checklist находится в `.github/pull_request_template.md`.
- Primary roadmap со статусами этапов находится в `docs/roadmap/roadmap.md`.
- Верхнеуровневый список этапов находится в `docs/ROADMAP.md`.
- Secondary roadmap разработки находится в `docs/development/roadmap.md`.
