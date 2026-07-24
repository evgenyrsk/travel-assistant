# Architecture Baseline

**Роль:** источник истины по текущей архитектуре и принятому стеку backend Travel Assistant. Исторические артефакты `docs/architecture/stage-*` и черновики контрактов сохраняют ход решений, но не переопределяют эту основу.

## 1. Назначение документа

Этот документ фиксирует актуальную архитектурную основу Travel Assistant после
закрытия рабочего hotel-only MVP в Stage 14.0. Текущий статус этапов и любое
будущее разрешённое расширение фиксируются только в
`docs/roadmap/roadmap.md`.

Он нужен как компактная точка входа в текущее архитектурное состояние: какие границы подтверждены, где находится conceptual architecture baseline и какие Stage 5 artifacts являются исходными источниками.

Документ не заменяет исторические артефакты Stage 5, не переписывает архитектурные решения и не добавляет принятые ADR. Roadmap остается источником истины по статусам этапов.

Этот документ не является implementation plan, API contract, OpenAPI specification, DB schema, storage model, provider adapter design или backlog задач.

## 2. Текущий статус архитектуры

- Stage 0–15 завершены; Stage 16 активирован для provider-neutral
  semantic-анализа типа размещения. Подробные статусы находятся в
  `docs/roadmap/roadmap.md`.
- Backend использует Kotlin + Ktor и сохраняет разделение domain, application, infrastructure и API слоев.
- `LlmClient` и `HotelOfferProviderBoundary` реализованы как application-owned асинхронные границы.
- OpenRouter и публичный Hotels API имеют opt-in adapters и отдельные `HttpClient`; оба режима по умолчанию остаются `FAKE`.
- Confirmation lifecycle не запускает hotel search до явного подтверждения и не создает `hotelSearchId` при отказе provider flow.
- Assistant, constraints, pending confirmation, execution attempt и hotel search stores остаются process-local.
- Локальная demo shell является chat-first; структурированная форма Stage 7.51 сохранена отдельной диагностической страницей.
- Public API/OpenAPI boundary сохранена; generated clients, durable storage, auth и промышленная инфраструктура не созданы.
- Stage 10.0 первоначально выбрал responsive web/PWA как первый
  cross-platform срез, а Stage 10.1 добавил online-only
  manifest/icons/mobile foundation без изменения backend/domain boundaries.
- Stage 10.2 подтвердил same-origin web/PWA и platform-neutral JSON/HTTP
  boundary; native/desktop остаются архитектурно совместимыми без готового SDK,
  а cross-origin web требует отдельной CORS policy.
- Stage 10.3 закрепил ограниченный chat-first API subset; Stage 13.5 дополнил
  его on-demand details выбранного opaque offer. Весь OpenAPI и generated
  clients по-прежнему имеют статус `not_ready`.
- Stage 10.4 закрепил Travel Assistant как самостоятельный backend-сервис, а
  текущий web/PWA — как локальную demo shell, не являющуюся будущим продуктовым
  клиентом. Web, Android, iOS и другие platform UI/SDK принадлежат отдельным
  интеграционным командам.
- Stage 11.0 добавил только локальный launcher/runbook и подтвердил полный REAL
  demo flow. Backend layers, public API, provider boundaries и runtime defaults
  при этом не изменились.
- Stage 12 реализовал итеративное уточнение в application orchestration:
  provider-neutral preferences накапливаются в session context, каждое
  изменение provider request требует нового подтверждения, hard filters
  передаются одним новым provider search, а пустая выдача не запускает
  автоматическое ослабление или retry.
- Stage 13 добавил provider-neutral details model, fixture-driven mapping,
  selected-offer resolution, transport adapter, platform-neutral endpoint,
  opt-in REAL wiring и явную кнопку demo shell. Provider `hotelId` остаётся
  внутри backend; массовая N+1-загрузка отсутствует.
- Stage 14.0 подтвердил полный flow одним REAL browser smoke и остановил
  функциональное расширение MVP. Это не подтверждает production readiness,
  официальный S2S SLA или generated-client readiness.
- Stage 14.1a и Stage 14.1b добавили fail-closed фильтрацию description
  sections, общую проверку безопасных HTTPS image URL и optional
  provider-neutral image contract без details lookup или N+1. Stage 14.1c
  ограничен responsive presentation и fallback, а Stage 14.6 безопасно
  разрешает подтверждённый `{size}` только для provider CDN.
