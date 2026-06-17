# Продуктовая документация

Этот раздел содержит продуктовую документацию Travel Assistant. Этот README является index-документом: он помогает найти актуальный product baseline и historical stage artifacts, но не является самостоятельной продуктовой спецификацией, roadmap или implementation backlog.

Главный roadmap со статусами этапов, progression, carryover и следующим разрешенным шагом находится в `docs/roadmap/roadmap.md`. Если статус этапа или следующий шаг важен для задачи, приоритет имеет primary roadmap.

`product-baseline.md` является текущим источником истины по продукту: он фиксирует актуальное продуктовое состояние, MVP boundaries и active product framing. Документы `stage-*` сохраняются как historical stage artifacts, deliverables и audit trail: они объясняют, как формировались решения, но не должны читаться как автоматическое расширение MVP scope.

Если historical artifact содержит более широкие или устаревшие формулировки, их нужно читать в историческом контексте. Они не переопределяют `product-baseline.md`, primary roadmap или явные границы текущей задачи. Для product scope Codex должен сначала открыть `product-baseline.md`, а этот README использовать только как навигацию.

## Иерархия ролей

1. `product-baseline.md` — текущий источник истины по продукту для MVP scope, product guardrails и active product framing.
2. `../roadmap/roadmap.md` — source of truth по статусам этапов, progression, carryover и следующему разрешенному шагу.
3. `README.md` — только product navigation/index; не конкурирующий baseline.
4. `stage-0/**` - `stage-4/**` — historical product discovery/design artifacts и review trail, если конкретный файл не помечен иначе.

## Инвентаризация product docs

| Документ | Классификация | Как читать |
|---|---|---|
| `product-baseline.md` | Current product source of truth | Начинать отсюда при проверке MVP scope, active product framing и product guardrails. |
| `README.md` | Product navigation/index | Использовать для поиска документов и понимания ролей, не как самостоятельную продуктовую спецификацию. |
| `stage-0/product-framing.md` | Historical product stage artifact | Исходная рамка продукта; читать как early discovery context. |
| `stage-0/initial-scenarios.md` | Historical product stage artifact | Ранние сценарии верхнего уровня; не расширяют текущий MVP. |
| `stage-0/mvp-boundaries.md` | Historical product stage artifact | Ранняя MVP-рамка; текущие границы сверять с `product-baseline.md`. |
| `stage-0/assumptions-and-open-questions.md` | Historical product stage artifact | Ранние допущения и вопросы; не active backlog. |
| `stage-1/target-audience.md` | Historical product stage artifact | Детали аудитории Stage 1; использовать как traceability. |
| `stage-1/business-scenarios.md` | Historical product stage artifact | Business scenarios Stage 1; MVP/post-MVP трактовать через baseline. |
| `stage-1/user-journeys.md` | Historical product stage artifact | User journeys Stage 1; flight/combined контекст исторический для MVP v1. |
| `stage-1/business-requirements.md` | Historical product stage artifact | Business requirements Stage 1; текущий scope сверять с baseline. |
| `stage-1/functional-requirements.md` | Historical product stage artifact | Functional requirements Stage 1; не active implementation backlog. |
| `stage-1/non-functional-requirements.md` | Historical product stage artifact | Product-level NFR context; не технический backlog. |
| `stage-1/assumptions-and-open-questions.md` | Historical product stage artifact | Stage 1 assumptions/open questions; не active backlog. |
| `stage-1/stage-1-summary.md` | Historical product stage artifact | Summary Stage 1 и traceability. |
| `stage-1/stage-1-consistency-review.md` | Product review/audit artifact | Quality gate Stage 1; не source of truth по текущему статусу. |
| `stage-2/use-cases.md` | Historical product stage artifact | Use cases Stage 2; читать с hotel-only MVP boundary. |
| `stage-2/edge-cases.md` | Historical product stage artifact | Edge cases Stage 2; не implementation checklist. |
| `stage-2/assistant-behaviour-rules.md` | Historical product stage artifact / reference-only | Product behaviour rules; полезный reference, но текущие границы задает baseline. |
| `stage-2/combined-search-levels.md` | Historical product stage artifact | Combined search context; future expansion, не MVP v1. |
| `stage-2/data-requirements.md` | Historical product stage artifact / reference-only | Product data needs без API/DB schema. |
| `stage-2/stage-2-summary.md` | Historical product stage artifact | Summary Stage 2 и carryover context. |
| `stage-2/stage-2-consistency-review.md` | Product review/audit artifact | Review Stage 2; не active backlog. |
| `stage-3/screen-map.md` | Historical product stage artifact | Hotel-only screen map; useful UX detail under baseline. |
| `stage-3/required-fields-and-acceptance-criteria.md` | Historical product stage artifact / reference-only | Required fields и acceptance criteria для hotel flow. |
| `stage-3/mvp-search-flow-details.md` | Historical product stage artifact / reference-only | Detailed hotel flow; не API/implementation contract. |
| `stage-3/combined-search-ux-decision.md` | Stale/superseded product artifact | Historical combined-search decision, superseded for MVP v1 by hotel-only scope. |
| `stage-3/stage-3-hotel-only-consistency-review.md` | Product review/audit artifact | Review hotel-only refocus. |
| `stage-3/stage-3-summary-and-carryover.md` | Historical product stage artifact / reference-only | UX/product detail under current baseline. |
| `stage-3/stage-3-plan-reconciliation.md` | Product review/audit artifact | Completion audit Stage 3. |
| `stage-4/visual-design-direction.md` | Historical product stage artifact | Visual/UX direction; не frontend implementation. |
| `stage-4/design-system-foundations.md` | Historical product stage artifact / reference-only | Draft design foundations; не final tokens. |
| `stage-4/component-inventory.md` | Historical product stage artifact / reference-only | Component inventory; не React/shadcn implementation backlog. |
| `stage-4/screen-specifications.md` | Historical product stage artifact / reference-only | Screen-level specs; не production UI code. |
| `stage-4/interaction-patterns.md` | Historical product stage artifact / reference-only | Interaction guidance for hotel flow. |
| `stage-4/stage-4-summary-and-carryover.md` | Historical product stage artifact | Summary Stage 4 и carryover. |
| `stage-4/stage-4-consistency-review.md` | Product review/audit artifact | Stage 4.1 quality gate. |

