# Backend Layering Rules

These rules apply to the Kotlin + Ktor backend in `services/backend`. They document the intended implementation boundaries; they do not create new roadmap scope or provider integrations.

## Layers

### API Layer

Responsibilities:

- Ktor routes and HTTP wiring.
- Request validation at the transport boundary.
- Request/response DTOs.
- HTTP status and error response mapping.
- Calling application use cases.

Allowed dependencies:

- Application layer.
- API-local DTOs, serializers, and route helpers.

Forbidden:

- Calling infrastructure implementations directly.
- Owning business rules.
- Returning domain errors without HTTP mapping.

### Application Layer

Responsibilities:

- Use cases.
- Workflow orchestration.
- Application result and error types.
- Ports/contracts needed to complete use cases.
- Mapping between domain concepts and application-facing results when needed.

Allowed dependencies:

- Domain layer.
- Application-owned contracts.

Forbidden:

- Depending on API DTOs.
- Depending on persistence entities.
- Depending on provider SDK DTOs.
- Owning Ktor-specific behavior.

### Domain Layer

Responsibilities:

- Business concepts.
- Domain rules and invariants.
- Framework-independent value objects and policies.
- Provider-independent travel assistant concepts.

Allowed dependencies:

- Kotlin standard library.
- Domain-local types.

Forbidden:

- Ktor.
- Database clients, persistence entities, migrations, or schemas.
- HTTP DTOs.
- Provider SDKs or provider-specific DTOs.
- LLM provider SDKs.
- Runtime configuration or environment access.

### Infrastructure Layer

Responsibilities:

- Persistence adapters.
- Provider adapters.
- LLM client implementations.
- Configuration-backed clients.
- External I/O.

Allowed dependencies:

- Application/domain contracts that the adapter implements.
- External libraries needed by the adapter.

Forbidden:

- Changing domain concepts to match provider/database payloads.
- Leaking provider/database errors directly into public API responses.
- Being called directly from API routes when an application use case should own the workflow.

## Dependency Direction

Recommended direction:

```text
API -> Application
Application -> Domain
Infrastructure -> Application/Domain contracts
Domain -> no framework dependencies
```

Infrastructure may depend on contracts owned by application/domain layers. Application should not depend on infrastructure implementations.

## DTO, Domain, and Entity Boundaries

- API DTOs are for HTTP shape.
- Domain models are for business meaning.
- Persistence entities are for storage shape.
- Provider DTOs are for external provider payloads.
- Mapping should happen at boundaries:
  - API maps HTTP DTOs to application inputs and application results to HTTP responses.
  - Infrastructure maps provider/database DTOs to application/domain contracts.
  - Application coordinates mapping only when a use case boundary requires it.

## Examples of Violations

- Domain -> Ktor import.
- Domain -> database entity or repository implementation.
- Domain -> HTTP request/response DTO.
- Application -> API DTO.
- Application -> persistence entity.
- API -> infrastructure implementation directly.
- Provider DTO reused as a domain `HotelOffer`.
- Ktor route performs ranking, clarification planning, or provider error taxonomy decisions inline.

## Provider Isolation

External travel providers must stay behind application/domain contracts. Start with mock/stub providers when allowed by the roadmap task. Do not hardcode provider-specific fields, credentials, endpoint assumptions, or SDK models into domain/application code.
