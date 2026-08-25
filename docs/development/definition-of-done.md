# Definition of Done

A task is done only when all applicable items are true.

## Scope

- The requested scope is implemented.
- No unrelated files were changed.
- No future roadmap stage or out-of-scope recommendation was started.
- No new product scope, provider integration, storage model, infrastructure, or tooling was added unless explicitly requested.

## Architecture and Code

- Architecture boundaries are preserved.
- Domain logic remains framework-independent.
- API, application, domain, and infrastructure responsibilities remain separated.
- No unexplained TODO/FIXME or commented-out code was added.
- Errors are handled explicitly where the task changes behavior.

## Tests

- Tests were added or updated where needed.
- Existing relevant tests pass, or failures are reported with the exact command and reason.
- Applicable build, lint, formatting, integration, contract, and schema checks pass when the repository provides them.
- Lack of tests for behavior changes is explicitly justified in the final report.

## Documentation

- Documentation was updated only where required by the task.
- Navigation/index files were updated when new or changed documents need to be discoverable.
- Roadmap/status files were updated only when project status actually changed.
- Links added by the task were checked for existence.

## Validation and Report

- Every acceptance criterion is checked individually.
- Applicable quality gates were run.
- The final diff was reviewed for correctness, scope, secrets, generated files, and unrelated changes.
- A substantial task received an independent read-only review from a fresh context when the harness supports it; blocking findings were returned to the implementation loop and resolved or explicitly reported.
- Diff hygiene was checked before final response.
- Known risks and open questions are reported.
- The final report lists created files, changed files, checks/tests, skipped checks with reasons, and known risks.
