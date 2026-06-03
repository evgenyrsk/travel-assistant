# Аудит согласованности проекта

Дата аудита: 2026-06-03

## 1. Краткое резюме

Проект находится в переходной точке: primary roadmap уже активировал Stage 7 только в границах Stage 7.1 backend skeleton, но документация о backend stack, baseline-статусах и правилах дальнейшей реализации рассинхронизирована.

Главный блокер для продолжения Stage 7: в репозитории одновременно существуют документы, закрепляющие Kotlin + Ktor как рабочую гипотезу backend, и фактический Stage 7.1 skeleton на Java 17 + Spring Boot. Это нельзя безопасно трактовать как мелкое расхождение в формулировках, потому что конфликт затрагивает архитектурные границы, будущие roadmap-задачи, prompts/governance и направление backend implementation.

Вердикт: Stage 7 implementation не следует продолжать до отдельной corrective-задачи по stack decision и синхронизации документации.

## 2. Оценка текущего состояния проекта

- Primary roadmap: `docs/roadmap/roadmap.md`.
- Текущий этап по primary roadmap: Stage 7 — MVP Implementation.
- Активированная часть Stage 7: только Stage 7.1 Backend Skeleton Preparation / Activation.
- Последний завершенный шаг по primary roadmap: Stage 7.1.
- Следующий шаг: должен быть явно активирован отдельной задачей; Stage 7.2+ не активированы.
- Фактический backend artifact: `services/backend/` со Spring Boot skeleton на Java 17.
- Реализация продукта beyond skeleton: не должна продолжаться до устранения consistency blockers.

## 3. Карта источников правды

### 3.1. Текущие authoritative источники

- `docs/roadmap/roadmap.md` — основной source of truth по progression, статусам этапов, carryover и разрешенному следующему шагу.
- `AGENTS.md` — обязательные правила работы Codex в этом репозитории.
- `docs/product/product-baseline.md` — intended product baseline, но сейчас содержит устаревшие stage/status statements.
- `docs/architecture/architecture-baseline.md` — intended architecture baseline, но сейчас содержит устаревшие stage/status/API statements.
- `docs/architecture/stage-7/stage-7-1-backend-skeleton-review.md` — review фактического Stage 7.1 skeleton.
- `services/backend/README.md` и `services/backend/build.gradle.kts` — фактическое состояние backend skeleton.

### 3.2. Secondary / reference источники

- `docs/development/roadmap.md` — future/reference material, но сейчас выглядит как потенциальный implementation backlog и содержит устаревшие Ktor-specific формулировки.
- `docs/development/milestones.md` — future/reference checkpoints, но содержит устаревшие milestone/status statements.
- `docs/development/implementation-strategy.md` — future/reference implementation guidance, но содержит Ktor-specific архитектурные формулировки.
- `docs/ROADMAP.md` — navigation summary, не конкурирующий source of truth.
- `README.md` — navigation entrypoint.
- `docs/product/README.md`, `docs/architecture/README.md`, `docs/decisions/README.md` — index/navigation docs.

### 3.3. Исторические review artifacts

Следующие файлы полезны как audit trail и не должны переписываться как active guidance без отдельной задачи:

- `docs/reviews/documentation-refactoring-plan.md`
- `docs/reviews/global-documentation-quality-review.md`
- `docs/reviews/pre-stage-6-documentation-consistency-review.md`
- `docs/reviews/roadmap-structure-and-process-fitness-review.md`
- stage-specific review файлы в `docs/architecture/stage-*`

## 4. Наблюдения по согласованности roadmap

Primary roadmap в `docs/roadmap/roadmap.md` в целом ясно фиксирует:

- Stage 6 завершен.
- Stage 7 активирован.
- Stage 7.1 завершен.
- Stage 7.2+ не активированы.
- Следующая задача должна быть явно выбрана.

Основная проблема не в primary roadmap, а в том, что часть baseline и development документов все еще описывает состояние до завершения Stage 6 и до появления Stage 7.1 skeleton. Это создает риск, что следующий agent или задача будут читать устаревшие документы как равноправный backlog.

## 5. Наблюдения по согласованности технологического стека

### 5.1. Kotlin + Ktor references

