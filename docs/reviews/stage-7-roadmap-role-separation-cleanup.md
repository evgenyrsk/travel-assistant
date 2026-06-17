# Stage 7 — Roadmap Role Separation Cleanup

## Цель

Развести роли `docs/ROADMAP.md` и `docs/roadmap/roadmap.md`, чтобы overview-документ не дублировал mutable roadmap/status information.

После cleanup:

- `docs/roadmap/roadmap.md` остается единственным source of truth по статусам, progression, carryover, ограничениям и следующему разрешенному шагу.
- `docs/ROADMAP.md` остается краткой картой назначения этапов без самостоятельной status matrix.

## Что изменено

- Из `docs/ROADMAP.md` удален раздел с таблицей текущего состояния.
- В `docs/ROADMAP.md` добавлен раздел `Как читать roadmap`, который направляет за актуальным состоянием и next-step information в primary roadmap.
- В `docs/roadmap/roadmap.md` уточнено правило: `docs/ROADMAP.md` не должен включать current state matrix, last completed step, next planned step или implementation readiness.
- `README.md` теперь описывает `docs/ROADMAP.md` как краткую карту этапов без текущих статусов и не хранит собственный status snapshot.
- `docs/reviews/README.md` получил ссылку на этот cleanup report.

## Что намеренно не менялось

- Roadmap sequencing.
- Текущий статус проекта.
- Последний завершенный этап.
- Следующий планируемый шаг.
- MVP scope.
- Architecture decisions.
- ADR status.
- OpenAPI/API contracts.
- Backend/frontend code или implementation state.

## Проверки

Ожидаемые проверки для этой cleanup-задачи:

- `rg -n "Последний завершенный|Следующий планируемый|current status|Текущий статус|Code/API/DB/UI|generated-client|readiness|Stage 7\\.36|Stage 7\\.37" docs/ROADMAP.md`
- `rg -n "Stage 7\\.36|Stage 7\\.37|source of truth|docs/ROADMAP.md" docs/roadmap/roadmap.md`
- `rg -n "docs/ROADMAP.md|docs/roadmap/roadmap.md" README.md AGENTS.md docs`
- `git diff --check`

## Итог

Cleanup сохраняет оба roadmap-документа, но убирает риск конкурирующих status updates. `docs/ROADMAP.md` теперь можно читать как стабильную stage-purpose map, а любые изменения текущего состояния должны проходить через `docs/roadmap/roadmap.md`.
