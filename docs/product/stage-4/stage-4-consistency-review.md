# Stage 4.1 — Visual Design Consistency Review

## 1. Назначение документа

Документ фиксирует consistency review Stage 4 Visual Design & UX System относительно Stage 0-3 и primary roadmap.

Review проверяет, что Stage 4:

- сохраняет Hotel-Only MVP v1;
- не возвращает flight, combined, booking, account history или полноценную authorization в MVP;
- не противоречит Stage 3 UX baseline;
- достаточно явно разделяет provider facts, assistant assumptions, user-provided constraints и unknown data;
- не создает скрытых implementation commitments;
- корректно формулирует accessibility, responsive readiness и carryover.

Документ не начинает Stage 5 Technical Architecture, не создает архитектурные документы, implementation backlog, API contracts, UI components или production code.

## 2. Источники проверки

Проверены:

- `README.md`;
- `docs/product/README.md`;
- `docs/roadmap/roadmap.md`;
- `docs/product/stage-0/`;
- `docs/product/stage-1/`;
- `docs/product/stage-2/`;
- `docs/product/stage-3/`;
- `docs/product/stage-4/`.

## 3. Executive Verdict

**Verdict: Passed with minor wording fixes.**

Stage 4 согласован с Stage 3 Hotel-Only MVP v1 baseline и primary roadmap. Critical или Major findings не обнаружены.

Найдены minor wording risks:

- заголовок `Combined composition` мог быть ошибочно прочитан как ссылка на combined travel flow;
- категория `user-provided constraints` была менее явно отделена от provider facts / assistant assumptions / unknown data, чем это требуется из Stage 2-3.

Обе проблемы исправлены точечными формулировочными правками без изменения scope.

## 4. Alignment with MVP Scope

### 4.1 Проверка

Stage 4 consistently сохраняет Hotel-Only MVP v1:

- `visual-design-direction.md` прямо фиксирует, что актуальный MVP v1 ограничен hotel search.
- `component-inventory.md` помечает `Hotel Offer Card` как MVP v1, а `Flight Offer Card` как Future expansion.
- `screen-specifications.md` отделяет active MVP screens от future flight/combined screens.
- `interaction-patterns.md` описывает fallback for flight/combined/booking/account history expectations.
- `stage-4-summary-and-carryover.md` повторяет, что MVP v1 остается hotel-only.

Flight, combined, booking, account-level saved trips/history и полноценная authorization не добавлены как active MVP v1 scope.

### 4.2 Scope risks

| ID | Risk | Severity | Resolution |
|---|---|---|---|
| SR-S4.1-001 | Future flight/combined sections in `screen-specifications.md` could be skimmed as planned active screens if reader ignores `MVP status`. | Low | No structural change required; sections already say Future expansion / not active MVP v1. |
| SR-S4.1-002 | `Combined composition` heading in `visual-design-direction.md` could be confused with combined hotel+flight flow. | Low | Fixed: renamed to `Combined chat/results composition`. |
| SR-S4.1-003 | Saved UI and session persistence mentions could be misread as account history if detached from current-session wording. | Low | Existing wording repeatedly says current search session; no additional edit required. |

## 5. Alignment with Stage 3 UX Baseline

### 5.1 Chat-first, but not chat-only

Stage 4 aligns with Stage 3:

- Stage 3 requires AI chat as main entry point and structured results outside long chat text.
- Stage 4 repeats this as `Chat-first, not chat-only`.
- Results area responsibilities in Stage 4 match Stage 3 Results Overview and Hotel Result Cards.

No contradiction found.

### 5.2 Search Intent Summary

Search Intent Summary aligns with Stage 3 Clarification Area and Trip Parameters / Clarification Area:

- shows extracted fields;
- shows missing fields;
- shows assumptions;
- supports changed/stale state after refinement;
- preserves active search session context.

Minor improvement made: Search Intent Summary now explicitly includes user-provided constraints as a separate category.

### 5.3 Hotel Offer Card and Results View

Hotel Offer Card aligns with Stage 3:

- hotel name, location, price, dates, rating/review score, amenities and reason summary are supported where provider data exists;
- unknown/source/freshness markers are included;
- save, details and comparison actions are scoped to current session;
- booking/payment is absent.

Results View aligns with Stage 3 requirement that structured hotel results appear next to or below chat depending on viewport.

### 5.4 UX/design consistency issues

No critical or major UX/design consistency issues found.

Minor notes:

- Future flight/combined screens are useful as carryover context but must remain visually separated from active MVP v1 screens in future design tasks.
- Comparison is correctly limited to 2-5 hotel offers; future implementation should avoid broad comparison tables on mobile.

## 6. AI / Provider Data Clarity

### 6.1 Проверка категорий данных

Stage 4 clearly covers:

- **provider facts:** price, currency, hotel name, location, rating/review score, amenities, cancellation policy, source/freshness when provider returns them;
- **assistant assumptions:** interpretation of cheap/quiet/central, room default, budget tier, preference priority, inferred travel style;
- **unknown or unavailable data:** missing provider fields, stale data, unsupported facts, unavailable source/freshness;
- **user-provided constraints:** now explicitly separated in Stage 4 wording after review.

