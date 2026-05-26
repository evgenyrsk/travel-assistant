# Milestones проекта Travel Assistant

Milestones описывают контрольные точки разработки. Каждый milestone должен завершаться проверяемым результатом, который можно использовать как основу для следующего этапа.

## Milestone 0 — Project Foundation

**Цель:** подготовить структуру проекта, документацию, правила разработки и базовые соглашения.

**Scope:**
- структура репозитория;
- базовые документы проекта;
- правила работы с задачами;
- соглашения по архитектурным границам.

**Out of scope:**
- код backend и frontend;
- реальные интеграции;
- production infrastructure.

**Deliverables:**
- актуальный `README.md`;
- документация в `docs/`;
- roadmap, milestones и implementation strategy;
- базовое описание процесса разработки.

**Acceptance criteria:**
- новый участник или код-агент понимает, где находятся документы и как двигаться по этапам;
- определены следующие milestones;
- нет изменений в коде приложения без необходимости.

**Риски:**
- слишком общий процесс без критериев готовности;
- преждевременное проектирование деталей, которые еще не подтверждены продуктово.

**Зависимости:** нет.

## Milestone 1 — Product & Architecture Foundation

**Цель:** зафиксировать требования, MVP scope, системную архитектуру, основные сценарии пользователя.

**Scope:**
- product requirements;
- MVP boundaries;
- user flows;
- high-level architecture;
- начальная доменная модель;
- API contracts draft.

**Out of scope:**
- реализация API;
- UI-компоненты;
- реальные LLM и travel provider integrations.

**Deliverables:**
- документ с требованиями;
- документ с MVP scope;
- архитектурная схема компонентов;
- черновик доменной модели;
- черновик backend/frontend API contracts.

**Acceptance criteria:**
- MVP сценарий описан end-to-end;
- backend, frontend, domain, AI и integrations имеют понятные границы;
- доменная модель не зависит от Ktor, Next.js или конкретных провайдеров;
- есть список открытых продуктовых вопросов.

**Риски:**
- недоопределенные обязательные параметры поездки;
- смешивание API DTO с доменными моделями;
- фиксация архитектуры вокруг mock-данных вместо реальной предметной области.

**Зависимости:** Milestone 0.

## Milestone 2 — Backend Foundation

**Цель:** создать backend skeleton на Kotlin + Ktor, базовые health endpoints, конфигурацию, слои приложения.

**Scope:**
- Gradle/project setup для backend;
- Ktor application module;
- health endpoint;
- config loading;
- слои `api`, `application`, `domain`, `infrastructure`;
- базовые тесты запуска и health endpoint.

**Out of scope:**
- LLM orchestration;
- travel search;
- PostgreSQL schema;
- Redis cache;
- авторизация.

**Deliverables:**
- запускаемый backend service;
- health endpoint;
- базовая структура пакетов;
- минимальные тесты;
- инструкция локального запуска.

**Acceptance criteria:**
- backend собирается локально;
- health endpoint возвращает ожидаемый статус;
- domain layer не зависит от Ktor;
- конфигурация не содержит секретов.

**Риски:**
- завязать бизнес-логику на Ktor routing;
- добавить лишние зависимости до появления use cases;
- усложнить skeleton раньше времени.

**Зависимости:** Milestone 1.

## Milestone 3 — AI Orchestration Foundation

**Цель:** создать `LlmClient` abstraction, `TravelAssistantOrchestrator`, структуру intent extraction / slot filling / clarification flow.

**Scope:**
- `LlmClient` interface;
- mock LLM client;
- модели intent и slots;
- conversation state;
- начальный `TravelAssistantOrchestrator`;
- clarification flow для недостающих параметров.

**Out of scope:**
- реальный LLM provider;
- streaming;
- tool calling;
- сложный graph runtime;
- долгосрочная память.

**Deliverables:**
- AI abstraction без привязки к провайдеру;
- orchestration use case;
- тесты intent/slot/clarification flow на mock-клиенте;
- документация по расширению orchestration.

**Acceptance criteria:**
- orchestrator может принять свободный запрос и определить, хватает ли данных для поиска;
- при нехватке данных возвращается уточняющий вопрос;
- реализацию можно развивать в сторону state-machine / LangGraph-like подхода;
- LLM ошибки обрабатываются явно.

**Риски:**
- превратить orchestrator в монолитный класс;
- захардкодить prompts и provider details в доменной логике;
- смешать conversation state с UI state.

**Зависимости:** Milestone 2.

## Milestone 4 — Travel Search Foundation