В репозитории есть несколько документов, которые прямо или косвенно закрепляют Kotlin + Ktor как backend direction:

- `AGENTS.md` — предварительный ориентир: backend Kotlin + Ktor.
- `AGENTS.md` — architecture control: Ktor routing должен оставаться тонким слоем.
- `docs/ARCHITECTURE.md` — preliminary architecture note: backend Kotlin + Ktor.
- `docs/development/roadmap.md` — backend planned as Kotlin + Ktor.
- `docs/development/milestones.md` — backend skeleton на Kotlin + Ktor.
- `docs/development/implementation-strategy.md` — Ktor details в infrastructure/adapters, Ktor routing thin.

### 5.2. Java 17 + Spring Boot references and artifacts

Фактический Stage 7.1 skeleton создан на Java 17 + Spring Boot:

- `services/backend/build.gradle.kts`
- `services/backend/README.md`
- `services/backend/src/main/java/...`
- `docs/architecture/stage-7/stage-7-1-backend-skeleton-review.md`

Также `docs/architecture/stage-6/openapi-contract-notes.md` описывает OpenAPI boundary для Next.js + React + TypeScript frontend и Spring Boot backend, хотя сам API contract остается framework-agnostic.

### 5.3. Вывод по stack consistency

Конфликт backend stack является critical. Его нельзя исправлять в рамках audit-only задачи, но его необходимо разрешить до продолжения Stage 7 implementation.

Минимально безопасное следующее действие: отдельная decision/correction задача, которая явно решит, является ли Spring Boot skeleton целевым направлением Stage 7 или должен быть пересогласован с ранее задокументированным Kotlin + Ktor направлением.

## 6. Наблюдения по актуальности документации

### 6.1. Устаревшие baseline документы

`docs/product/product-baseline.md` содержит утверждения, что Stage 6 planned / not started и production implementation not started. После завершения Stage 6 и Stage 7.1 эти statements устарели.

`docs/architecture/architecture-baseline.md` содержит утверждения, что Stage 6 planned / not started, API/OpenAPI contracts not created и production implementation not started. Это больше не соответствует текущему состоянию репозитория.

### 6.2. Устаревшие development документы

`docs/development/roadmap.md`, `docs/development/milestones.md` и `docs/development/implementation-strategy.md` остаются полезными как reference material, но сейчас содержат:

- Stage 6 planned/not started statements.
- Code/API/DB/UI not started statements.
- Ktor-specific implementation direction.
- Формулировки, которые могут выглядеть как active implementation backlog.

### 6.3. Root architecture note

`docs/ARCHITECTURE.md` остается предварительной архитектурной заметкой, но из-за имени и расположения может восприниматься как текущий authoritative architecture baseline. Сейчас он конфликтует с фактическим backend skeleton.

## 7. Наблюдения по структуре документации

- В `docs/reviews/` есть несколько review/audit файлов, но нет `docs/reviews/README.md`, который объяснял бы их статус и порядок чтения.
- В `docs/development/` есть future/reference документы, но нет `docs/development/README.md`, который явно маркировал бы их как secondary/reference относительно primary roadmap.
- `docs/product/README.md`, `docs/architecture/README.md` и `README.md` уже помогают с навигацией, но не полностью снимают риск чтения устаревших baselines как актуальных.
- Stage 7 review document написан на английском, тогда как текущие правила и большинство проектных документов ориентированы на русский язык. Это не блокирует audit, но снижает консистентность документации.
- Исторические reviews не всегда явно помечены как historical/as-of-date artifacts.

## 8. Наблюдения по правилам работы Codex и governance

### 8.1. Сильные стороны governance

- `AGENTS.md` хорошо фиксирует roadmap control, source-of-truth priority и запрет на implementation без активированной roadmap-задачи.
- Правила требуют читать обязательные документы перед изменениями.
- Правила хорошо ограничивают scope и запрещают перенос решений из других проектов.
- Prompt templates подталкивают к explicit status reporting и diff control.

### 8.2. Слабые места governance

