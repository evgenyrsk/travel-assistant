# Roadmap Progress — Travel Assistant

Этот документ является **основным roadmap** проекта Travel Assistant. Он фиксирует статусы и границы этапов, критерии качества, перенесенные пункты и следующий разрешенный шаг.

Roadmap не является трекером задач, продуктовой или архитектурной спецификацией, реестром ADR либо активным списком реализации. Подробные продуктовая и архитектурная основы вынесены в отдельные документы и указаны ниже.

## 1. Текущий статус проекта

| Пункт | Статус |
|---|---|
| Текущий этап | Нет активного implementation stage; Stage 17.0 corporate semantic portability завершён, REAL activation заблокирована |
| Последний завершенный этап | Stage 17.0 — internal gateway contract, adapter и corporate transfer readiness без REAL calls |
| Следующий планируемый шаг | Не активирован; corporate platform intake и model shortlist требуют доступных deployment constraints и отдельной задачи |
| Источник подробных статусов | Только этот документ: `docs/roadmap/roadmap.md` |

| Область | Текущее состояние |
|---|---|
| Stage 0-7 | Завершены |
| Stage 8 | Завершен (backend confirmation lifecycle) с carryover (InMemory stores, fake LLM/provider) |
| Stage 7 implementation foundation | Минимальная Kotlin + Ktor backend-основа и ограниченные assistant/conformance-tool slices завершены до Stage 7.25 включительно |
| Stage 7 documentation stabilization | Stage 7.26-7.30 завершены |
| Stage 7 resume development handoff | Stage 7.31 завершен; следующий technical task должен быть выбран отдельной явной roadmap-aligned задачей |
| Stage 7 technical context review | Stage 7.32 завершен; рекомендован Stage 7.33 manifest candidate definition без readiness claim |
| Stage 7 manifest candidate definition | Stage 7.33 завершен; non-readiness manifest candidate создан |
| Stage 7 manifest validation hardening | Stage 7.34 завершен; conformance tool блокирует преждевременные readiness promotion signals в manifest candidate |
| Stage 7 endpoint candidate review | Stage 7.35 завершен; endpoint candidates проанализированы без изменения manifest и без readiness claim |
| Stage 7 assistant endpoint candidate clarification | Stage 7.36 завершен; два assistant endpoint candidates уточнены без изменения manifest, OpenAPI/API contracts, runtime или readiness claim |
| Stage 7 assistant endpoint contract/runtime alignment notes | Stage 7.37 завершен; alignment, gaps, unknowns и carryover для двух assistant endpoint candidates зафиксированы без изменения manifest, OpenAPI/API contracts, runtime, conformance tool или readiness claim |
| Stage 7 assistant endpoint alignment cleanup decision | Stage 7.38 завершен; gaps классифицированы по documentation, OpenAPI/contract, backend/runtime tests, conformance/tooling и future-only buckets без изменений implementation/code, OpenAPI/contract, runtime, manifest или readiness state |
| Stage 7 assistant endpoint contract shape cleanup | Stage 7.39 завершен; Assistant request/response/error contract shape уточнен в OpenAPI draft и contract notes без backend runtime behavior changes, backend tests, conformance tool changes, manifest expansion, generated clients или readiness claim |
| Stage 7 assistant endpoint runtime contract test cleanup | Stage 7.40 завершен; runtime contract tests для Assistant endpoints уточнены без production backend behavior changes, OpenAPI contract changes, conformance tool changes, manifest expansion, generated clients или readiness claim |
| Stage 7 assistant endpoint conformance/tooling follow-up decision | Stage 7.41 завершен; будущие Assistant endpoint conformance/tooling checks классифицированы без conformance tool implementation, production backend changes, OpenAPI changes, manifest expansion, generated clients или readiness claim |
| Stage 7 assistant endpoint conformance candidate implementation | Stage 7.42 завершен; static/advisory Assistant endpoint candidate checks добавлены без backend runtime HTTP checks, OpenAPI changes, manifest expansion, generated clients, CI/Gradle gate или readiness claim |
| Stage 7 assistant endpoint conformance candidate verification | Stage 7.43 завершен; Stage 7.42 подтвержден как bounded/readiness-safe implementation, один Minor shape-guard hardening candidate перенесен в отдельный future stage без tool/backend/OpenAPI/manifest/generated-client/CI changes |
| Stage 7 assistant conformance shape guard hardening | Stage 7.44 завершен; property presence и required membership для `message`/`nextAction` проверяются отдельно, candidate inventory mismatch test добавлен, advisory/runtime/readiness boundaries сохранены |
| Stage 7 assistant conformance output/operator guidance | Stage 7.45 завершен; запуск и интерпретация JSON output, static/blocking/advisory semantics и Assistant checks документированы без изменения tool logic, tests или readiness state |
| Stage 7 assistant conformance documentation verification | Stage 7.46 завершен; Stage 7.45 guidance подтвержден относительно фактического tool output, source-of-truth roles и readiness boundaries без изменения README, tool logic/tests или implementation areas |
| Сверка оставшегося объёма Stage 7 | Stage 7.47 завершен; Assistant conformance подпоток признан достаточно закрытым, а обязательный остаток Stage 7 возвращён к hotel search, offers, ranking/explanation и end-to-end MVP behavior |
| Минимальный backend-поток поиска отелей | Stage 7.48 завершен; process-local search flow и deterministic `FakeHotelOfferProvider` добавлены без real provider, ranking, frontend, generated-client/manifest/CI/tool changes или readiness claims |
| Минимальное ранжирование hotel offers | Stage 7.49 завершен; provider-independent deterministic ranking и короткий `matchSummary` добавлены поверх local fake offers без LLM, real provider, frontend, OpenAPI/generated-client/manifest/CI/tool changes или readiness claims |
| Минимальная передача от Assistant к hotel search | Stage 7.50 завершен; strict explicit message format создает process-local search и возвращает `show_hotel_results` / `hotelSearchId` без LLM, real provider, frontend, generated-client/manifest/CI/tool changes или readiness claims |
| Минимальный frontend-сценарий hotel search | Stage 7.51 завершен; отдельная structured форма проверяет существующие process-local search/offers endpoints и показывает ranked offers. Это временная technical demo shell, а не целевой chat-first UI |
| Финальная сверка hotel-only MVP slice | Stage 7.52 завершен; Stage 7.48-7.51 согласованы по коду, contract shape и automated checks, а отсутствие live browser-to-backend проверки перенесено в явный carryover Stage 7.53 |
| Финальное закрытие Stage 7 | Stage 7.53 завершен; ограниченная hotel-only основа закрыта, оставшаяся работа перенесена без заявлений о готовности к промышленному использованию, generated clients, manifest или real provider и без активации Stage 8 |
| Готовность generated clients/OpenAPI | Не заявлена |
| Generated-client-ready subset / generated clients | Создан manifest-кандидат без заявления готовности; готовый subset и generated clients не созданы |
| Full conformance gate | Не реализован |
| Hotel search / broader implementation | Opt-in Hotels API и OpenRouter runtime, confirmation lifecycle и chat-first frontend реализованы; happy path и ограниченная pilot-матрица Stage 9.23 подтверждены, `FAKE` остается default, stores process-local |

| Этап | Статус | Краткое описание |
|---|---|---|
| Stage 0 | Завершен | Product framing, первичные сценарии, предварительные MVP boundaries и открытые вопросы. |
| Stage 1 | Завершен | Business requirements, user journeys, BR/FR/NFR, assumptions, риски и consistency review. |
| Stage 2 | Завершен | Use cases, edge cases, правила поведения ассистента, data requirements и combined search levels. |
| Stage 3 | Завершен | Hotel-only MVP UX, navigation, required fields, acceptance criteria и carryover. |
| Stage 4 | Завершен | Visual direction, основы design system, component inventory, screen specs и interaction patterns. |
| Stage 4.1 | Завершен | Visual design consistency review и небольшая правка формулировок. |
| Stage 5 | Завершен | Conceptual technical architecture, границы, decision inventory, summary и completion audit. |
| Stage 6 | Завершен | API Contracts / OpenAPI / Integration Boundary; Stage 6.1 OpenAPI draft, Stage 6.2 contract review, Stage 6.3 contract fixes, Stage 6.4 post-fix review, Stage 6.5 provider boundary / mapping notes, Stage 6.6 navigation/status cleanup, Stage 6.7 completion review, Stage 6.8 pre-implementation decisions cleanup и Stage 6.9 final closure / handoff завершены. |
| Stage 7 | Завершен | Ограниченная основа hotel-only MVP закрыта Stage 7.53: backend, поиск, fake provider, ранжирование, передача от Assistant и временная frontend-оболочка завершены в заявленных границах. Chat-first flow и LLM orchestration были вне Stage 7 и реализованы позднее в Stage 8-9. |
| Stage 8 | Завершен | Backend confirmation lifecycle: LLM orchestration boundary, confirmation flow, local search execution, consume-after-success. Закрыт с carryover (InMemory stores, fake LLM/provider). |
| Stage 9 | Завершен | Opt-in Hotels API и OpenRouter runtime, chat-first frontend и внутренний MVP-пилот закрыты в ограниченных границах; production readiness не заявлена. |
| Stage 10 | Завершен | Stage 10.0–10.4 укрепили bounded platform-neutral API subset и закрепили текущий web/PWA только как локальную demo shell; product clients принадлежат будущим интеграционным командам. |
| Stage 11 | Завершен | Локальный launcher, FAKE preflight и один полный REAL browser smoke подтвердили демонстрационный chat-first hotel flow без production-readiness claim. |
| Stage 12 | Завершен | Итеративное уточнение четырёх фильтров, повторное confirmation, новый provider search и safe no-results flow подтверждены regression и одним REAL smoke. |
| Stage 13 | Завершен | Details выбранного opaque offer доступны через platform-neutral API и загружаются demo shell только по явному запросу. |
| Stage 14 | Завершен | Stage 14.0–14.7 закрыли рабочий hotel-only MVP; финальная REAL-проверка подтвердила multi-room guard, сохранение критериев и exact-hotel details/rates flow без заявления production readiness. |
| Stage 15 | Завершен | Подтверждены backend-owned business logic и Java 17 portability; добавлены layering guards, correlation, stdout JSON events, probes, OpenMetrics и operational runbook. |
| Stage 16 | Activation readiness завершён; REAL rollout заблокирован | Provider-neutral async two-pass semantic-анализ `GLAMPING`, exact endpoint pinning, public polling, observability и evaluation harness прошли regression gates; provider content не передавался внешней модели. |
| Stage 17 | Portability foundation завершён; REAL rollout заблокирован | Internal semantic gateway adapter и contract v1 отделяют Travel Assistant от конкретной model/provider; transfer и evaluation checklist зафиксированы без live calls или deployment artifacts. |

## 2. Правила управления roadmap

- `docs/roadmap/roadmap.md` является источником истины по статусам и границам этапов, перенесенным пунктам и следующему разрешенному шагу.
- `docs/ROADMAP.md` является верхнеуровневым навигационным обзором, а не конкурирующим источником текущего статуса. Он содержит только карту назначения этапов и не ведет матрицу состояния, последний завершенный шаг, следующий планируемый шаг или готовность реализации.
- Документы в `docs/development/` являются активными инженерными правилами для явно ограниченных задач. `docs/development/roadmap.md` и `docs/development/implementation-strategy.md` остаются справочными материалами, а не активным списком реализации. Все документы разработки должны следовать этому roadmap.
- Запланированные и будущие этапы не являются активным списком задач. Каждый будущий этап начинается только после отдельной явной задачи roadmap.
- Рекомендации, перенесенные пункты и будущие кандидаты не должны автоматически выполняться во время задач проверки или чистки.
- Implementation, API/OpenAPI contracts, endpoint specs, DB schema, storage model, auth/security/DevOps/testing backlog и production code требуют отдельного явного roadmap step.
- Product baseline и architecture baseline кратко фиксируют текущее состояние; roadmap должен ссылаться на них, а не дублировать их полностью.
- ADR candidates, drafts и decision inventory не являются accepted ADR.
- Подтвержденный backend stack Travel Assistant — Kotlin + Ktor. Java/Spring Boot не является принятым backend stack без явного будущего ADR и задачи, согласованной с roadmap.
- Если implementation artifacts конфликтуют с architecture baseline по backend stack, дальнейшая implementation работа должна остановиться и зафиксировать архитектурное расхождение.

## 3. Baseline-документы

- `docs/product/product-baseline.md` — компактный actual product baseline после Stage 0-5.
- `docs/architecture/architecture-baseline.md` — компактный actual architecture baseline после Stage 5.
- `docs/decisions/README.md` — навигация и терминология для decisions / ADR.
- `docs/guides/documentation-style-guide.md` — правила языка, структуры, терминологии, guardrails и безопасного refactoring документации.
- `docs/reviews/documentation-refactoring-plan.md` — план controlled documentation refactoring; не active backlog.

## 4. MVP Scope

MVP v1 остается hotel-only:

- AI-assisted hotel search по естественному запросу;
- уточнение обязательных критериев и явное подтверждение до provider search;
- первичная provider-backed выдача без обязательных дополнительных фильтров;
- итеративное изменение необязательных предпочтений в чате с новым
  подтверждением и новым provider search;
- максимальная общая стоимость, звезды, минимальный гостевой рейтинг,
  обязательная бесплатная отмена и включённый завтрак в пределах
  подтвержденного Hotels API contract; пользовательская сортировка отложена;
- явное разделение provider facts, assistant assumptions и unknowns.