- Stage 14.2 сохранил platform-neutral message contract и начал использовать
  существующий optional `clientContext.timezone` как IANA timezone hint.
  Backend-owned `Clock` остаётся источником текущего instant; client timestamp
  не принимается. Отсутствующая или некорректная timezone переводит
  относительные и не содержащие год даты в clarification, а past dates
  отклоняются до confirmation и provider search. Assistant constraints имеют
  один номер по умолчанию; явно запрошенное большее количество очищается как
  неподдерживаемое до confirmation. Агрегированная модель гостей не имитирует
  распределение по нескольким номерам и не передаёт такой запрос provider.
  Внутренний `rooms=1` не переносится в обычный confirmation или demo controls.
- Stage 14.3 расширил прежнюю provider-neutral preference boundary включённым
  завтраком. Infrastructure mapping знает `meal_types=breakfast`, domain/API
  хранят только nullable breakfast fact, а неизвестные meal plans не
  интерпретируются.
- Stage 14.4 подключил к существующим observer boundaries безопасный локальный
  logger фиксированных OpenRouter/application категорий. User messages,
  prompts, raw responses, secrets, model slug и идентификаторы не логируются;
  это не заменяет будущий промышленный observability stack.
- Stage 14.5 добавил узкую application-policy для точной одиночной категории
  звёзд. Она дополняет семантический пропуск LLM, но не интерпретирует
  диапазоны, сравнения, отрицания или снятие фильтров. Stage 14.6 подтвердил,
  что search уже содержит image templates; пакетный источник и N+1 не нужны.
- Stage 14.7 сохраняет autocomplete locations и hotels разными внутренними
  типами. Обычный поиск использует numeric `destinationId`; exact-hotel ветка
  использует opaque provider reference только внутри infrastructure и после
  confirmation выполняет bounded details + v3 rates orchestration. Public API
  сохраняет application-owned `hotelSearchId`/`offerId`; room IDs, provider
  search ID и `bookHash` не моделируются и не раскрываются. До накопления
  критериев application может консервативно дополнить отсутствующий destination
  из явно названного отеля; transport, provider IDs и raw LLM data в эту policy
  не входят.
- Stage 15 подтвердил backend-owned business logic, Java 17 process portability
  и single-instance ограничение process-local stores; operational events,
  probes и metrics остаются infrastructure concerns за application-owned
  boundaries.
- Stage 16 вводит отдельную application-owned
  `AccommodationAnalysisClient`; существующий текстовый `LlmClient` не получает
  multimodal responsibility. Semantic verdict не меняет provider facts.
  OpenRouter vision является opt-in adapter и не активируется до подтверждения
  прав на provider descriptions/images, privacy routing и model compatibility.

Stage 16 завершил provider-neutral async/two-pass implementation в FAKE scope,
public polling contract и bounded observability. Process-local scheduler/cache
сохраняют single-instance ограничение. REAL vision остаётся закрыт внешними
policy, model/ZDR и quality gates, зафиксированными в roadmap.

Chat-first demo shell использует Assistant routes и загружает результаты только по
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

Текущий демонстрационный MVP ориентирован на hotel-only flow: пользователь
уточняет обязательные критерии, подтверждает поиск, получает hotel options и
может следующей репликой изменить необязательные фильтры для нового
подтвержденного provider search. Затем пользователь может явно запросить
provider-backed details одной сохранённой карточки. Пользовательская сортировка
отложена, потому что наблюдаемый Hotels API runtime ее не принимает.

Успешная пустая выдача моделируется как сохранённый `COMPLETED_NO_OFFERS`, а
provider failure не создаёт search resource. Чистая application policy может
выбрать одно provider-neutral preference для явного ослабления; API только
отображает typed suggestion. Policy не изменяет criteria, не вызывает provider
и не подключает `search-filters-availability`.

External provider layer отвечает за hotel facts: цены, availability, location, amenities, policies, ratings, source/freshness and related data, если эти данные доступны из provider/source.

LLM помогает интерпретировать запрос, уточнять недостающие параметры, объяснять, сравнивать, ранжировать и резюмировать. LLM не является источником provider facts.

Backend/application/orchestration conceptually координирует flow между user intent, assistant/LLM layer, hotel provider abstraction и results view. Framework layer для backend должен соответствовать Kotlin + Ktor, при этом domain/application logic остается независимой от Ktor.

UI остается conceptual/product-driven: Stage 5 не создает frontend implementation, component props, API endpoints или production screens.

Stage 8 определил границу `LlmClient`, разрешенные данные и fallback. Stage 9
добавил opt-in OpenRouter adapter и Hotels API adapter через отдельные runtime
factories. Выбор конкретной модели остается configuration-only, секреты не
передаются frontend, а provider DTO не выходят за infrastructure layer.