Неясных ролей нет: все текущие файлы `docs/product/**` классифицированы этим index.

## Актуальный product baseline

- `product-baseline.md` — текущий источник истины по продукту после Stage 0-5: hotel-only MVP v1 scope, явные исключения из MVP, core product flow, product guardrails, связь с historical stage artifacts и актуальный carryover.

MVP v1 остается hotel-only. Flight search, combined itinerary, booking, payment и account history остаются вне MVP v1 до отдельного roadmap decision.

## Этап 0 — перезапуск проекта и продуктовая рамка

- `stage-0/product-framing.md` — исходная продуктовая постановка, проблема, аудитория, отличие от обычного поиска, роль AI/LLM, внутренних API и кроссплатформенности.
- `stage-0/initial-scenarios.md` — основные пользовательские сценарии верхнего уровня без детализации до user stories.
- `stage-0/mvp-boundaries.md` — предварительная рамка MVP: что потенциально входит и что не входит в ранние этапы.
- `stage-0/assumptions-and-open-questions.md` — принципы разработки, рабочие допущения, риски и открытые вопросы.

## Этап 1 — бизнес-требования и пользовательские сценарии

- `stage-1/target-audience.md` — целевые сегменты, контекст использования, боли, мотивация, критерии успеха и MVP/post-MVP сегменты.
- `stage-1/business-scenarios.md` — бизнес-сценарии S-01 - S-10 с целями, потоками, результатами, требованиями к ассистенту и статусом MVP/post-MVP.
- `stage-1/user-journeys.md` — пользовательские пути для отелей, перелетов, комбинированного поиска, уточнений, сравнения, сохранения и возврата к поиску.
- `stage-1/business-requirements.md` — бизнес-требования BR-001 - BR-016 с приоритетами, MVP-статусом и связью со сценариями.
- `stage-1/functional-requirements.md` — функциональные требования FR-001 - FR-014 с acceptance criteria и traceability к бизнес-сценариям.
- `stage-1/non-functional-requirements.md` — нефункциональные требования NFR-001 - NFR-015 без выбора финального технического стека.
- `stage-1/assumptions-and-open-questions.md` — допущения, открытые вопросы, риски Stage 1 и зафиксированное расхождение по объему Stage 1 в roadmap.
- `stage-1/stage-1-summary.md` — краткое резюме Stage 1, MVP scope, основные вопросы и рекомендации для следующего этапа.
- `stage-1/stage-1-consistency-review.md` — quality gate Stage 1: traceability, MVP/post-MVP consistency, scope control, terminology, roadmap consistency и readiness for Stage 2.

