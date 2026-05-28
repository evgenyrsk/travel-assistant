# Продуктовая документация

Этот раздел содержит продуктовую документацию Travel Assistant. Документы фиксируют пользовательскую ценность, сценарии, границы MVP, открытые вопросы и правила движения по этапам до начала технической реализации.

Главный roadmap со статусами этапов, чеклистами выполненных артефактов, open questions, carryover и следующим шагом находится в `docs/roadmap/roadmap.md`. Этот index описывает карту product-документов, но не является источником текущего статуса этапов.

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

Stage 3 отвечает за UX-структуру, навигацию, hotel search flow, required fields, acceptance criteria и MVP/Post-MVP UX boundaries для MVP v1. Итоговый Stage 3 UX baseline зафиксирован в `stage-3/stage-3-summary-and-carryover.md`, а актуальные статусы этапов ведутся в primary roadmap. Flight search является next expansion после hotel flow, combined hotel+flight — later expansion после flight flow. Stage 4 остается отдельным будущим этапом Visual Design / UI Concept.

## Правила ведения product-документации

- Сначала фиксируются продуктовые цели, сценарии и границы, затем требования, UX, архитектура и реализация.
- Каждый этап должен иметь понятный результат и не должен выполнять работу следующих этапов.
- Технические детали должны вытекать из требований, а не подменять их.
- Спорные архитектурные решения в будущих этапах фиксируются через ADR в `docs/decisions/`.
- Интеграция с существующим travel API входит в MVP v1 для hotel offers; до предоставления API-контракта внутренние travel API рассматриваются как источники-провайдеры за абстракциями, а ранняя разработка может использовать mock/fake providers и contract placeholders без преждевременного проектирования контракта.