Явно вне MVP v1:

- flights;
- combined itinerary / combined hotel + flight package;
- booking;
- payment;
- account history и account-level storage;
- loyalty/profile system;
- production integrations за пределами явно активированной provider work.

Provider/API data является source of truth для travel facts. LLM может интерпретировать, объяснять, ранжировать, резюмировать и уточнять, но не должна выдумывать цены, доступность, рейтинги, amenities или другие provider facts.

## 5. Принятые решения и перенесенные ограничения

Stage 10.0 первоначально закрыл внутренние решения для PWA-среза, а Stage 10.4
уточнил его роль:

- responsive web/PWA используется как локальная demo shell, а не как будущий
  продуктовый клиент;
- `shownPrice` отображается как provider total за выбранный период без
  перерасчета и без заявления о включенных taxes/fees;
- `LIMITED` не определяется эвристически;
- demo shell не обещает resume, account history или cross-device sync и не
  требует durable storage/auth.

До публичного rollout остаются внешние gates: официальный server-to-server
статус, долгосрочная стабильность, SLA и rate limits Hotels API, а также
production security, observability и deployment boundaries. Неизвестные
provider source/freshness facts нельзя заполнять предположениями.

Hotel details, current-session shortlist и отдельный интерактивный
explanation/comparison flow остаются направлениями hotel-only расширения. Они
не являются обязательными для текущего демонстрационного MVP и не блокируют
итеративное уточнение поиска Stage 12.

Перенесенные ограничения не являются активным списком задач. Следующий шаг
активируется только отдельной явной roadmap-aligned задачей.

## 6. Завершенные этапы

### Stage 0 — Product Framing

**Статус:** завершен.

**Назначение:** зафиксировать исходную продуктовую рамку, первичные сценарии, предварительные MVP boundaries и правила дальнейшей работы.

**Ключевые артефакты:**

- `docs/product/stage-0/product-framing.md`
- `docs/product/stage-0/initial-scenarios.md`
- `docs/product/stage-0/mvp-boundaries.md`
- `docs/product/stage-0/assumptions-and-open-questions.md`
- `docs/product/README.md`

**Заметка о завершении:** Stage 0 не фиксировал финальные технические решения. Приоритетные пользователи, сценарии и MVP boundaries были уточнены на следующих product stages.

### Stage 1 — Business Requirements

**Статус:** завершен.

**Назначение:** зафиксировать аудиторию, business scenarios, user journeys, BR/FR/NFR, assumptions, open questions и risks.

**Ключевые артефакты:**

- `docs/product/stage-1/target-audience.md`
- `docs/product/stage-1/business-scenarios.md`
- `docs/product/stage-1/user-journeys.md`
- `docs/product/stage-1/business-requirements.md`
- `docs/product/stage-1/functional-requirements.md`
- `docs/product/stage-1/non-functional-requirements.md`
- `docs/product/stage-1/assumptions-and-open-questions.md`
- `docs/product/stage-1/stage-1-summary.md`
- `docs/product/stage-1/stage-1-consistency-review.md`

**Заметка о завершении:** Booking и payment исключены из MVP. Более ранние
flight и combined recommendations superseded для MVP v1; оба направления
остаются future candidates и требуют отдельного product/roadmap decision.

**Carryover:** точный existing travel API contract, provider-backed discovery, recommendation criteria, persistence/authorization и язык uncertainty/provider-error остаются входными данными для будущих решений.

### Stage 2 — Use Cases & Edge Cases

**Статус:** завершен.

**Назначение:** развернуть Stage 1 scenarios в use cases, edge cases, assistant behaviour rules, combined search levels и product data requirements.

**Ключевые артефакты:**

- `docs/product/stage-2/use-cases.md`
- `docs/product/stage-2/edge-cases.md`
- `docs/product/stage-2/assistant-behaviour-rules.md`
- `docs/product/stage-2/combined-search-levels.md`
- `docs/product/stage-2/data-requirements.md`
- `docs/product/stage-2/stage-2-summary.md`
- `docs/product/stage-2/stage-2-consistency-review.md`

**Заметка о завершении:** Provider/API data подтверждена как source of truth для travel facts. Stage 2 flight/combined context сохранен для historical traceability, но не является разрешением реализовывать flights или combined itinerary в MVP v1.

**Carryover:** minimum required fields, open destination discovery, existing travel API contract, adapter/error/reliability decisions и session/auth questions остаются будущими входными данными.

### Stage 3 — MVP UX / Navigation

**Статус:** завершен.

**Назначение:** определить UX-структуру hotel-only MVP, navigation model, search flow boundaries, required fields и acceptance criteria без API, архитектуры или implementation work.

**Ключевые артефакты:**

- `docs/product/stage-3/screen-map.md`
- `docs/product/stage-3/required-fields-and-acceptance-criteria.md`
- `docs/product/stage-3/mvp-search-flow-details.md`
- `docs/product/stage-3/combined-search-ux-decision.md` — historical decision, superseded for MVP v1.
- `docs/product/stage-3/stage-3-hotel-only-consistency-review.md`
- `docs/product/stage-3/stage-3-summary-and-carryover.md`
- `docs/product/stage-3/stage-3-plan-reconciliation.md`

**Quality gate / заметка о завершении:** Stage 3 финализировал hotel-only MVP UX и acceptance criteria. Flight search и combined hotel+flight не требуются для MVP v1. Stage 3 не разрешает implementation, API/OpenAPI contracts, DB schema, storage model, auth/account history или UI code.

### Stage 4 — Visual Design & UX System

**Статус:** завершен.

**Назначение:** зафиксировать visual style direction, design system foundations, component inventory, screen-level specifications и interaction patterns для hotel-only MVP.

**Ключевые артефакты:**

- `docs/product/stage-4/visual-design-direction.md`
- `docs/product/stage-4/design-system-foundations.md`
- `docs/product/stage-4/component-inventory.md`
- `docs/product/stage-4/screen-specifications.md`
- `docs/product/stage-4/interaction-patterns.md`
- `docs/product/stage-4/stage-4-summary-and-carryover.md`

**Заметка о завершении:** Stage 4 создал product/design documentation, а не frontend implementation. Draft palette, design foundations и component inventory не являются финальными implementation tokens.

**Carryover:** final design tokens, hotel imagery/source markers, session persistence level и accessibility gates остаются будущими входными данными.

### Stage 4.1 — Visual Design Consistency Review

**Статус:** завершен.

**Назначение:** проверить Stage 4 documentation на согласованность со Stage 0-3, MVP scope и roadmap boundaries.

**Ключевой артефакт:**

- `docs/product/stage-4/stage-4-consistency-review.md`

**Quality gate / заметка о завершении:** Verdict: passed with minor wording fixes. Stage 4.1 не начинал frontend implementation и не расширял MVP scope.

### Stage 5 — Technical Architecture

**Статус:** завершен.

**Назначение:** зафиксировать conceptual architecture baseline, system boundaries, domain/data boundaries, orchestration, integration architecture, NFR guidance и decision inventory.

**Ключевые артефакты:**

- `docs/architecture/architecture-baseline.md`
- `docs/architecture/stage-5/architecture-scope-and-principles.md`
- `docs/architecture/stage-5/system-context-and-boundaries.md`
- `docs/architecture/stage-5/domain-model-and-boundaries.md`
- `docs/architecture/stage-5/application-orchestration.md`
- `docs/architecture/stage-5/integration-architecture.md`
- `docs/architecture/stage-5/data-and-storage-boundaries.md`
- `docs/architecture/stage-5/non-functional-requirements.md`
- `docs/architecture/stage-5/architecture-decisions-draft.md`
- `docs/architecture/stage-5/stage-5-consistency-review.md`
- `docs/architecture/stage-5/stage-5-summary-and-carryover.md`

**Quality gate / заметка о завершении:** Stage 5.9 не нашел Critical/Major blockers. Stage 5 является conceptual architecture documentation и system design, а не production implementation.

**Carryover:** сохранить hotel-only MVP scope, provider facts как source data, разделение facts/assumptions/unknowns, provider abstraction как conceptual boundary, а не API contract, и NFR как quality guidance, а не active DevOps/security/testing backlog.

## 7. Текущий и будущие этапы

### Stage 6 — API Contracts / OpenAPI / Integration Boundary

**Статус:** завершен. Stage 6.1, Stage 6.2, Stage 6.3, Stage 6.4, Stage 6.5, Stage 6.6, Stage 6.7, Stage 6.8 и Stage 6.9 завершены через отдельные явные roadmap-задачи.

**Назначение:** API Contracts / OpenAPI / Integration Boundary и scoped implementation-preparation planning. Stage 6 должен сохранять hotel-only MVP v1, Stage 5 architecture baseline, provider-agnostic hotel boundary и отсутствие production implementation.

**Условие активации:** Stage 6 был начат отдельной явной задачей Stage 6.1 и закрыт отдельной явной задачей Stage 6.9. Завершение Stage 6.1, Stage 6.2, Stage 6.3, Stage 6.4, Stage 6.5, Stage 6.6, Stage 6.7, Stage 6.8 или Stage 6.9 не начинает Stage 7 и не разрешает backend/frontend implementation.

**Границы scope:** Stage 6 может определять API/OpenAPI contract drafts, implementation-preparation scope, sequencing, validation approach, local workflow boundaries и conceptual boundaries для mock/fake providers и contract placeholders. Он должен сохранить hotel-only scope MVP v1 и Stage 5 architecture baseline.

**Завершенные артефакты Stage 6.1:**

- `docs/architecture/stage-6/openapi-draft.yaml` — primary OpenAPI 3.1 draft для MVP hotel-only frontend/backend API.
- `docs/architecture/stage-6/openapi-contract-notes.md` — notes по MVP endpoints, exclusions, assumptions, open questions и связи со Stage 5 baseline.

**Завершенный артефакт Stage 6.2:**

- `docs/architecture/stage-6/openapi-contract-review.md` — review OpenAPI draft относительно Stage 2-5 product, UX и architecture baselines; verdict: passed for continued Stage 6 contract work with follow-up findings before client generation or implementation.

**Завершенный артефакт Stage 6.3:**

- `docs/architecture/stage-6/openapi-fixes-summary.md` — summary of OpenAPI fixes closing Stage 6.2 Major findings and addressing allowed Minor fixes without provider-specific DTOs, implementation code or Stage 7 activation.

**Завершенный артефакт Stage 6.4:**

- `docs/architecture/stage-6/post-fix-contract-review.md` — post-fix review of Stage 6.3 contract fixes; verdict: passed for continued Stage 6 contract work with one Minor follow-up and no Critical/Major blockers.

**Завершенный артефакт Stage 6.5:**

- `docs/architecture/stage-6/provider-boundary-mapping-notes.md` — conceptual provider boundary / mapping notes for future hotel provider/source data into existing client-facing OpenAPI concepts without provider-specific DTOs, provider contracts, backend/frontend implementation or Stage 7 activation.

**Завершенный cleanup Stage 6.6:**

- Documentation navigation / status sync cleanup updated root/navigation status wording in `README.md`, `docs/ROADMAP.md`, `docs/architecture/README.md` and this roadmap without changing OpenAPI, Stage 6 contract artifacts, provider boundary notes, backend/frontend implementation, DB/storage, ADR or Stage 7 activation.

**Завершенный артефакт Stage 6.7:**

- `docs/architecture/stage-6/stage-6-completion-review.md` — Stage 6 completion review / contract package summary; verdict: завершен с carryover; рекомендует отдельный явный Stage 6.8 pre-implementation decisions cleanup до generated clients или более широкой implementation work.

**Завершенный артефакт Stage 6.8:**

- `docs/architecture/stage-6/pre-implementation-decisions-cleanup.md` — pre-implementation decisions cleanup for nested 404 modeling, inline offer details, result-envelope search status and current-session page-refresh/persistence behavior; no new resource flows, provider DTOs, DB/storage or Stage 7 activation.

**Завершенный артефакт Stage 6.9:**

- `docs/architecture/stage-6/stage-6-final-closure-and-handoff.md` — final closure / handoff to implementation; verdict: Stage 6 completed with non-blocking carryover and recorded that Stage 7 had to start only through a separate explicit task.

**Явные исключения:** Stage 6.1, Stage 6.2, Stage 6.3, Stage 6.4, Stage 6.5, Stage 6.6, Stage 6.7, Stage 6.8 и Stage 6.9 не создают backend implementation, frontend implementation, DB schema, storage model, auth/security/DevOps/testing backlog, production implementation, provider-specific integration code, generated clients или Stage 7 implementation tasks. Следующие concrete contract, storage, auth, security, DevOps, testing или implementation artifacts требуют отдельной явной future-stage задачи.

**Quality gate:** Stage 6 закрыт после того, как Stage 6 artifacts явно отделили разрешенную contract/preparation work от исключенной DB/storage/auth/DevOps/testing/production work. Любой следующий concrete contract, implementation artifact или expansion за пределы hotel-only требует отдельного явного roadmap step.

### Stage 7 — MVP Implementation

**Статус:** завершен.

**Назначение:** реализация ограниченной основы hotel-only MVP после Stage 6 через отдельные задачи. Stage 7 закрыт с явным переносом оставшихся пунктов; его завершение не означает готовность к промышленному использованию.

**Прогресс Stage 7 по областям:**

