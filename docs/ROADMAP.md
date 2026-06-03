# Roadmap

Этот документ является кратким navigation overview по этапам Travel Assistant. Он помогает быстро увидеть порядок этапов и перейти к нужным roadmap-документам.

Primary roadmap и source of truth по статусам этапов, progression, carryover, границам этапов и следующему разрешенному шагу находится в `docs/roadmap/roadmap.md`.

`docs/ROADMAP.md` не является competing roadmap, task tracker или implementation backlog. Справочная development guidance находится в `docs/development/roadmap.md`, `docs/development/milestones.md` и `docs/development/implementation-strategy.md`; эти документы являются future/reference material и следуют primary roadmap.

## Текущий статус

| Этап | Статус |
|---|---|
| Stage 0 | Completed |
| Stage 1 | Completed |
| Stage 2 | Completed |
| Stage 3 | Completed |
| Stage 4 | Completed |
| Stage 4.1 | Completed |
| Stage 5 | Completed |
| Stage 6 | Completed; Stage 6.1-6.9 completed |
| Stage 7 | In progress; corrective stabilization completed through restart readiness review, Stage 7.0e audit, Stage 7.0f-a status/navigation sync cleanup, Stage 7.0f-b reviews index cleanup, Stage 7.0f-c prompt/governance deduplication cleanup, Stage 7.0f-d development docs shortening and Stage 7.0f-e product/architecture index role labels; Stage 7.2+ not activated |
| Code/API/DB/UI implementation | Minimal Kotlin + Ktor backend skeleton exists; DB/UI/provider integration not started |

Stage 6 завершен отдельными явными roadmap-задачами как contract/documentation phase. Stage 6.1-6.9 завершены. Stage 7 прошел corrective stabilization: Stage 7.0b заменил Java/Spring Boot drift на минимальный Kotlin + Ktor skeleton, Stage 7 restart readiness review прошел с minor notes, Stage 7.0e выявил оставшиеся documentation redundancy / structure issues, Stage 7.0f-a синхронизировал status/navigation wording, Stage 7.0f-b создал reviews index и role labeling для audit artifacts, Stage 7.0f-c дедуплицировал prompt/governance guidance вокруг `AGENTS.md`, Stage 7.0f-d сократил `docs/development/**` до secondary reference layer, а Stage 7.0f-e добавил role labels для product/architecture indexes. Stage 7 больше не заблокирован backend stack drift или restart readiness review. Stage 7.2+ не активированы и требуют отдельной явной roadmap-aligned задачи; broader documentation cleanup остается pending. Stage 7.0b/7.0f-a/7.0f-b/7.0f-c/7.0f-d/7.0f-e не означают автоматическое создание business logic, provider integration, DB schema, storage model, auth/security/DevOps/testing backlog, frontend, generated clients или production implementation. Подробные условия активации и исключения зафиксированы в `docs/roadmap/roadmap.md`.

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
