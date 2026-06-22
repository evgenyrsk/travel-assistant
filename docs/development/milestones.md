# Development milestones reference

**Роль:** deprecated compatibility reference. Уникальное milestone vocabulary перенесено в `roadmap.md`; этот файл сохранен только из-за ссылок в historical review artifacts и не должен использоваться для новых задач.

Этот документ является compact reference по возможным будущим development milestones Travel Assistant.

Он не является источником статуса roadmap, активным списком задач, трекером задач или разрешением начинать следующий этап. Текущий статус, следующий шаг, критерии перехода и перенесенные пункты фиксируются только в `docs/roadmap/roadmap.md`.

## Роль milestones

Milestones помогают обсуждать размер и направленность будущих задач реализации после явной активации через roadmap. Они не задают порядок выполнения и не заменяют основной roadmap.

Если milestone звучит конкретнее, чем `docs/roadmap/roadmap.md`, его следует читать как справочный материал для будущего, а не как текущую задачу.

## Compact milestone map

| Milestone area | Reference value |
|---|---|
| Project/process foundation | Навигация, правила задач, базовая документация и repo workflow. |
| Product/architecture foundation | Требования, MVP boundaries, architecture boundaries и domain/API preparation на уровне документации. |
| Backend foundation | Минимальная основа на Kotlin + Ktor после явной активации; текущая реализация сверяется с основным roadmap. |
| AI orchestration foundation | `LlmClient`, intent/slot/clarification flow и testable orchestration после отдельной задачи. |
| Hotel search foundation | Hotel-only provider abstraction, mock/fake providers and ranking-ready hotel data. |
| Web MVP | Chat UI, hotel results UI и frontend/backend integration после явной activation. |
| End-to-end MVP | Hotel-only flow from request to ranked/explained hotel offers. |
| Quality/readiness | Справочные темы тестирования, security, observability, локальной разработки и готовности к промышленному использованию. |

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
- активацию следующего этапа;
- changes to roadmap order or MVP scope.

## Future merge note

Терминология milestones объединена с `docs/development/roadmap.md` в рамках осторожного устранения повторов. Для новых задач используй `docs/development/roadmap.md` и `docs/development/implementation-strategy.md`; этот файл остается артефактом совместимости, а не активным списком задач.