| Область | Завершено до | Статус |
|---|---|---|
| Stabilization and restart readiness | Stage 7.0f-f | Backend stack drift исправлен; restart readiness пройден; cleanup читаемости navigation/governance/development/product/architecture/roadmap завершен. |
| Backend foundation and assistant boundaries | Stage 7.12 | Минимальная Kotlin + Ktor backend-основа, assistant session/message boundaries, local session/clarification/slot metadata и internal slot update boundary завершены. |
| API/runtime alignment and readiness reviews | Stage 7.15b | API/contract alignment checkpoint, assistant runtime contract alignment cleanup, generated-client/OpenAPI readiness checkpoint и response semantics/status sync завершены. |
| Generated-client/OpenAPI conformance planning and tooling | Stage 7.25 | Conformance planning, subset policy, skeleton planning/tooling decision, standalone read-only conformance tool, reporting tests и manifest detection/validation завершены. |
| Documentation stabilization track | Stage 7.30 | Documentation quality audit, governance rules cleanup, roadmap structure refactor, active documentation language normalization и final quality gate завершены. |
| Resume development handoff | Stage 7.31 | Documentation stabilization closure and guardrails for resumed bounded Stage 7 technical work completed; no technical implementation started. |
| Resume technical context review | Stage 7.32 | Technical context restored after documentation stabilization; next recommended bounded task is Stage 7.33 manifest candidate definition without readiness claim. |
| Ready subset manifest candidate definition | Stage 7.33 | Non-readiness `generated-client-ready-subset.yaml` candidate created for skeleton validation; no generated-client readiness claim. |
| Manifest candidate validation hardening | Stage 7.34 | Tool-local validation now blocks premature readiness promotion signals in the manifest candidate while keeping the report advisory/not_ready. |
| Endpoint candidate review | Stage 7.35 | Current OpenAPI/backend endpoint candidates reviewed for future manifest expansion; manifest and readiness state unchanged. |
| Assistant endpoint candidate clarification | Stage 7.36 | Two assistant endpoint candidates clarified for contract/runtime/security/product conditions; manifest and readiness state unchanged. |
| Assistant endpoint contract/runtime alignment notes | Stage 7.37 | Alignment, gaps, unknowns и carryover для двух assistant endpoint candidates зафиксированы; manifest, OpenAPI/API contracts, runtime, conformance tool и readiness state не изменены. |
| Assistant endpoint alignment cleanup decision | Stage 7.38 | Gaps классифицированы по documentation, OpenAPI/contract, backend/runtime tests, conformance/tooling и future-only buckets; implementation, contract, runtime, manifest и readiness state не изменены. |
| Assistant endpoint contract shape cleanup | Stage 7.39 | Assistant request/response/error contract shape уточнен в OpenAPI draft и contract notes; backend runtime behavior, backend tests, conformance tool, manifest, generated clients и readiness state не изменены. |
| Assistant endpoint runtime contract test cleanup | Stage 7.40 | Runtime contract tests для Assistant endpoints уточнены; production backend behavior, OpenAPI contracts, conformance tool, manifest, generated clients и readiness state не изменены. |
| Assistant endpoint conformance/tooling follow-up decision | Stage 7.41 | Будущие Assistant endpoint conformance/tooling checks классифицированы; conformance tool implementation, production backend behavior, OpenAPI contracts, manifest, generated clients и readiness state не изменены. |
| Assistant endpoint conformance candidate implementation | Stage 7.42 | Static/advisory Assistant endpoint candidate checks добавлены; backend runtime HTTP checks, OpenAPI contracts, manifest, generated clients, CI/Gradle gate и readiness state не изменены. |
| Assistant endpoint conformance candidate verification | Stage 7.43 | Stage 7.42 проверен на Stage 7.41 alignment, bounded scope, static/advisory distinction и readiness safety; Critical/Major findings отсутствуют, один Minor hardening candidate оставлен отдельному future stage. |
| Assistant conformance shape guard hardening | Stage 7.44 | Property presence и required membership для `message`/`nextAction` проверяются отдельно; добавлен targeted negative inventory test; runtime semantics остаются advisory-only, readiness state не изменен. |
| Assistant conformance output/operator guidance | Stage 7.45 | Документированы команды запуска, интерпретация `not_ready`/`readinessClaim`, blocking/advisory findings и Assistant checks; tool logic, tests и readiness state не изменены. |
| Assistant conformance documentation verification | Stage 7.46 | Operator guidance проверен относительно фактического JSON output, static/advisory behavior и source-of-truth boundaries; factual inconsistencies и readiness overclaims не найдены. |
| Сверка оставшегося объёма Stage 7 | Stage 7.47 | Подтверждено, что conformance подпоток не требует дальнейшего дробления; следующий практический шаг возвращён к минимальному hotel search flow с `fake provider`. |
| Минимальный backend-поток поиска отелей | Stage 7.48 | Existing hotel search contract реализован как process-local flow с deterministic `FakeHotelOfferProvider`, targeted validation/tests и нормализованными offers без ranking/readiness claims. |
| Минимальное ранжирование hotel offers | Stage 7.49 | Offers ранжируются по availability, rating, total stay price и stable offer ID; существующий `matchSummary` используется для короткого deterministic объяснения без LLM или readiness claims. |
| Минимальная передача от Assistant к hotel search | Stage 7.50 | Strict explicit Assistant message format вызывает существующий search boundary; response возвращает `show_hotel_results` и opaque `hotelSearchId`, а ordinary/incomplete messages сохраняют clarification behavior. |
| Минимальный frontend-сценарий hotel search | Stage 7.51 | Отдельная structured форма создаёт process-local session/search, загружает ranked offers и показывает `matchSummary` через ручной local API client без generated clients. |
| Финальная сверка hotel-only MVP slice | Stage 7.52 | Backend и frontend slices согласованы по request/response shape и проходят раздельные automated gates; live browser-to-backend E2E не заявлен. |
| Финальное закрытие и carryover | Stage 7.53 | Stage 7 закрыт в bounded foundation scope; future integration, production и AI/LLM work перенесены без автоматической активации. |

**Documentation stabilization track:**

- [x] Stage 7.26 — Documentation Quality Calibration Audit
- [x] Stage 7.27 — Documentation Governance Rules Cleanup
- [x] Stage 7.28 — Roadmap Structure Refactor
- [x] Stage 7.29 — Active Documentation Language Normalization
- [x] Stage 7.30 — Documentation Final Quality Gate

**Resume development handoff:**

- [x] Stage 7.31 — Resume Development Handoff

**Resume technical context review:**

- [x] Stage 7.32 — Resume Stage 7 Technical Context Review

**Manifest candidate definition:**

- [x] Stage 7.33 — Ready Subset Manifest Candidate Definition

**Manifest candidate validation hardening:**

- [x] Stage 7.34 — Manifest Candidate Validation Hardening

**Endpoint candidate review:**

- [x] Stage 7.35 — Endpoint Candidate Review

**Assistant endpoint candidate clarification:**

- [x] Stage 7.36 — Assistant Endpoint Candidate Clarification

**Assistant endpoint contract/runtime alignment notes:**

- [x] Stage 7.37 — Assistant Endpoint Contract/Runtime Alignment Notes

**Assistant endpoint alignment cleanup decision:**

- [x] Stage 7.38 — Assistant Endpoint Contract/Runtime Alignment Cleanup Decision

**Assistant endpoint contract shape cleanup:**

- [x] Stage 7.39 — Assistant Endpoint Contract Shape Cleanup

**Assistant endpoint runtime contract test cleanup:**

- [x] Stage 7.40 — Assistant Endpoint Runtime Contract Test Cleanup

**Assistant endpoint conformance/tooling follow-up decision:**

- [x] Stage 7.41 — Assistant Endpoint Conformance/Tooling Follow-up Decision

**Assistant endpoint conformance candidate implementation:**

- [x] Stage 7.42 — Assistant Endpoint Conformance Candidate Implementation

**Assistant endpoint conformance candidate verification:**

- [x] Stage 7.43 — Assistant Endpoint Conformance Candidate Verification

**Assistant conformance shape guard hardening:**

- [x] Stage 7.44 — Assistant Conformance Shape Guard Hardening

**Assistant conformance output/operator guidance:**

- [x] Stage 7.45 — Assistant Conformance Output Documentation / Operator Guidance

**Assistant conformance documentation verification:**

- [x] Stage 7.46 — Assistant Conformance Documentation Verification

**Сверка оставшегося объёма Stage 7:**

- [x] Stage 7.47 — Сверка оставшегося объёма Stage 7

**Минимальный backend-поток поиска отелей:**

- [x] Stage 7.48 — Минимальный backend-поток поиска отелей с `fake provider`

**Минимальное ранжирование hotel offers:**

- [x] Stage 7.49 — Минимальное ранжирование hotel offers

**Минимальная передача от Assistant к поиску отелей:**

- [x] Stage 7.50 — Минимальная передача от Assistant к поиску отелей

**Минимальный frontend-сценарий hotel search:**

- [x] Stage 7.51 — Минимальный frontend-сценарий hotel search

**Финальная сверка hotel-only MVP slice:**

- [x] Stage 7.52 — Финальная сверка hotel-only MVP slice

**Финальное закрытие Stage 7:**

- [x] Stage 7.53 — Финальное закрытие Stage 7 и перенос оставшихся пунктов

**Перенесенные пункты после Stage 7:**

| Категория | Статус после закрытия |
|---|---|
| Полная browser-to-backend E2E и визуальная проверка | Перенесены в отдельную задачу интеграции/стабилизации; не являются выполненными |
| Generated clients и готовый subset | Не созданы; manifest остается кандидатом без заявления готовности или расширения |
| Интеграция CI/Gradle и runtime conformance gate | Не реализованы; требуют отдельного решения по инструментам |
| Интеграция real provider | Перенесена в Stage 9 или отдельную задачу, согласованную с roadmap, после появления provider contract |
| LLM orchestration и расширенный Assistant UI | Перенесены в Stage 8; Stage 8 не активирован |
| DB/storage, Redis/cache, auth/account flows | Перенесены до отдельного продуктового или архитектурного решения |
| Booking, payment, flights и combined itinerary | Не входят в закрытые границы hotel-only Stage 7 |
| Production UI, security, observability и deployment hardening | Не заявлены и остаются будущей работой |

**Ключевое ограничение закрытия:** завершение Stage 7 не означает готовность generated clients, завершение OpenAPI, активацию DB/storage, интеграцию real provider, промышленную готовность frontend, готовность CI gate или автоматическую активацию Stage 8.

**Stage 7 linked artifacts by group:**

| Group | Key artifacts |
|---|---|
| Backend foundation | `services/backend/`, `services/backend/README.md` |
| Full review/audit trail | `docs/reviews/README.md` |
| Stack correction and restart readiness | `project-consistency-audit.md`, `backend-stack-decision-sync.md`, `backend-skeleton-correction.md`, `stage-7-restart-readiness-review.md` |
| Stage 7.2-7.15b implementation/readiness/status reports | См. `docs/reviews/README.md` |
| Stage 7.16-7.25 generated-client/OpenAPI conformance reports | См. `docs/reviews/README.md` |
| Stage 7.26-7.30 documentation stabilization reports | `stage-7-26-documentation-quality-calibration-audit.md`, `stage-7-27-documentation-governance-rules-cleanup.md`, `stage-7-28-roadmap-structure-refactor.md`, `stage-7-29-active-documentation-language-normalization.md`, `stage-7-30-documentation-final-quality-gate.md` |
| Stage 7.31 resume development handoff | `stage-7-31-resume-development-handoff.md` |
| Stage 7.32 resume technical context review | `stage-7-32-resume-stage-7-technical-context-review.md` |
| Stage 7.33 ready subset manifest candidate | `docs/architecture/stage-7/generated-client-ready-subset.yaml`, `stage-7-33-ready-subset-manifest-candidate-definition.md` |
| Stage 7.34 manifest candidate validation hardening | `stage-7-34-manifest-candidate-validation-hardening.md` |
| Stage 7.35 endpoint candidate review | `stage-7-35-endpoint-candidate-review.md` |
| Stage 7.36 assistant endpoint candidate clarification | `stage-7-36-assistant-endpoint-candidate-clarification.md` |
| Stage 7.37 assistant endpoint contract/runtime alignment notes | `stage-7-37-assistant-endpoint-contract-runtime-alignment-notes.md` |
| Stage 7.38 assistant endpoint alignment cleanup decision | `stage-7-38-assistant-endpoint-alignment-cleanup-decision.md` |
| Stage 7.39 assistant endpoint contract shape cleanup | `stage-7-39-assistant-endpoint-contract-shape-cleanup.md` |
| Stage 7.40 assistant endpoint runtime contract test cleanup | `stage-7-40-assistant-endpoint-runtime-contract-test-cleanup.md` |
| Stage 7.41 assistant endpoint conformance/tooling follow-up decision | `stage-7-41-assistant-endpoint-conformance-tooling-follow-up-decision.md` |
| Stage 7.42 assistant endpoint conformance candidate implementation | `stage-7-42-assistant-endpoint-conformance-candidate-implementation.md` |
| Stage 7.43 assistant endpoint conformance candidate verification | `stage-7-43-assistant-endpoint-conformance-candidate-verification.md` |
| Stage 7.44 assistant conformance shape guard hardening | `stage-7-44-assistant-conformance-shape-guard-hardening.md` |
| Stage 7.45 assistant conformance output/operator guidance | `stage-7-45-assistant-conformance-output-operator-guidance.md` |
| Stage 7.46 assistant conformance documentation verification | `stage-7-46-assistant-conformance-documentation-verification.md` |
| Stage 7.47 remaining scope review | `stage-7-47-stage-7-remaining-scope-review.md` |
| Stage 7.48 minimal backend hotel search with fake provider | `stage-7-48-minimal-backend-hotel-search-fake-provider.md` |
| Stage 7.49 minimal hotel offer ranking | `stage-7-49-minimal-hotel-offer-ranking.md` |
| Stage 7.50 minimal Assistant-to-hotel-search handoff | `stage-7-50-minimal-assistant-to-hotel-search-handoff.md` |
| Stage 7.51 minimal frontend hotel search scenario | `stage-7-51-minimal-frontend-hotel-search-scenario.md` |
| Stage 7.52 hotel-only MVP slice final review | `stage-7-52-hotel-only-mvp-slice-final-review.md` |
| Stage 7.53 final closure and carryover | `stage-7-53-final-stage-7-closure-and-carryover.md` |

