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
| Current stage | Stage 4 — Visual Design & UX System completed |
| Last completed stage | Stage 4.1 — Visual Design Consistency Review |
| Next planned step | Select next task explicitly; Stage 5 Technical Architecture is planned but not started |
| Stage 3 | Completed |
| Stage 4 | Completed |
| Code/API/DB/UI implementation | Not started |

## MVP Scope Note

- MVP v1 сфокусирован только на hotel search: natural-language hotel request, clarification, hotel results, hotel ranking/explanation, hotel details, hotel save/shortlist и базовое hotel comparison.
- Flight search исключен из MVP v1 и является следующим расширением после реализации hotel search flow.
- Combined hotel + flight search исключен из MVP v1 и возвращается после появления flight search flow.
- Интеграция с существующим travel API входит в MVP v1 для hotel offers.
- В организации уже есть travel API; его контракт должен быть предоставлен на соответствующем техническом этапе.
- Mock/fake providers, provider abstractions и contract placeholders допустимы только как промежуточные средства разработки.
- Финальный MVP v1 должен использовать предоставленный API-контракт для получения реальных hotel offers.
- Stage 0/1/2 не проектируют API-контракт, endpoints, DTO, database schema, provider adapter или UI-макеты.
- Provider/API data является primary source of truth для travel facts.
- LLM/assistant не должен выдумывать provider facts и должен отделять provider facts, assistant assumptions и unknown data.

## Open Decisions

- Какой объем provider-backed open destination discovery нужен в MVP v1, если он применим к hotel search.
- Когда и в каком виде будет предоставлен контракт существующего travel API.
- Adapter design, provider error taxonomy, reliability и production-hardening.
- Долгосрочная история, авторизация и account-level storage.
- Следующий этап или cleanup task должен быть выбран отдельной задачей.

## Stage 3 Dashboard

Этот раздел является компактной рабочей панелью текущего этапа. Детальные продуктовые решения остаются в Stage 3 документах, а статус и следующий шаг фиксируются здесь.

| Area | Status | Source / next step |
|---|---|---|
| Screen map and navigation model | Completed | `docs/product/stage-3/screen-map.md` |
| Required fields and acceptance criteria | Completed | `docs/product/stage-3/required-fields-and-acceptance-criteria.md` |
| MVP search flow details | Completed | `docs/product/stage-3/mvp-search-flow-details.md` |
| Combined Search UX Decision | Superseded for MVP v1 | `docs/product/stage-3/combined-search-ux-decision.md` сохранен как historical decision; combined перенесен за MVP v1. |
| MVP v1 Hotel-Only Scope Refocus | Completed | MVP v1 ограничен hotel search; flight является next expansion, combined — later expansion. |
| Session persistence / resume / authorization split | Carried over | `docs/product/stage-3/stage-3-summary-and-carryover.md`; решить на будущих этапах без преждевременной DB/auth architecture. |
| UX Consistency Review | Completed | `docs/product/stage-3/stage-3-hotel-only-consistency-review.md`; verdict: Passed with minor notes. |
| Stage 3 Summary & Carryover | Completed | `docs/product/stage-3/stage-3-summary-and-carryover.md`; Stage 3 can be closed. |
| Stage 3 Plan Reconciliation / Completion Audit | Completed | `docs/product/stage-3/stage-3-plan-reconciliation.md`; verdict: Complete with carryover. |

**Stage 3 closure notes:**

- Stage 3 UX/acceptance docs проверены: flight и combined не требуются для MVP v1.
- Stage 3 Summary & Carryover завершен.
- Stage 3 Plan Reconciliation подтвердил, что обязательные Stage 3 работы не пропущены.
- Historical note: на момент закрытия Stage 3 Stage 4 еще не был начат; теперь Stage 4 завершен отдельным documentation pack.
- Нужно не начинать Stage 5 Technical Architecture до явной задачи на архитектурный этап.

## Stage 4 Dashboard

Этот раздел фиксирует завершенный Stage 4 documentation pack. Детальные visual/UX решения находятся в Stage 4 документах, а этот roadmap отражает только статус и carryover.

