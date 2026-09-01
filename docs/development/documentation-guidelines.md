# Documentation Guidelines

Travel Assistant documentation must stay useful, concise, and aligned with the repository source-of-truth model.

## Source of Truth

Use current documents in this order unless the task states otherwise:

1. Explicit user request for the current task.
2. Accepted ADRs in `docs/decisions/`, if they exist and apply.
3. Primary roadmap: `../roadmap/roadmap.md`.
4. Product baseline: `../product/product-baseline.md`.
5. Architecture baseline: `../architecture/architecture-baseline.md`.
6. Public API contract intent: `../architecture/stage-6/openapi-draft.yaml`, while it remains explicitly marked as draft with generated-client status `not_ready`.
7. Active engineering rules in `../development/`.
8. Backend layering rules: `../architecture/backend-layering-rules.md`.
9. Prompt templates: `../prompts/`.
10. Historical stage artifacts and review reports for traceability only.

Do not turn historical wording, review findings, or future/reference material into active backlog.

У проекта нет принятой durable database schema или migrations. Process-local stores не являются разрешением проектировать persistent storage без отдельной roadmap-aligned задачи и применимого architecture decision.

## Active Docs vs Historical Artifacts

- Active docs define current rules, status, baselines, contracts, or navigation.
- Historical artifacts preserve what was true at the time of a stage, review, or cleanup.
- Do not rewrite historical reports only to normalize language or status wording.
- If a historical document is stale, clarify its role in an index or current source-of-truth document.

## When to Update Documentation

Update documentation when a task changes:

- repository navigation;
- public contracts or API behavior;
- architecture boundaries;
- development workflow, commands, or quality gates;
- testing expectations;
- language policy;
- product scope or roadmap status, but only when the task explicitly changes project status.

## When Not to Update Documentation

Do not update documentation only to:

- restate the same rule in another file;
- rewrite historical Russian product/stage docs into English;
- normalize old reports that are preserved as audit trail;
- add future work as active backlog;
- document unrelated improvements discovered during the task.

## Avoiding Duplication

- Link to the current source instead of copying long rule blocks.
- Keep README and index files navigational.
- Keep `AGENTS.md` as the agent entry point, not a full development manual.
- Put implementation rules in this directory and layering rules in `../architecture/backend-layering-rules.md`.

## Navigation and Index Files

- Update navigation only when files are created, moved, removed, or their role changes.
- Verify that new links point to existing files or files created by the same task.
- Do not duplicate roadmap status details in README or index files.

## Review Reports

- Store review and cleanup reports under `../reviews/`.
- Treat review reports as audit trail unless the task explicitly asks to act on them.
- Do not perform recommendations from a review report in the same task unless they are explicitly in scope.

## Language Policy

- Активная документация и документы-источники истины используют русский текст по умолчанию.
- Английский используется для исходного кода, package/class/method names, API-контрактов, идентификаторов базы данных, логов, ошибок, технических комментариев, названий ADR и других технических идентификаторов.
- Последовательно англоязычные инженерные правила и шаблоны Codex могут оставаться на английском; смешивать обычные русские и английские фразы без необходимости не следует.
- Не переписывай исторические отчеты или существующие русские документы только ради унификации языка.
- Технические термины можно оставлять на английском, когда перевод снижает точность.
