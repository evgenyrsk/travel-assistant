# Persistent task state

Эта директория хранит краткое операционное состояние одной существенной задачи на текущей branch/worktree. Она помогает продолжить работу после context compaction, restart или handoff между Codex, OpenCode и совместимыми harness.

Файлы не являются roadmap, backlog, ADR или историей диалога:

- `task.md` — цель, acceptance criteria, constraints, out-of-scope, Definition of Done и escalation triggers;
- `plan.md` — milestones, dependencies и statuses `pending`, `in progress`, `blocked`, `completed`;
- `progress.md` — текущий focus, выполненное, blocker и следующий шаг;
- `decisions.md` — только значимые решения, которые важно сохранить;
- `verification.md` — required checks, latest results и unresolved failures.

Правила использования:

1. Инициализировать файлы перед implementation существенной задачи.
2. Обновлять их после milestone, при blocker, перед длительной операцией и перед handoff.
3. Хранить факты и ссылки на evidence, а не transcript или длинное reasoning.
4. Для параллельных задач использовать отдельные branches/worktrees.
5. После завершения оставить concise completed state; следующая существенная задача заменяет содержимое в своей branch.

Полный workflow описан в `../docs/development/autonomous-engineering.md`.
