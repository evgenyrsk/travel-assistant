# Kotlin Backend Style Guide

This guide applies to the Kotlin + Ktor backend in `services/backend`.

## Files and Packages

- Use one primary class, interface, object, enum, or sealed type per file.
- The file name must match the primary declaration name.
- Keep packages aligned with architectural responsibility: `api`, `application`, `domain`, and future `infrastructure` or adapter packages.
- Do not place Ktor routes, serialization DTOs, or persistence details in domain packages.

## Naming

- Use `PascalCase` for classes, interfaces, objects, enums, and sealed types.
- Use `camelCase` for functions, properties, and local variables.
- Name use cases with a clear action and business outcome, for example `CreateAssistantSessionUseCase`.
- Name ports/boundaries by capability, for example `HotelSearchBoundary`, not by the future adapter implementation.
- Avoid suffixes such as `Impl` unless the codebase already needs more than one implementation in the same package.

## Use Cases

- A use case should expose one main operation, preferably through `operator fun invoke(...)` or a clearly named method.
- Keep use case inputs explicit and small.
- Return typed results for expected success/failure branches.
- Do not let use cases depend on Ktor DTOs, persistence entities, or provider SDK models.

Good:

```kotlin
class CreateAssistantSessionUseCase(
    private val sessionStore: AssistantSessionStateStore,
) {
    operator fun invoke(): CreateAssistantSessionResult
}
```

Avoid:

```kotlin
class AssistantManager {
    fun handle(call: ApplicationCall): Any
}
```

## DTO, Domain, and Entity Separation

- API DTOs describe transport shape only.
- Domain models describe business concepts only.
- Persistence entities describe storage shape only.
- Provider DTOs describe external provider payloads only.
- Map explicitly between layers; do not reuse DTOs as domain models or entities.
- Mapping should happen at layer boundaries: API routes/adapters for HTTP DTOs, infrastructure adapters for provider/persistence DTOs, and application services for orchestration-facing conversion when needed.

## Kotlin Readability

- Prefer immutable `val` values.
- Keep functions short enough to read without scrolling through multiple responsibilities.
- Prefer expression bodies only when they improve clarity.
- Avoid clever chaining when named intermediate values make the business rule clearer.
- Use data classes for simple value carriers.
- Use sealed interfaces/classes for closed result or error sets when the caller must handle every case.

## Nullability

- Make nullable types intentional.
- Prefer non-null domain fields when the value is required by the domain concept.
- Use nullable values for genuinely optional or unknown data, not as a replacement for validation.
- Validate external inputs at the boundary and convert them into explicit domain/application types.

## Results and Errors

- Use sealed result/error types for meaningful application outcomes.
- Keep HTTP status mapping in the API layer.
- Keep provider/database error translation in infrastructure/application boundaries.
- Do not throw generic exceptions for expected domain or application outcomes.

## Dependency Injection

- Inject dependencies through constructor parameters and interfaces.
- Define contracts close to the layer that owns the need.
- Do not instantiate provider clients, database clients, or framework services inside domain models.

## Tests

- Test class names should match the subject under test, for example `CreateAssistantSessionUseCaseTest`.
- Test names should describe behavior, not implementation details.
- Prefer readable test names with spaces when using Kotlin test backticks.

Example:

```kotlin
@Test
fun `creates a local assistant session with initial collecting status`() {
    // ...
}
```
