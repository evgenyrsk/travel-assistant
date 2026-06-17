# Roadmap

Этот документ является кратким навигационным обзором этапов Travel Assistant. Он помогает быстро увидеть порядок этапов и перейти к нужным roadmap-документам.

Primary roadmap и source of truth по статусам этапов, progression, carryover, границам этапов и следующему разрешенному шагу находится в `docs/roadmap/roadmap.md`.

`docs/ROADMAP.md` не конкурирует с primary roadmap, не является task tracker или implementation backlog. Справочные development guidance находятся в `docs/development/roadmap.md`, `docs/development/milestones.md` и `docs/development/implementation-strategy.md`; эти документы являются future/reference material и следуют primary roadmap.

## Текущий статус

| Этап | Статус |
|---|---|
| Stage 0 | Завершен |
| Stage 1 | Завершен |
| Stage 2 | Завершен |
| Stage 3 | Завершен |
| Stage 4 | Завершен |
| Stage 4.1 | Завершен |
| Stage 5 | Завершен |
| Stage 6 | Завершен; Stage 6.1-6.9 завершены |
| Stage 7 | В работе / ожидает отдельную явную задачу. Подробный checklist и текущий next step находятся в `docs/roadmap/roadmap.md`. |
| Stage 8-10 | Запланированы |
| Code/API/DB/UI implementation | Минимальная Kotlin + Ktor backend-основа существует; DB/UI/provider integration не начаты. Подробности и ограничения находятся в primary roadmap. |

Этот обзор не является подробным журналом статусов. Подробные статусы Stage 7, documentation stabilization checklist, ограничения generated-client/OpenAPI readiness, generated clients, full conformance gate и следующий разрешенный шаг зафиксированы только в `docs/roadmap/roadmap.md`.

## Этап 0 — перезапуск проекта и продуктовая рамка

- Зафиксировать исходную постановку Travel Assistant.
- Описать пользовательскую ценность, целевую аудиторию и роль AI/LLM.
- Зафиксировать верхнеуровневые сценарии.
- Зафиксировать предварительные MVP boundaries.
- Собрать открытые вопросы.

## Этап 1 — бизнес-требования

- Уточнить бизнес-цели.
- Определить приоритетные типы пользователей.
- Сформулировать критерии успешной рекомендации.
- Зафиксировать бизнес-ограничения продукта.

## Этап 2 — пользовательские сценарии и варианты использования

- Развернуть верхнеуровневые сценарии в варианты использования.
- Описать основные пользовательские потоки.
- Зафиксировать ключевые пограничные случаи.
- Разделить обязательные и дополнительные сценарии.

## Этап 3 — MVP UX / Navigation

- Определить screen map и navigation model MVP.
- Описать основные UX flows и screen states.
- Зафиксировать required fields и acceptance criteria для MVP user flows.
- Зафиксировать hotel-only scope для MVP v1.
- Перенести flight search в next expansion, а combined search — в later expansion после flight flow.
- Отделить MVP UX boundaries от будущего объема работ.

## Этап 4 — Visual Design / UI Concept

- Описать visual style и layout direction.
- Определить UI components, typography, colors и design system direction.
- Подготовить wireframes или mockups, если они предусмотрены отдельной задачей.
- Сохранить возможность будущих мобильных и кроссплатформенных клиентов.

## Этап 5 — техническая архитектура

- Описать архитектурные границы.
- Зафиксировать подход к AI/LLM-абстракции.
- Зафиксировать подход к hotel provider abstraction для MVP v1 и future flight provider abstraction для следующего расширения.
- Определить ответственность backend, frontend, domain и integrations.

## Этап 6 — подготовка реализации

- Подготовить рамку будущих задач реализации.
- Уточнить подход к provider abstractions, mock/fake providers, contract placeholders и использованию предоставленного travel API-контракта на уровне scope/planning.
- Подготовить стратегию проверки и границы локального рабочего процесса.
- Не создавать backend/frontend implementation, DB schema, storage model, auth/security/DevOps/testing backlog или production implementation без отдельной явной roadmap-задачи.

## Этап 7 — реализация MVP

- Реализовать согласованный MVP.
- Собрать основной end-to-end сценарий.
- Подключить существующий travel API по предоставленному контракту для hotel offers; на ранних шагах использовать mock/fake providers и contract placeholders.
- Проверить базовое ранжирование и объяснения.

## Этап 8 — улучшение AI/LLM-оркестрации

- Улучшить уточняющие вопросы.
- Улучшить объяснения и сравнения.
- Повысить устойчивость к неполным и противоречивым запросам.
- Развивать оркестрацию без привязки к одному провайдеру.

## Этап 9 — интеграция с реальными провайдерами/API

- Доработать интеграцию с реальными источниками provider/API через утвержденные абстракции.
- Обработать ошибки, отсутствие офферов и ограничения провайдеров.
- Не смешивать DTO провайдеров с внутренней продуктовой моделью.

## Этап 10 — кроссплатформенное расширение

- Расширить продукт за пределы первой платформы.
- Поддержать mobile iOS/Android или другие интерфейсы.
- Сохранить общую продуктовую и доменную логику.
