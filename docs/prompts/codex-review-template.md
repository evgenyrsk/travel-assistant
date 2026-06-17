# Codex Review Template

Use this template for review-only tasks. The review is read-only unless the prompt explicitly asks Codex to make fixes.

## Required Context to Read

- `AGENTS.md`
- `README.md`
- `docs/roadmap/roadmap.md`
- `docs/development/README.md`
- `docs/development/coding-standards.md`
- `docs/development/testing-strategy.md`
- `docs/development/documentation-guidelines.md`
- `docs/development/definition-of-done.md`
- `docs/development/quality-gates.md`
- `docs/architecture/architecture-baseline.md`, if architecture or backend scope is affected
- `docs/architecture/backend-layering-rules.md`, if backend code is affected
- `docs/development/kotlin-backend-style-guide.md`, if Kotlin backend code is affected
- `docs/product/product-baseline.md`, if product/MVP scope is affected
- Relevant ADR files in `docs/decisions/`, if any exist and apply

## Review Target

```text
PR/branch/patch:
Task or acceptance criteria:
Expected scope:
Out of scope:
```

## Document Context

- Current roadmap stage:
- Documents whose roles must be checked:
- Source-of-truth documents affected, if any:
- Historical or review artifacts that must not be rewritten:

## Checks

Review for:

- scope drift;
- unrelated changes;
- roadmap/status inconsistency;
- architecture boundary violations;
- backend layering violations;
- coding standards violations;
- Kotlin style violations, when applicable;
- missing or weak test coverage;
- API/contract inconsistency;
- documentation inconsistency;
- ordinary English prose in Russian active documentation without technical need;
- unclear document role or source-of-truth hierarchy;
- checklist/table formatting issues in status-heavy docs;
- source-of-truth duplication;
- stale status wording in active docs;
- broken links or navigation;
- historical docs rewritten without need;
- recommendations implemented without explicit scope.

## Severity

Classify findings by severity:

- Critical - violates roadmap/ADR/architecture, breaks core behavior, exposes secrets, or creates unsafe implementation direction.
- Major - likely regression, missing required tests, broken public contract, or significant layer violation.
- Minor - localized maintainability, clarity, navigation, or low-risk consistency issue.
- Note - observation or follow-up that does not block acceptance.

## Output Format

Put findings first. For each finding include:

- severity;
- file and line when possible;
- what is wrong;
- why it matters;
- suggested fix.

Then include:

1. Open questions
2. Test and validation gaps
3. Documentation/navigation gaps
4. Scope-control notes
5. Brief summary