**Следующий шаг:** Stage 8.1 — LLM Orchestration Boundary and Safety Plan, только через отдельную явную planning-only задачу. Закрытие Stage 7 и эта Pre-Stage 8 синхронизация не активируют Stage 8 автоматически.

### Stage 8 — AI/LLM Orchestration Improvements

**Статус:** завершен.

**Назначение:** развитие chat-first AI/LLM orchestration: provider-independent `LlmClient` boundary, confirmation lifecycle, local search execution после user confirmation и consume-after-success policy.

**Прогресс Stage 8 по областям:**

| Область | Завершено до | Статус |
|---|---|---|
| Entry review and planning | Stage 8.0 | Carryover classification и Stage 8.1 planning завершены. |
| LLM boundary design | Stage 8.1–8.5 | `LlmClient` boundary, skeleton, orchestration, decision planning, pipeline composition завершены. |
| Handoff planning and minimal wiring | Stage 8.6–8.8 | Readiness gate, clarification/fallback wiring завершены; `ProceedWithCandidate` deferred. |
| Criteria validation and confirmation | Stage 8.9–8.15 | Proceed candidate validator, confirmation proposal, prompt wiring завершены. |
| Pending confirmation lifecycle | Stage 8.16–8.24 | Pending state, reply classifier, decision composition, consuming wiring завершены. |
| Confirmed-search pipeline | Stage 8.25–8.39 | Criteria mapper, creation plan, command builder, execution result, guard, attempt store, orchestration завершены. |
| Response mapping and integration | Stage 8.40–8.50 | Lifecycle policy, TTL/stale, retry, response mapping, integration composition, non-results wiring завершены. |
| Actual execution and consume | Stage 8.51–8.55 | Actual `CreateHotelSearchUseCase` call, SUCCEEDED recording, consume-after-success завершены. |
| Lifecycle verification and closure | Stage 8.56–8.57 | End-to-end lifecycle verified; Stage 8 formally closed with carryover. |

**Перенесенные пункты после Stage 8:**

| Категория | Статус после закрытия |
|---|---|
| InMemory stores (pending, attempt, session, search) | Accepted carryover; durable persistence — future infrastructure work |
| FakeLlmClient (deterministic) | Accepted carryover; real LLM provider — Stage 9+ |
| FakeHotelOfferProvider | Accepted carryover; real hotel provider — Stage 9 |
| Static message text | Accepted carryover; production UX copy — future work |
| Real provider/API integration | Перенесена в Stage 9 |
| Durable persistence (PostgreSQL, Redis) | Перенесена до отдельного infrastructure решения |
| Frontend UX polish (rich cards, inline retry) | Перенесена в отдельную UX задачу |
| Auth/API keys, production observability | Перенесены до отдельных security/operations решений |
| Booking, payment, flights и combined itinerary | Не входят в закрытые границы hotel-only Stage 8 |

**Ключевое ограничение закрытия:** завершение Stage 8 не означает ready real provider, real LLM, durable persistence, production readiness или автоматическую активацию Stage 9.

**Stage 8 linked artifacts by group:**

| Group | Key artifacts |
|---|---|
| Full review/audit trail | `docs/reviews/README.md` (Stage 8.0–8.57) |
| Closure and lifecycle verification | `stage-8-57-stage-8-closure-and-readiness-gate.md`, `stage-8-56-end-to-end-confirmation-lifecycle-verification.md` |

**Следующий шаг:** Stage 9 planning/readiness review, только через отдельную явную planning-only задачу. Закрытие Stage 8 не активирует Stage 9 автоматически.

### Stage 9 — Real Provider/API Integration Hardening

**Статус:** завершен Stage 9.23c. Один контролируемый chat-first happy path прошел цепочку OpenRouter → confirmation → REAL Hotels → 5 карточек из пула 20 предложений; clarification, отказные и data-integrity сценарии закрыты детерминированными проверками. `FAKE` остается режимом по умолчанию для обоих providers.

**Planning:**

| Sub-stage | Scope | Статус |
|---|---|---|
| Stage 9.0 | Documentation audit и Stage 9 planning readiness review | Завершен |
| Stage 9.1 | Hotel provider boundary review и adapter design | Завершен |
| Stage 9.2 | Provider result contract и domain mapping | Завершен |
| Stage 9.3 | Provider adapter skeleton и fake-vs-real seam | Завершен |
| Stage 9.4 | Provider error taxonomy и error handling | Завершен |
| Stage 9.5 | Provider integration verification | Завершен |
| Stage 9.6 | Real provider selection background comparison и configuration design | Завершен |
| Stage 9.7 | Selected Hotels API contract reconciliation и implementation plan | Завершен |
| Stage 9.8 | Hotels API configuration skeleton без HTTP | Завершен |
| Stage 9.8a | Hotels API authentication configuration reconciliation без HTTP/JWT signing | Завершен |
| Stage 9.9 | HTTP-транспорт публичного Hotels API без авторизации, проверяемый через `MockEngine` | Завершен; расхождение с auth в Swagger остается риском sandbox |
| Stage 9.10 | Autocomplete/location contract boundary, provider DTO и location mapper | Завершен без transport/runtime wiring; response fixture проверен в Stage 9.14 |
| Stage 9.11a | DTO поиска по Swagger без преобразования доменных моделей | Завершен; использован синтетический fixture |
| Stage 9.11b | Provider target and mapping policy readiness gate | Завершен; внутренний API и `https://hotels.tbank.ru/` подтверждены, public web orchestration не активирована |
| Stage 9.11b1 | Configuration-only public base URL reconciliation | Завершен; default URL изменен на `https://hotels.tbank.ru/` без transport/runtime wiring |
| Stage 9.11b2 | Guest occupancy contract | Завершен: канонический `childrenAges`, совместимость с `children`, clarification и idempotency rules |
| Stage 9.11b3 | Partial `HotelOffer` facts contract | Завершен: nullable rating/review/amenities и ranking без выдуманных provider facts |
| Stage 9.11b4 | Public contract alignment | Завершен: OpenAPI `childrenAges`, optional facts и frontend regression test |
| Stage 9.11c | Преобразование search DTO выбранного API в доменные модели | Завершен как mapper-only implementation без transport/runtime wiring |
| Stage 9.12 | Internal orchestration resolver → один search call → mapper без runtime wiring | Завершен; только `MockEngine`, REAL adapter не подключен |
| Stage 9.13 | Single-page candidate window без pagination | Завершен; один запрос, до 20 уникальных кандидатов, только `MockEngine` |
| Stage 9.14 | Sanitized fixture contract verification | Завершен; provider-derived responses совместимы с текущими response DTO и mapper policy |
| Stage 9.15 | Sandbox readiness gate | Завершен; выбран direct search call, обязательный preflight вынесен в Stage 9.15a |
| Stage 9.15a | Mock-only QA transport preflight | Завершен; test-scoped engine и opt-in harness проверены только через `MockEngine`, live call отсутствует |
| Stage 9.16 | Первый контролируемый QA call через проектный transport | Завершен; один request, `200`, 20 hotels/offers, без runtime wiring |
| Stage 9.17 | REAL runtime wiring readiness gate | Завершен; фактическое подключение заблокировано границами async/result/resolver/configuration |
| Stage 9.17a | Async provider/result contract reconciliation | Завершен как review/design-only этап; согласованы `suspend`, typed outcomes и state creation policy |
| Stage 9.17a1 | Backend-only async/result contract migration | Завершен; `suspend`, typed outcomes и state creation policy реализованы без REAL runtime wiring |
| Stage 9.17b | Autocomplete resolver transport adapter | Завершен; отдельный `input` DTO, `MockEngine`, без runtime wiring |
| Stage 9.17c | Opt-in REAL runtime wiring с FAKE default | Завершен; public-only config, production client lifecycle и typed adapter composition |
| Stage 9.18 | Integration closure verification | Завершен после Stage 9.18a: regression/failure matrix и повторный runtime smoke пройдены |
| Stage 9.18a | Deterministic location candidate selection | Завершен; exact match по нормализованным `name`/`signature`, без first-result fallback |
| Stage 9.19a | Накопление контекста диалога | Завершен; process-local canonical hotel constraints, correction и child-age clarification по assistant session |
| Stage 9.19b | Асинхронная LLM-граница | Завершен; сквозной `suspend`, безопасный fallback и проброс cancellation без изменения public API |
| Stage 9.20 | OpenRouter adapter без runtime wiring | Завершен; typed config, strict JSON Schema и safe response mapping проверены только через `MockEngine` |
| Stage 9.21 | Opt-in OpenRouter runtime и QA | Завершен; отдельный `HttpClient`, fail-closed wiring и контролируемый confirmation QA подтверждены, `FAKE` остается default |
| Stage 9.21a | Безопасная диагностика результатов OpenRouter | Завершен; ограниченные внутренние категории и runtime observer проверены через `MockEngine`, повторный live-вызов отсутствует |
| Stage 9.21b | Диагностический QA-повтор | Завершен; один вызов вернул `200`, `ask_clarification`, confirmation prompt и `CANDIDATE_DECODED` без `hotelSearchId` |
| Stage 9.21c | Ограниченная политика повторов OpenRouter | Завершен; максимум две одинаковые попытки только для временных, пустых или некорректных результатов, без смены модели и live QA |
| Stage 9.22 | Chat-first frontend | Завершен; одна Assistant session, clarification/boundary в transcript, до пяти уже ранжированных offers и отдельная diagnostic page |
| Stage 9.23 | Внутренний пилот chat-first MVP | Завершен; один REAL happy path и детерминированная pilot-матрица подтверждены без production-readiness claim |
| Stage 9.23a | Усиление семантического контракта OpenRouter candidate | Завершен; descriptions полей схемы, правила outcome и нормализация пустых nullable-значений без повторного live QA |
| Stage 9.23b | Сквозная проверка успешного chat-first сценария | Завершен; confirmation, явное «Да», REAL Hotels и 5 карточек из пула 20 предложений подтверждены |
| Stage 9.23c | Pilot-матрица и стабилизация пользовательского текста | Завершен; clarification, отказные и data-integrity ветки проверены, русскоязычный текст и favicon исправлены |

**Границы:** выбранный public flow использует `/search-api/search/autocomplete` и `POST /api/v1/hotels/search` на `https://hotels.tbank.ru/`. Его анонимный вызов подтвержден технически, но официальный server-to-server статус и долгосрочная стабильность не подтверждены. `REAL` включается только явно, `FAKE` остается default. Повтор OpenRouter не применяется при ошибках аутентификации, недостатке средств, `429`, некорректном request и неизвестных отказах; задержка, `Retry-After` и смена модели отсутствуют. Booking/payment/cancellation, durable storage и production-hardening не входят в текущий slice.

**Следующий шаг:** Stage 10 не активирован. Перед cross-platform expansion нужна отдельная planning/readiness задача, которая выберет первый ограниченный клиентский сценарий и подтвердит, что он не расширяет hotel-only MVP без решения владельца. Booking, payment, pagination, durable storage и production readiness не следуют автоматически из закрытия Stage 9.

### Stage 10 — Cross-platform Expansion

**Статус:** завершен. Stage 10.0–10.4 подтвердили платформонезависимую границу
сервиса и отделили локальную demo shell от будущих продуктовых клиентов.

**Границы:** подтверждение платформонезависимой сервисной границы без
реализации будущих продуктовых клиентов и без переноса business logic из
backend.

| Sub-stage | Scope | Статус |
|---|---|---|
| Stage 10.0 | Cross-platform readiness и сверка открытых вопросов | Завершен; первым target выбран устанавливаемый responsive web/PWA |
| Stage 10.1 | Bounded PWA foundation для проверенного chat-first hotel flow | Завершен; manifest, локальные icons, standalone/mobile metadata, safe-area и `no-store` boundary |
| Stage 10.2 | Cross-platform client contract и accessibility verification | Завершен; same-origin web/PWA проверен, native/desktop API-совместимы архитектурно, OpenAPI остается `not_ready` |
| Stage 10.3 | Platform-neutral API contract hardening | Завершен; три chat-first endpoint согласованы с runtime, строгая message validation и manifest reference checks добавлены, весь OpenAPI остается `not_ready` |
| Stage 10.4 | Service integration boundary и client ownership | Завершен; web/PWA остается локальной demo shell, product UI/SDK принадлежат будущим интеграционным командам |

**Принятые решения Stage 10.0:** первый клиентский срез остается online-only и
обращается только к Travel Assistant API. Он не кэширует API responses,
transcript или provider facts. Native iOS/Android clients, auth, durable storage
и cross-device sync не входят в Stage 10.1. Неизвестные SLA, rate limits и
официальный server-to-server статус Hotels API не блокируют локальную PWA
foundation, но остаются обязательными внешними gates перед публичным rollout.

