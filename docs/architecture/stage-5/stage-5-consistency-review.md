# Stage 5.9 — Consistency Review / Completion Audit Stage 5

## Назначение

Этот документ проверяет architecture documentation pack Stage 5 на consistency, scope safety и roadmap alignment.

Он проверяет, сохраняет ли Stage 5 Hotel-Only MVP v1 baseline из Stage 3/4, оставляет ли future expansion вне MVP, избегает ли premature implementation design и поддерживает ли обязательное разделение между user-provided constraints, provider facts, assistant assumptions и unknown data.

Этот review не является implementation plan, API contract, database design, ADR, delivery backlog или Stage 6 preparation task.

## Проверенные документы

Проверенные документы Stage 5:

- `architecture-scope-and-principles.md`;
- `system-context-and-boundaries.md`;
- `domain-model-and-boundaries.md`;
- `application-orchestration.md`;
- `integration-architecture.md`;
- `data-and-storage-boundaries.md`;
- `non-functional-requirements.md`;
- `architecture-decisions-draft.md`.

Baseline-документы Stage 3/4, использованные для сравнения:

- `docs/product/stage-3/stage-3-summary-and-carryover.md`;
- `docs/product/stage-3/stage-3-hotel-only-consistency-review.md`;
- `docs/product/stage-3/mvp-search-flow-details.md`;
- `docs/product/stage-3/required-fields-and-acceptance-criteria.md`;
- `docs/product/stage-4/stage-4-summary-and-carryover.md`;
- `docs/product/stage-4/stage-4-consistency-review.md`;
- `docs/product/stage-4/interaction-patterns.md`;
- `docs/product/stage-4/component-inventory.md`;
- `docs/product/stage-4/screen-specifications.md`.

Historical Stage 0-2 documents также были проверены как traceability context. Stage 3 Hotel-Only MVP v1 имеет приоритет там, где более ранние документы содержат superseded broader scope.

## Критерии review

Review проверял, что:

- MVP v1 остается hotel-only;
- flight, combined itinerary, booking, payment, account history или full-auth scope не протекают в MVP;
- production code не вводится;
- API/OpenAPI contracts не создаются;
- DB schema, ERD или migrations не создаются;
- DTOs, classes, interfaces, enums или module/package structure не определяются;
- vendor, tool или concrete provider не выбираются;
- implementation backlog не создается;
- facts/assumptions/unknowns separation сохраняется;
- provider facts остаются source-owned;
- LLM не владеет factual hotel data;
- current-session state не становится account history;
- future expansion ясно отмечен.

## Consistency findings

| Area | Status | Finding | Severity | Recommended action |
|---|---|---|---|---|
| MVP scope | Passed | Stage 5 последовательно сохраняет Hotel-Only MVP v1 и трактует hotel search, comparison, details и current-session shortlist как active boundary. | None | None. |
| Future expansion boundaries | Passed | Flights, combined itinerary, booking, payments, account history и full auth последовательно отмечены как outside MVP/future-only. | None | Keep future sections visibly labeled in later docs. |
| Domain model consistency | Passed | Domain concepts остаются conceptual и сохраняют user constraints, provider facts, assistant assumptions и unknown data. | None | Use Stage 5.3 as baseline for later implementation preparation. |
| Orchestration consistency | Passed | Orchestration остается conceptual и hotel-only; она не определяет state machine implementation, endpoints, payloads или retry/caching policy. | None | Preserve conceptual phase boundaries in later Stage 6 work. |
| Integration boundaries | Passed | Provider, LLM и frontend/backend boundaries остаются provider-agnostic и избегают concrete vendors, SDKs, interfaces или contracts. | None | Defer concrete provider/API mapping until existing contract is provided. |
| Data/storage boundaries | Passed with watch item | Current-session state отделен от account history/full auth; refresh persistence остается open. | Minor | Keep refresh persistence as a future decision, not an assumed MVP requirement. |
| NFR boundaries | Passed with watch item | NFRs остаются architecture-level и избегают SLO/SLA, deployment, monitoring/security implementation или test backlog. | Minor | Prevent NFRs from becoming DevOps/security/testing backlog in Stage 6. |
| Decision draft consistency | Passed | Decision inventory различает confirmed decisions, deferred decisions и future ADR candidates без создания ADR files. | None | Create ADRs only when future triggers occur. |
| Roadmap alignment | Passed | Stage 5 следует roadmap order и не начинает Stage 6. | None | Mark Stage 5 complete after this review if no blockers remain. |
| Documentation navigation | Passed | Product index и roadmap включают Stage 5 architecture docs. | None | Add Stage 5.9 links as part of this task. |

## Scope leakage review

