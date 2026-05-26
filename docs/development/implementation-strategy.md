# Стратегия реализации через opencode

Этот документ задает правила работы с задачами для Travel Assistant. Он нужен, чтобы развитие продукта шло маленькими проверяемыми шагами, а архитектура оставалась пригодной для web, mobile, desktop и будущих provider-интеграций.

## Общий подход

- Не реализовывать большие куски за один проход.
- Каждый шаг должен быть маленьким, проверяемым и атомарным.
- Сначала документация и контракты, потом код.
- Каждый новый модуль должен иметь понятную ответственность.
- Избегать преждевременной интеграции с реальными travel API.
- Использовать mock/stub providers до стабилизации доменной модели.
- Проектировать backend так, чтобы позже можно было относительно спокойно перейти с Ktor на Spring, если потребуется.
- Не завязывать доменную логику на конкретный framework.
- Не завязывать LLM orchestration на конкретного провайдера.
- Предпочитать интерфейсы и use cases в application/domain слоях, а детали Ktor, PostgreSQL, Redis, LLM и travel API держать в infrastructure/adapters.
- Для MVP считать внешние travel API нестабильной зависимостью и работать через абстрактные порты.

## Формат работы с задачами

Каждая задача для opencode должна быть оформлена так, чтобы ее можно было выполнить и проверить независимо.

```text
Task ID:
Goal:
Context:
Expected changes:
Files to create/update:
Constraints:
Acceptance criteria:
Validation steps:
```

### Описание полей

**Task ID:** короткий стабильный идентификатор, например `BE-001`, `AI-003`, `WEB-004`.

**Goal:** конкретная цель задачи в одном или двух предложениях.

**Context:** ссылки на документы, решения, предыдущие задачи и ограничения.

**Expected changes:** список ожидаемых изменений без избыточных деталей реализации.

**Files to create/update:** предполагаемые файлы или директории. Если список неизвестен, нужно указать ожидаемые модули.

**Constraints:** архитектурные и продуктовые ограничения, например отсутствие реальных travel API или запрет на framework-зависимости в domain layer.

**Acceptance criteria:** проверяемые условия завершения задачи.

**Validation steps:** команды, ручные проверки или тестовые сценарии, которые подтверждают результат.

## Рекомендуемый размер задачи

Хорошая задача должна помещаться в один понятный pull request или один небольшой набор изменений. Если задача требует одновременно менять backend, frontend, API contracts, persistence и UI, ее нужно разбить.

Примеры хорошего разбиения:
- сначала описать API contract для chat endpoint;
- затем создать backend route без orchestration;
- затем добавить use case;
- затем подключить mock orchestrator;
- затем обновить frontend API client;
- затем подключить Chat UI.

## Роли субагентов

### system-analytics

Полезен на этапах product requirements, MVP scope, system architecture и API contracts. Помогает уточнять сценарии, границы системы, зависимости и риски.

### backend-builder

Полезен на этапах backend skeleton, API contracts implementation, use cases, provider abstractions и local development. Создает новые backend-модули по уже описанным контрактам.

### backend-refactor

Полезен после появления работающего backend-кода. Помогает выделять domain/application/infrastructure слои, уменьшать связанность и готовить код к будущей миграции с Ktor на другой framework.

### ai-llm-architect

Полезен на этапах LLM abstraction и Travel Assistant orchestration. Проектирует `LlmClient`, intent extraction, slot filling, clarification flow и будущий путь к state-machine / LangGraph-like orchestration.

### data-search-engineer

Полезен на этапах flight search abstraction, hotel search abstraction, offer matching and ranking. Отвечает за модели поиска, mock providers, ранжирование и объяснимость рекомендаций.

### builder-web-platform

Полезен на этапах web frontend skeleton, backend integration, frontend API client и state management. Следит за структурой Next.js приложения и отделением UI от API-деталей.

### ui-ux-designer

Полезен на этапах Chat UI, Search Results UI и User Preferences. Помогает сделать основной сценарий понятным, компактным и пригодным для дальнейшего развития на mobile и desktop.

### test-generator

Полезен начиная с backend foundation и особенно на этапах orchestration, matching, API и frontend. Добавляет unit, integration, component и smoke tests под уже определенное поведение.

### security-reviewer

Полезен на этапах security baseline, API contracts, configuration, provider integrations и production readiness. Проверяет secrets, validation, error handling, CORS, rate limiting и обработку чувствительных данных.

### documentation-writer

Полезен на всех этапах, особенно при изменении архитектуры, контрактов и пользовательских сценариев. Обновляет roadmap, API docs, architecture notes, README и checklists.

## Definition of Done

Задача считается завершенной, если выполнены все применимые условия:

- изменения соответствуют текущей архитектуре;
- нет лишних зависимостей;
- нет захардкоженных внешних API;
- публичные контракты описаны;
- ошибки обработаны;
- добавлены или обновлены тесты, если задача затрагивает бизнес-логику;
- обновлена документация, если изменилось поведение или архитектура;
- локальная сборка не сломана;
- mock/stub providers используются там, где реальные интеграции еще не утверждены;
- domain layer не зависит от Ktor, Next.js, PostgreSQL, Redis или конкретного LLM provider;
- validation steps выполнены и результат зафиксирован в ответе агента.

## Правила для backend-задач

- Ktor routing должен быть тонким слоем над application use cases.
- Domain models не должны импортировать Ktor, Exposed, JDBC, Redis clients или LLM SDK.
- Infrastructure adapters должны реализовывать интерфейсы, объявленные ближе к domain/application слоям.
- Ошибки provider-ов нужно переводить в собственные ошибки приложения.
- Конфигурация должна приходить из environment/config files, а не из хардкода.

## Правила для AI-задач

- Любой LLM provider должен подключаться через `LlmClient`.
- Prompt templates и parsing rules должны быть отделены от transport-кода provider-а.
- Orchestrator должен явно работать с conversation state.
- Slot filling и clarification flow должны быть тестируемыми на mock LLM client.
- Не добавлять реального provider-а, пока не стабилизированы intent, slots и domain model.

## Правила для travel search-задач

- Flight и hotel search должны быть отдельными портами.
- Mock providers должны возвращать достаточно богатые данные для проверки ranking и UI.
- Внешние provider DTO не должны становиться внутренней доменной моделью.
- Реальные API credentials не должны появляться в репозитории.
- Кеширование через Redis добавлять только после появления понятного сценария и ключей кеша.

## Правила для frontend-задач

- Первый экран web MVP должен быть рабочим интерфейсом, а не landing page.
- Chat UI и Search Results UI должны быть разделены на понятные компоненты.
- API client должен быть отделен от React-компонентов.
- UI должен поддерживать loading, error и empty states.
- Компоненты не должны содержать backend orchestration logic.

## Порядок реализации MVP

1. Зафиксировать требования, MVP scope и API contracts.
2. Создать backend skeleton и health endpoint.
3. Добавить `LlmClient` abstraction и mock implementation.
4. Создать `TravelAssistantOrchestrator` с clarification flow.
5. Добавить flight/hotel search abstractions и mock providers.
6. Добавить offer matching and ranking.
7. Создать web skeleton.
8. Реализовать Chat UI.
9. Реализовать Search Results UI.
10. Собрать end-to-end MVP flow.
11. Добавить тестовый, security и observability baseline.
12. Подготовить Docker/local development и production readiness checklist.