`shownPrice` отображается как provider total за выбранный период без
перерасчета и без утверждения о включенных taxes/fees. `LIMITED` не выводится
эвристически. Hotel details, current-session shortlist и отдельный
explanation/comparison flow пока не реализованы: это не блокирует PWA
  foundation, но блокирует заявление о полной реализации MVP v1.

Stage 10.2 подтвердил desktop/mobile browser matrix, keyboard/focus semantics,
live regions, touch targets и отсутствие horizontal overflow. Любой клиент
должен использовать Travel Assistant `/api/v1/**`; provider/LLM orchestration и
business rules остаются в backend. Same-origin web/PWA поддерживается,
cross-origin web требует будущей CORS allowlist, а native/desktop SDK и
resume/cross-device пока не реализованы.

Stage 10.3 закрепил платформенный subset из создания Assistant session,
продолжения диалога и чтения offers по opaque `searchId`. `GET /health`
классифицирован как operational, прямой `POST /hotel-searches` — как
diagnostic-only, shortlist/explanation — как placeholders. Message body
проверяется на строгий JSON и длину 1–4000 Unicode code points. CORS остается
default-deny: wildcard и credentials не разрешены; будущая allowlist может
содержать только точные origin со scheme/host/port. OpenAPI в целом и generated
clients остаются `not_ready`.

Stage 10.4 закрепил backend как самостоятельный сервис и web/PWA как локальную
demo shell MVP. Product web, Android, iOS и другие UI будут создаваться позже
отдельными командами и задачами. Сервис предоставляет три chat-first endpoint и
OpenAPI, но не выбирает заранее platform SDK, toolchain или UI architecture.
Backend/domain logic, provider DTO и secrets не передаются клиентам. Решение
зафиксировано в `ADR-0001`; OpenAPI/SDK readiness не заявлена. CORS остается
default-deny, auth, durable storage, resume и cross-device sync — вне текущего
среза.

### Stage 11 — Local MVP Demonstration Readiness

**Статус:** завершен Stage 11.0. Следующим активирован Stage 12.

**Границы:** воспроизводимая локальная демонстрация уже реализованного
chat-first hotel flow. Stage не создает product web/mobile clients, deployment,
auth, durable storage, booking или новые продуктовые сценарии.

| Sub-stage | Scope | Статус |
|---|---|---|
| Stage 11.0 | Local REAL MVP demo readiness | Завершен; launcher, runbook, deterministic FAKE preflight и один контролируемый REAL browser smoke с 5 карточками из provider pool 20 |

Основной демонстрационный профиль использует opt-in `OPENROUTER` и `REAL`
Hotels API. `FAKE` сохраняется как default production configuration и как
детерминированный preflight/fallback. Успех локального демо не означает
production readiness или готовность к внешнему rollout.

Stage 11.0 добавил безопасный launcher, который читает игнорируемый `.env` как
данные, проверяет Java 17/Node.js/configuration/ports, запускает backend и demo
shell и завершает их совместно. Контролируемый REAL browser smoke подтвердил
confirmation до поиска, отсутствие карточек до «Да», получение 20 REAL offers
и отображение первых 5. Raw provider/LLM data и secrets не публиковались.

Stage 12 завершил функциональный MVP итеративного уточнения hotel search.
Deployment, product clients и production hardening по-прежнему требуют
отдельных roadmap-решений. Текущим отдельным решением активирован только
on-demand details-срез Stage 13.

### Stage 12 — Итеративное уточнение hotel search

**Статус:** завершен Stage 12.8; production readiness не заявлена.

**Цель:** позволить пользователю получить первичные предложения, уточнить
необязательные предпочтения в чате, подтвердить полный обновленный набор
критериев и выполнить новый ограниченный provider search.

| Sub-stage | Scope | Статус |
|---|---|---|
| Stage 12.0 | MVP и roadmap reconciliation | Завершен; итеративное уточнение принято как текущий функциональный MVP, shortlist/details/comparison перенесены в расширение |
| Stage 12.1 | Filter contract verification | Завершен с contract drift: catalog вернул `review_rating` как `radio` со значениями `9..5`, а не как ожидаемый `range`; availability и filtered search не вызывались |
| Stage 12.1a | Reconciliation семантики минимального гостевого рейтинга | Завершен; разрешены только дискретные пороги `5`, `6`, `7`, `8`, `9`, без округления |
| Stage 12.1b | Controlled filter request verification | Завершен; availability endpoint принял четыре filter shape и вернул пустой payload, filtered search отклонил `sort` с `sorting_is_not_allowed_yet`; пользовательские sort preferences отложены |
| Stage 12.1c | Filtered search verification without sort | Завершен; один запрос без `sort` вернул `200` и 20 предложений, соответствующих price/stars/rating/cancellation facts |
| Stage 12.2 | Provider-neutral preference model | Завершен; четыре preferences, атомарный session patch, confirmation и idempotency support без provider/runtime wiring |
| Stage 12.3 | LLM extraction для уточнений | Завершен; typed `SET`/`CLEAR`, строгая refinement schema и fail-closed core runtime без provider execution |
| Stage 12.4 | Hotels API filter mapping | Завершен; четыре preferences детерминированно преобразуются в проверенные filters, offer facts дополнены stars/cancellation без runtime refinement |
| Stage 12.5 | Refinement runtime flow | Завершен; typed patch сохраняется в session context, полный confirmation предшествует одному новому provider search |
| Stage 12.6 | Platform-neutral response alignment | Завершен; offers response содержит только активные preferences и подтверждённые nullable stars/cancellation facts |
| Stage 12.7 | No-results refinement | Завершен; один provider-neutral совет без автоматического изменения preferences или нового поиска |
| Stage 12.8 | MVP verification | Завершен; regression, safe empty/failure outcomes и один отдельно разрешенный REAL smoke пройдены |

Принятые продуктовые правила Stage 12.0:

- необязательные фильтры не задерживают первый поиск;
- бюджет без валюты трактуется как RUB, автоматическая конвертация не
  выполняется;
- цена означает provider total за весь период;
- каждое изменение provider request требует нового подтверждения;
- фильтры применяются новым provider search, а не только к сохраненному
  пулу из 20 предложений;
- успешное уточнение создает новый `hotelSearchId`, предыдущий process-local
  search остается доступен;
- pagination, filter panel, auth, durable storage, booking и payment не входят
  в Stage 12.

Контрактная основа для проверки Stage 12.1:

- `POST /search-api/search/autocomplete` — разрешение destination;
- `POST /api/v1/hotels/search` — основной поиск с `filters` и `sort`;
- `GET /api/v2/hotels/search-filters` — каталог значений фильтров provider;
- `POST /api/v1/hotels/search-filters-availability` — возможное будущее
  объяснение доступных или ослабляемых фильтров.

Stage 12.1b выполнил два отдельно разрешенных вызова без retries. Availability
endpoint принял четыре filter shape, но вернул пустой payload, а filtered search
запретил `sort`. Stage 12.1c затем выполнил один запрос без `sort` и получил
`200` с 20 предложениями, фактически соответствующими price, stars,
review-rating и free-cancellation условиям. Пользовательские sort preferences и
filter availability остаются отложенными. Это разблокировало Stage 12.2,
который добавил внутреннюю модель четырех provider-neutral preferences,
атомарные операции `KEEP`/`SET`/`CLEAR`, session-bound накопление и учет в
confirmation/idempotency без LLM extraction, provider mapping или runtime
search. Stage 12.3 добавил отдельный strict structured-output профиль для
typed preference patch и его чистое преобразование в `KEEP`/`SET`/`CLEAR`.
Stage 12.4 добавил точные provider filter DTO/mapping, сохранил один request с
`offset=0`, `limit=20`, не добавил `sort` и сохранил подтвержденные
`starRating` и `freeCancellationUntil` как внутренние facts. Stage 12.5 затем
активировал strict refinement-профиль OpenRouter, session-bound применение
typed patch и полный повторный confirmation. До ответа «Да» provider не
вызывается; успешное уточнение выполняет один новый поиск, а предыдущий
`hotelSearchId` остается доступным. Stage 12.6 добавил в прежний offers
endpoint необязательные `starRating`, `freeCancellationUntil` и
`appliedPreferences`, согласовал OpenAPI/conformance и показ этих данных в
локальной demo shell. Неизвестные факты отсутствуют в JSON, provider DTO не
раскрываются. Stage 12.7 разделил успешную пустую выдачу и provider failure,
добавил одну typed-рекомендацию ослабить активное preference в порядке
`minimumGuestRating` → `stars` → `freeCancellationRequired` → `maxTotalPrice`.
Backend и demo shell не применяют совет автоматически: пользовательская
реплика проходит прежний confirmation flow и только затем может создать новый
поиск. `search-filters-availability` не подключён. Stage 12.8 подтвердил
regression-сценариями и одним REAL browser smoke, что initial search и
refinement выполняются только после отдельных подтверждений, refinement
создаёт новый `hotelSearchId`, предыдущий поиск остаётся доступен, а пустой
успешный результат и provider failure не смешиваются. Пул из 20 предложений
сохраняется на backend, demo shell показывает первые 5. Stage 12 завершён;
Stage 13 активирован отдельной задачей и не меняет границы закрытого MVP.

### Stage 13 — Детали выбранного hotel offer

**Статус:** завершён Stage 13.7; следующий шаг — Stage 14.0.

**Цель:** позволить пользователю явно выбрать предложение из сохранённого
поиска и запросить дополнительные provider-backed facts без N+1-загрузки,
раскрытия provider `hotelId` или перехода к booking/rates lifecycle.

| Sub-stage | Scope | Статус |
|---|---|---|
| Stage 13.0 | Selected hotel details readiness и open-question reconciliation | Завершен; выбран `GET /api/v1/hotels/{hotelId}` как первый contract candidate |
| Stage 13.1 | Controlled hotel details contract verification | Завершен; search и details вернули `200`, строковый search `hotelId` принят details path, добавлен sanitized fixture |
| Stage 13.2 | Provider-neutral details model и mapping | Завершен; bounded domain model, tolerant DTO и fixture-driven typed mapping без HTTP |
| Stage 13.3 | Opaque offer identity и selected-offer resolution | Завершен; provider candidates не назначают ID, public `providerOfferRef` удалён, resolve ограничен указанным search |
| Stage 13.4 | Details transport и provider adapter | Завершен; safe GET, host/path protection, typed outcomes и mapping через `MockEngine`, без wiring |
| Stage 13.5 | Platform-neutral details API | Завершен; typed details endpoint, safe errors и четвёртый `platform_client_candidate`, OpenAPI остаётся `not_ready` |
| Stage 13.6 | Opt-in REAL details runtime wiring | Завершен; search/details используют один runtime и общий Hotels API client, `FAKE` остаётся default |
| Stage 13.7 | Selected hotel details demo flow | Завершен; явная кнопка загружает details выбранного opaque offer без N+1 и chat selection commands |

Stage 13.0 закрыл сортировку и `search-filters-availability` как осознанно
отложенные возможности текущего MVP. Taxes/fees в `shownPrice` остаются
unknown без перерасчёта. Официальный S2S-статус, SLA и rate limits остаются
внешними rollout gates, но не блокируют один контролируемый contract probe.
Details разрешены только on demand после явного выбора пользователя; массовая
загрузка для всех 20 offers запрещена. Rates, deeplink, shortlist, comparison,
booking и payment не активированы.

Stage 13.1 выполнил один prerequisite search и один details request без auth,
redirect или retry. Оба вернули `200 application/json`; строковый `hotelId` из
search response принят `GET /api/v1/hotels/{hotelId}`. Обезличенный fixture
подтвердил `payload` с location, images, facilities, rules, payment methods и
certification data. Одно наблюдение не доказывает universal requiredness,
официальную S2S-поддержку или error semantics. Поэтому Stage 13.2 должен
оставаться provider-neutral и устойчивым к отсутствующим optional fields, не
перенося contacts/certification в public model автоматически.

Stage 13.2 добавил provider-neutral `HotelDetails`, tolerant provider DTO и
fixture-driven mapper. В модель входят только отображаемые facts; contacts,
certification, owner/register data, provider codes и сложные rules исключены.
Optional поля сохраняют unknown, images проходят bounded HTTPS policy, а
невалидные identity/location/time values дают typed mapping error. HTTP,
selected-offer resolution и runtime wiring ещё не добавлены.

Stage 13.3 перенёс назначение `offerId` в application layer: provider boundary
возвращает кандидатов без client-facing ID, а сохранённый search получает
process-local opaque IDs, не содержащие provider `hotelId`. Публичное поле
`providerOfferRef` удалено из runtime JSON и OpenAPI. Выбранное предложение
разрешается только по паре `hotelSearchId + offerId`; unknown search и unknown
offer остаются разными typed outcomes. Details transport и runtime вызов ещё
не подключены.

Stage 13.4 добавил safe JSON GET в существующий public Hotels API transport и
application-owned `HotelDetailsProviderBoundary`. Adapter кодирует opaque
provider reference как один path segment, проверяет identity ответа и
возвращает `Loaded`, `NotFound`, `ResponseRejected` или
`ProviderUnavailable`. Все проверки выполнены через `MockEngine`;
`Application.kt`, routes и runtime composition не изменены.

Stage 13.5 добавил platform-neutral endpoint деталей по паре opaque
`hotelSearchId + offerId`. Разрешение выбора остаётся search-bound, provider
identity не попадает в response, а not-found, invalid provider response и
temporary unavailable получают безопасные typed HTTP outcomes. OpenAPI и
subset manifest согласованы с runtime как четвёртый
`platform_client_candidate`, но сохраняют `not_ready`. На момент Stage 13.5
REAL details adapter ещё не был подключён к runtime.

