# Roadmap Progress — Travel Assistant

Этот документ является **primary roadmap** проекта Travel Assistant. Он фиксирует этапы, текущий прогресс, артефакты, открытые вопросы, carryover и следующий шаг.

Связанные документы:

- `docs/ROADMAP.md` — краткий верхнеуровневый список этапов.
- `docs/development/roadmap.md` — детальный roadmap разработки и будущих implementation-задач.
- `docs/development/milestones.md` — вехи и контрольные точки реализации.
- `docs/product/README.md` — индекс продуктовых документов.

## Current Status

| Item | Status |
|---|---|
| Current stage | Cleanup после Stage 2, перед Stage 2 Consistency Review |
| Last completed stage | Stage 2 — Use Cases & Edge Cases |
| Next planned step | Stage 2 Consistency Review |
| Stage 3 | Not started |
| Code/API/DB/UI implementation | Not started |

## MVP Scope Note

- Интеграция с существующим travel API входит в MVP.
- В организации уже есть travel API; его контракт должен быть предоставлен на соответствующем техническом этапе.
- Mock/fake providers, provider abstractions и contract placeholders допустимы только как промежуточные средства разработки.
- Финальный MVP должен использовать предоставленный API-контракт для получения реальных travel offers.
- Stage 0/1/2 не проектируют API-контракт, endpoints, DTO, database schema, provider adapter или UI-макеты.
- Provider/API data является primary source of truth для travel facts.
- LLM/assistant не должен выдумывать provider facts и должен отделять provider facts, assistant assumptions и unknown data.

## Open Decisions

- Минимальный required field set для hotel search, flight search и combined search.
- Входит ли Level 3 coordinated combined search в MVP.
- Какой объем open destination discovery нужен в MVP.
- Когда и в каком виде будет предоставлен контракт существующего travel API.
- Adapter design, provider error taxonomy, reliability и production-hardening.
- Долгосрочная история, авторизация и account-level storage.
- Acceptance criteria для Stage 3.

## Stage 0 — Product Framing

**Status:** Completed.

**Goal:** зафиксировать исходную продуктовую рамку, первичные сценарии, предварительные MVP boundaries и правила дальнейшей работы.

**Artifacts checklist:**

- [x] `docs/product/stage-0/product-framing.md`
- [x] `docs/product/stage-0/initial-scenarios.md`
- [x] `docs/product/stage-0/mvp-boundaries.md`
- [x] `docs/product/stage-0/assumptions-and-open-questions.md`
- [x] Product documentation index: `docs/product/README.md`

**Open questions:**

- Приоритетные пользователи и сценарии MVP были уточнены в Stage 1.
- Точные MVP boundaries перенесены на Stage 3.
- Технические контракты и архитектурные решения не фиксировались на Stage 0.

**Recommendations / carryover:**

- Сохранять поэтапный порядок: product framing → requirements → use cases → MVP boundaries → UX/architecture → implementation.
- Не использовать Stage 0 как источник финальных технических решений.

## Stage 1 — Business Requirements

**Status:** Completed.

**Goal:** зафиксировать аудиторию, business scenarios, user journeys, BR/FR/NFR, assumptions, open questions и risks.

**Artifacts checklist:**

- [x] `docs/product/stage-1/target-audience.md`
- [x] `docs/product/stage-1/business-scenarios.md`
- [x] `docs/product/stage-1/user-journeys.md`
- [x] `docs/product/stage-1/business-requirements.md`
- [x] `docs/product/stage-1/functional-requirements.md`
- [x] `docs/product/stage-1/non-functional-requirements.md`
- [x] `docs/product/stage-1/assumptions-and-open-questions.md`
- [x] `docs/product/stage-1/stage-1-summary.md`
- [x] `docs/product/stage-1/stage-1-consistency-review.md`

**Follow-ups checklist:**

- [x] Stage 1 Consistency Review
- [x] Stage 1 Follow-up Cleanup
- [x] Stage 1 Scope Correction

**Key results:**

- Business scenarios S-01 - S-10 зафиксированы.
- BR-001 - BR-016 зафиксированы.
- FR-001 - FR-014 зафиксированы.
- NFR-001 - NFR-015 зафиксированы.
- Booking и payment исключены из MVP.
- Provider abstraction и LLM provider abstraction зафиксированы как обязательные границы.

**Open questions:**

- Q-001: уровень поддержки combined search.
- Q-002/Q-003: обязательные параметры hotel и flight search.
- Q-004: критерии успешной рекомендации.
- Q-005: open destination.
- Q-006/Q-010: сохранение и авторизация.
- Q-007/Q-009: порог уточнений и язык uncertainty/provider errors.
- Q-012: когда будет предоставлен контракт существующего travel API.

**Recommendations / carryover:**

- Разделить combined intent recognition, same-dialog assistance, coordinated search и full package ranking.
- Не проектировать API-контракт до предоставления существующего контракта.
- На Stage 3 финализировать MVP boundaries и acceptance criteria.

