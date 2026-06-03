# Architecture Baseline

## 1. Назначение документа

Этот документ фиксирует актуальный architecture baseline Travel Assistant после завершения Stage 5, Stage 7.0a backend stack decision sync и Stage 7.0b backend skeleton correction.

Он нужен как компактная точка входа в текущее архитектурное состояние: какие границы подтверждены, где находится conceptual architecture baseline и какие Stage 5 artifacts являются исходными источниками.

Документ не заменяет historical Stage 5 artifacts, не переписывает architecture decisions и не добавляет accepted ADR. Roadmap остается source of truth по статусам этапов и progression.

Этот документ не является implementation plan, API contract, OpenAPI specification, DB schema, storage model, provider adapter design или backlog задач.

## 2. Текущий статус архитектуры

- Stage 5 - Completed.
- Stage 6 - Completed as contract/documentation phase.
- Stage 7 - In progress / blocked after Stage 7.0b.
- Architecture baseline сформирован на conceptual level и дополнен backend stack decision на уровне документации/governance.
- Minimal Kotlin + Ktor backend skeleton существует в `services/backend/`.
- Business logic, provider integration, DB/storage, frontend, generated clients и production implementation еще не создавались.
- API/OpenAPI contract draft создан в Stage 6 как documentation-level frontend/backend boundary; provider adapters еще не создавались.
- Auth/security/DevOps/testing backlog еще не создавался.

Следующая implementation задача может начаться только после restart readiness review, выбранного отдельной явной задачей, согласованной с roadmap.

## 3. Backend stack baseline

Подтвержденный backend stack Travel Assistant: Kotlin + Ktor.

Java/Spring Boot не является принятым backend stack для Travel Assistant. Stage 7.0b заменил Java/Spring Boot skeleton в `services/backend/` на минимальный Kotlin + Ktor skeleton. Этот документ не начинает Stage 7.2.

Перед любой backend implementation задачей Codex должен сверить backend stack с этим architecture baseline. Если файлы реализации конфликтуют с подтвержденным stack, Codex должен остановиться и сообщить об архитектурном расхождении, а не продолжать реализацию поверх конфликтующего skeleton.

Любое будущее изменение backend stack требует явного architecture decision / ADR и отдельной задачи, согласованной с roadmap. Historical stage artifacts, review notes или future/reference development docs не должны использоваться как текущий источник истины по stack, если существуют roadmap и architecture baseline.

## 4. Scope архитектуры

Актуальный architecture baseline включает результаты Stage 5:

- `stage-5/architecture-scope-and-principles.md` - scope Stage 5, guardrails и архитектурные принципы.
- `stage-5/system-context-and-boundaries.md` - system context, actors, external dependencies и MVP boundaries.
- `stage-5/domain-model-and-boundaries.md` - conceptual domain model и responsibility boundaries.
- `stage-5/application-orchestration.md` - conceptual application orchestration.
- `stage-5/integration-architecture.md` - boundaries provider, LLM и frontend/backend integrations.
- `stage-5/data-and-storage-boundaries.md` - conceptual data ownership, volatility и storage boundaries.
- `stage-5/non-functional-requirements.md` - architecture-level quality attributes и NFR boundaries.
- `stage-5/architecture-decisions-draft.md` - non-ADR decision inventory, deferred decisions и future ADR candidates.
- `stage-5/stage-5-consistency-review.md` - Stage 5 consistency review / completion audit.
- `stage-5/stage-5-summary-and-carryover.md` - итог Stage 5 и carryover.

Эти документы описывают architecture baseline для hotel-only MVP v1 без старта production implementation.

## 5. System context

Пользователь взаимодействует с AI-assisted travel assistant через chat-first, not chat-only experience.

MVP v1 ориентирован на hotel-only flow: пользователь уточняет запрос, получает hotel options, объяснения, сравнение и current-session shortlist.

External provider layer отвечает за hotel facts: цены, availability, location, amenities, policies, ratings, source/freshness and related data, если эти данные доступны из provider/source.

