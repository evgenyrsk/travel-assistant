# Architecture Baseline

**Роль:** источник истины по текущей архитектуре и принятому стеку backend Travel Assistant. Исторические артефакты `docs/architecture/stage-*` и черновики контрактов сохраняют ход решений, но не переопределяют эту основу.

## 1. Назначение документа

Этот документ фиксирует актуальную архитектурную основу Travel Assistant после завершения Stage 0–9, исправления стека backend и последующей синхронизации документации. Текущий статус этапов, последний завершенный шаг и следующий разрешенный шаг фиксируются только в `docs/roadmap/roadmap.md`.

Он нужен как компактная точка входа в текущее архитектурное состояние: какие границы подтверждены, где находится conceptual architecture baseline и какие Stage 5 artifacts являются исходными источниками.

Документ не заменяет исторические артефакты Stage 5, не переписывает архитектурные решения и не добавляет принятые ADR. Roadmap остается источником истины по статусам этапов.

Этот документ не является implementation plan, API contract, OpenAPI specification, DB schema, storage model, provider adapter design или backlog задач.

## 2. Текущий статус архитектуры

- Stage 0–9 завершены; подробные статусы и перенесенные пункты находятся в `docs/roadmap/roadmap.md`.
- Backend использует Kotlin + Ktor и сохраняет разделение domain, application, infrastructure и API слоев.
- `LlmClient` и `HotelOfferProviderBoundary` реализованы как application-owned асинхронные границы.
- OpenRouter и публичный Hotels API имеют opt-in adapters и отдельные `HttpClient`; оба режима по умолчанию остаются `FAKE`.
- Confirmation lifecycle не запускает hotel search до явного подтверждения и не создает `hotelSearchId` при отказе provider flow.
- Assistant, constraints, pending confirmation, execution attempt и hotel search stores остаются process-local.
- Основной frontend является chat-first; структурированная форма Stage 7.51 сохранена отдельной диагностической страницей.
- Public API/OpenAPI boundary сохранена; generated clients, durable storage, auth и промышленная инфраструктура не созданы.
- Stage 10.0 выбрал responsive web/PWA как первый cross-platform target без
  изменения backend/domain boundaries; implementation Stage 10.1 еще не начат.

Следующая задача реализации может начаться только через отдельную явную задачу, согласованную с roadmap.

Chat-first frontend использует Assistant routes и загружает результаты только по
полученному `hotelSearchId`. Диагностическая форма Stage 7.51 вызывает
hotel-search API напрямую и не является основным продуктовым сценарием.
Backend/application сохраняет orchestration boundary: LLM интерпретирует запрос
через `LlmClient`, а provider API остается источником hotel facts за
`HotelOfferProviderBoundary`.

## 3. Backend stack baseline

Подтвержденный backend stack Travel Assistant: Kotlin + Ktor.

Java/Spring Boot не является принятым backend stack для Travel Assistant. Stage 7.0b заменил Java/Spring Boot skeleton в `services/backend/` на минимальный Kotlin + Ktor skeleton. Этот документ не начинает future implementation work.

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

Stage 8 определил границу `LlmClient`, разрешенные данные и fallback. Stage 9
добавил opt-in OpenRouter adapter и Hotels API adapter через отдельные runtime
factories. Выбор конкретной модели остается configuration-only, секреты не
передаются frontend, а provider DTO не выходят за infrastructure layer.

Первый Stage 10 client target переиспользует текущий легковесный frontend как
online-only PWA. Клиент обращается только к Travel Assistant `/api/v1/**` и не
кэширует transcript, API responses или provider facts. Native clients,
cross-device sync и offline hotel search требуют отдельных решений.

## 6. Основные архитектурные границы

Ключевые границы:

- Product boundary: MVP v1 остается hotel-only.
- Provider boundary: provider layer является источником hotel facts.
- LLM boundary: LLM не создает provider facts и не заменяет provider data.
- Data boundary: current-session shortlist не является account history, persistent saved trips или cross-device sync.
- Integration boundary: provider abstractions являются conceptual boundaries, а не API contracts.
- Stack boundary: backend implementation использует Kotlin + Ktor, если только будущий ADR явно не меняет это решение.
- Implementation boundary: Stage 7–9 завершили process-local MVP и opt-in real integrations; durable infrastructure и production hardening не активированы.

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

Stage 7–9 реализовали ограниченные application/domain модели Assistant session,
hotel search и hotel offer. Более широкий Search Intent Summary,
current-session shortlist и comparison остаются продуктовыми понятиями, а не
разрешением создавать database schema или новые public contracts.

Storage boundaries остаются conceptual. Stage 5 не создает DB schema, ERD, migrations, tables, fields, indexes, retention policy или storage technology choice.

Account history, persistent saved trips, full user profile, full auth, booking records, payment records, flight data и combined itinerary data не входят в MVP v1.

## 9. Integration baseline

`HotelOfferProviderBoundary` отделяет Travel Assistant от hotel offer sources.
Stage 9 реализовал `FakeHotelOfferProvider` и opt-in adapter публичного Hotels
API; provider DTO и transport остаются в infrastructure layer, а application
получает typed provider-independent outcomes.

`LlmClient` аналогично отделяет application flow от LLM provider. OpenRouter
adapter включается только явно и использует отдельный runtime client, поэтому
его `Authorization` не может попасть в Hotels API transport.

Provider abstraction не является публичным API/OpenAPI contract. Изменения
внешнего provider contract требуют отдельной сверки и не должны менять domain
модель напрямую.

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

## 13. Перенос архитектурных вопросов

Актуальные перенесенные вопросы уже зафиксированы в `docs/architecture/stage-5/stage-5-summary-and-carryover.md` и связанных артефактах Stage 5. Этот раздел не добавляет новые пункты.

Ключевые перенесенные темы:

- сохранить facts / assumptions / unknowns separation;
- сохранить provider-agnostic hotel boundary;
- сохранить chat-first, not chat-only UX;
- сохранить Search Intent Summary как UX/domain bridge;
- не превращать current-session shortlist в account history;
- не возвращать flight, combined itinerary, booking или payment в MVP v1;
- сохранить source/freshness uncertainty as visible concept;
- подтверждать изменения Hotels API contract и официальный server-to-server статус отдельно от наблюдаемого публичного web-flow;
- решать storage, auth, telemetry, security и provider hardening только через отдельные future decisions.

Перенесенные темы не являются активным списком задач, списком Stage 6 или планом реализации.

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