### 6.2 UX risk zones

| Risk zone | Why it matters | Carryover |
|---|---|---|
| Confidence/rationale labels | Labels like "Strong match" can imply too much certainty if provider data is partial. | Define wording and thresholds before implementation. |
| Hotel photos/imagery | Images may be provider facts, cached media or absent data; visual design must not imply verified quality if source is unclear. | Revisit after provider contract. |
| Source/freshness markers | Stage 4 asks for them, but provider capabilities are unknown. | Stage 5/API contract stage should preserve source/freshness state without inventing fields. |
| User-provided constraints | Visa/accessibility constraints can be user-supplied but not provider-verified. | Keep separate from provider facts and legal advice. |
| Stale saved items | Saved offer snapshot can look current if stale warning is weak. | Future UI should make stale/freshness visible at card/details level. |

## 7. Hidden Implementation Commitments

### 7.1 Frontend framework

Stage 4 mentions Next.js + React + Tailwind + shadcn/ui only as a preliminary repository hypothesis and explicitly says Stage 4 does not approve the stack or create artifacts.

**Finding:** No hidden framework commitment.

### 7.2 Components

Stage 4 names future UI components as inventory, not implementation contracts. `component-inventory.md` says it does not define props/API or choose a library.

**Finding:** No hidden component implementation commitment.

### 7.3 Design tokens and HEX colors

Stage 4 uses draft palette, semantic roles and token ideas. It explicitly says final HEX colors and final design tokens are not approved.

**Finding:** No hidden final token commitment.

### 7.4 API structure

Stage 4 references provider/API only as source of hotel facts and future contract dependency. It does not define endpoints, DTOs, OpenAPI, provider adapters or database schema.

**Finding:** No hidden API structure commitment.

### 7.5 Authorization, persistence and account history

Stage 4 keeps save/shortlist within current search session and lists session persistence/account history as open/carryover.

**Finding:** No hidden authorization/account-history commitment.

### 7.6 Booking flow

Stage 4 repeatedly excludes booking/payment CTA, booking flow and price/availability guarantees.

**Finding:** No hidden booking commitment.

## 8. Accessibility and Responsive Readiness

### 8.1 Accessibility

Stage 4 covers:

- keyboard accessibility;
- visible focus states;
- WCAG AA contrast direction;
- non-color-only status communication;
- accessible names for controls;
- reduced motion;
- screen reader considerations for chat and dynamic results.

**Gap:** Stage 4 does not define concrete acceptance gates or test checklist. This is acceptable because the task is visual/product documentation, not implementation preparation.

### 8.2 Responsive readiness

Stage 4 aligns with web-first / desktop-first / mobile-aware baseline:

- desktop supports chat + results workspace;
- mobile uses stacked/tabs/bottom-sheet patterns as future implementation options;
- mobile remains first-class adaptation, not separate mobile app scope;
- responsive behaviour preserves the same product logic and data-confidence distinctions.

No conflict with future cross-platform direction found.

## 9. Carryover Quality

Carryover is correctly formulated as open questions and future-stage input, not current tasks.

Good carryover examples:

- final brand colors/design tokens;
- hotel imagery availability;
- source/freshness markers from provider/API;
- session persistence level without authorization;
- accessibility gates for frontend implementation;
- confidence/rationale label placement.

No carryover item was converted into implementation backlog or Stage 5 work inside Stage 4.

## 10. Wording Fixes Applied

Minimal wording fixes made during review:

- `visual-design-direction.md`: renamed `Combined composition` to `Combined chat/results composition`.
- `visual-design-direction.md`: added `user-provided constraints` to visual treatment and visual decisions.
- `design-system-foundations.md`: added `user-provided constraints` to design system goals and responsive distinction language.
- `component-inventory.md`: added `user-provided constraints` to Search Intent Summary.
- `screen-specifications.md`: added `user-provided constraints` to cross-screen data distinction rule.
- `interaction-patterns.md`: added `user-provided constraints` to understood request and expanded Facts/Constraints/Assumptions pattern.
- `stage-4-summary-and-carryover.md`: added `user-provided constraints` to key design decision and Stage 5 carryover wording.

No mass rewrite was performed.

## 11. Remaining Open Questions

- Which source/freshness markers are available in the existing travel API?
- Whether confidence/rationale labels should be card-level, details-level or comparison-only.
- How to handle hotel photos if provider media is absent, stale or partial.
- What minimum session persistence is available without authorization.
- What accessibility checks become gates on frontend implementation stages.

## 12. Recommendations, Not Executed

- Do not start Stage 5 until explicitly requested.
- On Stage 5, preserve domain/application separation for provider facts, assistant assumptions, user-provided constraints and unknown data.
- On future design task, keep future flight/combined screens visually and navigationally separated from active MVP screens.
- After provider contract is available, revisit hotel card/details fields, imagery, source/freshness and stale-state UX.
- During implementation preparation, create accessibility and responsive QA checklist from Stage 4 foundations.

