# Stage 2 Consistency & Documentation Structure Review

> MVP v1 scope update: этот review отражает состояние Stage 2 на момент его проведения. Последующее решение о hotel-only MVP v1 supersedes flight/combined MVP recommendations; они остаются historical traceability и future scope.

## Цель review

Проверить согласованность Stage 2 с Stage 0, Stage 1, Stage 1 Scope Correction и текущей roadmap-структурой. Review оценивает полноту use cases, edge cases, assistant behaviour rules, combined search decision, provider/API data handling, структуру документации, навигацию, дублирование и языковую согласованность.

Review не выполняет Stage 3, не создает API contracts, DTO, OpenAPI, database schema, UI wireframes, provider adapters или код.

## Scope review

В scope входят:

- Stage 2 product documents;
- Stage 0 и Stage 1 как источники traceability;
- primary roadmap и secondary roadmap/index документы;
- MVP-scope по существующему travel API;
- documentation structure, navigation, duplication и language consistency.

Вне scope:

- исправление смысловых требований;
- удаление или объединение документов;
- проектирование API/DB/UI/architecture;
- Stage 3 artifacts;
- implementation work.

## Reviewed documents

- `README.md`
- `docs/product/README.md`
- `docs/roadmap/roadmap.md`
- `docs/ROADMAP.md`
- `docs/development/roadmap.md`
- `docs/development/milestones.md`
- `docs/development/implementation-strategy.md`
- `docs/product/stage-0/product-framing.md`
- `docs/product/stage-0/initial-scenarios.md`
- `docs/product/stage-0/mvp-boundaries.md`
- `docs/product/stage-0/assumptions-and-open-questions.md`
- `docs/product/stage-1/target-audience.md`
- `docs/product/stage-1/business-scenarios.md`
- `docs/product/stage-1/user-journeys.md`
- `docs/product/stage-1/business-requirements.md`
- `docs/product/stage-1/functional-requirements.md`
- `docs/product/stage-1/non-functional-requirements.md`
- `docs/product/stage-1/assumptions-and-open-questions.md`
- `docs/product/stage-1/stage-1-summary.md`
- `docs/product/stage-1/stage-1-consistency-review.md`
- `docs/product/stage-2/use-cases.md`
- `docs/product/stage-2/edge-cases.md`
- `docs/product/stage-2/assistant-behaviour-rules.md`
- `docs/product/stage-2/combined-search-levels.md`
- `docs/product/stage-2/data-requirements.md`
- `docs/product/stage-2/stage-2-summary.md`
- `docs/PROJECT_BRIEF.md`
- `docs/ARCHITECTURE.md`
- `docs/decisions/README.md`
- `docs/prompts/codex-rules.md`

## Executive summary

Stage 2 в целом согласован со Stage 0, Stage 1 и Stage 1 Scope Correction. Use cases, edge cases, assistant behaviour rules, combined search levels и data requirements покрывают ожидаемый продуктовый уровень и не переходят в реализацию. Реальная интеграция с существующим travel API сохранена в MVP-scope, а mock/fake providers и contract placeholders описаны как временные средства разработки.

Критических и major-блокеров не найдено. Основные замечания minor: в Stage 2 заметно смешение английских и русских формулировок, а roadmap/development документы имеют допустимое, но потенциально рискованное пересечение ролей, которое нужно держать под контролем через primary roadmap.

## Overall verdict

**Passed with minor notes.**

## Stage 2 consistency findings

- UC-01 - UC-15 присутствуют.
- EC-001 - EC-035 присутствуют.
- ABR-001 - ABR-018 присутствуют.
- Stage 2 не добавляет новые BR/FR и явно фиксирует это в `stage-2-summary.md`.
- UC-15 корректно фиксирует, что финальный MVP использует реальные travel offers из существующего API/provider, а не LLM-generated facts.
- Booking/payment/legal advice не включены в MVP как поддерживаемые действия; fallback на такие запросы включен как безопасное поведение.
- Stage 2 не содержит API contract, DTO, OpenAPI, database schema, UI mockups, provider adapter design или code.

## Traceability review