| Area | Status | Source / next step |
|---|---|---|
| Visual Design Direction | Completed | `docs/product/stage-4/visual-design-direction.md` |
| Design System Foundations | Completed | `docs/product/stage-4/design-system-foundations.md` |
| Component Inventory | Completed | `docs/product/stage-4/component-inventory.md` |
| Screen Specifications | Completed | `docs/product/stage-4/screen-specifications.md` |
| Interaction Patterns | Completed | `docs/product/stage-4/interaction-patterns.md` |
| Stage 4 Summary & Carryover | Completed | `docs/product/stage-4/stage-4-summary-and-carryover.md` |
| Stage 4.1 Visual Design Consistency Review | Completed | `docs/product/stage-4/stage-4-consistency-review.md`; verdict: Passed with minor wording fixes. |
| Frontend implementation | Not started | Stage 4 intentionally created documentation only. |
| Stage 5 Technical Architecture | Not started | Must be selected as a separate task. |

**Stage 4 closure notes:**

- Stage 4 опирается на Hotel-Only MVP v1 baseline и не меняет MVP scope.
- Visual design direction, foundational system, component inventory, screen specs and interaction patterns зафиксированы как product/design documentation.
- Draft palette and design foundations are not final implementation tokens.
- Flight and combined UI remain future expansion references, not active MVP v1 scope.
- Production code, React components, Tailwind/shadcn configuration, API contracts, architecture and implementation were not started.
- Stage 4.1 consistency review completed; minor wording fixes clarified combined chat/results composition and user-provided constraints separation.

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
- Later scope note: прежние flight и combined MVP recommendations superseded для MVP v1; flight search — next expansion после hotel flow, combined — later expansion после flight flow.

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
- [x] `docs/product/stage-2/stage-2-consistency-review.md`

**Follow-ups checklist:**

- [x] Stage 2 Consistency Review
- [x] Stage 2 Minor Cleanup — Language & Roadmap Navigation Polish

**Key results:**

- UC-01 - UC-15 зафиксированы.
- EC-001 - EC-035 зафиксированы.
- ABR-001 - ABR-018 зафиксированы.
- Combined search разделен на 4 уровня:
  - Level 1 — Combined intent recognition: superseded for MVP v1; future expansion.
  - Level 2 — Same-dialog hotel and flight assistance: superseded for MVP v1; future expansion after flight flow.
  - Level 3 — Coordinated combined search: superseded for MVP v1; later expansion after flight flow.
  - Level 4 — Full combined package ranking: Post-MVP/Open.
- Provider/API data зафиксирована как primary source of truth для travel facts.
- LLM/assistant не должен выдумывать provider facts.
- MVP v1 scope refocus: Stage 2 flight/combined recommendations сохраняются как historical traceability, но не являются разрешением реализовывать flight/combined в MVP v1.

**Open questions:**

- Минимальный required field set для каждого intent.
- Open destination discovery.
- Конкретный API-контракт существующего travel API.
- Adapter design, error handling taxonomy, reliability и production-hardening.
- Session persistence, resume behaviour, long-term history и authorization.

**Recommendations / carryover:**

- На Stage 3 превратить Stage 2 use cases и edge cases в финальные MVP boundaries и acceptance criteria.
- Сохранить запрет на API contracts, DB schema, UI mockups и код до соответствующих этапов.

## Stage 3 — MVP UX / Navigation

**Status:** Completed.

**Goal:** определить UX-структуру MVP, navigation model, search flow boundaries, required fields и acceptance criteria для пользовательских потоков без перехода к визуальному дизайну, API, архитектуре или реализации.

**Artifacts checklist:**

- [x] Stage 3.1 — MVP Screen Map / UX Navigation: `docs/product/stage-3/screen-map.md`.
- [x] Stage 3.2 — Required Fields & Acceptance Criteria: `docs/product/stage-3/required-fields-and-acceptance-criteria.md`.
- [x] Stage 3.3 — MVP Search Flow Details: `docs/product/stage-3/mvp-search-flow-details.md`.
- [x] Stage 3.4 — Combined Search UX Decision: `docs/product/stage-3/combined-search-ux-decision.md` (superseded for MVP v1).
- [x] Stage 3.5 — MVP v1 Hotel-Only Scope Refocus.
- [x] Stage 3.5 — Hotel-Only UX Consistency Review: `docs/product/stage-3/stage-3-hotel-only-consistency-review.md`.
- [x] Stage 3.6 — Stage 3 Summary & Carryover: `docs/product/stage-3/stage-3-summary-and-carryover.md`.
- [x] Stage 3.7 — Stage 3 Plan Reconciliation / Completion Audit: `docs/product/stage-3/stage-3-plan-reconciliation.md`.
- [x] MVP/Post-MVP split для session persistence, resume и authorization перенесен в carryover без технического проектирования.
- [x] Carryover list для visual design, architecture и technical stages.

