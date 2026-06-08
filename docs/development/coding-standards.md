# Coding Standards

These rules apply to code in any technology used by Travel Assistant. Kotlin-specific rules live in [kotlin-backend-style-guide.md](kotlin-backend-style-guide.md).

## Responsibility

- Keep one clear responsibility per file, class, function, or module.
- Prefer small focused units over broad objects that know about many workflows.
- Keep API, application, domain, and infrastructure concerns separated.
- Do not place business rules in routing, serialization, persistence, or provider adapter code.

## Naming

- Use names that describe business meaning or technical responsibility.
- Avoid vague names such as `Manager`, `Helper`, `Util`, or `Processor` unless the role is genuinely generic and local.
- Name interfaces by the capability they expose, not by the implementation that will satisfy them.
- Avoid abbreviations unless they are standard in the codebase or domain.

## Dependency Direction

- Dependencies must point inward toward stable business rules.
- Domain code must not depend on frameworks, transport DTOs, persistence entities, provider SDKs, or runtime configuration.
- Application code may depend on domain models and contracts, but not on API DTOs or persistence entities.
- Infrastructure implements contracts owned by application/domain layers.

## Error Handling

- Handle expected errors explicitly.
- Prefer typed results, domain errors, or application errors over throwing generic exceptions across layer boundaries.
- Do not leak provider, database, HTTP, or framework errors directly into domain models or public API responses.
- Preserve useful diagnostics in logs without exposing secrets or provider credentials.

## Abstraction

- Avoid premature abstraction. Add an interface, base type, or shared helper only when it removes real duplication or protects an actual boundary.
- Keep abstractions narrow and task-focused.
- Do not introduce frameworks, patterns, or libraries only because they may help future work.

## State and Side Effects

- Avoid global mutable state.
- Keep state ownership explicit.
- Make side effects visible in function names, use case boundaries, or infrastructure adapters.
- Do not hide I/O inside domain operations.

## Diff Hygiene

- Keep changes scoped to the requested task.
- Do not modify unrelated files or reformat unrelated sections.
- Do not leave commented-out code.
- Do not add dependencies, scripts, config, generated files, or directories unless the task explicitly requires them.

## TODO and FIXME

- Do not add TODO or FIXME comments for work that can be completed in the current task.
- A TODO/FIXME is allowed only when it names a concrete deferred decision or follow-up and explains why it is not part of the current scope.
- Do not use TODO/FIXME as a substitute for error handling, tests, or acceptance criteria.

## Layer Separation

- Domain: business concepts, rules, and framework-independent decisions.
- Application: use cases, orchestration, ports, and transaction/workflow boundaries.
- API: HTTP routes, request/response DTOs, validation at the transport boundary, and response mapping.
- Infrastructure: persistence, provider clients, external integrations, configuration, and adapter implementations.