| Area | Present in MVP? | If mentioned, is it clearly future/outside MVP? | Risk level |
|---|---|---|---|
| Flights | No | Yes. Упоминается только как next/future expansion after hotel flow. | Low |
| Combined itinerary | No | Yes. Отмечен как later expansion after flight flow. | Low |
| Booking | No | Yes. Исключен из MVP и связан с future transactional decisions. | Low |
| Payments | No | Yes. Исключены из MVP и связаны с future compliance/security decisions. | Low |
| Account history | No | Yes. Исключен из MVP; current-session shortlist не является account history. | Low |
| Full auth | No | Yes. Исключен из MVP; только future identity scope. | Low |
| Persistent saved trips | No | Yes. Исключены из MVP; только current-session shortlist. | Low |
| Production provider integration | No | Yes. Real provider contract deferred; provider boundary conceptual. | Low |
| API contracts | No | Yes. Многократно указаны как non-goal/deferred. | Low |
| DB schema | No | Yes. Многократно указана как non-goal/deferred. | Low |
| Implementation backlog | No | Yes. Stage 5 docs держат recommendations и questions отдельно от tasks. | Low |

## Review facts / assumptions / unknowns separation

Stage 5 сохраняет data clarity requirements из Stage 3/4:

- user-provided constraints отделены от provider facts и остаются traceable к user input или clarification;
- provider facts отделены от assistant assumptions и остаются source-owned;
- unknown data не выдумывается и остается visible, когда decision-critical;
- LLM explanations grounded in user constraints и provider facts;
- frontend boundaries указывают, что uncertainty и freshness limitations не должны быть скрыты;
- freshness limitations сохраняются conceptually и не заменяются assistant confidence;
- provider facts override assistant assumptions;
- user corrections override assistant assumptions.

Critical или Major issues не найдены.

## Review current-session shortlist

- Current-session shortlist остается только current-session.
- Он не подразумевает account history.
- Он не подразумевает full auth.
- Он не подразумевает persistent saved trips.
- Он не подразумевает cross-device sync.
- Он не подразумевает booking, payment, price guarantee или availability guarantee.
- Refresh persistence остается open/deferred и не повышается до hard MVP requirement.

Minor risk: будущая implementation preparation может случайно трактовать current-session shortlist как account storage или persistent saved trips. Это должно оставаться visible carryover risk.

## Review глубины архитектуры

Документы Stage 5 остаются на intended architecture depth:

- нет implementation algorithm;
- нет endpoint naming;
- нет payload design;
- нет DB fields/tables;
- нет ERD;
- нет concrete state machine spec;
- нет retry/caching policy;
- нет deployment topology;
- нет concrete monitoring stack;
- нет concrete security implementation;
- нет DevOps/security/testing backlog.

Critical или Major issues не найдены.

## Review open questions

Unresolved questions Stage 5 остаются сгруппированными как architecture inputs, а не implementation tasks.

### Provider capabilities/freshness

- Какие minimum hotel provider capabilities требуются после предоставления existing travel API contract?
- Какие minimum provider facts нужны для useful Hotel Offer Card?
- Какие source/freshness markers доступны из provider data?
- Как conceptually представлять provider freshness до знания exact provider fields?

### Search Intent Summary editability/correction

- Должен ли Search Intent Summary быть directly editable, only confirmable или corrected through conversation?
- Должны ли corrections Search Intent Summary оставаться session-only или позже становиться domain event?

### Refresh persistence current-session shortlist

- Должен ли current-session shortlist переживать page refresh в MVP?
- Как долго, если вообще, может жить current-session search context?
- Какой context нужен, чтобы не подразумевать account history или guaranteed fresh provider facts?

### LLM validation / assumptions visibility

- Как conceptually валидировать LLM outputs against provider facts?
- Какой объем LLM reasoning trace должен быть visible пользователям?
- Насколько visible должны быть assumptions и unknowns в UI surfaces?

### Telemetry/privacy

- Какая telemetry допустима для MVP quality и reliability без overengineering или privacy risk?
- Какой уровень diagnostic logging допустим до выбора tools или schemas?

### Provider unavailable behavior

- Какое minimum reliability behavior ожидается, когда hotel provider unavailable?
- Какие qualitative reliability/performance expectations позже должны стать measurable, если такие есть?

### Future security/threat model

- Какой future review должен определить security и threat-model scope?
- Какие future architecture decisions требуют ADRs, а не ordinary architecture notes?

## Stage 5 completion assessment

Stage 5 можно считать завершенным.

Обоснование:

- все planned Stage 5 architecture documents from Stage 5.1-5.8 созданы;
- этот consistency review не нашел Critical или Major blockers;
- MVP v1 остается hotel-only;
- future expansion clearly outside MVP;
- production code, API contracts, OpenAPI, DB schema, ERD, DTO/classes/interfaces, vendor/tool selection или implementation backlog не введены;
- Stage 5 последовательно сохраняет provider facts, user-provided constraints, assistant assumptions и unknown data как distinct categories;
- deferred decisions identified without being prematurely resolved.

Этот completion assessment не начинает Stage 6.

## Recommendations / рекомендации

- Начинать Stage 6 только по отдельному явному запросу.
- До API contract work получить или предоставить existing hotel offer API contract.
- Сохранять provider-agnostic boundary при будущем provider/API mapping.
- Решить current-session refresh persistence до введения любого storage model.
- Держать future flight, combined, booking, payment, account history и full auth за отдельными product decisions и вероятными ADRs.
- Создавать ADR files только когда произойдет future decision trigger.
- Не позволять Stage 6 preparation становиться production implementation.
