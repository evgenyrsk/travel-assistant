# Review согласованности документации перед Stage 6

## Статус документа

Этот документ является audit trail и quality gate по согласованности документации перед Stage 6. Он фиксирует findings, verdict и рекомендации на момент review.

Рекомендации в этом документе не являются активным backlog, roadmap или разрешением выполнять cleanup автоматически. Любая рекомендация требует отдельной явной задачи и должна оставаться согласованной с `docs/roadmap/roadmap.md`.

Stage 6 остается в статусе `Planned / not started`. Этот review не начинает Stage 6, не создает implementation backlog, API/OpenAPI contracts, DB schema/storage model, auth/security/DevOps/testing backlog или production implementation.

## 1. Контекст review

Этот review проведен перед началом Stage 6, чтобы проверить согласованность документационного baseline после завершения Stage 0 - Stage 5.

Ожидаемое состояние проекта на момент review:

- Stage 0 - Stage 5 завершены.
- Stage 6 запланирован, но не начат.
- Реализация Code/API/DB/UI не начата.
- Эта задача является review-only.
- Продуктовые и архитектурные решения в рамках этого review не менялись.

Review сфокусирован на согласованности документации, ясности roadmap, навигации, выравнивании статусов и защите от преждевременного implementation/API/DB/storage/auth/DevOps backlog.

## 2. Scope review

Проверенные зоны:

- Root docs: `README.md`, `docs/PROJECT_BRIEF.md`, `docs/ROADMAP.md`, `docs/ARCHITECTURE.md`.
- Roadmap docs: `docs/roadmap/roadmap.md`.
- Product docs: `docs/product/README.md`, `docs/product/stage-0/*`, `docs/product/stage-1/*`, `docs/product/stage-2/*`, `docs/product/stage-3/*`, `docs/product/stage-4/*`.
- Architecture docs: `docs/architecture/stage-5/*`.
- Decisions docs: `docs/decisions/README.md`, `docs/decisions/*`.
- Development docs: `docs/development/roadmap.md`, `docs/development/milestones.md`, `docs/development/implementation-strategy.md`.
- Prompts/agent docs: `docs/prompts/*`, `AGENTS.md`.
- GitHub templates, связанные с процессом задач: `.github/ISSUE_TEMPLATE/codex_task.yml`, `.github/pull_request_template.md`.

`docs/architecture/README.md` на момент review отсутствовал, поэтому навигация по архитектуре проверялась через `README.md`, `docs/product/README.md` и `docs/roadmap/roadmap.md`.

## 3. Ожидаемый baseline

- Stage 0 - Completed.
- Stage 1 - Completed.
- Stage 2 - Completed.
- Stage 3 - Completed.
- Stage 4 - Completed.
- Stage 4.1 - Completed.
- Stage 5 - Completed.
- Stage 6 - Planned / not started.
- MVP v1 - hotel-only.
- Flights / combined itinerary / booking / payment - outside MVP.
- Только current-session shortlist.
- Account history пока нет.
- Production implementation пока нет.
- OpenAPI/API contracts пока нет, если они явно не запланированы на будущий этап.
- DB schema/storage model пока нет, если они явно не запланированы на будущий этап.
- Provider facts должны приходить от providers, а не от LLM.
- LLM может интерпретировать, объяснять, ранжировать, резюмировать и уточнять, но не должен выдумывать provider facts.
- Разделение facts / assumptions / unknowns должно оставаться явным.

## 4. Summary findings

| Severity | Count |
|---|---:|
| Critical | 0 |
| Major | 2 |
| Minor | 4 |
| Notes | 3 |

Определения severity:

- Critical: противоречие, которое может сломать roadmap, MVP scope или привести к преждевременному старту Stage 6/implementation.
- Major: существенное расхождение между документами, статусами, MVP boundaries или architecture baseline.
- Minor: навигационная, ссылочная, структурная или wording-проблема, которая не меняет смысл, но ухудшает читаемость и поддержку.
- Note: наблюдение или рекомендация без необходимости немедленного исправления.

## 5. Detailed findings

### [MJ-001] Secondary development roadmap читается как активный implementation backlog

Severity: Major  
Area: Development docs / Cross-doc  
Files:

- `docs/development/roadmap.md`
- `docs/development/milestones.md`
- `docs/development/implementation-strategy.md`
- `docs/roadmap/roadmap.md`

Finding:
Development roadmap и milestones содержат конкретные future implementation areas: API contracts, backend skeleton, LLM abstraction, hotel search abstraction, web skeleton, UI, tests, security и local development. Файлы указывают, что primary roadmap является source of truth, а development roadmap является secondary, но уровень детализации все еще может быть ошибочно воспринят как активный backlog до начала Stage 6 planning.