| Area | Status | Notes |
|---|---|---|
| UC -> Stage 1 scenarios | Passed | UC-01 - UC-10 напрямую разворачивают S-01 - S-10; UC-11 - UC-15 следуют из Stage 1 open questions, risks и Scope Correction. |
| UC -> BR | Passed | Каждый UC содержит linked BR IDs; новых BR не добавлено. |
| UC -> FR | Passed | Каждый UC содержит linked FR IDs; новых FR не добавлено. |
| UC -> EC | Passed | Use cases ссылаются на релевантные EC IDs. |
| EC -> UC | Passed | Каждый edge case имеет related use cases. |
| ABR -> UC | Passed | Каждое правило ABR содержит related use cases. |
| Provider facts / assumptions / unknown data | Passed | Разделение последовательно повторяется в use cases, edge cases, rules и data requirements. |

## Combined search review

Combined search разделен на четыре уровня:

- Level 1 — Combined intent recognition.
- Level 2 — Same-dialog hotel and flight assistance.
- Level 3 — Coordinated combined search.
- Level 4 — Full combined package ranking.

Рекомендация Stage 2:

- Level 1 — historical Stage 2 recommendation; superseded for MVP v1.
- Level 2 — historical Stage 2 recommendation; superseded for MVP v1.
- Level 3 — Open for Stage 3.
- Level 4 — Post-MVP/Open.

**Verdict:** MJ-S1-001 закрыт достаточно явно для Stage 2. Level 3 не включен в MVP без решения, Level 4 не описан как обязательный MVP. Риск остается только в будущей Stage 3 формализации acceptance criteria.

## Provider/API data handling review

Проверено:

- provider/API data является primary source of truth для цен, availability, расписаний, параметров offers, ограничений и freshness;
- assistant assumptions отделены от provider facts;
- unknown data явно описаны;
- LLM/assistant не назначен источником travel facts;
- mock/fake providers и contract placeholders описаны как временные средства разработки;
- отсутствие API-контракта зафиксировано как Open input для будущих технических этапов, а не как Post-MVP перенос интеграции;
- реальная интеграция с существующим travel API сохранена в MVP-scope.

**Verdict:** Passed.

## Documentation structure review

Текущие роли документов:

- **Primary roadmap:** `docs/roadmap/roadmap.md`.
- **Secondary roadmap:** `docs/ROADMAP.md`.
- **Development roadmap:** `docs/development/roadmap.md`.
- **Milestones:** `docs/development/milestones.md`.
- **Implementation strategy:** `docs/development/implementation-strategy.md`.
- **Product index:** `docs/product/README.md`.
- **Root navigation:** `README.md`.
- **Architecture context:** `docs/ARCHITECTURE.md`.
- **Project brief:** `docs/PROJECT_BRIEF.md`.
- **ADR index:** `docs/decisions/README.md`; ADR пока не созданы.
- **Codex rules:** `docs/prompts/codex-rules.md`.

Структура понятна после Roadmap Progress Structure Cleanup. Primary roadmap содержит статусы, чеклисты, open decisions, carryover и следующий шаг. Secondary документы ссылаются на primary roadmap и не должны использоваться как источник текущего статуса.

## Duplicate/overlapping documents review

| Files | Type | Description | Recommendation | Status |
|---|---|---|---|---|
| `docs/roadmap/roadmap.md`, `docs/ROADMAP.md` | acceptable overlap | Оба перечисляют этапы, но primary roadmap содержит прогресс, а `docs/ROADMAP.md` оставлен как краткий список этапов. | Оставить как есть; поддерживать ссылку на primary roadmap. | Open / monitor |
| `docs/roadmap/roadmap.md`, `docs/development/roadmap.md` | acceptable overlap with risk | Оба описывают развитие проекта, но development roadmap детализирует будущие implementation-задачи. Риск: development roadmap может устаревать относительно primary roadmap. | Не объединять сейчас; статусы и stage gates вести только в primary roadmap. | Fixed in follow-up |
| `docs/development/roadmap.md`, `docs/development/milestones.md` | acceptable overlap | Оба описывают будущую реализацию: roadmap как порядок задач, milestones как контрольные точки. | Оставить как есть. | Open / monitor |
| `docs/PROJECT_BRIEF.md`, `docs/product/stage-0/*`, `docs/product/stage-1/*` | acceptable historical overlap | Project brief содержит краткое резюме, stage-документы — детальную историю решений. | Оставить как контекстный документ; не использовать вместо stage docs. | Open / monitor |
| `docs/ARCHITECTURE.md`, `docs/development/implementation-strategy.md` | acceptable overlap with risk | Оба упоминают архитектурные границы и technical guardrails. | На Stage 5 пересмотреть и при необходимости вынести финальные решения в ADR/design docs. | Deferred |

