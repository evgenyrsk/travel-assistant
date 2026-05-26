# Архитектурные заметки

Этот документ описывает целевую архитектуру и границы ответственности. Практический порядок реализации описан отдельно в `docs/development/implementation-strategy.md`.

## Целевой стек

- Backend: Kotlin + Ktor.
- Frontend: Next.js + React + Tailwind + shadcn/ui.
- Данные: PostgreSQL; Redis только при наличии понятного сценария кэширования.
- AI: независимый от провайдера `LlmClient`.
- Оркестрация: собственный `TravelAssistantOrchestrator` с возможностью будущего перехода к state-machine или LangGraph-like модели.
- Интеграции: travel API скрываются за интерфейсами провайдеров перелетов и отелей, начиная с mock/stub провайдеров.

## Начальные границы

```text
app/       Будущий frontend/application слой.
services/  Будущие backend/services модули, AI orchestration, integrations, persistence.
scripts/   Будущие local development и automation helpers.
tests/     Будущие shared fixtures, integration tests и end-to-end scenarios.
```

## Возможные компоненты

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
