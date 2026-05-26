# Architecture Notes

Этот документ описывает целевую архитектуру и границы ответственности. Практический порядок реализации описан отдельно в `docs/development/implementation-strategy.md`.

## Target Stack

- Backend: Kotlin + Ktor.
- Frontend: Next.js + React + Tailwind + shadcn/ui.
- Data: PostgreSQL; Redis только при наличии понятного сценария кэширования.
- AI: provider-neutral `LlmClient`.
- Orchestration: собственный `TravelAssistantOrchestrator` с возможностью будущего перехода к state-machine или LangGraph-like модели.
- Integrations: travel API скрываются за flight и hotel provider-интерфейсами, начиная с mock/stub providers.

## Initial Boundaries

```text
app/       Будущий frontend/application слой.
services/  Будущие backend/services модули, AI orchestration, integrations, persistence.
scripts/   Будущие local development и automation helpers.
tests/     Будущие shared fixtures, integration tests и end-to-end scenarios.
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
- Keep domain logic independent from Ktor, Next.js, PostgreSQL, Redis, and concrete LLM providers.
- Keep secrets out of the repository.