Текущий легковесный frontend используется как локальная online-only PWA demo
shell. Она обращается только к Travel Assistant `/api/v1/**` и не кэширует
transcript, API responses или provider facts. Будущие продуктовые web/native
клиенты, cross-device sync и offline hotel search требуют отдельных решений.

## 6. Основные архитектурные границы

Ключевые границы:

- Product boundary: MVP v1 остается hotel-only.
- Provider boundary: provider layer является источником hotel facts.
- LLM boundary: LLM не создает provider facts и не заменяет provider data.
- Data boundary: session context и предыдущие search results остаются
  process-local и не являются account history, persistent saved trips или
  cross-device sync.
- Integration boundary: provider abstractions являются conceptual boundaries, а не API contracts.
- Client boundary: любой platform client использует Travel Assistant
  `/api/v1/**`; provider/LLM orchestration, secrets, business validation и
  ranking не дублируются на web, iOS, Android или desktop.
- Platform contract boundary: продуктовые клиенты используют только
  `POST /assistant/sessions`, `POST /assistant/sessions/{sessionId}/messages`,
  `GET /hotel-searches/{searchId}/offers` и
  `GET /hotel-searches/{searchId}/offers/{offerId}/details` под `/api/v1`.
  Health является operational endpoint, прямое создание hotel search —
  diagnostic-only.
- Selected-details boundary: public `offerId` назначается application layer и
  не содержит provider reference. Backend разрешает offer только внутри
  указанного search, а provider `hotelId` передаётся исключительно details
  adapter.
- Provider-content safety boundary: description sections проходят allowlist и
  content-фильтрацию до application/public model; служебные certification,
  registry, owner и contact данные не доходят до API. Optional offer image
  допускается только как HTTPS URL без credentials и не влияет на принятие
  offer.
- Cross-origin boundary: CORS по умолчанию не включён. Будущая web allowlist
  должна содержать точные origin со scheme/host/port, без wildcard и
  credentials, и требует отдельного этапа активации.
- External client boundary: web, Android, iOS и другие продуктовые клиенты
  создаются только отдельными командами и задачами. Текущий `app/` остается
  локальной demo shell. Сервис предоставляет versioned HTTP API, OpenAPI и
  integration guidance, но не выбирает UI/SDK/toolchain и не передает
  backend/domain modules. Решение зафиксировано в
  [`ADR-0001`](../decisions/adr-0001-service-core-and-client-integration-boundary.md).
- Stack boundary: backend implementation использует Kotlin + Ktor, если только будущий ADR явно не меняет это решение.
- Implementation boundary: Stage 7–14 завершили process-local рабочий MVP и
  opt-in real integrations; durable infrastructure и production hardening не
  активированы.

Future flights, combined itinerary, booking, payment, account history и full auth остаются outside MVP v1.

## 7. Baseline application orchestration

Application orchestration на conceptual level отвечает за управление hotel-only flow:

- принять пользовательский запрос;
- определить intent и недостающие decision-critical constraints;
- задать уточняющий вопрос, когда данных недостаточно;
- сформировать или обновить Search Intent Summary;
- подготовить hotel search intent для provider layer;
- накопить необязательные preferences в provider-neutral модели отдельно от
  provider DTO;
- применить одно сообщение как явное изменение одного или нескольких
  preferences, сохранив неизмененные значения;
- повторно запросить подтверждение полного search intent после каждого
  изменения provider request;
- получить и сохранить разделение provider facts, user-provided constraints, assistant assumptions и unknown data;
- передать данные в LLM для интерпретации уточнения, объяснения, ранжирования и
  резюмирования;
- координировать assistant conversation, новый provider search и results view.

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

Для Stage 12 зафиксирована граница: предпочтения представлены через
provider-neutral domain/application model. Идентификаторы и polymorphic filter
values Hotels API остаются в infrastructure mapping. Повторный поиск создает
новый `hotelSearchId`, а предыдущий process-local search не мутируется.

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

Принят [`ADR-0001`](../decisions/adr-0001-service-core-and-client-integration-boundary.md),
который фиксирует backend как удалённое сервисное ядро и отделяет его от
локальной demo shell и будущих product clients. Stage 5 также создал non-ADR
decision inventory в `docs/architecture/stage-5/architecture-decisions-draft.md`.

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
- не превращать session context, предыдущие search results или будущий
  shortlist в account history;
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