**Цель:** создать абстракции для поиска перелетов и отелей, mock providers, базовые DTO и use cases.

**Scope:**
- flight search port;
- hotel search port;
- mock providers;
- модели поиска и результатов;
- use cases для поиска по заполненному trip request.

**Out of scope:**
- реальные travel API;
- live availability;
- бронирование;
- платежи;
- сложное кеширование.

**Deliverables:**
- `FlightSearchClient` или аналогичный интерфейс;
- `HotelSearchClient` или аналогичный интерфейс;
- mock implementations;
- тесты поиска на стабильных данных;
- базовые ошибки provider-слоя.

**Acceptance criteria:**
- поиск работает без внешних credentials;
- use cases зависят от интерфейсов, а не от mock-реализаций;
- flight и hotel модели можно использовать в matching/ranking;
- provider errors не протекают напрямую в UI/API.

**Риски:**
- слишком рано подстроиться под конкретный внешний API;
- сделать mock-данные бедными и непригодными для end-to-end сценария;
- смешать flight и hotel responsibilities.

**Зависимости:** Milestone 2, частично Milestone 3.

## Milestone 5 — Web MVP

**Цель:** создать web UI с чатовым интерфейсом, выводом результатов и базовой интеграцией с backend.

**Scope:**
- Next.js + React + Tailwind + shadcn/ui skeleton;
- chat UI;
- search results UI;
- frontend API client;
- loading/error/empty states;
- базовая responsive layout.

**Out of scope:**
- mobile app;
- desktop app;
- сложная дизайн-система;
- realtime streaming;
- авторизация.

**Deliverables:**
- запускаемый web-клиент;
- чатовый интерфейс;
- отображение mock travel offers;
- базовая интеграция с backend endpoint-ами;
- документация запуска.

**Acceptance criteria:**
- пользователь может отправить запрос из UI;
- UI показывает уточняющие вопросы и результаты;
- ошибки backend отображаются понятно;
- компоненты не содержат backend business logic.

**Риски:**
- начать с декоративной landing page вместо рабочего продукта;
- смешать DTO mapping и UI rendering;
- не заложить места для mobile/desktop клиентов.

**Зависимости:** Milestone 2, Milestone 3, Milestone 4.

## Milestone 6 — End-to-End MVP

**Цель:** собрать полный сценарий: пользователь пишет запрос → ассистент уточняет → ищет mock-предложения → ранжирует → объясняет результат.

**Scope:**
- end-to-end conversation flow;
- search orchestration;
- offer matching and ranking;
- explanation generation;
- basic conversation history for session;
- smoke tests.

**Out of scope:**
- реальные API;
- аккаунты пользователей;
- платежи и бронирование;
- production deployment.

**Deliverables:**
- рабочий MVP сценарий через web UI;
- backend orchestration для полного flow;
- ranking service;
- объяснения рекомендаций;
- e2e или integration smoke tests.

**Acceptance criteria:**
- пользовательский сценарий проходит от первого сообщения до списка предложений;
- при нехватке параметров ассистент задает уточняющий вопрос;
- найденные mock-предложения отсортированы и объяснены;
- поведение воспроизводимо локально.

**Риски:**
- хрупкая связка между chat flow и search flow;
- непрозрачное ранжирование;
- слишком большая задача без промежуточных проверок.

**Зависимости:** Milestone 3, Milestone 4, Milestone 5.

## Milestone 7 — Quality & Production Readiness

**Цель:** добавить тесты, security baseline, observability, Docker, README и checklist для дальнейшей production-ready разработки.

**Scope:**
- testing strategy implementation;
- security baseline;
- observability baseline;
- Docker/local development;
- production readiness checklist;
- README updates.

**Out of scope:**
- production launch;
- Kubernetes/cloud infrastructure;
- enterprise security compliance;
- реальные provider contracts.

**Deliverables:**
- unit/integration/e2e baseline tests;
- Docker Compose для локальной инфраструктуры;
- structured logging и request id;
- security checklist;
- production readiness checklist;
- обновленная документация запуска и проверки.

**Acceptance criteria:**
- локальная сборка и тесты проходят;
- секреты не хранятся в репозитории;
- есть понятный локальный запуск backend, frontend, PostgreSQL и Redis при необходимости;
- readiness checklist показывает, что еще нужно до production.

**Риски:**
- оставить качество на финальный этап без покрытия критичной логики ранее;
- перепутать MVP readiness и production readiness;
- добавить инфраструктурную сложность без необходимости.

**Зависимости:** Milestone 6.
