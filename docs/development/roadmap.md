# Roadmap разработки Travel Assistant

Этот roadmap описывает пошаговую разработку AI-powered travel assistant. Его цель — помогать формулировать небольшие, проверяемые задачи для последующей реализации через opencode.

Верхнеуровневый product roadmap находится в `docs/ROADMAP.md`. Этот файл является детальным roadmap разработки и не требует создания будущих директорий до отдельной задачи на соответствующий этап.

## 1. Repository & Process Setup

**Цель:** подготовить репозиторий, базовые соглашения и рабочий процесс.

**Результат:** понятная структура проекта, правила ветвления, формат задач, базовые документы для команды и код-агентов.

**Файлы и модули:** `README.md`, `docs/`, `.gitignore`, будущие конфиги форматирования, CI, шаблоны задач.

**Критерии готовности:**
- описана структура репозитория;
- зафиксированы правила разработки и проверки изменений;
- есть базовая документация для запуска и навигации по проекту;
- определен подход к маленьким атомарным задачам.

**Не входит:** реализация backend, frontend, интеграций и production CI/CD.

## 2. Product Requirements

**Цель:** зафиксировать, какую пользовательскую проблему решает продукт и какие сценарии поддерживаются первыми.

**Результат:** набор требований, пользовательских сценариев и ограничений для MVP.

**Файлы и модули:** `docs/PROJECT_BRIEF.md`; дополнительные product-документы только если отдельная задача явно утверждает их структуру.

**Критерии готовности:**
- описаны основные пользователи;
- описаны ключевые сценарии диалога;
- определены обязательные и необязательные параметры поездки;
- зафиксированы вопросы, которые требуют продуктового решения позже.

**Не входит:** проектирование API, UI-макеты, доменная модель и код.

## 3. MVP Scope

**Цель:** отделить минимальный рабочий сценарий от будущих возможностей.

**Результат:** согласованный MVP: пользователь пишет запрос, ассистент уточняет недостающие параметры, ищет mock-предложения, ранжирует их и объясняет выбор.

**Файлы и модули:** `docs/PROJECT_BRIEF.md`, `docs/ROADMAP.md`, `docs/development/roadmap.md`, `docs/development/milestones.md`; отдельный MVP scope-документ только если будет явно создан отдельной задачей.

**Критерии готовности:**
- есть список in scope и out of scope;
- определены границы mock-интеграций;
- описан end-to-end сценарий MVP;
- есть критерии приемки MVP.

**Не входит:** реальные платежи, бронирования, реальные travel API, мобильные и desktop-клиенты.

## 4. System Architecture

**Цель:** описать высокоуровневую архитектуру web, backend, domain, integrations, persistence и AI orchestration.

**Результат:** схема компонентов и границ ответственности.

**Файлы и модули:** `docs/ARCHITECTURE.md`, будущие `services/backend/`, `app/web/`; дополнительные architecture-документы только если отдельная задача явно утверждает их структуру.

**Критерии готовности:**
- backend спроектирован как Kotlin + Ktor приложение;
- frontend спроектирован как Next.js + React + Tailwind + shadcn/ui;
- доменная логика отделена от framework-слоя;
- travel providers и LLM providers описаны как заменяемые адаптеры;
- PostgreSQL и Redis обозначены как инфраструктурные зависимости.

**Не входит:** детальная реализация классов, миграций, UI и provider-интеграций.

## 5. Domain Model

**Цель:** определить основные сущности и value objects предметной области.

**Результат:** согласованный черновик доменной модели для поездок, предпочтений, запросов, предложений и диалогового состояния.

**Файлы и модули:** будущие `services/backend/domain/`, `docs/domain/model.md`.

**Критерии готовности:**
- описаны `TripRequest`, `TravelerPreferences`, `TravelOffer`, `FlightOption`, `HotelOption`, `ConversationState`;
- определены обязательные и опциональные поля;
- зафиксированы доменные инварианты;
- модель не зависит от Ktor, React, PostgreSQL или конкретного LLM provider.

**Не входит:** SQL-схема, реальные API DTO, UI-модели и алгоритмы ранжирования.

## 6. API Contracts

**Цель:** описать контракты между клиентами и backend.

**Результат:** черновик HTTP API для чата, получения результатов, истории и пользовательских предпочтений.

**Файлы и модули:** будущие `docs/api/contracts.md`, `services/backend/api/`, OpenAPI-файл при необходимости.

**Критерии готовности:**
- описаны endpoint-ы MVP;
- описаны request/response DTO;
- определены ошибки и статусы;
- контракты не раскрывают внутренние детали orchestration.

**Не входит:** реализация endpoint-ов, авторизация, realtime-протоколы и мобильные SDK.

## 7. Backend Skeleton