- `AGENTS.md` содержит Ktor-specific architecture guidance, которая теперь конфликтует с Spring Boot skeleton.
- Prompt templates не требуют отдельного stack consistency gate перед backend implementation.
- `.github/pull_request_template.md` не содержит явной проверки, что изменения не противоречат active stack/architecture decision.
- `.github/ISSUE_TEMPLATE/codex_task.yml` перечисляет development docs среди обязательного чтения, но не делает достаточно явным, что они secondary/future-reference относительно primary roadmap.
- Нет явного readiness gate перед продолжением Stage 7 после corrective documentation/stack tasks.

## 9. Матрица рисков

### Critical

#### CR-001: Неразрешенный конфликт backend stack

В документации одновременно существуют Kotlin + Ktor direction и Spring Boot + Java 17 implementation artifact. Продолжение backend work без решения приведет к архитектурному drift, несовместимым задачам и возможной переделке skeleton.

Затронутые файлы:

- `AGENTS.md`
- `docs/ARCHITECTURE.md`
- `docs/development/roadmap.md`
- `docs/development/milestones.md`
- `docs/development/implementation-strategy.md`
- `docs/architecture/stage-6/openapi-contract-notes.md`
- `services/backend/`

Рекомендация: отдельная stack decision задача до любых Stage 7.2+ implementation changes.

#### CR-002: Stage 7 foundation может быть построен на незафиксированном решении

Stage 7.1 skeleton уже существует, но repository-wide architecture docs не подтверждают выбранный backend stack. Это делает следующую implementation-задачу небезопасной даже при чистом build.

Рекомендация: остановить Stage 7 implementation и выполнить correction plan: decision, documentation sync, readiness review.

### Major

#### MJ-001: Baseline документы устарели после Stage 6 и Stage 7.1

`docs/product/product-baseline.md` и `docs/architecture/architecture-baseline.md` больше не отражают текущие stage/status facts.

Рекомендация: actualize baselines или явно пометить их как baseline as of Stage 5 с ссылкой на primary roadmap.

#### MJ-002: Development docs выглядят как active backlog, хотя являются secondary/reference

`docs/development/*` содержит future/reference guidance, но формулировки могут быть ошибочно использованы как разрешение на Ktor implementation.

Рекомендация: после stack decision обновить статус, роль и предупреждения в development docs.

#### MJ-003: Root `docs/ARCHITECTURE.md` может конкурировать с architecture baseline

Файл выглядит как общий architecture source, но содержит предварительную Ktor-oriented архитектуру и не отражает Stage 6/7.1.

Рекомендация: отдельной задачей переименовывать не нужно; достаточно clearly label/reconcile после stack decision.

#### MJ-004: Governance не блокирует stack mismatch достаточно явно

Текущие правила хорошо защищают roadmap order, но слабее защищают от противоречий между stack guidance и фактическими artifacts.

Рекомендация: добавить явный consistency check в Codex/task/PR rules после принятия решения по stack.

#### MJ-005: `AGENTS.md` содержит implementation-specific Ktor guidance

Так как `AGENTS.md` имеет высокий приоритет, Ktor-specific routing guidance конфликтует с Spring Boot skeleton и может заставить будущих agents принимать неверные решения.

Рекомендация: обновить `AGENTS.md` только после stack decision.

#### MJ-006: Stage 7.1 review написан на английском

`docs/architecture/stage-7/stage-7-1-backend-skeleton-review.md` не соответствует языковой консистентности большинства документации.

Рекомендация: не переписывать исторический review в рамках audit; решить отдельной cleanup-задачей, если важна единая language policy.

#### MJ-007: Нет явного Stage 7 restart readiness gate

После corrective tasks нужен короткий gate, подтверждающий, что roadmap, baselines, architecture docs, governance и skeleton больше не конфликтуют.

Рекомендация: добавить отдельную readiness review задачу перед Stage 7.2.

### Minor

#### MN-001: Нет `docs/reviews/README.md`

Из-за отсутствия index трудно отличить historical reviews от active review findings.

#### MN-002: Нет `docs/development/README.md`

Development docs не имеют локального index с явным secondary/reference статусом.

#### MN-003: Source-of-truth правила повторяются в нескольких местах

Повторение само по себе не критично, но повышает цену синхронизации.

#### MN-004: Не все исторические документы имеют явную as-of маркировку

