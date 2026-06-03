# Stage 7 Development Docs Merge/Shortening Cleanup

Дата: 2026-06-03

## Цель cleanup

Сделать `docs/development/**` явно вторичным, справочным и не конкурирующим с primary roadmap, product baseline, architecture baseline или `AGENTS.md`.

Cleanup является узкой documentation stabilization задачей. Он не начинает Stage 7.2, не выполняет broad documentation refactoring, не удаляет и не объединяет документы, не меняет architecture decisions, ADR, OpenAPI, backend/frontend code или MVP scope.

## Что было проблемой

`docs/development/roadmap.md` и `docs/development/milestones.md` выглядели как параллельный roadmap/backlog: содержали длинные последовательности будущих этапов, readiness criteria, artifacts, risks и dependencies. Это могло конкурировать с `docs/roadmap/roadmap.md`.

`docs/development/implementation-strategy.md` был полезнее, но тоже содержал устаревший Stage 7.0f-b status wording и roadmap-like sequence wording.

## Какие docs/development files были проверены

- `docs/development/roadmap.md` — development reference / future guidance; duplicate roadmap content; candidate for shortening.
- `docs/development/milestones.md` — milestone vocabulary / future guidance; duplicate milestone content; candidate for shortening and possible future merge.
- `docs/development/implementation-strategy.md` — implementation strategy / future task guidance; useful guidance preserved, activation wording tightened.

## Где были найдены дубли

- `docs/development/roadmap.md` дублировал stage sequencing, future implementation areas, criteria and exclusions already governed by primary roadmap and baseline docs.
- `docs/development/milestones.md` дублировал milestone-like ordering, artifacts, risks and dependencies that could be read as active backlog.
- `docs/development/implementation-strategy.md` duplicated status/activation wording and contained a roadmap-like implementation sequence.
- `README.md`, `docs/ROADMAP.md` and `docs/roadmap/roadmap.md` still referred to development docs with wording that could sound roadmap-like.

## Что изменено

- `docs/development/roadmap.md` rewritten as compact development reference.
- `docs/development/milestones.md` rewritten as compact milestone vocabulary.
- `docs/development/implementation-strategy.md` kept as practical future implementation guidance with updated Stage 7.0f-d activation wording.
- `README.md`, `docs/ROADMAP.md`, `docs/roadmap/roadmap.md` and `docs/reviews/README.md` minimally updated to reflect Stage 7.0f-d and secondary development-doc roles.

## Что было сокращено

- Длинные roadmap-like stage lists in `docs/development/roadmap.md`.
- Detailed milestone artifacts, criteria, risks and dependencies in `docs/development/milestones.md`.
- Stale Stage 7.0f-b status wording in `docs/development/implementation-strategy.md`.
- Wording that could make development docs look like active roadmap, backlog or implementation plan.

## Что намеренно не менялось

- Файлы не удалялись, не перемещались, не переименовывались и не объединялись.
- Historical stage artifacts не переписывались.
- Roadmap order не менялся.
- Stage 7.2 не активировался.
- Backend/frontend code, OpenAPI, ADR, architecture decisions, provider contracts и MVP scope не менялись.
- Useful technical guidance in `docs/development/implementation-strategy.md` была сохранена.

## Как теперь использовать docs/development/**

- Начинай со `docs/roadmap/roadmap.md`, если нужен текущий status, stage gate или следующий разрешенный шаг.
- Используй `docs/development/roadmap.md` как compact overview of future development areas.
- Используй `docs/development/milestones.md` как vocabulary для возможных future milestone areas.
- Используй `docs/development/implementation-strategy.md` как guidance по декомпозиции future implementation tasks после явной roadmap activation.
- Не используй `docs/development/**` как active backlog, task tracker, source of truth по статусам или разрешение начинать Stage 7.2+.

## Remaining documentation cleanup items

- Style guide stale wording cleanup.
- Product/architecture index role labels.
- Roadmap readability cleanup.
- Broader documentation redundancy cleanup.

Эти items не являются active backlog и требуют отдельных явных roadmap-aligned задач.

## Final verdict

Passed — `docs/development/**` теперь clearly secondary, reference-oriented and non-authoritative. Development docs больше не должны конкурировать с `docs/roadmap/roadmap.md` или выглядеть как active implementation backlog.

Stage 7.2+ не активированы.

## Scope control confirmation

- Cleanup ограничен development docs shortening and minimal navigation/status updates.
- Backend/frontend implementation не создавались.
- Stage 7.2 не начинался.
- Product scope и MVP boundaries не менялись.
- Architecture decisions, ADR, OpenAPI и provider/API contracts не менялись.
- Historical reports не переписывались.
- Unrelated files не изменялись.