**Цель:** создать минимальный backend foundation на Kotlin + Ktor.

**Результат:** запускаемое backend-приложение с health endpoint, конфигурацией и слоями приложения.

**Файлы и модули:** будущие `services/backend/`, Gradle-конфиги, Ktor application module, config files.

**Критерии готовности:**
- приложение собирается и запускается локально;
- есть `/health` или аналогичный endpoint;
- выделены слои API, application, domain, infrastructure;
- конфигурация отделена от кода.

**Не входит:** LLM orchestration, travel search, persistence и полноценная бизнес-логика.

## 8. LLM Abstraction

**Цель:** создать `LlmClient` abstraction для работы с языковыми моделями.

**Результат:** интерфейс LLM provider, mock implementation и базовые модели запросов/ответов.

**Файлы и модули:** будущие `services/backend/ai/`, `services/backend/domain/ai/`, тесты.

**Критерии готовности:**
- доменная логика не зависит от конкретного LLM provider;
- есть mock/stub клиент для локальной разработки и тестов;
- описаны ошибки provider-слоя;
- нет захардкоженных API keys.

**Не входит:** интеграция с реальным провайдером, prompt optimization, streaming и tool calling.

## 9. Travel Assistant Orchestration

**Цель:** реализовать начальный `TravelAssistantOrchestrator`.

**Результат:** orchestrator управляет intent extraction, slot filling, clarification flow, поиском и объяснением результата.

**Файлы и модули:** будущие `services/backend/application/orchestration/`, `services/backend/domain/conversation/`.

**Критерии готовности:**
- orchestration разбита на понятные шаги;
- можно заменить реализацию на state-machine / LangGraph-like orchestration позже;
- состояние диалога явно моделируется;
- clarification flow покрывает недостающие параметры поездки.

**Не входит:** сложный graph runtime, multi-agent execution, долгосрочная память и реальные бронирования.

## 10. Flight Search Abstraction

**Цель:** создать интерфейс поиска перелетов.

**Результат:** `FlightSearchClient` или аналогичный порт, mock provider и доменные модели перелетов.

**Файлы и модули:** будущие `services/backend/domain/travel/`, `services/backend/infrastructure/travel/flights/`.

**Критерии готовности:**
- поиск перелетов вызывается через интерфейс;
- есть mock-данные для MVP;
- ошибки поиска описаны отдельно от HTTP/provider-ошибок;
- модель не привязана к конкретному внешнему API.

**Не входит:** реальная интеграция с авиапоиском, покупка билетов, live pricing.

## 11. Hotel Search Abstraction

**Цель:** создать интерфейс поиска отелей.

**Результат:** `HotelSearchClient` или аналогичный порт, mock provider и доменные модели проживания.

**Файлы и модули:** будущие `services/backend/domain/travel/`, `services/backend/infrastructure/travel/hotels/`.

**Критерии готовности:**
- поиск отелей вызывается через интерфейс;
- есть mock-данные для MVP;
- модель поддерживает цену, локацию, рейтинг и базовые удобства;
- provider можно заменить без изменения use cases.

**Не входит:** real-time availability, бронирование, отзывы, карты и платежи.

## 12. Offer Matching and Ranking

**Цель:** сопоставлять найденные предложения с предпочтениями пользователя.

**Результат:** базовый ranking service, который сортирует mock-предложения и формирует причины рекомендации.

**Файлы и модули:** будущие `services/backend/domain/matching/`, `services/backend/application/usecase/`.

**Критерии готовности:**
- критерии ранжирования явно описаны;
- результат содержит объяснение выбора;
- можно протестировать ranking без Ktor и внешних API;
- поведение воспроизводимо на mock-данных.

**Не входит:** ML ranking, персонализация на истории пользователя, A/B testing.

## 13. Web Frontend Skeleton

**Цель:** создать базовый web-клиент.

**Результат:** Next.js + React + Tailwind + shadcn/ui приложение с базовой структурой страниц и компонентов.

**Файлы и модули:** будущие `app/web/`, компоненты UI, frontend-конфиги.

**Критерии готовности:**
- приложение запускается локально;
- настроены Tailwind и shadcn/ui;
- выделены области для chat, results, preferences;
- API-клиент изолирован от компонентов.

**Не входит:** полноценная визуальная полировка, мобильное приложение, desktop-клиент.

## 14. Chat UI

**Цель:** реализовать пользовательский чат как основной интерфейс Travel Assistant.

**Результат:** UI для ввода запроса, отображения сообщений, уточняющих вопросов и ответов ассистента.

**Файлы и модули:** будущие `app/web/components/chat/`, `app/web/app/`, frontend state.

**Критерии готовности:**
- пользователь может отправить свободный запрос;
- UI показывает сообщения пользователя и ассистента;
- поддержаны loading/error states;
- компоненты не содержат бизнес-логики orchestration.

