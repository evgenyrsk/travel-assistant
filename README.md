# Travel Assistant

Travel Assistant — приватный проект для разработки AI-powered помощника по планированию путешествий.

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
├── docs/                # Продуктовая, архитектурная и development-документация
├── tests/               # Будущие тесты, fixtures и e2e-сценарии
└── scripts/             # Будущие скрипты разработки и автоматизации
```

## Карта документации

- `README.md` — главная входная точка проекта и краткая навигация.
- `AGENTS.md` — обязательные правила для Codex/AI-агентов внутри этого репозитория.
- `docs/PROJECT_BRIEF.md` — продуктовый контекст, пользователи, ключевые сценарии и открытые продуктовые вопросы.
- `docs/ARCHITECTURE.md` — целевая архитектура, границы слоев и технические принципы.
- `docs/ROADMAP.md` — верхнеуровневый product roadmap.
- `docs/development/roadmap.md` — детальный roadmap разработки и порядок этапов.
- `docs/development/milestones.md` — milestones, контрольные точки, scope и acceptance criteria.
- `docs/development/implementation-strategy.md` — практическая стратегия реализации и правила декомпозиции задач.
- `docs/prompts/` — переиспользуемые правила и шаблоны Codex/opencode задач.
- `.github/` — GitHub templates для постановки задач и описания pull requests.

## Начало работы

Текущий этап — подготовка структуры, документации и процесса. Целевой стек и архитектурные границы фиксируются в `AGENTS.md`, `docs/ARCHITECTURE.md` и roadmap-документах; app и service scaffolding добавляются только на соответствующих этапах `docs/development/roadmap.md`.

## Рабочий процесс Codex

- Обязательные правила репозитория для Codex-агентов находятся в `AGENTS.md`.
- Переиспользуемый шаблон постановки задач находится в `docs/prompts/task-template.md`.
- Общие правила roadmap, scope, ADR и отчетности находятся в `docs/prompts/codex-rules.md`.
- Шаблон review-задач с проверками roadmap drift, scope creep и future-stage implementation находится в `docs/prompts/review-template.md`.
- GitHub PR checklist находится в `.github/pull_request_template.md`.
- Верхнеуровневый product roadmap находится в `docs/ROADMAP.md`.
- Детальный roadmap разработки находится в `docs/development/roadmap.md`.