Stage 13.6 подключил details boundary к общему hotel provider runtime. В режиме
`REAL` search и details используют один application-owned Hotels API
`HttpClient`; в режиме `FAKE` доступен детерминированный details provider без
network calls. Details по-прежнему загружаются только по явному endpoint-вызову
для выбранного offer; live call и frontend flow не выполнялись.

Stage 13.7 добавил в локальную demo shell явную кнопку «Подробнее». Frontend
передаёт backend только opaque `hotelSearchId + offerId`, загружает сведения
одной выбранной карточки и не обращается к Hotels API напрямую. Неизвестные
поля не отображаются как нули или пустые значения; предусмотрены loading,
empty и безопасное error-состояния. Повторное раскрытие уже загруженного блока
не создаёт новый запрос. Browser QA подтвердил focus management, отсутствие
горизонтального overflow на 320 и 390 CSS px и высоту основной кнопки 44 CSS
px. Stage 13 завершён без rates, deeplink, shortlist, comparison или booking.

### Stage 14 — Закрытие рабочего hotel-only MVP

**Статус:** Stage 14.0–14.7 завершены. Финальная REAL-проверка
подтвердила exact-hotel flow и раннюю multi-room границу.

| Sub-stage | Scope | Статус |
|---|---|---|
| Stage 14.0 | Закрытие рабочего hotel-only MVP | Завершён; зафиксирован `demo-ready MVP` |
| Stage 14.1a | Безопасность details и понятное confirmation | Завершён; служебные sections фильтруются fail-closed, confirmation переведён на естественный русский текст, внутренний offer получает первый безопасный image URL |
| Stage 14.1b | Optional image в публичном offer contract | Завершён; `imageUrl` optional, только HTTPS, без provider identity или N+1; OpenAPI остаётся `not_ready` |
| Stage 14.1c | Компактные карточки результатов | Завершён; responsive redesign, fallback и ветка реальных изображений подтверждены browser QA |
| Stage 14.2 | Часовой пояс клиента и безопасность дат | Завершён; один номер используется по умолчанию, timezone устройства объединяется с backend `Clock`, относительные даты и даты без года при отсутствии timezone требуют уточнения, прошедшие даты из LLM не доходят до поиска |
| Stage 14.3 | Фильтр включённого завтрака | Завершён; provider catalog подтвердил `meal_types=["breakfast"]`, preference проходит confirmation/idempotency/search, nullable breakfast fact отображается без догадок |
| Stage 14.4 | Безопасная диагностика LLM и fallback UX | Завершён; runtime пишет только фиксированные OpenRouter/application категории, временные сбои и ошибки понимания имеют разные безопасные сообщения, проблемная фраза закреплена regression test |
| Stage 14.5 | Явное уточнение звёзд и первичная диагностика изображений | Завершён; точная одиночная категория звёзд детерминированно дополняет пропуск LLM, а причина отсутствия `imageUrl` передана в Stage 14.6 |
| Stage 14.6 | Разрешение provider image template | Завершён; `{size}` безопасно заменяется на подтверждённый `1024x768`, пять REAL-карточек загрузили изображения без proxy или N+1 |
| Stage 14.7 | Поиск конкретного отеля и устойчивость уточнений | Завершён; REAL-перепроверка подтвердила раннюю блокировку нескольких номеров, сохранение destination, дат и гостей после исправления, новое confirmation и exact-hotel details/rates с одним результатом |

Stage 14.0 повторно выполнил backend, frontend, launcher и OpenAPI conformance
gates, проверил secret/provider-ID boundaries и провёл один разрешённый REAL
browser smoke без retry. Полный запрос создал confirmation prompt до поиска;
после отдельного «Да» backend получил 20 provider offers, demo shell показала
5 карточек, а явный выбор одной карточки выполнил ровно один details request.
Browser использовал только локальные `/api/v1/**`; public response и URL не
раскрыли provider `hotelId`.

Итоговый статус до закрытия Stage 14.7 остаётся `demo-ready MVP`. Stores остаются process-local, `FAKE`
остаётся default, OpenAPI/generated clients сохраняют `not_ready`. Это не
production readiness и не готовность к внешнему rollout. Rates, deeplink,
shortlist, comparison, booking, payment, auth, durable storage, deployment и
product clients не активированы. Публичный rates/booking lifecycle не
активирован. Любая следующая функциональность требует
отдельного product/roadmap decision.

Stage 14.1 активирован отдельной задачей после REAL smoke Stage 14.0. Stage
14.1a закрыл обнаруженное раскрытие certification/registry/owner/contact data
через provider description sections с помощью строгой allowlist, вынес общую
проверку безопасных HTTPS images и подготовил первое изображение во внутренней
модели offer. Confirmation теперь показывает даты, гостей, номера и active
preferences обычным русским текстом; поиск по-прежнему запускается только
после отдельного «Да». Публичный image contract и demo-redesign остаются
раздельными Stage 14.1b и Stage 14.1c. Stage 14.1b добавил optional `imageUrl`
в provider-neutral offers response и OpenAPI без generated clients, details
lookup или N+1. Subset manifest сохранил `not_ready` и
`readinessClaim=false`.

Кандидат Stage 14.1c переработал только локальную demo shell: на desktop карточки имеют
горизонтальный layout, на mobile — вертикальный; первый optional image
загружается с `no-referrer`, а unknown или ошибочный URL заменяется нейтральным
CSS-placeholder. Повторяющийся `matchSummary` скрыт в presentation-слое, при
этом API field и backend ranking не изменены. Правило ранжирования отображается
один раз над списком. On-demand details, keyboard focus и отсутствие N+1
сохранены. Локальные browser/gate проверки прошли. Единственный REAL smoke
успешно выполнил confirmation, один search и один details request. На момент
Stage 14.1c public offers response не содержал `imageUrl`, поэтому ветка
реальных изображений ещё не была подтверждена; точную причину и исправление
позже отдельно зафиксировал Stage 14.6. Автоматический retry не выполнялся.

Stage 14.2 активирован отдельным сообщением о критической ошибке после ручной проверки
demo shell. Assistant flow теперь считает неуказанное количество номеров равным
одному. Demo shell передаёт IANA timezone устройства, а backend вычисляет
локальную reference date по собственному `Clock`; timestamp устройства не
используется. При отсутствии или ошибке timezone относительные и не содержащие
год даты не угадываются, а запрашиваются явно. Любая дата заезда в прошлом,
включая ошибочно извлечённый LLM год `2025` при текущем `2026`, очищается до
confirmation и не создаёт search. Source whitespace больше не создаёт ложный
абзац в assistant bubble.

Stage 14.3 закрыл открытый вопрос о завтраке без нового live probe: сохранённый
provider-derived catalog подтверждает `meal_types` с `$objectType=array` и
значением `breakfast`, а search fixture содержит `mealType=breakfast` и
`mealType=nomeal`. Requirement добавлен как пятое optional preference,
учитывается в LLM patch, confirmation, idempotency и одном provider search.
Другие meal plans не выводятся как наличие завтрака и остаются unknown.
Offers API и demo shell показывают nullable факт без provider DTO или ID.

Stage 14.4 воспроизвёл проблемную формулировку одним контролируемым OpenRouter
вызовом: текущий candidate корректно прошёл decoder и validator, поэтому
детерминированная ошибка извлечения не подтверждена. Runtime теперь пишет
только фиксированные категории transport/decoder и application fallback без
prompt, raw response, secrets, модели или идентификаторов. Временный сбой
просит повторить сообщение, а неоднозначный candidate — переформулировать его.
Существующий `SINGLE_RETRY` сохранён без дополнительного сетевого повтора.

Stage 14.5 воспроизвёл отдельный семантический пропуск: OpenRouter candidate был
успешно декодирован, но не содержал `stars` для фразы «отель должен быть
пятизвездочным». Application-owned parser теперь детерминированно дополняет
только точную одиночную категорию и не угадывает диапазоны, сравнения,
отрицания или снятие ограничения. Событие дополнения логируется фиксированной
безопасной категорией.

Stage 14.6 уточнил прежнюю диагностику изображений. Основной search возвращает
HTTPS templates с буквальным `{size}`; policy отклоняла их до URI validation.
Ограниченная live-проверка подтвердила `1024x768` ответом `200 image/jpeg`.
Подстановка разрешена только для подтверждённого CDN, неизвестные placeholders
отклоняются. REAL browser smoke показал пять полностью загруженных изображений.
`getHotelStaticInfo` также проверен, но не подключён: дополнительные provider
calls для карточек не нужны.

Stage 14.7 активирован явным решением владельца после воспроизведения запроса
конкретного отеля. Вспомогательная длительность проживания теперь сохраняется
между репликами и позволяет вычислить checkout после уточнения check-in;
детерминированная policy формирует вопрос только по фактически отсутствующим
полям. Autocomplete hotel candidate хранится отдельно от location candidate:
строковый provider hotel reference никогда не используется как числовой
`destinationId`. Для единственного явно названного отеля после confirmation
выполняются один details и один v3 rates request; в публичный результат
попадает прежний opaque `offerId`, а `bookHash` и provider IDs не моделируются.
Первый REAL smoke выявил, что валидный OpenRouter candidate может пропустить
явно названный отель. Application теперь консервативно дополняет отсутствующий
destination из достаточно явного собственного названия и пишет только
фиксированное событие `DESTINATION_ENRICHED`. Автоматический live retry не
выполнялся. Последующая ручная перепроверка подтвердила exact-hotel поиск, но
выявила, что запрос двух номеров доходил до confirmation и отклонялся только
provider mapper-ом. Теперь MVP явно поддерживает один номер на один поиск:
большее значение очищается, получает понятное уточнение и не создаёт
pending confirmation или вызов provider; распределение гостей между номерами
не имитируется. Внутренний `rooms=1` скрыт из обычного confirmation и demo
controls. Пустая успешная выдача также больше не сопровождается фразой
«Результат готов». Финальная REAL-перепроверка выявила узкий пробел в local confirmation
classifier для словесных room counts. После regression-исправления сценарий подтвердил раннюю
multi-room блокировку, сохранение destination, дат и гостей, новое confirmation и один успешный
exact-hotel details/rates flow. Автоматический live retry не выполнялся. Stage 14.7 завершён;
общий room/rates browsing и booking flow остаются вне MVP.

### Stage 15 — Портируемый backend и эксплуатационная наблюдаемость

**Статус:** завершён.

| Sub-stage | Scope | Статус |
|---|---|---|
| Stage 15.0 | Архитектурная и deployment-проверка | Завершён; responsibility map и source-level guards подтверждают backend-owned business logic, а process-local stores явно ограничивают deployment одним instance |
| Stage 15.1–15.2 | Request correlation и структурированные operational events | Завершён; safe `X-Request-ID`, application-owned sink, stdout JSON Lines и bounded service/HTTP/business/dependency/error events покрыты tests |
| Stage 15.3 | Liveness, readiness и OpenMetrics | Завершён; root probes и OpenMetrics 1.0 работают без upstream polling, IDs/raw paths в labels и self-scrape noise |
| Stage 15.4 | Operational contract, conformance и runbook | Завершён; `X-Request-ID` и error contract синхронизированы, proxy/conformance защищены tests, root operational routes исключены из product clients, runbook зафиксирован |

Stage 15 не меняет business behavior, ranking, provider mapping и product success
responses. Durable storage, multi-instance coordination, distributed tracing, raw conversation
capture, auth, CORS, deployment manifests и monitoring vendor остаются вне этапа.

Итоговые quality gates подтвердили backend tests, frontend tests/lint/build,
OpenAPI conformance tests/check и чистый `git diff --check`. Stage 15 закрыт без
заявления multi-instance/HA или полной production readiness.

### Stage 16 — Semantic-анализ типа размещения

**Статус:** Stage 16.0–16.9 readiness завершены. Смешанный REAL Hotels + FAKE
semantic runtime закрыт fail-closed, OpenRouter adapter фиксирует exact endpoint
без fallback, а launcher сохраняет semantic `FAKE`. REAL rollout не активирован.

