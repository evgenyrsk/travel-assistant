# Development Governance

This directory contains active engineering rules for implementation, review, testing, documentation, and repository maintenance work.

These documents do not define roadmap status or product scope. Use `../roadmap/roadmap.md` for roadmap status, `../product/product-baseline.md` for MVP scope, and `../architecture/architecture-baseline.md` for architecture baseline and backend stack authority.

## Active Rules

- [Coding standards](coding-standards.md) - general code writing rules independent of a specific technology.
- [Kotlin backend style guide](kotlin-backend-style-guide.md) - Kotlin + Ktor backend implementation style.
- [Testing strategy](testing-strategy.md) - test expectations and coverage rules.
- [Documentation guidelines](documentation-guidelines.md) - documentation source-of-truth, navigation, and language rules.
- [Definition of Done](definition-of-done.md) - completion criteria for repository tasks.
- [Quality gates](quality-gates.md) - validation commands, diff checks, and risk reporting.
- [Backend layering rules](../architecture/backend-layering-rules.md) - allowed dependencies and layer boundaries.
- [Codex task template](../prompts/codex-task-template.md) - implementation-task prompt template.
- [Codex review template](../prompts/codex-review-template.md) - review-only prompt template.

## Reference-Only Development Context

- [Development roadmap reference](roadmap.md) - future/reference vocabulary, not active backlog.
- [Development milestones reference](milestones.md) - future milestone vocabulary, not active backlog.
- [Implementation strategy reference](implementation-strategy.md) - future implementation approach, not task activation.

If an active engineering rule conflicts with `AGENTS.md`, the primary roadmap, an accepted ADR, or the architecture/product baselines, follow the higher-priority source and report the conflict.