Why it matters:
Baseline проекта говорит, что Stage 6 находится в статусе `Planned / not started` и implementation еще не начат. Детализированный implementation-oriented roadmap может подтолкнуть будущие задачи к преждевременному старту API/frontend/backend/storage/security work.

Recommendation:
В отдельной cleanup-задаче уточнить, что development roadmap и milestones являются справочными материалами для будущей реализации до тех пор, пока Stage 6 planning явно не выберет и не ограничит work. Сохранить их подчинение `docs/roadmap/roadmap.md` и не трактовать их пункты как текущие задачи.

Allowed timing:
Before Stage 6.

### [MJ-002] Wording Stage 6 про contracts может читаться слишком широко

Severity: Major  
Area: Roadmap / Root docs / Cross-doc  
Files:

- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/development/roadmap.md`
- `docs/development/implementation-strategy.md`

Finding:
Stage 6 корректно отмечен как `Planned / not started`, но в нескольких местах future preparation описана через wording вроде "контракты", "API Contracts" или "подготовить контракты". Architecture baseline говорит, что API/OpenAPI contracts или DB schema пока не существуют, а existing travel API contract должен быть предоставлен до concrete provider mapping. Текущее wording не является прямым противоречием, но достаточно широкое, чтобы его можно было прочитать как разрешение создавать OpenAPI/API contracts сразу при старте Stage 6.

Why it matters:
Stage 6 не должен превращаться в преждевременную работу по API/OpenAPI, DB schema, storage model, auth, DevOps или production implementation. Contract preparation должен оставаться ограниченным existing travel API contract и явным Stage 6 planning.

Recommendation:
Перед Stage 6 добавить узкое уточнение в roadmap/development docs: Stage 6 contract work означает только scoped preparation и alignment, а не разрешает создавать новые OpenAPI/API contracts, DB schema или storage model, если отдельная Stage 6 planning task явно не определит такой scope.

Allowed timing:
Before Stage 6.

### [MN-001] Новая reviews-зона требует root navigation

Severity: Minor  
Area: Root docs / Navigation  
Files:

- `README.md`
- `docs/reviews/pre-stage-6-documentation-consistency-review.md`

Finding:
Root documentation map в целом полезен, но `docs/reviews/` является новой зоной и требует navigation entry, чтобы pre-Stage 6 review был обнаружим из входной точки репозитория. Минимальная ссылка в root README была добавлена в рамках этой review-задачи.

Why it matters:
Review должен направлять следующий controlled cleanup и решение о Stage 6 planning. Если он не виден из root documentation map, будущие agents могут его пропустить.

Recommendation:
Сохранить ссылку root README на этот review-документ. Если позже будет добавлен более широкий reviews index, сослаться на него без переписывания unrelated documentation.

Allowed timing:
Before Stage 6.

### [MN-002] У architecture docs нет локального README/index

Severity: Minor  
Area: Architecture docs / Navigation  
Files:

- `docs/architecture/stage-5/*`
- `docs/product/README.md`
- `docs/roadmap/roadmap.md`

Finding:
`docs/architecture/README.md` отсутствует. Stage 5 deliverables обнаружимы через `docs/product/README.md` и `docs/roadmap/roadmap.md`, но architecture-local index отсутствует.

Why it matters:
Stage 5 теперь завершен и содержит architecture baseline для будущего planning. Локальный architecture index уменьшит навигационное трение без изменения architecture decisions.

Recommendation:
Рассмотреть добавление architecture README/index в отдельной documentation cleanup-задаче, ограничив его ссылками и status/navigation.

Allowed timing:
Before Stage 6.

### [MN-003] Status в Decisions README технически корректен, но устарел

Severity: Minor  
Area: Decisions  
Files:

- `docs/decisions/README.md`
- `docs/architecture/stage-5/architecture-decisions-draft.md`

Finding:
`docs/decisions/README.md` говорит, что ADRs не созданы, и ссылается на контекст Stage 0. Это технически верно, потому что ADR-файлов нет, но после Stage 5 появился architecture decisions draft с confirmed guardrails, deferred decisions и future ADR candidates.

Why it matters:
Будущие contributors могут не понять различие между "ADR files yet нет" и "Stage 5 содержит decision inventory / ADR candidate draft".

Recommendation:
В отдельной cleanup-задаче обновить `docs/decisions/README.md`: указать, что standalone ADR files пока нет, а Stage 5 содержит non-ADR architecture decision inventory.

Allowed timing:
Before Stage 6.

### [MN-004] Historical Stage 1/2 MVP labels требуют внимательного чтения

Severity: Minor  
Area: Product docs  
Files:

- `docs/product/stage-1/business-requirements.md`
- `docs/product/stage-1/functional-requirements.md`
- `docs/product/stage-1/user-journeys.md`
- `docs/product/stage-2/combined-search-levels.md`
- `docs/product/README.md`
- `docs/roadmap/roadmap.md`

Finding:
Документы Stage 1 и Stage 2 сохраняют historical references, где flight или combined search ранее были отмечены шире текущего hotel-only MVP. Большинство этих документов содержит явные superseded/future-scope notes, а primary roadmap и Stage 3/4/5 baselines корректно имеют приоритет над ними.

Why it matters:
Текущая документация безопасна при чтении с superseded notes, но изолированные выдержки из Stage 1/2 можно ошибочно прочитать как active MVP scope.

Recommendation:
Не переписывать historical documents в рамках этого review. В отдельной cleanup-задаче можно добавить одну компактную "Historical scope note" в любой оставшийся Stage 1/2 document, тело которого все еще содержит active-looking flight/combined MVP labels.

Allowed timing:
During Stage 6 planning.

### [NT-001] Primary roadmap является корректным source of truth

Severity: Note  
Area: Roadmap  
Files:

- `docs/roadmap/roadmap.md`
- `README.md`
- `docs/ROADMAP.md`
- `docs/product/README.md`

Finding:
`docs/roadmap/roadmap.md` явно определяет себя как primary roadmap. Root README, `docs/ROADMAP.md` и `docs/product/README.md` возвращают читателя к нему за текущими статусами.

Why it matters:
Это защищает проект от конфликтующих status sources и поддерживает ожидаемый baseline: Stage 0-5 completed / Stage 6 planned.

Recommendation:
Сохранять status changes централизованными в `docs/roadmap/roadmap.md`.

Allowed timing:
No immediate action.

### [NT-002] Architecture baseline Stage 5 хорошо защищен

Severity: Note  
Area: Architecture docs  
Files:

- `docs/architecture/stage-5/*`

Finding:
Документы Stage 5 последовательно избегают production code, API/OpenAPI contracts, DB schema, DTOs/classes/interfaces, provider adapters, vendor selection и implementation backlog. Они сохраняют hotel-only MVP boundary и facts / assumptions / unknowns separation.

Why it matters:
Это сильный baseline для Stage 6 planning, пока carryover не трактуется как implementation backlog.

Recommendation:
Использовать Stage 5 как architecture baseline и boundary reference, а не как прямой список задач.

Allowed timing:
No immediate action.

### [NT-003] Top-level product brief смешивает full-product scenarios с MVP boundaries, но корректно разделяет MVP

Severity: Note  
Area: Root docs / Product docs  
Files:

- `docs/PROJECT_BRIEF.md`

Finding:
Project brief содержит broad future product scenarios, например saved places, bookings, notes и export. Затем section MVP boundaries корректно исключает booking/payment, complex account history и flight/combined search из MVP v1.

Why it matters:
Документ не имеет внутреннего противоречия, но будущим readers стоит трактовать широкий список сценариев как product vision/context, а section MVP boundaries как active MVP scope.

Recommendation:
Немедленное изменение не требуется. Если позже будет выполняться wording cleanup, оставить broad product vision визуально отделенным от active MVP scope.

Allowed timing:
Later / future stage.

## 6. Roadmap coverage review

`docs/roadmap/roadmap.md` является factual source of truth для stages, status, artifact checklists, carryover и next step. Он сильнее, чем checklist-only roadmap: содержит stage goals, key results, open questions, guardrails, quality/completion notes и future-stage boundaries.

Главное нужное улучшение — не rewrite, а небольшое уточнение вокруг Stage 6 contract/preparation scope, чтобы его нельзя было ошибочно прочитать как разрешение на преждевременный API/OpenAPI/DB/storage implementation work.

| Stage | Expected Status | Actual Status in Roadmap | Coverage Quality | Notes |
|---|---|---|---|---|
| Stage 0 | Completed | Completed | Good | Есть goal, artifacts, open questions и carryover. |
| Stage 1 | Completed | Completed | Good | Есть requirements, follow-ups, key results и carryover. Historical flight/combined labels mitigated by superseded notes. |
| Stage 2 | Completed | Completed | Good | Listed use cases, edge cases, behavior rules, data requirements и consistency review. |
| Stage 3 | Completed | Completed | Good | Задокументированы hotel-only UX baseline, acceptance criteria, summary, reconciliation и carryover. |
| Stage 4 | Completed | Completed | Good | Listed Visual/UX system docs и Stage 4.1 consistency review. |
| Stage 4.1 | Completed | Completed | Good | Включен как Stage 4.1 Visual Design Consistency Review. |
| Stage 5 | Completed | Completed | Good | Есть architecture deliverables, guardrails и completion notes. |
| Stage 6 | Planned / not started | Planned; next planned step says Stage 6 is not started | Partial | Status корректен, но wording вокруг "contracts" нужно уточнить до Stage 6 planning. |
| Stage 7+ | Planned | Planned | Good | Future stages scoped at high level и не выглядят начатыми. |

## 7. Documentation navigation review

| Area | Entry Point | Status | Issues |
|---|---|---|---|
| Root documentation | `README.md` | Good | Ссылка на новый pre-Stage 6 review добавлена в этой задаче. |
| Product documentation | `docs/product/README.md` | Good | Product files, найденные в stage-0 - stage-4, перечислены. |
| Architecture documentation | `docs/product/README.md`, `docs/roadmap/roadmap.md` | Partial | Нет `docs/architecture/README.md`; Stage 5 обнаружим, но не из architecture-local index. |
| Decisions / ADR | `docs/decisions/README.md` | Partial | Корректно говорит, что ADR files нет, но не упоминает Stage 5 decision inventory как non-ADR context. |
| Roadmap | `docs/roadmap/roadmap.md`, `docs/ROADMAP.md` | Good | Primary/secondary split ясен; Stage 6 wording требует guardrail clarification. |
| Development docs | `docs/development/roadmap.md`, `docs/development/milestones.md`, `docs/development/implementation-strategy.md` | Warning | Они явно secondary, но достаточно детальны, чтобы их можно было принять за активный implementation backlog. |
| Prompts / agent rules | `docs/prompts/*`, `AGENTS.md` | Good | Сильный roadmap/scope control; конфликта с review-only task не найдено. |

## 8. Scope boundary review

| Boundary | Status | Evidence / Notes |
|---|---|---|
| MVP remains hotel-only | Pass | Primary roadmap, Stage 3/4 product docs и Stage 5 architecture docs последовательно сохраняют hotel-only MVP v1. |
| Flights remain outside MVP | Pass | Flight search отмечен как next expansion после hotel flow. Historical Stage 1/2 mentions superseded. |
| Booking/payment remain outside MVP | Pass | Root brief, product docs, roadmap и architecture docs исключают booking/payment из MVP. |
| Provider facts not created by LLM | Pass | Stage 3/4/5 многократно фиксируют provider facts как source-owned и запрещают LLM fabricating them. |
| Provider abstraction not API contract | Pass | Stage 5 integration architecture и summary явно избегают API/OpenAPI contracts и provider DTOs. |
| Current-session shortlist not account history | Pass | Stage 3 и Stage 5 отделяют current-session shortlist от account history, persistent saved trips и full auth. |
| No premature DB/storage decision | Pass | Stage 5 data/storage docs избегают DB schema/storage model и откладывают DB/storage technology decisions. |
| No premature implementation backlog | Warning | Stage 5 docs избегают backlog, но development roadmap/milestones являются детальными материалами для будущей реализации и требуют более сильного framing "not active backlog" до Stage 6. |
| Stage 6 not started | Pass | Primary roadmap говорит, что Stage 6 находится в статусе `Planned / not started`; Stage 5 summary говорит не начинать Stage 6 в той задаче. |

## 9. Recommended cleanup plan

### Recommended before Stage 6

- Уточнить wording Stage 6 в roadmap/development docs, чтобы "contracts" нельзя было прочитать как immediate OpenAPI/API/DB/storage work.
- Добавить или усилить framing "future reference only / not active backlog" в development roadmap и milestones.
- При необходимости добавить lightweight architecture documentation index, ограниченный navigation и status links.
- Обновить decisions README: указать, что standalone ADR files пока нет, а Stage 5 содержит non-ADR decision inventory.
- Сохранить новый review document обнаружимым из root и primary roadmap navigation.

### Can be handled during Stage 6 planning

- Решить, какие carryover items Stage 5 входят в scope Stage 6 planning, а какие остаются deferred.
- Добавить compact historical-scope notes в любые Stage 1/2 documents, которые все еще выглядят active при чтении в изоляции.
- Определить Stage 6 entry/exit criteria без создания API/OpenAPI contracts, DB schema, storage model, auth/security/DevOps backlog или production implementation.

### Later / future stage

- Создавать standalone ADR files только когда future trigger происходит и решение действительно принимается.
- Рассмотреть отделение broad product vision от active MVP scope в project brief, если brief позже будет пересматриваться.
- Возвращаться к account history, auth, booking/payment, flights и combined itinerary только через явные future product decisions.

## 10. Final verdict

Passed with major notes.

Documentation baseline достаточно согласован, чтобы перейти к controlled Stage 6 planning task: Stage 0-5 completed, Stage 6 planned / not started, MVP v1 остается hotel-only, provider facts остаются source-owned, production implementation отсутствует.

Critical blockers перед Stage 6 нет. Два Major notes нужно закрыть как limited documentation cleanup before Stage 6 planning, чтобы development docs и Stage 6 contract wording нельзя было принять за active implementation/API/DB/storage/auth/DevOps backlog.