## Этап 2 — use cases и edge cases

- `stage-2/use-cases.md` — use cases UC-01 - UC-15 с traceability к Stage 1 scenarios, BR и FR.
- `stage-2/edge-cases.md` — edge cases EC-001 - EC-035 для missing data, ambiguous requests, provider/data problems, LLM/assistant risks и unsupported actions.
- `stage-2/assistant-behaviour-rules.md` — продуктовые правила поведения ассистента ABR-001 - ABR-018 без prompt engineering.
- `stage-2/combined-search-levels.md` — уровни combined search и MVP recommendation по MJ-S1-001.
- `stage-2/data-requirements.md` — продуктовые требования к данным без API schema, DTO, OpenAPI или database schema.
- `stage-2/stage-2-summary.md` — краткое резюме Stage 2, open questions, переносы на Stage 3 и readiness.
- `stage-2/stage-2-consistency-review.md` — review Stage 2 и структуры документации: consistency, traceability, combined search, provider/API data handling, navigation, duplication и language notes.
- Stage 2 Minor Cleanup — follow-up cleanup по language notes и roadmap navigation polish; результат зафиксирован в `stage-2/stage-2-consistency-review.md` и primary roadmap.

## Этап 3 — MVP UX / Navigation

- `stage-3/screen-map.md` — Stage 3.1: карта экранов hotel-only MVP v1, навигационная модель, основные UX-потоки, состояния экранов и MVP/Post-MVP разделение.
- `stage-3/required-fields-and-acceptance-criteria.md` — Stage 3.2: required/optional/derived fields, missing data behaviour и acceptance criteria для hotel search flow.
- `stage-3/mvp-search-flow-details.md` — Stage 3.3: подробный MVP v1 hotel search flow, refinement, save/shortlist и recovery states.
- `stage-3/combined-search-ux-decision.md` — Stage 3.4: historical decision по limited Level 3 coordinated combined search; superseded для MVP v1 решением о hotel-only scope.
- `stage-3/stage-3-hotel-only-consistency-review.md` — Stage 3.5: контрольный review после hotel-only refocus; проверяет, что flight/combined scope отделен от active MVP v1.
- `stage-3/stage-3-summary-and-carryover.md` — Stage 3.6: итоговый Hotel-Only MVP v1 UX baseline и carryover для Stage 4, architecture, API/provider contract, implementation и future expansions.
- `stage-3/stage-3-plan-reconciliation.md` — Stage 3.7: completion audit, сверяющий original Stage 3 plan, actual deliverables, superseded scope и carryover.

Stage 3 отвечает за UX-структуру, навигацию, hotel search flow, required fields, acceptance criteria и MVP/Post-MVP UX boundaries для MVP v1. Итоговый Stage 3 UX baseline зафиксирован в `stage-3/stage-3-summary-and-carryover.md`, а актуальные статусы этапов ведутся в primary roadmap. Flight search является next expansion после hotel flow, combined hotel+flight — later expansion после flight flow. Stage 4 использует этот baseline как вход для Visual Design & UX System.

## Этап 4 — Visual Design & UX System

- `stage-4/visual-design-direction.md` — цель Stage 4, роль визуального дизайна, дизайн-принципы, желаемое ощущение интерфейса, visual style direction, баланс chat/results и границы этапа.
- `stage-4/design-system-foundations.md` — foundational design system: draft color system, semantic roles, typography, spacing, radius, elevation, layout, responsive behaviour, accessibility и states.
- `stage-4/component-inventory.md` — MVP UI component inventory с назначением, использованием, состояниями, UX-правилами и MVP/future статусом.
- `stage-4/screen-specifications.md` — screen-level specs для Entry, chat, clarification, hotel results, offer details, saved results, error/no results и future flight/combined screens.
- `stage-4/interaction-patterns.md` — UX interaction patterns для clarification, understood request, parameter changes, comparison, saving, loading, partial/no results, confidence/rationale и facts/assumptions separation.
- `stage-4/stage-4-summary-and-carryover.md` — итог Stage 4, созданные документы, ключевые design decisions, carryover на следующие этапы и явно не выполненные items.
- `stage-4/stage-4-consistency-review.md` — Stage 4.1: consistency review Stage 4 относительно Stage 0-3 и roadmap; проверяет MVP scope, UX alignment, data clarity, hidden implementation commitments, accessibility/responsive readiness и carryover quality.