LLM помогает интерпретировать запрос, уточнять недостающие параметры, объяснять, сравнивать, ранжировать и резюмировать. LLM не является источником provider facts.

Backend/application/orchestration conceptually координирует flow между user intent, assistant/LLM layer, hotel provider abstraction и results view. Framework layer для backend должен соответствовать Kotlin + Ktor, при этом domain/application logic остается независимой от Ktor.

UI остается conceptual/product-driven: Stage 5 не создает frontend implementation, component props, API endpoints или production screens.

## 6. Основные архитектурные границы

Ключевые границы:

- Product boundary: MVP v1 остается hotel-only.
- Provider boundary: provider layer является источником hotel facts.
- LLM boundary: LLM не создает provider facts и не заменяет provider data.
- Data boundary: current-session shortlist не является account history, persistent saved trips или cross-device sync.
- Integration boundary: provider abstractions являются conceptual boundaries, а не API contracts.
- Stack boundary: backend implementation использует Kotlin + Ktor, если только будущий ADR явно не меняет это решение.
- Implementation boundary: production implementation не начата; Stage 7.0b ограничен минимальным Kotlin + Ktor skeleton и health endpoint.

Future flights, combined itinerary, booking, payment, account history и full auth остаются outside MVP v1.

## 7. Baseline application orchestration

Application orchestration на conceptual level отвечает за управление hotel-only flow:

- принять пользовательский запрос;
- определить intent и недостающие decision-critical constraints;
- задать уточняющий вопрос, когда данных недостаточно;
- сформировать или обновить Search Intent Summary;
- подготовить hotel search intent для provider layer;
- получить и сохранить разделение provider facts, user-provided constraints, assistant assumptions и unknown data;
- передать данные в LLM для объяснения, сравнения, ранжирования и резюмирования;
- координировать assistant conversation, results view и current-session shortlist.

Это не code design, не state machine specification, не endpoint design и не implementation plan.

## 8. Domain и data baseline

Stage 5 зафиксировал conceptual domain areas:

- User / Traveler;
- User Request;
- User-provided constraints;
- Search Intent Summary;
- Hotel Search Intent;
- Hotel Offer;
- Provider facts;
- Assistant assumptions;
- Unknown data;
- Hotel Comparison;
- Current-session Shortlist.

Hotel Offer, Search Intent Summary, current-session shortlist и recommendation explanation существуют на conceptual level. Они не являются DTO, database schema, API payload, frontend props или implementation classes.

Storage boundaries остаются conceptual. Stage 5 не создает DB schema, ERD, migrations, tables, fields, indexes, retention policy или storage technology choice.

Account history, persistent saved trips, full user profile, full auth, booking records, payment records, flight data и combined itinerary data не входят в MVP v1.

## 9. Integration baseline

Provider abstraction нужна как conceptual boundary между Travel Assistant и hotel offer sources.

Provider facts приходят извне: из provider/source data или future internal company API, когда соответствующий contract будет предоставлен и отдельно разобран.

Provider abstraction не является API/OpenAPI contract. Stage 5 не создает:

- concrete endpoints;
- request/response schemas;
- OpenAPI specs;
- provider SDK design;
- provider adapter implementation;
- DTO mapping tables;
- provider-specific error taxonomy.

Любые API contracts требуют отдельного explicit roadmap step и должны уважать provider-agnostic boundary.

## 10. NFR / quality attributes baseline

NFR и quality attributes Stage 5 задают architecture-level expectations:

- usability и UX consistency;
- reliability expectations;
- performance expectations на conceptual level;
- maintainability;
- extensibility без scope leakage;
- observability as a concept;
- privacy and data minimization;
- security boundaries;
- AI/LLM quality and safety;
- testability на architecture level.

Они не являются активным DevOps/security/testing backlog. Stage 5 не создает production SLO/SLA, deployment topology, monitoring stack, security implementation, auth provider, test plan или QA backlog.

Operational, security, observability и testing details требуют отдельной активации в roadmap.

## 11. Связь с decisions и ADR

