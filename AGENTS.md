# AGENTS.md

Travel Assistant is an AI assistant for planning trips. The current MVP direction is hotel-only: users describe travel needs in natural language, the assistant clarifies missing parameters, provider-backed hotel facts are retrieved through abstractions, and recommendations explain why an option fits.

This file is the mandatory entry point for Codex/AI agents in this repository. It defines repository-level guardrails and points to detailed development rules. It is not a full style guide, roadmap, architecture spec, or implementation backlog.

## Required Context Before Changes

Before making changes, read the files relevant to the task:

- `README.md`;
- `docs/roadmap/roadmap.md`;
- `docs/ROADMAP.md`;
- `docs/product/product-baseline.md`, if product or MVP scope is affected;
- `docs/architecture/architecture-baseline.md`, if architecture or backend scope is affected;
- `docs/architecture/backend-layering-rules.md`, if backend code is affected;
- `docs/development/README.md` and the relevant rules under `docs/development/`;
- `docs/prompts/README.md` and the relevant Codex task/review template under `docs/prompts/`;
- `docs/decisions/`, only if the directory contains ADRs relevant to the task;
- `docs/guides/documentation-style-guide.md`, if documentation structure or style is affected;
- `docs/reviews/README.md`, if review/audit history is relevant.

Use actual repository files as the source of truth. Do not import structure, decisions, stage directories, or conventions from other projects.

## Source-of-Truth Order

Use current documents in this order:

1. Explicit user request for the current task.
2. Accepted ADRs in `docs/decisions/`, if they exist and apply.
3. Primary roadmap: `docs/roadmap/roadmap.md`.
4. Product baseline: `docs/product/product-baseline.md`.
5. Architecture baseline: `docs/architecture/architecture-baseline.md`.
6. Active engineering rules: `docs/development/`.
7. Backend layering rules: `docs/architecture/backend-layering-rules.md`.
8. Prompt templates: `docs/prompts/`.
9. Historical stage artifacts and review reports for traceability only.

If documents conflict, follow the higher-priority source, report the conflict, and do not perform work that may violate roadmap, ADR, product scope, or architecture boundaries.

## Roadmap and Scope Control

- Determine the current roadmap stage from `docs/roadmap/roadmap.md` before implementation or status-sensitive documentation work.
- Do not change roadmap order or start a future stage without an explicit roadmap-aligned task.
- Do not treat recommendations, carryover, future/reference documents, ADR candidates, or review findings as active backlog.
- Keep every change small, atomic, and limited to the requested task.
- Do not modify unrelated files.
- Do not add dependencies, tooling, directories, skeletons, provider integrations, database schema, frontend code, or infrastructure unless explicitly requested and roadmap-aligned.

## Architecture Guardrails

- Backend implementation must use Kotlin + Ktor unless a future accepted ADR and roadmap-aligned task explicitly change the stack.
- Java/Spring Boot is not the accepted backend stack.
- If implementation files conflict with the Kotlin + Ktor backend baseline, stop and report the architecture mismatch.
- Keep domain logic independent from Ktor, Next.js, PostgreSQL, Redis, provider SDKs, and concrete LLM providers.
- Keep Ktor routing thin and delegate behavior to application/domain use cases.
- Keep LLM orchestration behind provider-independent contracts such as `LlmClient`.
- Keep travel provider integrations behind provider interfaces; use mock/stub providers until a task explicitly activates real provider work.
- Follow `docs/architecture/backend-layering-rules.md` for backend dependency direction and layer boundaries.

## Development Governance

Detailed rules live in:

- `docs/development/coding-standards.md`;
- `docs/development/kotlin-backend-style-guide.md`;
- `docs/development/testing-strategy.md`;
- `docs/development/documentation-guidelines.md`;
- `docs/development/definition-of-done.md`;
- `docs/development/quality-gates.md`;
- `docs/architecture/backend-layering-rules.md`.

Before implementation work, read the relevant files under `docs/development/` and `docs/architecture/backend-layering-rules.md`. Use `docs/prompts/codex-task-template.md` for implementation/maintenance prompts and `docs/prompts/codex-review-template.md` for review-only prompts.

## Language Policy

- Use English for source code, package names, class names, method names, API contracts, database identifiers, logs, errors, technical comments, ADRs, development standards, engineering governance docs, and Codex prompt templates.
- Product documentation, roadmap, stage reports, review reports, and business-facing documentation may remain in Russian.
- Do not rewrite existing Russian product, roadmap, stage, or historical documents only to normalize language.
- Prefer English for documents that directly constrain implementation.
- Avoid mixing Russian and English inside one technical artifact unless English technical terms improve precision.

## Documentation Rules

- README and index files should stay navigational.
- Do not duplicate long rules across documents; link to the active source instead.
- Do not add links to missing files unless the same task creates those files.
- Do not rewrite historical stage or review artifacts unless the task explicitly asks for that.
- Update roadmap/status files only when project status actually changes.

## Validation

- For documentation-only changes, run `git diff --check`.
- For backend code, build, test, or behavior changes, run the relevant supported Gradle checks from `services/backend`; see `docs/development/quality-gates.md`.
- Manually verify new or changed documentation links.
- Do not claim a check passed unless it actually ran.
- If a check is skipped or fails, report the exact command and reason.

## Final Report

Answer in the language requested by the task. If the task does not specify another format, end with:

1. Created files
2. Changed files
3. Brief summary
4. Checks
5. Decisions made
6. Open questions
7. Scope control
8. Recommendations not implemented

Keep the report concise. Write `None` for empty sections.
