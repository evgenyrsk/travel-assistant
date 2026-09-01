# Codex Task Template

Use this template for a small implementation, documentation, or repository-maintenance task.

For a substantial or long-running task, use the same task fields and also initialize the persistent execution state described in `.agent/README.md` before implementation.

## Required Context to Read

- `AGENTS.md`
- `README.md`
- `docs/roadmap/roadmap.md`
- `docs/ROADMAP.md`
- `docs/development/README.md`
- `docs/development/coding-standards.md`
- `docs/development/testing-strategy.md`
- `docs/development/documentation-guidelines.md`
- `docs/development/definition-of-done.md`
- `docs/development/quality-gates.md`
- `docs/development/autonomous-engineering.md`, if the task is substantial or long-running
- `.agent/README.md` and the current `.agent/task.md`, if persistent task state is used
- `docs/architecture/architecture-baseline.md`, if architecture or backend scope is affected
- `docs/architecture/backend-layering-rules.md`, if backend code is affected
- `docs/development/kotlin-backend-style-guide.md`, if Kotlin backend code is affected
- `docs/product/product-baseline.md`, if product/MVP scope is affected
- Relevant ADR files in `docs/decisions/`, if any exist and apply

## Task

Describe one concrete task.

```text
Goal:
Why this is in scope:
Expected outcome:
```

## Acceptance Criteria

- [ ]
- [ ]

## Roadmap Context

- Current roadmap stage:
- Why the task belongs to this stage:
- Next/future stage that must not be started:
- Future/reference documents that must not be treated as active backlog:

## Allowed Scope

- Files or directories expected to change:
- Behavior or documentation expected to change:
- Existing docs/rules to reuse or link:

## Explicitly Forbidden Changes

- Do not change:
- Do not create:
- Do not start future roadmap work:
- Do not modify unrelated files:

## Rule References

- Repository governance: `AGENTS.md`
- Coding standards: `docs/development/coding-standards.md`
- Kotlin backend style: `docs/development/kotlin-backend-style-guide.md`
- Backend layering: `docs/architecture/backend-layering-rules.md`
- Testing strategy: `docs/development/testing-strategy.md`
- Documentation guidelines: `docs/development/documentation-guidelines.md`
- Definition of Done: `docs/development/definition-of-done.md`
- Quality gates: `docs/development/quality-gates.md`

## Testing Expectations

- Tests to add or update:
- Existing tests to run:
- If tests are not required, explain why:

## Documentation Expectations

- Docs to update:
- Navigation/index files to update:
- Docs that must not be rewritten:

## Validation Expectations

- Required commands:
- Manual link/navigation checks:
- Diff hygiene checks:

## Long-Running State, If Applicable

- `.agent/task.md` initialized with goal, acceptance criteria, constraints, out-of-scope items, Definition of Done, and task-specific escalation triggers.
- `.agent/plan.md` tracks milestones, dependencies, and statuses.
- `.agent/progress.md` contains only the current resumable state and next action.
- Meaningful decisions and verification evidence are recorded in `.agent/decisions.md` and `.agent/verification.md`.
- Independent reviewer receives the original task, acceptance criteria, applicable architecture/governance context, and final diff from a fresh context when supported.

## Final Report Format

Return a concise report with:

1. Created files
2. Changed files
3. Brief summary
4. Checks
5. Decisions made
6. Open questions
7. Scope control
8. Recommendations not implemented
9. Known risks