**Entry criteria:**

- [x] Stage 0 completed.
- [x] Stage 1 completed.
- [x] Stage 1 Consistency Review completed.
- [x] Stage 1 Follow-up Cleanup completed.
- [x] Stage 1 Scope Correction completed.
- [x] Stage 2 completed.
- [x] Stage 2 Consistency Review completed.
- [x] Stage 2 Minor Cleanup completed.

**Exit criteria:**

- MVP UX scope финализирован.
- Required fields и acceptance criteria описаны и проверяемы.
- Open/Post-MVP пункты отделены от MVP.
- Flight search и combined hotel+flight не требуются для MVP v1 и перенесены в future scope.
- Real travel API integration сохранена в MVP v1 scope для hotel offers без проектирования контракта до его предоставления.
- Stage 4 был выполнен без неявного расширения MVP и без смешивания UX-навигации с implementation.
- Stage 3 summary and carryover зафиксированы.

## Stage 4 — Visual Design & UX System

**Status:** Completed.

**Goal:** зафиксировать visual style direction, foundational design system, component inventory, screen-level specifications и interaction patterns для Travel Assistant поверх Stage 3 Hotel-Only MVP v1 UX baseline.

**Artifacts checklist:**

- [x] `docs/product/stage-4/visual-design-direction.md`
- [x] `docs/product/stage-4/design-system-foundations.md`
- [x] `docs/product/stage-4/component-inventory.md`
- [x] `docs/product/stage-4/screen-specifications.md`
- [x] `docs/product/stage-4/interaction-patterns.md`
- [x] `docs/product/stage-4/stage-4-summary-and-carryover.md`
- [x] Stage 4.1 — Visual Design Consistency Review: `docs/product/stage-4/stage-4-consistency-review.md`
- [x] Product documentation index updated: `docs/product/README.md`
- [x] Root documentation map updated: `README.md`

**Key results:**

- Visual direction: modern, clean, calm, trusted, travel-oriented and AI-assisted.
- Chat-first, not chat-only: chat remains the main entry point, while structured hotel results are shown separately.
- Design system foundations describe semantic color roles, typography, spacing, layout, states and accessibility without final implementation tokens.
- MVP component inventory is hotel-first and marks flight/combined components as future expansion.
- Screen specifications cover active MVP screens and explicitly mark flight/combined screens as future.
- Interaction patterns define clarification, understood request, refinement, comparison, save/shortlist, loading, no results, partial/provider error, rationale and facts/assumptions separation.
- Stage 4.1 review confirmed alignment with Stage 0-3 and roadmap; no critical or major findings found.

**Open questions:**

- Final brand colors and design tokens.
- Hotel imagery availability from provider/API.
- Minimum source/freshness markers available from existing travel API.
- Session persistence level without authorization.
- Accessibility gates for frontend implementation.
- Whether confidence/rationale labels should appear at card, details or comparison level.

**Recommendations / carryover:**

- Use Stage 4 as input for Stage 5 Technical Architecture, but not as technical contract.
- Do not start frontend implementation until the appropriate roadmap stage.
- Convert component inventory into implementation backlog only after architecture and implementation preparation stages.
- Refine hotel card/details fields after existing travel API contract is provided.

## Future Stages

### Stage 5 — Technical Architecture

**Status:** Planned.

**Scope:** архитектурные границы, компоненты, AI/LLM abstraction, provider abstraction, backend/frontend/domain/integrations responsibilities.

### Stage 6 — Implementation Preparation

**Status:** Planned.

**Scope:** задачи реализации, контракты, тестовая стратегия, mock/fake providers, contract placeholders и локальный workflow.

### Stage 7 — MVP Implementation

**Status:** Planned.

**Scope:** реализация согласованного hotel-only MVP v1, включая интеграцию с существующим travel API для hotel offers после предоставления API-контракта. Flight search — следующий expansion после hotel flow; combined hotel+flight — более поздний expansion после flight flow.

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
