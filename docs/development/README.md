# Development Governance

Эта директория содержит активные engineering rules для implementation, review, testing, documentation и maintenance задач в репозитории.

Эти документы не задают roadmap status или product scope. Для roadmap status используй `../roadmap/roadmap.md`, для MVP scope — `../product/product-baseline.md`, для architecture baseline и backend stack authority — `../architecture/architecture-baseline.md`.

## Active Rules

- [Coding standards](coding-standards.md) - общие правила написания кода, независимые от конкретной технологии.
- [Kotlin backend style guide](kotlin-backend-style-guide.md) - стиль Kotlin + Ktor backend implementation.
- [Testing strategy](testing-strategy.md) - ожидания по тестам и правила coverage.
- [Documentation guidelines](documentation-guidelines.md) - правила source-of-truth, navigation и language policy для документации.
- [Definition of Done](definition-of-done.md) - критерии завершения задач в репозитории.
- [Quality gates](quality-gates.md) - validation commands, diff checks и risk reporting.
- [Backend layering rules](../architecture/backend-layering-rules.md) - допустимые зависимости и границы слоев.
- [Codex task template](../prompts/codex-task-template.md) - prompt template для implementation-задач.
- [Codex review template](../prompts/codex-review-template.md) - prompt template для review-only задач.

## Reference-Only Development Context

- [Development roadmap reference](roadmap.md) - future/reference vocabulary and milestone vocabulary, не active backlog.
- [Implementation strategy reference](implementation-strategy.md) - future implementation approach, не активация задачи.

Если active engineering rule конфликтует с `AGENTS.md`, primary roadmap, accepted ADR или architecture/product baselines, следуй источнику с более высоким приоритетом и сообщи о конфликте.