Это повышает риск чтения старых findings как текущих.

#### MN-005: `docs/ROADMAP.md` остается полезным summary, но требует дисциплины синхронизации

Файл не должен конкурировать с `docs/roadmap/roadmap.md`.

### Notes

#### NT-001: Primary roadmap достаточно ясно фиксирует текущий этап

`docs/roadmap/roadmap.md` уже содержит достаточную базу, чтобы не продолжать Stage 7 без explicit activation.

#### NT-002: Stage 6 OpenAPI contract выглядит framework-agnostic

Даже при Spring Boot упоминании Stage 6 contract не требует конкретного backend framework на уровне API semantics.

#### NT-003: Stage 7.1 skeleton review полезен, но не заменяет stack decision

Review подтверждает форму skeleton, но не устраняет repository-wide конфликт backend stack.

## 10. Рекомендуемая последовательность следующих задач

1. Заморозить дальнейшую Stage 7 implementation работу до corrective tasks.
2. Провести backend stack decision: Kotlin + Ktor или Java 17 + Spring Boot.
3. Если решение требует ADR, создать ADR отдельной задачей.
4. Принять решение по судьбе `services/backend/`: оставить, адаптировать или пересоздать в соответствии с выбранным stack.
5. Синхронизировать `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/architecture/architecture-baseline.md` и Stage 7 notes с выбранным stack.
6. Обновить или clearly label `docs/product/product-baseline.md` и `docs/architecture/architecture-baseline.md` относительно Stage 6/7.1.
7. Обновить `docs/development/*` как secondary/future-reference docs, чтобы они не выглядели active backlog.
8. Усилить governance: prompts, PR template и issue template должны проверять stack/architecture consistency перед implementation.
9. Добавить navigation indexes для `docs/reviews/` и `docs/development/`, если это будет явно разрешено cleanup-задачей.
10. Выполнить короткий Stage 7 restart readiness review.
11. Только после этого активировать Stage 7.2 или следующую Stage 7.x задачу.

## 11. Файлы, которые вероятно потребуют правок

Эти файлы не изменялись в рамках audit-only задачи, но вероятно потребуют отдельной corrective work:

- `AGENTS.md`
- `docs/ARCHITECTURE.md`
- `docs/roadmap/roadmap.md`
- `docs/ROADMAP.md`
- `docs/product/product-baseline.md`
- `docs/architecture/architecture-baseline.md`
- `docs/architecture/README.md`
- `docs/development/roadmap.md`
- `docs/development/milestones.md`
- `docs/development/implementation-strategy.md`
- `docs/prompts/codex-rules.md`
- `docs/prompts/task-template.md`
- `docs/prompts/review-template.md`
- `.github/pull_request_template.md`
- `.github/ISSUE_TEMPLATE/codex_task.yml`
- `services/backend/README.md`
- `services/backend/build.gradle.kts`

## 12. Файлы, которые следует оставить историческими

Следующие файлы лучше не переписывать как active guidance. Их можно пометить, проиндексировать или сослать как historical artifacts отдельной задачей:

- `docs/reviews/documentation-refactoring-plan.md`
- `docs/reviews/global-documentation-quality-review.md`
- `docs/reviews/pre-stage-6-documentation-consistency-review.md`
- `docs/reviews/roadmap-structure-and-process-fitness-review.md`
- `docs/architecture/stage-5/*`
- `docs/product/stage-1/*`
- `docs/product/stage-2/*`
- `docs/product/stage-3/*`
- `docs/product/stage-4/*`
- `docs/product/stage-5/*`

## 13. Финальный вердикт

Stage 7 implementation сейчас продолжать небезопасно.

Причина: проект имеет critical-level consistency blocker по backend stack. До выбора и фиксации stack decision дальнейшая работа над backend skeleton, application wiring, domain boundaries, provider interfaces, persistence, frontend integration или runtime infrastructure будет расширять архитектурный drift.

Документационная структура достаточна для audit/stabilization работы, но недостаточно безопасна для продолжения implementation. Следующий безопасный шаг — отдельная corrective documentation/architecture governance задача, начинающаяся со stack decision и заканчивающаяся Stage 7 restart readiness check.
