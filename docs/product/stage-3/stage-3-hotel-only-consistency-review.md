# Stage 3.5 — Hotel-Only UX Consistency Review

## 1. Назначение review

Проверить, что актуальная документация MVP v1 после hotel-only refocus больше не содержит активных требований к flight search, combined hotel+flight search, package ranking или преждевременным API/DB/provider implementation details.

Review являлся контрольным quality gate перед Stage 3 Summary & Carryover. Он не закрывал Stage 3 целиком, не начинал Stage 4 и не превращал UX-документы в implementation planning.

## 2. Scope review

Проверялись:

- `README.md`;
- `docs/PROJECT_BRIEF.md`;
- `docs/ARCHITECTURE.md`;
- `docs/ROADMAP.md`;
- `docs/roadmap/roadmap.md`;
- `docs/product/README.md`;
- `docs/product/stage-0/`;
- `docs/product/stage-1/`;
- `docs/product/stage-2/`;
- `docs/product/stage-3/`;
- `docs/development/`;
- `docs/decisions/README.md`.

## 3. Источники

Основные источники истины:

1. Текущая задача Stage 3.5.
2. `docs/roadmap/roadmap.md`.
3. `docs/product/stage-3/screen-map.md`.
4. `docs/product/stage-3/required-fields-and-acceptance-criteria.md`.
5. `docs/product/stage-3/mvp-search-flow-details.md`.
6. `docs/product/stage-3/combined-search-ux-decision.md`.
7. `docs/PROJECT_BRIEF.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`.
8. Historical Stage 0/1/2 product docs.
9. Development docs in `docs/development/`.

ADR не применялись: в `docs/decisions/` есть только index, ADR пока не созданы.

## 4. Current MVP v1 Baseline

Active MVP v1 является Hotel-Only MVP v1:

- natural-language hotel request;
- AI clarification;
- hotel results;
- ranking / explanation;
- hotel offer details;
- save / shortlist в текущей search session;
- basic hotel comparison.

Flight search исключен из MVP v1 и является next expansion после hotel flow.

Combined hotel + flight search исключен из MVP v1 и является later expansion после появления flight flow.

Stage 0/1/2 документы могут сохранять старые flight/combined идеи только как historical traceability, superseded context или future scope.

## 5. Review Method

Проверка включала:

- keyword search по risky terms;
- semantic review формулировок вокруг full trip planning, package, itinerary, transport, travel offers и provider orchestration;
- roadmap consistency check между `docs/roadmap/roadmap.md`, `docs/ROADMAP.md` и Stage 3 docs;
- Stage 3 document consistency check для screen map, required fields, search flow details и combined decision;
- historical/superseded separation check для Stage 0/1/2;
- architecture/development premature-detail check по API, DB, DTO, endpoints, provider adapters и implementation details.

Запускались команды:

```text
rg -n "flight|flights|ticket|tickets|combined|hotel \+ flight|Level 1|Level 2|Level 3|package|bundle|itinerary|transport|full trip|complete trip|end-to-end" README.md docs
rg -n "MVP|MVP v1|Hotel-Only|hotel-only|future scope|superseded|historical" README.md docs
rg -n "flight|combined|package|bundle|itinerary|transport|full trip|complete trip|tickets|перелет|перелетов|перелеты|билет|билеты|маршрут|пакет" docs/product/stage-0 docs/product/stage-1 docs/product/stage-2
rg -n "API|DTO|endpoint|database|schema|provider adapter|adapter design|OpenAPI|PostgreSQL|Redis|Ktor|Next.js|React" docs/product docs/roadmap/roadmap.md docs/ROADMAP.md docs/development README.md
git diff --check
```

## 6. Summary Verdict

**Passed with minor notes — можно было переходить к Stage 3 Summary & Carryover.**

Активный MVP v1 scope согласован как hotel-only. Critical и Major findings не обнаружены. Найденная minor inconsistency была исправлена во время review: нескольким Stage 1 historical documents добавлены явные top-level пометки о hotel-only refocus и future scope для flight/combined.

## 7. Findings

### 7.1 Critical Findings

Нет.

### 7.2 Major Findings

Нет.

### 7.3 Minor Findings

