# Stage 7 — Documentation Dedup / Status Sync Cleanup

## Цель

Выполнить conservative documentation cleanup: убрать устаревшие локальные status snapshots из активных baseline/reference документов, сократить дублирующий development/prompt слой и сохранить historical audit trail.

Задача не меняет roadmap order, current/next stage, MVP scope, architecture decisions, ADR status, OpenAPI/API contracts, backend/frontend code, provider integration, DB/storage или production implementation.

## Что было изменено

- `docs/product/product-baseline.md` больше не фиксирует локальный Stage 7.0f status snapshot; текущий Stage 7 статус отсылается к `docs/roadmap/roadmap.md`.
- `docs/architecture/architecture-baseline.md` больше не содержит устаревшие Stage 7.0f cleanup markers, `Stage 7.2` activation wording или pending broad cleanup wording.
- `docs/development/roadmap.md` получил milestone vocabulary из `docs/development/milestones.md` и остается future/reference material.
- `docs/development/implementation-strategy.md` больше не фиксирует локальный Stage 7.0f snapshot.
- `docs/prompts/codex-task-template.md` и `docs/prompts/codex-review-template.md` получили полезные task/review context fields из legacy templates.
- `docs/prompts/task-template.md` и `docs/prompts/review-template.md` сокращены до compatibility redirects для старых ссылок.
- `docs/development/milestones.md` помечен как deprecated compatibility reference, потому что historical artifacts продолжают ссылаться на этот файл.
- `docs/PROJECT_BRIEF.md` и `docs/ARCHITECTURE.md` получили явные role notes.
- README, roadmap/navigation, development, prompt и reviews indexes обновлены под новую структуру.

## Почему файлы не удалялись

План допускал conservative fallback, если обнаружатся ссылки на legacy templates или milestones. Проверка нашла множество ссылок на `docs/development/milestones.md`, `docs/prompts/task-template.md` и `docs/prompts/review-template.md` внутри historical review/product artifacts.

Чтобы не переписывать audit trail задним числом, файлы сохранены как deprecated compatibility artifacts. Новые задачи должны использовать:

- `docs/development/roadmap.md` вместо `docs/development/milestones.md`;
- `docs/prompts/codex-task-template.md` вместо `docs/prompts/task-template.md`;
- `docs/prompts/codex-review-template.md` вместо `docs/prompts/review-template.md`.

## Scope control

Не выполнялись:

- удаление `docs/product/stage-*`, `docs/architecture/stage-*` или `docs/reviews/**`;
- удаление `docs/ROADMAP.md`, `docs/PROJECT_BRIEF.md` или `docs/ARCHITECTURE.md`;
- изменение roadmap sequencing, MVP scope или architecture decisions;
- превращение recommendations из старых review reports в active backlog;
- backend/frontend/runtime changes.

## Проверки

Ожидаемые проверки для этой cleanup-задачи:

- `rg -n "docs/development/milestones.md|development/milestones.md|docs/prompts/task-template.md|prompts/task-template.md|docs/prompts/review-template.md|prompts/review-template.md" README.md docs .github AGENTS.md`
- `rg -n "Stage 7\\.0f|Stage 7\\.2 не активирован|На момент Stage 7\\.0f|Broader documentation cleanup остается pending" README.md docs/product docs/architecture docs/development docs/prompts docs/ROADMAP.md`
- `git diff --check`

Первый check может возвращать intentional historical/compatibility references. Активные navigation references должны указывать на canonical replacement docs.

## Итог

Cleanup уменьшает риск conflicting source-of-truth wording без потери traceability. Active docs теперь отсылают к primary roadmap для текущего статуса, development milestone vocabulary объединен в один reference layer, а legacy prompt templates не конкурируют с Codex templates.
