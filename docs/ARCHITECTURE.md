# Предварительные архитектурные ориентиры

**Роль:** preliminary/root architecture note. Текущий architecture baseline и backend stack authority находятся в `docs/architecture/architecture-baseline.md`; этот документ остается вторичным контекстом и не является current architecture source of truth.

Этот документ фиксирует рабочие архитектурные гипотезы и защитные границы до отдельного Stage 5 Technical Architecture.

Он не является финальной архитектурой, ADR, API-контрактом, database schema, provider adapter design или разрешением начинать техническую реализацию. Практический порядок реализации описан отдельно в `docs/development/implementation-strategy.md`, а актуальный этап и следующий шаг фиксируются в `docs/roadmap/roadmap.md`.

Текущий compact architecture baseline находится в `docs/architecture/architecture-baseline.md`. После Stage 7.0a подтверждено, что целевой backend stack Travel Assistant — Kotlin + Ktor. Stage 7.0b заменил Java/Spring Boot skeleton в `services/backend/` на минимальный Kotlin + Ktor skeleton. Java/Spring Boot не является принятым backend stack без будущего явного ADR.

## Рабочие гипотезы стека

- Backend: Kotlin + Ktor.
- Frontend: Next.js + React + Tailwind + shadcn/ui.
- Данные: PostgreSQL; Redis только при наличии понятного сценария кэширования.
- AI: независимый от провайдера `LlmClient`.
- Оркестрация: простая собственная логика с возможностью будущего перехода к state-machine или LangGraph-like модели, если это будет подтверждено требованиями.
- Интеграции: для MVP v1 travel API скрывается за hotel provider abstraction; flight provider abstraction относится к next expansion после hotel flow. На ранних этапах допустимы mock/fake providers и contract placeholders до предоставления API-контракта существующего travel API.

## Начальные границы

```text
app/       Будущий frontend/application слой.
services/  Будущие backend/services модули, AI orchestration, integrations, persistence.
scripts/   Будущие local development и automation helpers.
tests/     Будущие shared fixtures, integration tests и end-to-end scenarios.
```

## Возможные области будущего проектирования

- Процесс планирования поездки.
- Профиль предпочтений.
- Генератор маршрута.
- Поиск мест и активностей.
- Оценка бюджета.
- Хранение travel-документов и заметок.
- Слой внешних интеграций.

## Принципы

- Документировать продуктовые решения до фиксации деталей реализации.
- Разделять UI, доменную логику, интеграции и хранение данных.
- Считать API провайдеров заменяемыми адаптерами.
- Держать доменную логику независимой от Ktor, Next.js, PostgreSQL, Redis и конкретных LLM-провайдеров.
- Не хранить секреты в репозитории.
- Финальные архитектурные решения, публичные контракты, provider adapter design и выборы с долгосрочными последствиями фиксировать на соответствующем roadmap-этапе и через ADR, если решение требует ADR.
- Любое будущее изменение backend stack требует явного architecture decision / ADR и задачи, согласованной с roadmap.
