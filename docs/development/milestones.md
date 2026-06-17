# Development milestones reference

**Роль:** deprecated compatibility reference. Уникальное milestone vocabulary перенесено в `roadmap.md`; этот файл сохранен только из-за ссылок в historical review artifacts и не должен использоваться для новых задач.

Этот документ является compact reference по возможным будущим development milestones Travel Assistant.

Он не является source of truth по roadmap status, active backlog, task tracker или разрешением начинать следующий Stage 7 шаг. Текущий статус, следующий шаг, stage gates и carryover фиксируются только в `docs/roadmap/roadmap.md`.

## Роль milestones

Milestones помогают обсуждать размер и направленность будущих implementation задач после явной roadmap activation. Они не задают порядок выполнения и не заменяют primary roadmap.

Если milestone звучит конкретнее, чем `docs/roadmap/roadmap.md`, он должен читаться как future reference, а не как текущая задача.

## Compact milestone map

| Milestone area | Reference value |
|---|---|
| Project/process foundation | Навигация, правила задач, базовая документация и repo workflow. |
| Product/architecture foundation | Требования, MVP boundaries, architecture boundaries и domain/API preparation на уровне документации. |
| Backend foundation | Минимальный Kotlin + Ktor foundation после явной activation; current skeleton already exists from Stage 7.0b. |
| AI orchestration foundation | `LlmClient`, intent/slot/clarification flow и testable orchestration после отдельной задачи. |
| Hotel search foundation | Hotel-only provider abstraction, mock/fake providers and ranking-ready hotel data. |
| Web MVP | Chat UI, hotel results UI и frontend/backend integration после явной activation. |
| End-to-end MVP | Hotel-only flow from request to ranked/explained hotel offers. |
| Quality/readiness | Testing, security, observability, local development and production readiness references. |

## Как использовать

- Используй milestones как vocabulary для будущих маленьких задач.
- Для текущего статуса всегда проверяй `docs/roadmap/roadmap.md`.
- Для product scope проверяй `docs/product/product-baseline.md`.
- Для architecture/backend stack проверяй `docs/architecture/architecture-baseline.md`.
- Для agent workflow и scope control проверяй `AGENTS.md`.
- Для active engineering rules проверяй `docs/development/README.md`.

## Что не входит

Этот документ не создает:

- active backend/frontend tasks;
- API/OpenAPI contracts;
- DB schema/storage model;
- auth/security/DevOps/testing backlog;
- provider-specific integration work;
- next Stage 7 step activation;
- changes to roadmap order or MVP scope.

## Future merge note

Milestone vocabulary объединен с `docs/development/roadmap.md` в рамках conservative documentation dedup cleanup. Для новых задач используй `docs/development/roadmap.md` и `docs/development/implementation-strategy.md`; этот файл остается compatibility artifact, а не active backlog.
