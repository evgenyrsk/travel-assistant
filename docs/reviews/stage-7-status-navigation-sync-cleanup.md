# Stage 7 Status & Navigation Sync Cleanup

Дата: 2026-06-03

## Цель cleanup

Синхронизировать активные status/navigation/source-of-truth документы после Stage 7 restart readiness review и Stage 7.0e documentation redundancy / structure audit.

Cleanup устраняет stale wording о том, что Stage 7 implementation все еще заблокирована до restart readiness review. Задача не начинает Stage 7.2, не выполняет broad documentation refactoring и не меняет MVP scope, architecture decisions, ADR, OpenAPI или backend/frontend code.

## Что было устаревшим

Устаревшие активные формулировки:

- Stage 7 implementation заблокирована до restart readiness review;
- restart readiness review должен быть выбран явно как следующий шаг;
- Stage 7.2+ требуют отдельной задачи после restart readiness review, без учета того, что review уже прошел;
- architecture baseline и architecture README все еще описывали Stage 7 как blocked after Stage 7.0b;
- development docs все еще говорили, что дальнейшая Stage 7 implementation работа заблокирована до restart readiness review.

Фактическое текущее состояние:

- Java/Spring Boot backend drift исправлен на минимальный Kotlin + Ktor skeleton в Stage 7.0b;
- Stage 7 restart readiness review прошел с minor notes;
- product baseline status cleanup выполнен;
- Stage 7.0e documentation redundancy / structure audit выполнен и нашел remaining cleanup needs;
- Stage 7.0f-a синхронизирует только status/navigation wording;
- Stage 7.2+ остаются неактивированными до отдельной явной roadmap-aligned задачи.

## Какие файлы проверены

- `README.md`
- `AGENTS.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/product/README.md`
- `docs/architecture/README.md`
- `docs/architecture/architecture-baseline.md`
- `docs/development/roadmap.md`
- `docs/development/milestones.md`
- `docs/development/implementation-strategy.md`
- `docs/prompts/codex-rules.md`
- `docs/prompts/task-template.md`
- `docs/prompts/review-template.md`
- `docs/reviews/**`
- `.github/ISSUE_TEMPLATE/codex_task.yml`
- `.github/pull_request_template.md`

## Какие файлы изменены

- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/architecture/README.md`
- `docs/architecture/architecture-baseline.md`
- `docs/development/roadmap.md`
- `docs/development/milestones.md`
- `docs/development/implementation-strategy.md`
- `docs/reviews/stage-7-status-navigation-sync-cleanup.md`

## Что было синхронизировано

- Active README/navigation wording теперь говорит, что Stage 7 больше не заблокирован backend stack drift или restart readiness review.
- Primary roadmap теперь фиксирует Stage 7.0f-a как последний завершенный cleanup step и сохраняет Stage 7.2+ как неактивированные.
- `docs/ROADMAP.md` теперь отражает, что restart readiness review прошел, Stage 7.0e нашел remaining documentation cleanup needs, а Stage 7.0f-a является narrow status/navigation cleanup.
- `docs/architecture/README.md` теперь помечает старый Stage 7.1 Java/Spring Boot skeleton review как historical artifact, superseded by Stage 7.0b Kotlin + Ktor correction.
- `docs/architecture/architecture-baseline.md` теперь не говорит, что следующая implementation задача ждет restart readiness review; вместо этого она требует отдельную roadmap-aligned задачу.
- `docs/development/*` теперь описывают Stage 7.0f-a context и не выглядят так, будто restart readiness review еще pending.
- Во всех измененных активных документах сохранено: Stage 7.2+ не активированы, broader documentation cleanup pending, business logic/provider/frontend/DB/auth/production implementation не создаются без отдельной явной roadmap activation.

## Что намеренно не менялось

- Backend/frontend code не менялся.
- Stage 7.2 не начинался.
- Roadmap не переопределялся и порядок этапов не менялся.
- Architecture decisions, ADR и OpenAPI artifacts не менялись.
- MVP scope не расширялся.
- `AGENTS.md`, `docs/product/README.md`, `docs/prompts/**` и `.github/**` не менялись, потому что проверка не нашла в них stale active wording про pending restart readiness review.
- Historical review artifacts в `docs/reviews/**` не переписывались. Упоминания Java/Spring Boot, blocked status, restart readiness review и Stage 7.2 в них сохранены как audit trail состояния на момент соответствующих reviews.
- `docs/reviews/documentation-redundancy-structure-audit.md` не переписывался; он остается Stage 7.0e audit artifact and source for this cleanup.

## Remaining documentation cleanup items

- Style guide stale wording cleanup: `docs/guides/documentation-style-guide.md` все еще содержит historical/examples wording про Stage 6 `Planned / not started`.
- Reviews index / archival labels: нужен будущий `docs/reviews/README.md` или аналогичная маркировка historical/current/superseded review artifacts.
- Prompt/governance deduplication: `AGENTS.md`, `docs/prompts/**` и `.github/**` все еще частично дублируют guardrails and final report formats.
- Development docs merge/shortening: `docs/development/roadmap.md`, `docs/development/milestones.md` и `docs/development/implementation-strategy.md` остаются overlapping future/reference documents.
- Product/architecture index role labels: historical/superseded artifacts, включая combined search docs и old Stage 7.1 review, стоит промаркировать системнее.
- Roadmap readability cleanup остается pending; текущая задача исправила status wording, но не выполняла broad roadmap refactoring.

## Final verdict

Passed — stale active wording о pending restart readiness review удален из проверенных active/navigation/source-of-truth документов.

Stage 7 больше не заблокирован backend stack drift или restart readiness review. Stage 7.2+ при этом не активированы и требуют отдельной явной roadmap-aligned задачи.

Broader documentation cleanup все еще требуется, но больше не является blocker из-за stale restart readiness wording.

## Scope control confirmation

- Cleanup ограничен Stage 7 status/navigation wording и коротким cleanup report.
- Backend/frontend implementation не создавались.
- Stage 7.2 не начинался.
- Product scope и MVP boundaries не менялись.
- Architecture decisions, ADR, OpenAPI и provider/API contracts не менялись.
- Historical artifacts не переписывались.
- Unrelated files не изменялись.