## Stage 2 — Use Cases & Edge Cases

**Status:** Completed.

**Goal:** развернуть Stage 1 scenarios в use cases, edge cases, assistant behaviour rules, combined search levels и product data requirements.

**Artifacts checklist:**

- [x] `docs/product/stage-2/use-cases.md`
- [x] `docs/product/stage-2/edge-cases.md`
- [x] `docs/product/stage-2/assistant-behaviour-rules.md`
- [x] `docs/product/stage-2/combined-search-levels.md`
- [x] `docs/product/stage-2/data-requirements.md`
- [x] `docs/product/stage-2/stage-2-summary.md`

**Follow-ups checklist:**

- [ ] Stage 2 Consistency Review

**Key results:**

- UC-01 - UC-15 зафиксированы.
- EC-001 - EC-035 зафиксированы.
- ABR-001 - ABR-018 зафиксированы.
- Combined search разделен на 4 уровня:
  - Level 1 — Combined intent recognition: In MVP.
  - Level 2 — Same-dialog hotel and flight assistance: In MVP.
  - Level 3 — Coordinated combined search: Open for Stage 3.
  - Level 4 — Full combined package ranking: Post-MVP/Open.
- Provider/API data зафиксирована как primary source of truth для travel facts.
- LLM/assistant не должен выдумывать provider facts.

**Open questions:**

- Минимальный required field set для каждого intent.
- MVP-решение по Level 3 coordinated combined search.
- Open destination discovery.
- Конкретный API-контракт существующего travel API.
- Adapter design, error handling taxonomy, reliability и production-hardening.
- Session persistence, resume behaviour, long-term history и authorization.

**Recommendations / carryover:**

- Выполнить Stage 2 Consistency Review отдельной задачей.
- На Stage 3 превратить Stage 2 use cases и edge cases в финальные MVP boundaries и acceptance criteria.
- Сохранить запрет на API contracts, DB schema, UI mockups и код до соответствующих этапов.

## Stage 3 — MVP Boundaries & Acceptance Criteria

**Status:** Not started / Planned.

**Goal:** определить финальные границы MVP, функциональные требования в финальном виде и acceptance criteria.

**Planned artifacts checklist:**

- [ ] Финализированные MVP boundaries.
- [ ] Acceptance criteria для MVP use cases.
- [ ] Решение по Level 3 coordinated combined search.
- [ ] Required fields per intent.
- [ ] MVP/Post-MVP split для session persistence, resume и authorization.
- [ ] Carryover list для architecture/technical stages.

**Entry criteria:**

- [x] Stage 0 completed.
- [x] Stage 1 completed.
- [x] Stage 1 Consistency Review completed.
- [x] Stage 1 Follow-up Cleanup completed.
- [x] Stage 1 Scope Correction completed.
- [x] Stage 2 completed.
- [ ] Stage 2 Consistency Review completed.

**Exit criteria:**

- MVP scope финализирован.
- Acceptance criteria описаны и проверяемы.
- Open/Post-MVP пункты отделены от MVP.
- Real travel API integration сохранена в MVP scope без проектирования контракта до его предоставления.
- Stage 4 может начинаться без неявного расширения MVP.

## Future Stages

### Stage 4 — UX/UI Concept

**Status:** Planned.

**Scope:** UX-концепция, основные состояния, структура диалога и отображение результатов без реализации UI.

### Stage 5 — Technical Architecture

**Status:** Planned.

**Scope:** архитектурные границы, компоненты, AI/LLM abstraction, provider abstraction, backend/frontend/domain/integrations responsibilities.

### Stage 6 — Implementation Preparation

**Status:** Planned.

**Scope:** задачи реализации, контракты, тестовая стратегия, mock/fake providers, contract placeholders и локальный workflow.

### Stage 7 — MVP Implementation

**Status:** Planned.

**Scope:** реализация согласованного MVP, включая интеграцию с существующим travel API после предоставления API-контракта.

### Stage 8 — AI/LLM Orchestration Improvements

**Status:** Planned.

**Scope:** улучшение уточнений, объяснений, сравнения и устойчивости AI-поведения без привязки к одному LLM provider.

### Stage 9 — Real Provider/API Integration Hardening

**Status:** Planned.

**Scope:** adapter design, provider-specific error handling, reliability и production-hardening вокруг реального provider/API.

### Stage 10 — Cross-platform Expansion

**Status:** Planned.

**Scope:** развитие за пределы первой платформы без переписывания продуктовой и доменной логики.

## Roadmap Rules

- Не начинать следующий этап без явной задачи.
- Не менять порядок этапов без отдельного решения.
- Не выполнять recommendations в рамках cleanup или review задач.
- Не создавать документы будущих этапов до соответствующей задачи.
- Не проектировать API contracts, database schema, UI mockups или code до соответствующих этапов.
- Спорные архитектурные решения фиксировать через ADR, если они появляются на будущих этапах.