| Sub-stage | Scope | Статус |
|---|---|---|
| Stage 16.0 | Feasibility, taxonomy и policy gates | Завершён; широкое определение `GLAMPING`, границы provider facts/semantic assessment и критерии quality evaluation закреплены. Право передавать provider descriptions/images внешней модели и совместимость выбранного ZDR endpoint не подтверждены, поэтому REAL vision не активирован и controlled probe не выполнялся |
| Stage 16.1 | Conversation model | Завершён; managed `GLAMPING` извлекается и снимается детерминированно и через strict LLM schema, сохраняется между уточнениями, входит в confirmation/idempotency и сопровождается booking-boundary copy |
| Stage 16.2 | Classification core | Завершён; application-owned port, typed verdict/evidence/signals, fail-closed batch validation, merge/selection policy и deterministic network-free FAKE implementation покрыты tests |
| Stage 16.3 | Multimodal adapter | Завершён; opt-in OpenRouter adapter, strict schema, exact-host URL policy, privacy routing, batching и typed failures проверены только через MockEngine; REAL activation отсутствует |
| Stage 16.4 | Async lifecycle | Завершён; semantic search сначала сохраняется как `searching`, application-owned scheduler ограничивает job 45 секундами, не допускает duplicate launch, атомарно публикует terminal state и отменяется при shutdown |
| Stage 16.5 | Two-pass orchestration | Завершён; coarse до 20, deep до 6, concurrency 3/2, 45-second lifecycle budget, partial fallback, bounded details cache и FAKE-default runtime composition покрыты tests |
| Stage 16.6 | Public contract и demo shell | Завершён; response/OpenAPI/conformance расширены async status, bounded analysis/semantic fields, demo polling 1–3 секунды с лимитом 120 секунд и безопасным evidence presentation |
| Stage 16.7 | Observability, evaluation и closure | Завершён; bounded semantic search/dependency events и metrics, rights-safe evaluation harness/schema и aggregate report добавлены, полный backend regression gate пройден. REAL smoke не выполнялся, потому что provider-content approval, ZDR/model probe и dataset quality gates не закрыты |
| Stage 16.8a | Backend semantic runtime safety | Завершён; composition policy запрещает REAL Hotels + FAKE semantic, не создаёт analyzer runtime/scheduler и возвращает сохранённый terminal `failed` snapshot с пустыми offers без provider calls. Обычный REAL search и полный FAKE flow не изменены |
| Stage 16.8b | Async UX и прозрачность режимов | Завершён; initial `searching` сообщает о запуске проверки отдельно от duplicate, terminal frontend states не сохраняют loading copy, а launcher явно показывает LLM, Hotels и semantic modes без secrets/model slug и фиксирует semantic `FAKE` в обоих demo-профилях |
| Stage 16.8c | Regression и closure | Завершён; exact word-form patterns и structural-evidence sufficiency блокируют ложные `MATCH`/`PROBABLE` для `город`, `городской`, `домашний`, обычных hotel descriptions и nature/amenity-only fixtures. Полные backend/frontend/conformance gates пройдены без REAL vision call |
| Stage 16.9 | REAL semantic activation readiness | Завершён как configuration/policy readiness без REAL calls: model и exact endpoint задаются отдельно, `provider.only` содержит одну пару, fallback запрещён, EU-only shortlist и rights/evaluation gates зафиксированы. Activation остаётся `BLOCKED` до письменного разрешения, controlled probe и quality dataset |

Диагноз и общие границы Stage 16.8 зафиксированы в
`docs/reviews/stage-16-8-semantic-runtime-safety-plan.md`, результат Stage 16.8a —
в `docs/reviews/stage-16-8a-backend-semantic-runtime-safety.md`, а результат
Stage 16.8b — в
`docs/reviews/stage-16-8b-async-ux-and-runtime-mode-transparency.md`, а closure
Stage 16.8c — в
`docs/reviews/stage-16-8c-semantic-runtime-safety-closure.md`. Stage 16.9
readiness зафиксирован в
`docs/reviews/stage-16-9-real-semantic-activation-readiness.md`. Stage 16.8 не
меняет taxonomy, ranking, provider mapping или обычный hotel search. Backend
safety, frontend/demo изменения и closure выполнены отдельными commits и review
reports.

Первый и единственный активный semantic concept — `GLAMPING`. Он охватывает
оборудованные tents, domes, yurts, safari tents, tiny houses и отдельные cabins
в природном формате. Обычные hotel rooms, apartment blocks, пустые camping
pitches и стандартные cottages без признаков glamping исключаются. `APARTMENT`
не активирован и требует отдельного определения и quality evaluation.

Semantic verdict является assessment ассистента, а не provider fact. Provider
остаётся источником availability, цены, рейтинга, amenities, descriptions и
изображений. Booking и payment не входят в Stage 16; запрос «забронировать
глемпинг» запускает подбор с явным сообщением этой границы.

До закрытия внешнего policy gate разрешены conversation slice, provider-neutral
classification core, FAKE mode, тестовые fixtures и evaluation harness без
provider content. Передача provider descriptions/images во внешнюю модель,
REAL vision activation и controlled live probe запрещены, пока владелец данных
не подтвердит такое использование, а выбранные model/provider endpoint —
совместимость с `require_parameters=true`, `data_collection=deny` и `zdr=true`.

Quality gate перед REAL rollout требует не менее 100 вручную размеченных
кандидатов из нескольких направлений, precision `MATCH` не ниже 90%, precision
`MATCH + PROBABLE` не ниже 80%, recall не ниже 70% и false-positive rate для
обычных отелей не выше 5%. Пограничная часть размечается двумя независимыми
reviewer. Изображения не коммитятся без подтверждённых прав.

Evaluation harness находится в `tools/semantic-evaluation/`; текущий aggregate
report имеет статус `NOT_RUN`, поскольку rights-approved dataset ещё не
предоставлен. Stage 16.9 подготовил EU-only shortlist и exact endpoint pinning,
но это не закрывает rights, probe и quality gates. REAL activation не разрешена
автоматически.

### Stage 17 — Корпоративная переносимость semantic analysis

**Статус:** Stage 17.0 завершён без REAL calls. Model/provider/runtime остаются
сменным deployment за внутренним gateway; `FAKE` сохраняется default.

| Sub-stage | Scope | Статус |
|---|---|---|
| Stage 17.0 | Provider-neutral corporate semantic portability | Завершён; добавлены `INTERNAL_GATEWAY` mode, contract v1, opaque deployment ID, exact endpoint/token/allowlist config, fail-closed adapter tests, ADR и transfer readiness checklist |

Stage 17.0 сохраняет существующий `AccommodationAnalysisClient` и
two-pass orchestration. Gateway request не содержит session/search/offer или
provider IDs и ограничивает descriptions, amenities и images. Response обязан
вернуть ту же contract version и deployment ID; schema/deployment drift,
unknown verdict/evidence и network failures преобразуются в существующий typed
failure без retry или automatic fallback.

Конкретная модель не выбрана предположением. После переноса корпоративная
платформа формирует 2–3 доступных deployment и оценивает их на одном
rights-approved dataset. Первый rollout использует один deployment для coarse
и deep passes; второй прошедший candidate может быть только ручным rollback
target. Порядок переноса и model-selection gate находятся в
`docs/guides/corporate-transfer-readiness.md`, архитектурное решение — в
`docs/decisions/adr-0002-provider-neutral-semantic-gateway-boundary.md`, а
closure — в
`docs/reviews/stage-17-0-corporate-semantic-portability.md`.

Stage 17.0 не меняет public API/OpenAPI, taxonomy `GLAMPING`, ranking, Hotels
provider mapping, frontend, durable storage или deployment manifests. Internal
gateway implementation, corporate auth/workload identity, model weights,
probe, evaluation и rollout требуют отдельных задач после появления внешних
входных данных.

### Параллельный experimental toolstream — T-Bank Banking MCP

**Статус:** первый безопасный срез реализован отдельно от core roadmap stages.

`tools/tbank-banking-mcp` добавляет локальный phone auth вне LLM, read-only
счета и агрегаты расходов, spending-based travel profile за 90 дней и
`preview_only` hotel payment intent. Он подключается вторым MCP рядом с Hotels
MCP и не меняет Kotlin backend, public API, product MVP или provider ranking.

Реальные payment calls, передача banking session в Hotels API, автоматическая
персонализация и production rollout не активированы. Следующий разрешённый срез
для этого toolstream — sandbox/read-only проверка phone auth и агрегатов, затем
отдельный contract/security gate для связи Hotels `orderId/payment setup` с
banking payment, idempotency, reconciliation и trusted human confirmation.
Архитектурная граница зафиксирована в `ADR-0003`.

Опциональный локальный auth broker реализует первый совместимый срез: оба MCP
остаются независимо подключаемыми, broker единолично обновляет mobile session,
а Hotels allowlist ограничен route-level подтверждёнными `get_customer`,
`list_bookings`, `get_booking_v1` и voucher. Voucher выдаётся только через
owner-only local handoff: binary content не входит в MCP JSON. Расширение endpoint
matrix и payment linkage ведётся по
`docs/guides/tbank-mobile-auth-and-hotel-payment-research.md`; неизвестные
routes и реальные mutations остаются заблокированы. Решение зафиксировано в
`ADR-0004`.

После внешнего review локальный hardening обновлён до Banking MCP `0.5.0` и
Hotels MCP `0.11.0`: broker protocol v2 разделяет client scopes, session refresh
защищён межпроцессной блокировкой, readiness проверяет доступность broker,
добавлены bounded state, усиленная redaction, owner-only socket hardening и
локальный `--logout`. Для следующего шага добавлен отдельный CLI-пробник с
фиксированными read-only Hotels routes и тремя ограниченными auth profiles; он
не читает response bodies и не доступен модели. Probe `1.1` подтвердил auth
effect для `customerdata` и `booking_list`: unauthenticated control получил
`401`, а Bearer-only — `200`; дополнительные session/cookie/device/IP данные не
потребовались. Эти два reads подключены через broker в Banking MCP `0.5.0` и
Hotels MCP `0.11.0`; обезличенный end-to-end smoke подтвердил
`mobile_read_only_ready` и получение обоих provider payloads без вывода PII или
order identifiers. Следующий разрешённый шаг toolstream — route-level проверка
оставшихся order reads при наличии собственных identifiers; booking/payment
mutations остаются отдельным gate.

Текущий локальный checkpoint toolstream: Hotels MCP `0.28.0`, Banking/broker
`0.17.0`, local toolkit `0.13.1`. Safe voucher handoff реализован и проверен
только на fixture/fake transport после ранее зафиксированного read-only auth
evidence. Публичная граница оформления закреплена в `ADR-0005`: после выбора
тарифа Hotels MCP создаёт безопасный hosted-checkout handoff без PII,
`bookHash`, payment credentials и provider write; точный тариф не считается
перенесённым или зарезервированным. Hotels entrypoint, stdio framing, tool
contracts, config и runtime разделены на модули. Banking server и broker
получают только `CuratedMobileSession` с allowlisted read-операциями, хотя
vendored mobile client сохранён для совместимого phone login/refresh. Hotels search
journey теперь принимает semantic `breakfastIncluded`, применяет подтверждённый
`meal_types=breakfast` до поиска и сравнения, строго валидирует четыре
discriminator-формы low-level filters и запрещает автоматический unfiltered
fallback или перебор payload после отказа обязательного фильтра. Qwen 3.8 Max
review дал `READY`; локальный P3 hardening дополнительно разделил
provider auth rejection, закрыл network/condition test gaps и задокументировал
неподтверждённые rates filters. После восстановления `searchReady=true` первый
естественный breakfast smoke прошёл двумя journey-tools без low-level перебора
и writes; control search также прошёл двумя journey tools. Smoke выявил UX gap:
модель скрывала основные сравнительные поля и могла смешать неподтверждённый и
исключённый завтрак. В `0.15.3` evidence стал трёхсостоянийным, а compare response
получил обязательный presentation scope. Повторный control сохранил scope, но
модель всё ещё скрыла сложный provider price. В `0.16.0` сохранены плоские
`comparisonRows` и добавлен крупный compatibility batch: key-file auth без PEM
в config, одноразовый setup, offline doctor, launcher/config generator для
Hotels-only/Banking-only/combined, manifests и clean-restart conformance обоих
MCP. Первый полный естественный прогон подтвердил search, breakfast, rates и
preview-only flows, но обнаружил одну подмену `ratingsCount` в текстовом выводе
и отсутствие broker после client restart. В `0.17.0` compare возвращает готовую
Markdown-таблицу с правилом неизменности provider facts, а toolkit `0.2.0`
привязывает broker lifecycle к Banking launcher в combined profile. Повтор
подтвердил исправление comparison и broker lifecycle, но выявил избыточное
раскрытие travel history и абсолютных банковских агрегатов в privacy-запросах.
Hotels `0.18.0` добавляет count-only booking summary, а Banking `0.8.1` — один
portfolio travel profile без счетов, абсолютных сумм, разбивки категорий и booking
history. Первый повтор подтвердил Hotels summary и выявил, что Banking-ответ
раскрывал количество счетов и неверно говорил, что категории не использовались.
В `0.8.1` количество счетов исключено, а provenance явно разделяет внутреннее
использование агрегированных категорий и отсутствие их разбивки в ответе.
Повтор privacy-кейса 5 прошёл, после чего выполнен release-focused review.
Release-focused review подтвердил отсутствие P0–P2. В patch checkpoint
`0.18.1` / `0.2.1` закрыты все семь P3: component-scoped env, актуальные
версии и test counts, локальные ignore-правила, owner-only key enforcement и
расширенное удаление provider booking identifiers. Следующий portability gate —
Codex CLI также подтверждён: оба MCP зарегистрированы без env-секретов,
combined doctor готов, встроенный tool discovery нашёл оба status-tool и
локальные `connection_status` вернули Hotels `0.18.1` / Banking `0.8.1` без
provider requests. OpenCode и Codex входят в подтверждённую acceptance matrix,
Claude Code исключён решением владельца. Следующий шаг — статическая офлайн-
цепочка hotel order → Hotels payment state → Banking payment preview.
В Hotels `0.20.0` / Banking `0.11.0` следующий безопасный участок этой цепочки
закрыт общим broker capability: Hotels преобразует собственный process-local
`bookingRef` в короткоживущий `paymentHandoffRef`, Banking проверяет capability у
того же broker и не получает provider `orderId/paymentToken`. Binding брони
подтверждён локально. Один явно разрешённый structure-only capture собственной
активной брони подтвердил пути booking v1 `paymentPrice.amount/currency` и raw
`paymentStatus` без сохранения значений. Broker теперь связывает эти facts с
capability, а Banking больше не принимает сумму от модели. Raw status не
интерпретируется, а paymentPrice не считается автоматически задолженностью или
разрешённой суммой списания.
Для следующего contract intake local toolkit `0.3.0` добавляет полностью
офлайн-команду `inspect-booking-fixture`: она удаляет значения и динамические
identifiers из уже имеющегося JSON собственной брони и оставляет только
наблюдаемую структуру. Следующий gate — получить просмотренный владельцем
structure-only отчёт и проверить наличие подтверждённых amount, currency и
payment-state facts; исходный fixture не входит в репозиторий или prompt.
Если исходного fixture нет, тот же toolkit может только по явному
`--acknowledge-read-own-data` выполнить два bounded Hotels reads собственной
брони через broker и сразу записать structure-only результат без raw
persistence. Этот capture не является MCP-tool и не разрешает writes.
External review payment handoff не выявил P0/P1 и подтвердил `preview_only` GO.
В следующем локальном hardening checkpoint исправлен request accounting,
readiness привязан к активной mobile session, raw status переименован в
`paymentStatusObservation`, capability стал одноразовым, маскирование коротких
dynamic keys усилено, а негативные и bounded-store тесты добавлены. Эти
изменения не активируют payment setup или execution.
Следующий offline hardening делает сумму decimal-safe внутри MCP-контракта,
добавляет freshness window, проверку process-local source account до поглощения
capability и единый fail-closed readiness report. Локальная команда
`payment-readiness` фиксирует оставшиеся provider/security blockers и запрещает
автоматический retry после неизвестного исхода. Статический аудит подтверждает,
что банковский `/v1/pay` и известные marketplace payment-gateway flows не
доказывают Hotels payment contract и не переиспользуются.
Review этого checkpoint не выявил P0–P2. В patch follow-up `payloadHash`
защищён per-process pepper, а ошибка после поглощения невалидного capability
содержит однозначный recovery-шаг создать новый handoff.
Реальная оплата, booking/payment setup, мутации и remote transport этим
checkpoint не активированы.