**Не входит:** streaming, voice input, attachments, полноценная история с аккаунтом.

## 15. Search Results UI

**Цель:** показать найденные предложения в удобном виде.

**Результат:** UI для карточек перелетов, отелей и комбинированных travel offers.

**Файлы и модули:** будущие `app/web/components/results/`, DTO mappings, UI states.

**Критерии готовности:**
- результаты отображаются структурированно;
- видны цена, даты, маршрут, отель и причины рекомендации;
- есть состояния empty/loading/error;
- UI не зависит от mock provider напрямую.

**Не входит:** checkout, бронирование, сравнение на карте, сложные фильтры.

## 16. User Preferences

**Цель:** сохранить и использовать базовые предпочтения пользователя.

**Результат:** модель предпочтений и простой flow их передачи в backend.

**Файлы и модули:** будущие `services/backend/domain/preferences/`, `app/web/components/preferences/`, persistence позже.

**Критерии готовности:**
- поддержаны бюджет, стиль поездки, тип проживания, ограничения по датам;
- preferences могут участвовать в matching/ranking;
- модель не зависит от UI-компонентов;
- определено, что хранится временно, а что позже попадет в PostgreSQL.

**Не входит:** полноценный профиль, авторизация, синхронизация между устройствами.

## 17. Conversation History

**Цель:** поддержать историю диалога для текущего сценария.

**Результат:** backend и web могут отображать и использовать историю текущей conversation session.

**Файлы и модули:** будущие `services/backend/domain/conversation/`, `services/backend/application/conversation/`, `app/web/components/chat/`.

**Критерии готовности:**
- у conversation есть идентификатор;
- сообщения имеют роль, время и содержимое;
- orchestrator получает нужный контекст;
- определена стратегия будущего хранения в PostgreSQL.

**Не входит:** аккаунты, поиск по истории, long-term memory.

## 18. Testing Strategy

**Цель:** определить и внедрить базовую стратегию тестирования.

**Результат:** набор уровней тестов: domain unit tests, application tests, API tests, frontend component tests и e2e smoke tests.

**Файлы и модули:** будущие `tests/`, backend test source sets, frontend test setup, `docs/testing.md`.

**Критерии готовности:**
- определены обязательные тесты для бизнес-логики;
- mock providers используются в тестах;
- acceptance criteria задач включают validation steps;
- локальная проверка документирована.

**Не входит:** полное покрытие, performance testing, production monitoring.

## 19. Security Baseline

**Цель:** зафиксировать минимальные правила безопасности.

**Результат:** baseline по secrets, input validation, error handling, CORS и защите внешних интеграций.

**Файлы и модули:** будущие `docs/security.md`, backend config, middleware, validation utilities.

**Критерии готовности:**
- секреты не хранятся в репозитории;
- входные данные валидируются;
- ошибки не раскрывают внутренние детали;
- CORS и rate limiting описаны для дальнейшей реализации.

**Не входит:** полноценная авторизация, compliance, threat modeling enterprise-уровня.

## 20. Observability Baseline

**Цель:** подготовить минимальную наблюдаемость системы.

**Результат:** подход к structured logging, request ids, basic metrics и диагностике orchestration.

**Файлы и модули:** будущие `docs/observability.md`, backend logging config, middleware.

**Критерии готовности:**
- запросы можно связать по request id;
- ошибки и ключевые этапы orchestration логируются;
- sensitive data не попадает в логи;
- определены базовые метрики.

**Не входит:** production tracing, dashboards, alerting.

## 21. Docker and Local Development

**Цель:** упростить локальный запуск backend, frontend и инфраструктуры.

**Результат:** Docker Compose для PostgreSQL, Redis и сервисов при необходимости.

**Файлы и модули:** будущие `docker-compose.yml`, Dockerfiles, `.env.example`, `scripts/`.

**Критерии готовности:**
- локальный запуск описан одной понятной инструкцией;
- PostgreSQL и Redis запускаются локально;
- переменные окружения документированы;
- mock providers работают без внешних credentials.

**Не входит:** production deployment, Kubernetes, cloud infrastructure.

## 22. Production Readiness Checklist

**Цель:** собрать список требований перед переходом к production-ready разработке.

**Результат:** checklist для надежности, безопасности, наблюдаемости, данных, интеграций и UX.

**Файлы и модули:** будущие `docs/production-readiness.md`, README, release checklist.

**Критерии готовности:**
- checklist покрывает backend, frontend, data, AI, security и operations;
- ясно, что обязательно для MVP, а что для production;
- риски вынесены явно;
- checklist можно использовать перед релизом.

**Не входит:** фактический production launch, SLA, юридические документы и реальные договоры с travel providers.
