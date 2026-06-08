# Testing Strategy

Tests should describe user-visible or business-relevant behavior, not implementation details.

## Test Types

- Unit tests cover small deterministic functions, value objects, mappers, and policies.
- Domain tests cover business rules without framework, database, HTTP, provider, or LLM dependencies.
- Application use case tests cover orchestration, state transitions, ports, and expected result/error branches.
- API and integration tests cover routing, validation, serialization, HTTP status mapping, and boundary behavior.
- Contract/API consistency checks should be added when public contracts, OpenAPI artifacts, or generated clients become active in a roadmap task.

## Required Coverage

- Every new use case must have tests.
- Every behavior change must be covered by tests unless the final report explicitly justifies why tests were not added.
- Every new expected error branch must be tested.
- Every new API route or changed route behavior must have API-level tests.
- Bug fixes should include a regression test when the behavior can be tested locally.

## Fakes and Stubs

- Use fakes/stubs for provider, LLM, persistence, and clock/time boundaries.
- Keep fakes small and behavior-focused.
- Do not call real external travel providers, LLM providers, or network services in normal automated tests.
- Prefer in-memory fakes for application tests until persistence is explicitly introduced.

## Naming

- Test files should be named after the subject under test.
- Test names should describe the expected behavior and condition.
- Avoid test names that only repeat method names.

Good:

```kotlin
@Test
fun `returns validation error for blank assistant message`() {
    // ...
}
```

Avoid:

```kotlin
@Test
fun testHandleMessage() {
    // ...
}
```

## When Tests Are Required

Tests are required for code changes that affect:

- domain rules;
- application use cases;
- state transitions;
- error handling;
- API request/response behavior;
- mapping between layers;
- provider, LLM, persistence, or configuration boundaries.

Documentation-only changes do not require backend tests unless they change commands, contracts, or documented behavior that should be verified against the codebase.

## Reporting Missing Tests

If tests are not added for a behavior change, the final report must explain:

- what behavior changed;
- why tests were not added;
- what residual risk remains;
- what follow-up test should be added if the gap is accepted.
