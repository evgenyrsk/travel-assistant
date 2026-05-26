# Architecture Notes

## Initial Boundaries

```text
app/       User interface and client-side experience
services/  Server-side APIs, AI orchestration, integrations, persistence
scripts/   Local development and automation helpers
tests/     Shared fixtures, integration tests, and end-to-end scenarios
```

## Candidate Components

- Trip planning flow
- Preference profile
- Itinerary generator
- Place and activity search
- Budget estimator
- Travel document and note storage
- External integrations layer

## Principles

- Keep product decisions documented before locking in implementation details.
- Separate UI, domain logic, integrations, and persistence.
- Treat provider APIs as replaceable adapters.
- Keep secrets out of the repository.