Accepted ADR должны находиться в `docs/decisions/`.

На текущий момент standalone accepted ADR files отсутствуют. Stage 5 создал non-ADR decision inventory в `docs/architecture/stage-5/architecture-decisions-draft.md`.

Этот inventory содержит confirmed architecture guardrails, deferred decisions и future ADR candidates, но не создает accepted ADR и не активирует future decisions.

Future ADR candidates не являются текущими задачами. Они могут стать ADR только после отдельного решения, если будущая задача меняет architecture boundaries, public contracts, provider strategy, storage, identity, security, backend stack или long-term technical direction.

## 12. Связь со Stage 5 artifacts

Stage 5 documents сохраняются как historical architecture artifacts и audit trail. Они являются подробными источниками для conceptual architecture baseline.

`architecture-baseline.md` - это compact entry point. Он помогает быстро понять текущее архитектурное состояние, но не заменяет Stage 5 artifacts.

Если где-то возникает расхождение между старым exploratory wording и текущим baseline, приоритет имеют:

1. явный запрос текущей задачи;
2. `docs/roadmap/roadmap.md` для stage status и progression;
3. `docs/product/product-baseline.md` для актуального product scope;
4. этот architecture baseline для compact architecture state и backend stack authority;
5. Stage 5 artifacts для detailed architecture context и audit trail.

Roadmap остается source of truth по статусам и progression.

## 13. Architecture carryover

Актуальный carryover уже зафиксирован в `docs/architecture/stage-5/stage-5-summary-and-carryover.md` и related Stage 5 artifacts. Этот раздел не добавляет новые carryover items.

Ключевые темы carryover:

- сохранить facts / assumptions / unknowns separation;
- сохранить provider-agnostic hotel boundary;
- сохранить chat-first, not chat-only UX;
- сохранить Search Intent Summary как UX/domain bridge;
- не превращать current-session shortlist в account history;
- не возвращать flight, combined itinerary, booking или payment в MVP v1;
- сохранить source/freshness uncertainty as visible concept;
- получить existing hotel provider/API contract до concrete API mapping;
- решать storage, auth, telemetry, security и provider hardening только через отдельные future decisions.

Carryover не является active backlog, Stage 6 task list или implementation plan.

## 14. Связанные документы

- `docs/roadmap/roadmap.md` - primary roadmap и source of truth по статусам этапов и progression.
- `docs/product/product-baseline.md` - актуальный compact product baseline.
- `docs/architecture/README.md` - index архитектурной документации.
- `docs/reviews/project-consistency-audit.md` - audit, выявивший backend stack blocker.
- `docs/reviews/backend-stack-decision-sync.md` - Stage 7.0a backend stack decision and documentation sync handoff.
- `docs/reviews/backend-skeleton-correction.md` - Stage 7.0b backend skeleton correction report.
- `docs/guides/documentation-style-guide.md` - правила языка, структуры и безопасного documentation refactoring.
- `docs/reviews/documentation-refactoring-plan.md` - план controlled documentation refactoring.
- `docs/decisions/README.md` - ADR governance и decision index.
- `docs/architecture/stage-5/architecture-scope-and-principles.md` - scope и architecture principles.
- `docs/architecture/stage-5/system-context-and-boundaries.md` - system context и boundaries.
- `docs/architecture/stage-5/domain-model-and-boundaries.md` - conceptual domain model.
- `docs/architecture/stage-5/application-orchestration.md` - conceptual orchestration.
- `docs/architecture/stage-5/integration-architecture.md` - integration boundaries.
- `docs/architecture/stage-5/data-and-storage-boundaries.md` - data/storage boundaries.
- `docs/architecture/stage-5/non-functional-requirements.md` - quality attributes.
- `docs/architecture/stage-5/architecture-decisions-draft.md` - non-ADR decision inventory.
- `docs/architecture/stage-5/stage-5-consistency-review.md` - Stage 5 review.
- `docs/architecture/stage-5/stage-5-summary-and-carryover.md` - Stage 5 summary and carryover.
