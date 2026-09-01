# Автономная инженерная работа

## Назначение

Этот документ задаёт portable workflow для длительных задач, выполняемых Codex, OpenCode или совместимым coding-agent harness. Он дополняет `AGENTS.md`, Definition of Done и quality gates, но не меняет product scope, roadmap, архитектурные решения или статус этапов.

Протокол применяется к существенным задачам, которые занимают несколько этапов работы, могут пережить context compaction или требуют независимого review. Маленькие read-only проверки и узкие правки не обязаны создавать persistent state.

## Источники истины

Полная иерархия определена в `../../AGENTS.md`. Для автономной работы особенно важны:

1. явная задача пользователя;
2. accepted ADR в `../decisions/`;
3. `../roadmap/roadmap.md` для статуса и разрешённого следующего шага;
4. `../product/product-baseline.md` для границ продукта;
5. `../architecture/architecture-baseline.md` для архитектуры и backend stack;
6. `../architecture/stage-6/openapi-draft.yaml` для текущего public contract intent с явным статусом draft / `not_ready`;
7. активные инженерные правила в этой директории и layering rules.

У проекта нет принятой durable database schema и migrations. Process-local stores не дают разрешения проектировать persistent storage. Расхождение OpenAPI draft с runtime-кодом или тестами нужно зафиксировать; если исправление меняет публичный контракт, оно требует явного scope или эскалации.

## Политика автономности

Агент самостоятельно:

- читает и ищет файлы, документацию и историю Git;
- создаёт и изменяет код, тесты и supporting files в границах задачи;
- выбирает обоснованный implementation approach;
- запускает локальные build, test, lint, formatting и contract checks;
- исправляет вызванные своими изменениями failures;
- пробует разные гипотезы восстановления;
- поддерживает `.agent/` для существенной задачи;
- использует read-only explorer/reviewer, если harness поддерживает child agents.

Человек нужен только когда решение материально затрагивает:

- product behavior или scope за пределами задачи;
- public API или внешний contract с незапланированным breaking change;
- accepted architecture boundary, stack или security model;
- destructive или irreversible database/data operation;
- production deployment, production mutation или privileged access;
- недоступные secrets/credentials;
- значимые внешние расходы;
- противоречивые требования с разными бизнес-последствиями, которые нельзя разрешить по репозиторию;
- blocker, сохранившийся после нескольких действительно разных попыток.

По умолчанию запрещены force push, history rewrite, удаление production data, изменение production systems, раскрытие secrets, отключение тестов ради зелёного результата, ослабление security controls, скрытое расширение scope и ложное завершение задачи.

## Жизненный цикл задачи

```text
goal → plan → implementation → verification → review → repair → final verification → completion
```

1. Зафиксировать goal, acceptance criteria, constraints, out-of-scope, Definition of Done и task-specific escalation triggers.
2. Составить иерархический plan с dependencies и одним текущим focus.
3. Реализовывать небольшими проверяемыми шагами, не начиная future roadmap work.
4. После локального изменения запускать минимальную релевантную проверку.
5. После завершения реализации запускать более широкий применимый gate.
6. Передать исходную задачу, acceptance criteria, rules и итоговый diff независимому read-only reviewer из свежего контекста.
7. Вернуть findings в implementation loop, исправить и повторить релевантные проверки.
8. Выполнить final verification, просмотреть diff и проверить каждый acceptance criterion.
9. Завершить только при выполненной Definition of Done; известные ограничения сообщить явно.

## Persistent state

Операционные файлы находятся в `../../.agent/`. Они должны содержать текущее состояние задачи, а не conversation transcript.

- `task.md` — постановка и проверяемые границы;
- `plan.md` — milestones, tasks, dependencies и statuses;
- `progress.md` — текущий focus, выполненное, blocker и следующий шаг;
- `decisions.md` — только решения с долгосрочной ценностью;
- `verification.md` — required checks, последние результаты и нерешённые failures.

Обновлять persistent state нужно после milestone, перед длительной операцией, при blocker и перед handoff. На отдельной branch/worktree хранится одно активное состояние. Параллельные задачи должны использовать отдельные worktrees, чтобы не конкурировать за один набор файлов.

## Recovery loop

После обычного failure агент не эскалирует сразу:

1. наблюдает failure и сохраняет точную диагностику;
2. изучает логи, diff и ближайший relevant code/config;
3. формулирует вероятную root cause;
4. применяет targeted fix;
5. повторяет минимальную релевантную проверку;
6. после локального успеха повторяет более широкий gate;
7. если failure остался, проверяет действительно другую гипотезу.

Одинаковые команды без нового evidence нельзя повторять бесконечно. Persistent blocker кратко записывается в `progress.md` и `verification.md`. Эскалация допустима после нескольких materially different attempts или сразу при совпадении с обязательной категорией эскалации.

## Проверка и Definition of Done

Точные правила находятся в `definition-of-done.md` и `quality-gates.md`. Унифицированный entry point:

```bash
./scripts/verify.sh docs
./scripts/verify.sh core
./scripts/verify.sh all
```

Выбирается минимальный достаточный profile. `core` не заменяет task-specific integration или contract tests. `all` предназначен только для repository-wide изменений, затрагивающих experimental tools. Ни один profile не выполняет REAL provider calls.

Если formatter, schema validator или единый CI gate отсутствует, агент не имитирует его результат. Он запускает доступные проверки и явно фиксирует gap.

## Независимое review

Reviewer должен быть read-only и по возможности работать из свежего контекста. Он получает оригинальную задачу, acceptance criteria, out-of-scope, применимые ADR/baseline/rules, список проверок и final diff.

Reviewer проверяет functional correctness, незакрытые критерии, edge cases, regression risk, architecture/layering, security, concurrency/state, error handling, лишнюю сложность, тесты и документацию. Findings оформляются по severity из `quality-gates.md`. Обычные findings не эскалируются пользователю: implementer исправляет их и повторяет review/verification.

## Git и безопасность рабочей копии

- Перед изменениями проверить branch, status, diff и недавнюю историю.
- Считать существующие uncommitted changes пользовательскими и не изменять их без прямой связи с задачей.
- Для существенной работы предпочитать отдельную `codex/` branch; для параллельной — отдельный worktree.
- Делать маленькие meaningful commits только если задача разрешает commits.
- Не выполнять push, merge в protected branch, force push или history rewrite без явного разрешения.
- Перед завершением отдельно просмотреть staged и unstaged diff, generated/local files и возможные secrets.

## Harness adapters и permissions

Shared project layer — `AGENTS.md`, `docs/development/`, `.agent/` и `scripts/verify.sh`. Он не зависит от vendor.

Для Codex repository-wide правила должны оставаться в `AGENTS.md`. Project `.codex/config.toml` нужен только для capability, которую нельзя выразить shared rules; он не должен ослаблять managed sandbox или user-level policy. Безопасный baseline: repository-local writes, локальные проверки без confirmation, network/external writes через approval, production и destructive actions закрыты.

Для OpenCode `AGENTS.md` остаётся canonical governance. Если добавляется project configuration, primary agent получает repository-local read/write и локальные build/test commands; reviewer и explorer остаются read-only. `git push`, destructive filesystem operations, secrets/env files, external directories и production tools должны быть ask/deny. Локальный ignored `opencode.json` не является portable governance и не должен становиться source of truth.

Vendor-specific model, MCP, sandbox и permission settings остаются adapter layer. Они не должны дублировать product, architecture, Definition of Done или escalation policy.