## Navigation/index review

Навигация в целом достаточна:

- `README.md` показывает root navigation и текущий следующий шаг.
- `docs/product/README.md` индексирует Stage 0/1/2 product docs.
- `docs/roadmap/roadmap.md` явно объявлен primary roadmap.
- `docs/ROADMAP.md`, development roadmap, milestones и implementation strategy ссылаются на primary roadmap.
- `docs/decisions/README.md` объясняет статус ADR.

Минимальная безопасная правка выполнена в рамках review: ссылка на этот review добавлена в `docs/product/README.md`, а primary roadmap обновлен после создания review.

## Language consistency review

Основной язык документации — русский. Английский используется для устоявшихся терминов и ID, что допустимо. На момент review было найдено minor-смешение английских фраз в Stage 2 таблицах и названиях колонок.

Примеры:

- `docs/product/stage-2/use-cases.md`: `Hotel search by natural language request`, `Linked BR IDs`, `Main flow`, `Expected result`.
- `docs/product/stage-2/edge-cases.md`: `Provider latency too high`, `Expected assistant behaviour`, `Fallback behaviour`.
- `docs/product/stage-2/assistant-behaviour-rules.md`: `Rule`, `Rationale`, `Examples`, `Related use cases`.
- `docs/product/stage-2/data-requirements.md`: `Required for MVP`, `Why needed`, `Notes`.

Severity: minor. Массово переписывать в рамках review не требовалось, потому что термины понятны и не меняли смысл. В follow-up cleanup служебные заголовки и длинные английские фразы были точечно приведены к русской основе без изменения требований.

## Roadmap/progress review

Проверено:

- `docs/roadmap/roadmap.md` действительно является primary roadmap.
- Stage 0, Stage 1 и Stage 2 отмечены Completed.
- Stage 1 Consistency Review, Stage 1 Follow-up Cleanup и Stage 1 Scope Correction отмечены Completed.
- До создания этого review Stage 2 Consistency Review был unchecked.
- Stage 3 не начат.
- MVP Scope Note по реальному API сохранен.
- Open decisions и carryover не потеряны.

После создания review primary roadmap обновлен: Stage 2 Consistency Review отмечен выполненным, Stage 3 остается Not started / Planned.

## Findings table

| ID | Severity | Title | Files affected | Status |
|---|---|---|---|---|
| MN-S2-001 | Minor | Смешение английских и русских формулировок в Stage 2 таблицах | `docs/product/stage-2/*` | Fixed in follow-up |
| MN-S2-002 | Minor | Потенциально рискованное пересечение primary/development roadmap | `docs/roadmap/roadmap.md`, `docs/development/roadmap.md`, `docs/development/milestones.md` | Fixed in follow-up |
| MN-S2-003 | Minor | Stage 2 review отсутствовал в product index и roadmap до этой задачи | `docs/product/README.md`, `docs/roadmap/roadmap.md` | Fixed in this review |
| NT-S2-001 | Note | Stage 2 не добавляет новые BR/FR | `docs/product/stage-2/stage-2-summary.md` | Open / informational |
| NT-S2-002 | Note | Документация сохраняет MVP-scope реального travel API | Stage 1/2 docs, roadmap docs | Open / informational |

## Critical findings

Нет.

## Major findings

Нет.

## Minor findings

### MN-S2-001. Смешение английских и русских формулировок в Stage 2 таблицах

**Severity:** Minor.

**Files affected:** `docs/product/stage-2/use-cases.md`, `docs/product/stage-2/edge-cases.md`, `docs/product/stage-2/assistant-behaviour-rules.md`, `docs/product/stage-2/data-requirements.md`.

**Description:** документы в основном на русском, но таблицы и поля используют английские заголовки и местами длинные английские фразы.

**Impact:** не блокирует Stage 3, но снижает единообразие чтения.