Stage 4 отвечает за visual/UX direction поверх Stage 3 Hotel-Only MVP v1 baseline. Он не создает production UI, React/Next.js components, design-token implementation, API contracts или architecture decisions. Flight search и combined hotel+flight остаются future expansion и не становятся active MVP v1 UI.

## Этап 5 — Technical Architecture / System Design

- `../architecture/stage-5/architecture-scope-and-principles.md` — Stage 5.1: scope, guardrails and principles для technical architecture; фиксирует, что Stage 5 переводит решения Stage 0-4 в архитектурные границы без production-кода, API contracts, database schema или implementation backlog.
- `../architecture/stage-5/system-context-and-boundaries.md` — Stage 5.2: context-level system actors, external dependencies, MVP boundaries, boundary rules and future expansion boundaries без API contracts, database schema или implementation plan.
- `../architecture/stage-5/domain-model-and-boundaries.md` — Stage 5.3: conceptual domain model and responsibility boundaries для hotel-only MVP v1 без DTO, classes, interfaces, database schema, API payloads или implementation backlog.
- `../architecture/stage-5/application-orchestration.md` — Stage 5.4: conceptual application orchestration between user intent, assistant/LLM, hotel provider abstraction, Search Intent Summary and results view без state machine implementation, API contracts, DTO/classes/interfaces или queues/events.
- `../architecture/stage-5/integration-architecture.md` — Stage 5.5: conceptual integration architecture for hotel provider, LLM/AI, frontend/backend, optional telemetry and optional current-session persistence boundaries без OpenAPI, API payloads, provider SDK, concrete vendors или implementation backlog.
- `../architecture/stage-5/data-and-storage-boundaries.md` — Stage 5.6: conceptual data ownership, volatility and storage boundaries for hotel-only MVP v1 без DB schema, ERD, migrations, tables/fields/indexes, storage technology или retention policy.
- `../architecture/stage-5/non-functional-requirements.md` — Stage 5.7: architecture-level non-functional requirements and quality attributes для hotel-only MVP v1 без production SLO/SLA, deployment topology, monitoring stack, security implementation, test plan или implementation backlog.
- `../architecture/stage-5/architecture-decisions-draft.md` — Stage 5.8: draft-level inventory of architecture decisions, ADR candidates, deferred decisions and guardrails без создания отдельных ADR, API/DB contracts, vendor/tool selection или implementation backlog.
- `../architecture/stage-5/stage-5-consistency-review.md` — Stage 5.9: consistency review / completion audit для Stage 5 architecture docs; проверяет scope safety, roadmap alignment, no API/DB/implementation leakage и readiness to close Stage 5.
- `../architecture/stage-5/stage-5-summary-and-carryover.md` — Stage 5 summary: итог архитектурного baseline и carryover к будущим этапам без запуска Stage 6 или implementation backlog.

Stage 5 отвечает за архитектурные границы и принципы для hotel-only MVP v1. Он должен сохранять разделение user-provided constraints, provider facts, assistant assumptions и unknown data. Flight search, combined itinerary, booking/payment flows, account history и full auth остаются future expansion и не становятся active MVP v1 scope.

## Правила ведения product-документации

- Сначала фиксируются продуктовые цели, сценарии и границы, затем требования, UX, архитектура и реализация.
- Каждый этап должен иметь понятный результат и не должен выполнять работу следующих этапов.
- Технические детали должны вытекать из требований, а не подменять их.
- Спорные архитектурные решения в будущих этапах фиксируются через ADR в `docs/decisions/`.
- Интеграция с существующим travel API входит в MVP v1 для hotel offers; до предоставления API-контракта внутренние travel API рассматриваются как источники-провайдеры за абстракциями, а ранняя разработка может использовать mock/fake providers и contract placeholders без преждевременного проектирования контракта.