**MN-S3.5-001 — Несколько Stage 1 historical documents не имели явной top-level пометки о hotel-only refocus.**

Файлы `docs/product/stage-1/business-scenarios.md`, `docs/product/stage-1/non-functional-requirements.md` и `docs/product/stage-1/target-audience.md` содержали старый широкий продуктовый контекст, включая flight/combined mentions, но в отличие от соседних Stage 1 документов не имели верхнего scope update note. Это могло слабее отделять historical context от active MVP v1.

Статус: исправлено во время review.

**MN-S3.5-002 — Несколько Stage 3 documents ссылались на hotel-only UX review как на следующий шаг.**

После создания этого review документы `required-fields-and-acceptance-criteria.md`, `mvp-search-flow-details.md` и `combined-search-ux-decision.md` нуждались в навигационном обновлении, чтобы не отправлять следующий Stage 3 шаг обратно к уже завершенному review.

Статус: исправлено во время review.

### 7.4 Notes

**NT-S3.5-001 — Stage 2 review сохраняет историческую формулировку о Level 3 как Open for Stage 3.**

`docs/product/stage-2/stage-2-consistency-review.md` отражает состояние на момент Stage 2 review. Верхняя пометка уже объясняет, что последующее hotel-only решение supersedes flight/combined MVP recommendations. Это не конфликтует с текущим MVP v1.

**NT-S3.5-002 — Development docs содержат будущие API, backend, frontend, domain и provider tasks.**

`docs/development/roadmap.md`, `docs/development/milestones.md` и `docs/development/implementation-strategy.md` описывают будущую реализацию, но явно позиционируются как secondary/future planning и не являются разрешением начинать Stage 4/5/implementation. Это не блокирует Stage 3.

**NT-S3.5-003 — README и PROJECT_BRIEF остаются шире Hotel-Only MVP v1 на уровне долгосрочного продукта.**

Широкие формулировки про planning, routes, notes и future travel capabilities отделены от MVP v1 boundaries, где flight/combined исключены. Это допустимый долгосрочный контекст, не active MVP v1 requirement.

## 8. Hotel-Only MVP Consistency Check

Active Stage 3 documents consistently define MVP v1 as hotel-only:

- `screen-map.md` включает hotel request, hotel result cards, offer details, comparison и current-session shortlist.
- `required-fields-and-acceptance-criteria.md` задает required fields для hotel search и не задает active flight/combined acceptance criteria.
- `mvp-search-flow-details.md` описывает hotel search flow, refinement, save/shortlist и recovery states.
- `combined-search-ux-decision.md` помечен as superseded for MVP v1.

Hotel-only MVP baseline не конфликтует с roadmap, product brief или architecture notes.

## 9. Flight / Combined Scope Check

Активных требований MVP v1 к следующему не найдено:

- flight search;
- flight cards;
- flight provider behaviour;
- flight booking;
- combined hotel + flight search;
- package ranking;
- bundle optimization;
- dynamic packaging;
- end-to-end trip package;
- complete travel itinerary;
- full trip planning;
- transport search;
- multi-provider orchestration для flight/combined.

Все найденные mentions находятся в одном из безопасных контекстов:

- future expansion после hotel flow;
- later expansion после flight flow;
- historical traceability в Stage 0/1/2;
- explicit not-in-MVP / Post-MVP / Open boundary;
- future implementation roadmap, не текущий Stage 3 scope.

## 10. Stage 3 UX Documents Check

`docs/product/stage-3/screen-map.md`:

- Passed.
- Flight search и combined search явно не входят в MVP v1.
- Future flight/combined results отделены от hotel results.
- Документ не проектирует visual design, API, DB или implementation.

`docs/product/stage-3/required-fields-and-acceptance-criteria.md`:

- Passed.
- Active acceptance criteria относятся к hotel search, open destination clarification-first flow, AI clarification, results/error states и save/shortlist.
- Flight и combined sections явно future/later scope.

`docs/product/stage-3/mvp-search-flow-details.md`:

- Passed.
- Flow является hotel-first/hotel-only для MVP v1.
- Flight-only и combined intents получают future-scope fallback, а не search flow.
- Provider/API details остаются на уровне product facts and boundaries.

`docs/product/stage-3/combined-search-ux-decision.md`:

- Passed.
- Документ явно superseded for MVP v1.
- Future combined scope не подается как active MVP v1 requirement.

## 11. Roadmap and Navigation Check

`docs/roadmap/roadmap.md` согласован с Hotel-Only MVP v1:

- Current stage остается Stage 3.
- Code/API/DB/UI implementation не начаты.
- MVP Scope Note явно исключает flight и combined из MVP v1.
- Stage 3 dashboard содержит UX Consistency Review как завершенный gate.

`docs/ROADMAP.md` согласован:

- Stage 3 фиксирует hotel-only scope.
- Stage 5/6/7/9 упоминают provider/API topics как будущие этапы.

`docs/product/README.md` обновлен ссылкой на этот review.

## 12. Architecture / Development Docs Check

`docs/ARCHITECTURE.md`:

- Passed.
- Документ остается preliminary guidance до Stage 5.
- Flight provider abstraction относится к next expansion.
- Нет API contract, DB schema или provider adapter design.

`docs/development/roadmap.md`, `docs/development/milestones.md`, `docs/development/implementation-strategy.md`:

- Passed with notes.
- Они содержат будущие implementation areas, включая API contracts, backend/frontend skeleton, domain model и provider abstractions.
- Эти sections clearly future/secondary and do not override primary roadmap.
- Flight search abstraction помещен после hotel MVP v1 как next expansion.

## 13. Historical / Superseded Docs Check

Stage 0 documents already had top-level scope update notes.

Most Stage 1 documents already had top-level scope update notes. During review, equivalent notes were added to:

- `docs/product/stage-1/business-scenarios.md`;
- `docs/product/stage-1/non-functional-requirements.md`;
- `docs/product/stage-1/target-audience.md`.

Stage 2 documents already had top-level scope update notes. Some historical details remain intentionally preserved for traceability.

## 14. Changes Applied During Review

Created:

- `docs/product/stage-3/stage-3-hotel-only-consistency-review.md`.

Updated:

- `docs/product/README.md` — added Stage 3.5 review link.
- `docs/roadmap/roadmap.md` — marked hotel-only UX consistency review as completed and moved next step to Stage 3 Summary & Carryover.
- `docs/product/stage-3/required-fields-and-acceptance-criteria.md` — updated recommendation from pending review to completed review and carryover.
- `docs/product/stage-3/mvp-search-flow-details.md` — updated recommendation from pending review to completed review and carryover.
- `docs/product/stage-3/combined-search-ux-decision.md` — updated Stage 3 consequence from pending review to completed review and carryover.
- `docs/product/stage-1/business-scenarios.md` — added top-level MVP v1 scope update note.
- `docs/product/stage-1/non-functional-requirements.md` — added top-level MVP v1 scope update note.
- `docs/product/stage-1/target-audience.md` — added top-level MVP v1 scope update note.

## 15. Remaining Open Questions

- Какой объем provider-backed open destination discovery нужен в MVP v1, если он применим к hotel search?
- Какой MVP/Post-MVP split нужен для session persistence, resume и authorization?
- Когда и в каком виде будет предоставлен контракт существующего travel API для hotel offers?
- Какие freshness/source markers будут доступны из provider/API data?

## 16. Recommendations

- Notes из этого review перенесены в `docs/product/stage-3/stage-3-summary-and-carryover.md`.
- Stage 3 Summary & Carryover подготовлен без начала Stage 4.
- Если session persistence / resume / authorization остаются relevant для MVP v1, зафиксировать только MVP/Post-MVP split, не проектируя DB schema или auth architecture.
- На будущих технических этапах сохранить запрет на flight/combined implementation до отдельного expansion после hotel flow.

## 17. Что намеренно не делалось

- Не переписывались Stage 0/1/2 documents целиком.
- Не удалялся historical context.
- Не закрывался Stage 3 целиком.
- Не начинался Stage 4 Visual Design / UI Concept.
- Не создавались visual design, wireframes или UI kit.
- Не создавался React/Next.js/Kotlin/Ktor code.
- Не создавались API contracts, endpoints, DTO, database schema или provider adapters.
- Не добавлялись новые MVP features.
- Flight/combined не возвращались в MVP v1.
- Review не превращался в implementation planning.