**Recommendation:** выполнить отдельный language cleanup: сохранить устоявшиеся термины и ID, но перевести длинные служебные заголовки и фразы.

**Status:** Fixed in follow-up.

### MN-S2-002. Потенциально рискованное пересечение primary/development roadmap

**Severity:** Minor.

**Files affected:** `docs/roadmap/roadmap.md`, `docs/development/roadmap.md`, `docs/development/milestones.md`.

**Description:** primary roadmap и development roadmap/milestones частично описывают одни и те же будущие области. Сейчас роли документов обозначены, но при дальнейшем развитии возможен drift.

**Impact:** риск будущих противоречий по статусам или порядку работ.

**Recommendation:** считать `docs/roadmap/roadmap.md` единственным источником текущего прогресса; development docs использовать только для детализации будущей реализации.

**Status:** Fixed in follow-up.

### MN-S2-003. Stage 2 review отсутствовал в product index и roadmap до этой задачи

**Severity:** Minor.

**Files affected:** `docs/product/README.md`, `docs/roadmap/roadmap.md`.

**Description:** до создания review ссылка и статус Stage 2 Consistency Review отсутствовали как выполненные, что было ожидаемо до этой задачи.

**Impact:** после создания review индексы должны быть обновлены, чтобы навигация оставалась полной.

**Recommendation:** добавить ссылку на review и отметить follow-up как completed.

**Status:** Fixed in this review.

## Notes

### NT-S2-001. Новые BR/FR не добавлялись

Stage 2 развернул существующие BR-001 - BR-016 и FR-001 - FR-014. UC-15 следует из Stage 1 Scope Correction и не требует нового BR/FR.

### NT-S2-002. MVP-scope реального travel API сохранен

Review не нашел mock-only формулировок в Stage 2 и roadmap. Реальная интеграция с существующим travel API остается частью MVP.

## Fixed in this review

- Создан `docs/product/stage-2/stage-2-consistency-review.md`.
- Добавлена ссылка на Stage 2 Consistency Review в `docs/product/README.md`.
- В `docs/roadmap/roadmap.md` Stage 2 Consistency Review отмечен выполненным.
- В `docs/roadmap/roadmap.md` следующий шаг обновлен на Stage 3, при этом Stage 3 остается Not started / Planned.
- В `README.md` и `docs/ROADMAP.md` следующий шаг обновлен после завершения review.

## Follow-up cleanup result

Stage 2 Minor Cleanup — Language & Roadmap Navigation Polish выполнен после этого review.

- MN-S2-001 закрыт: в Stage 2 документах переведены длинные служебные заголовки и английские фразы, при этом ID, traceability, MVP status, provider/API data handling и combined search decision сохранены.
- MN-S2-002 закрыт: `docs/roadmap/roadmap.md` дополнительно подтвержден как единственный primary roadmap; `docs/ROADMAP.md`, `docs/development/roadmap.md`, `docs/development/milestones.md` и `docs/development/implementation-strategy.md` уточнены как secondary/navigation/development documents, которые не ведут конкурирующие статусы.
- MN-S2-003 оставлен как Fixed in this review.
- Stage 3 остается следующим шагом и не начат в рамках cleanup.

## Deferred follow-up actions

- На Stage 3 финализировать MVP boundaries и acceptance criteria.
- Stage 3 later superseded Level 3 for MVP v1 through hotel-only scope refocus.
- На Stage 3 уточнить required fields per intent.
- После получения API-контракта выполнить отдельную техническую проработку provider/API contract usage без ретроактивного изменения Stage 2.
- На Stage 5 пересмотреть `docs/ARCHITECTURE.md` и решить, нужны ли ADR/design docs по provider integration и orchestration.

## Readiness for Stage 3

**Ready.**

Stage 2 Minor Cleanup выполнен. Блокеров, critical или major findings не найдено. Stage 3 можно начинать отдельной задачей; в рамках этого cleanup Stage 3 не начат.

## Recommendations

- Не выполнять Stage 3 в рамках этого review.
- В Stage 3 использовать Stage 2 UC/EC/ABR/data requirements как вход для MVP boundaries и acceptance criteria.
- Продолжать считать `docs/roadmap/roadmap.md` primary roadmap.