Live smoke customer summary и portfolio profile подтвердил корректность самих
read-only tools, но выявил зависимость mobile auth от порядка ленивого запуска
MCP-клиентом. Lifecycle hardening устранил ручной шаг: combined launcher обоих
MCP обеспечивает один persistent local broker для Hotels-first, Banking-first и
одновременного старта; закрытие одного MCP не обрывает session, а `logout` и
`stop-broker` выполняют явное graceful shutdown. Provider API при проверке этого
изменения не вызывались.

Следующий локальный release-candidate slice добавил прямой privacy-safe handoff
`hotelDefaults` → `hotelPreferences`: Hotels принимает только ценовой диапазон,
`best_value` и разрешение показывать альтернативы, не получает счета,
категории или абсолютные суммы и не превращает профиль в provider price filter.
Детерминированный `best_value_v2` отделён от provider facts, варианты вне
диапазона остаются видимыми. Presentation-поля для денег, времени и отмены
сохраняют точные факты и исходное UTC-смещение без догадок о timezone отеля.
Смена только локального ranking переиспользует тот же короткий search cache и
не увеличивает provider traffic. Offline gate дополнен устанавливаемыми вне
checkout npm tarball candidate для Hotels и wheel candidate для Banking. Это
не считается публикацией или portable release: независимый review, шесть
естественных smoke-кейсов, fresh-machine/client/OS matrix, secure storage,
checksums, SBOM и provenance ещё не закрыты. Execution tiers и remote transport
не активированы.

Независимый Qwen 3.8 Max review этого release candidate дал `CONDITIONAL
READY` и не обнаружил P0–P2. Локальный follow-up закрыл все P3: overview и
cancellation preview используют opaque `bookingRef` через mobile broker,
legacy broker без явного `verifiedOperations` не получает customer readiness,
а booking draft не разбирает и не сохраняет guest PII при недоступном
execution. Historical payment report синхронизирован с одноразовой consume
семантикой. Следующий gate остаётся прежним: полный offline regression, затем
естественный read-only/preview-only smoke после рестарта клиента; writes и
remote transport не активированы.

Первый post-review live smoke не прошёл штатный tool path: stale inline PEM из
родительского OpenCode environment смешался с каноничным key-file local config.
Toolkit `0.6.3` делает configured Hotels auth-profile авторитетным, отбрасывает
родительские auth credentials и mutation activation, но сохраняет явный
transport URL. Это исправляет регрессию `0.6.2`, которая подменила рабочий
`https://hotels.tbank.ru/` устаревшим private origin из local config и вызвала
DNS `ENOTFOUND`. Выполненный моделью ad-hoc direct provider driver не считается
acceptance evidence. Повтор начинается с одного обычного кейса после полного
рестарта; обход MCP и model-side изменение config запрещены. Hotels `0.23.1` возвращает
для такого сбоя terminal `search_unavailable` с запретом retry/low-level
fallback, а status явно ограничивает `searchReady` локальной конфигурацией.
Acceptance остаётся pending до полного рестарта MCP-клиента и повторного
естественного smoke через исправленный effective transport URL.

Следующий live smoke подтвердил transport/auth/customer-read и portfolio
flows, но выявил две ошибки композиции. Модель вызвала booking preview до
выбора тарифа, а в двухночном персонализированном поиске не передала
`hotelPreferences`, хотя заявила применение профиля; дополнительно provider
`shownPrice` total ошибочно сравнивался с диапазоном за ночь. Hotels `0.23.2`
разделяет `totalPriceDisplay` и MCP-derived `pricePerNightDisplay`, использует
цену за ночь в `best_value` и `pricePreferenceFit` и требует последовательность
rates → select rate → preview в tool guidance. Banking `0.14.1` возвращает
готовый `hotelPreferences` и точное правило передачи без преобразований;
применение можно утверждать только при `preferencesApplied.applied=true`.
Toolkit `0.6.4` синхронизирует versioned manifests. Повторный personalized
smoke остаётся acceptance gate; writes не активированы.

Повторный natural-language smoke подтвердил исправление total/per-night,
точный profile handoff, последовательный rates-preview и customer/banking
reads. Он также показал, что `best_value_v1` чрезмерно награждал цену ниже
нижней границы: far-below вариант мог возглавить мягкий диапазон. Hotels
`0.23.3` заменяет его band-aware `best_value_v2`, нормализует отсутствующую
звёздность и не раскрывает внутренние trusted-header blockers в обычном
preview. Toolkit `0.6.5` синхронизирует manifests; нужен один focused live
repeat персонализированного поиска после restart клиента.

Focused repeat подтвердил `best_value_v2`, но выявил model-side location retry:
локализованный `countryName=Россия` вернул пустой каталог, после чего модель
перебрала несколько вариантов `resolve_destination`. Hotels `0.23.4` выполняет
этот bounded fallback внутри исходного `plan_stay`, локально сопоставляет
русское и международное название страны и запрещает автоматический перебор
после terminal clarification. Toolkit `0.6.6` синхронизирует manifests; нужен
один короткий repeat Казани, writes остаются отключены.

Финальный Kazan repeat прошёл через portfolio profile → один `plan_stay` →
compare без отдельных resolver-вызовов, retry или writes. Live evidence
подтвердил `preferencesApplied.applied=true`, корректный total/per-night и
band-aware top-5. Одна строка поясняющего текста модели неверно пересказала
review rating при корректной MCP-таблице; это зафиксировано как неблокирующий
P3 presentation issue. Локальный read-only/preview-only release candidate готов
к финальному independent review; mutations по-прежнему NO-GO.

Следующий contract-intake checkpoint сверил предоставленные владельцем
Swagger-экспорты `HotelsApi` и `HotelsApi.Payments` полностью офлайн. Booking
DTO дополнен подтверждёнными `paymentMeans=pos`, `isBusinessTrip` и UUID card
reference. Для будущей публичной оплаты выбран hosted payment form; raw-card,
fingerprint и 3-D Secure endpoints намеренно исключены из MCP. Hotels добавляет
локальный `tbank_hotels_create_payment_form_preview` и отдельный
`paymentFormExecution` readiness без PII, credentials или provider-вызовов.
Owner-provided production origin зафиксирован без сетевой проверки; execution
остаётся `NO-GO`: нет доступного non-production origin, Swagger не подтверждает
customer auth, доверенный источник client IP, idempotency, recovery после
timeout до `taskId` и безопасный owner-bound handoff `paymentUrl`. Полная
матрица и activation gates находятся в
`tools/tbank-hotels-mcp/docs/booking-payment-contract-readiness.md`.

Post-smoke hardening `0.27.0/0.16.0/0.10.0` закрыл ошибки
естественной композиции. Повторное сравнение без явного scope
теперь остаётся в предыдущей показанной comparison-группе; выход на
всю journey требует `scope=all_journey_options`. Privacy-first portfolio flow
ведёт в отдельный `tbank_hotels_plan_personalized_stay`, где
`hotelPreferences` обязателен; лишние account/summary calls прямо запрещены
в tool guidance. Checkout handoff больше не ведёт на generic entry point:
он сохраняет выбор отеля, даты и число взрослых для простой occupancy через
подтверждённые public query-параметры. Сложный состав гостей переносится только
в подтверждённой части, а exact rate и бронь не переносятся. Номера тарифов
стабильны во всём journey, а готовая таблица предназначена для однократного
показа. Full offline gate остаётся обязательным без provider requests;
booking/payment execution остаётся `NO-GO`.

Publication hardening `0.27.0/0.17.0/0.11.0` устраняет техническую зависимость
локального launcher от repository checkout. Launcher разрешает отдельно
установленные Hotels, Banking, auth broker и phone-login команды через `PATH`
или проверенные абсолютные overrides; repository layout остаётся только
development fallback. Phone login включён в Banking wheel как отдельный
terminal entry point, а toolkit npm artifact ограничен runtime/manifests/README.
Artifact tests устанавливают пакеты вне checkout и проверяют MCP initialize и
login/logout без provider network. Фактическая registry-публикация всё ещё
требует решения владельца по лицензии, package namespace и аудитории service
JWT credentials; booking/payment execution остаётся `NO-GO`.

Developer-preview publication checkpoint `0.28.0/0.17.0/0.12.0` делает
публичный Hotels search анонимным по умолчанию: достаточно настроить
`https://hotels.tbank.ru/api`, `Authorization` не отправляется. Service JWT и
static token остаются только опциональными integration overrides, а customer
reads используют локальную mobile session через broker. Anonymous mode не
разрешает booking/payment mutations. `tbank-hotels-mcp@0.28.0` и
`tbank-mcp-local@0.12.0` опубликованы в публичном npm registry и установлены
обратно из registry в чистое временное окружение; Hotels MCP подтвердил
`initialize`, toolkit — локальный `payment-readiness` без provider requests.
Для полного combined release остаются PyPI upload Banking package и
fresh-machine client matrix.

Combined installer checkpoint `0.28.0/0.17.0/0.13.1` добавляет одну публичную
команду `connect` для OpenCode/Codex. Она устанавливает фиксированные версии в
owner-only runtime, сохраняет проверенные абсолютные executable paths,
регистрирует Hotels и Banking как два отдельных MCP и запускает terminal-only
mobile login. Hotels-only профиль не требует Python, mobile session или broker.
Первая опубликованная `0.13.0` была superseded до объявления combined release:
fresh-install gate обнаружил лишний `--ensure-broker` в Hotels-only
регистрации; `0.13.1` исправляет это и имеет отдельный regression test.
`tbank-mcp-local@0.13.1` опубликован и повторно проверен из npm вне checkout.
Banking wheel `0.17.0` прошёл `twine check`, опубликован в PyPI и установлен
обратно публичным `connect`. Чистые OpenCode и Codex CLI приняли обе
регистрации; оба MCP ответили на `initialize`, installer/login regression
использует управляемый session path. Provider requests в release smoke не
выполнялись. Local stdio read-only/preview-only combined developer preview
считается опубликованным; remote ChatGPT transport, stable license,
SBOM/provenance и production mutations остаются отдельными будущими gates.

Финальный Qwen-аудит не выявил P0–P2 и подтвердил готовность read-only и
preview-only tiers. Follow-up закрыл четыре P3: version consistency теперь
проверяется автоматически, нестандартные MCP annotations объявляются рядом с
tools, runtime использует единый handler registry с полным contract test, а
локальный Banking editable install синхронизирован с `0.16.0`. Полный offline
gate предыдущего checkpoint: toolkit 14/14, Hotels 58/58, Banking 52/52, без
provider requests. Для `0.27.0/0.16.0/0.10.0` добавлен отдельный offline gate.
Следующий разрешённый шаг — bounded human live smoke после чистого перезапуска;
booking/payment execution остаётся отдельным `NO-GO` gate.

**Правило активации будущих этапов:** planned stages не являются active backlog. Каждый будущий этап начинается только после отдельной явной roadmap-задачи, которая активирует этап и подтверждает нужные предыдущие решения.

## 8. Связанные документы и audit trail

- `docs/ROADMAP.md` — верхнеуровневый обзор roadmap.
- `docs/product/README.md` — индекс продуктовой документации.
- `docs/architecture/README.md` — индекс архитектурной документации.
- `docs/development/roadmap.md` — компактный development reference и milestone vocabulary; только future/reference material.
- `docs/development/implementation-strategy.md` — implementation strategy; future/reference material до активации.
- `docs/reviews/README.md` — индекс review/audit artifacts и правила чтения historical/current cleanup reports.
- `docs/reviews/stage-7-33-ready-subset-manifest-candidate-definition.md` — latest ready subset manifest candidate definition report.
