# Stage 7 Reviews Index & Historical Artifact Labeling Cleanup

Дата: 2026-06-03

## Цель cleanup

Создать понятный index для `docs/reviews/**` и минимально промаркировать роли review/audit artifacts, чтобы historical reports не конкурировали с primary roadmap, product baseline, architecture baseline или AGENTS.md.

Cleanup является узкой documentation stabilization задачей. Он не начинает Stage 7.2, не выполняет broad documentation refactoring, не удаляет и не переписывает historical artifacts, не меняет architecture decisions, ADR, OpenAPI, backend/frontend code или MVP scope.

## Что было проблемой

В `docs/reviews/**` накопились review, audit, readiness and cleanup reports разных этапов. Они полезны как audit trail, но без index могли создавать путаницу:

- старые pre-Stage 6 reports содержат historical status wording;
- old Stage 7 reports упоминают Java/Spring Boot state, который уже superseded by Kotlin + Ktor correction;
- `documentation-refactoring-plan.md` частично superseded later baseline docs and Stage 7 cleanup reports;
- current cleanup chain после Stage 7.0e/7.0f-a не была выделена в одном месте;
- Codex мог прочитать old findings как active backlog или source-of-truth instructions.

## Какие review artifacts были классифицированы

Классифицированы все текущие files в `docs/reviews/**`:

- `project-consistency-audit.md` — historical audit trail.
- `backend-stack-decision-sync.md` — historical audit trail / completed cleanup report.
- `backend-skeleton-correction.md` — completed cleanup report.
- `stage-7-restart-readiness-review.md` — historical readiness gate / reference-only after Stage 7.0f-a.
- `product-baseline-status-cleanup.md` — completed cleanup report.
- `documentation-redundancy-structure-audit.md` — current active review context / Stage 7.0e audit.
- `stage-7-status-navigation-sync-cleanup.md` — completed cleanup report / Stage 7.0f-a.
- `stage-7-reviews-index-historical-labeling-cleanup.md` — completed cleanup report / Stage 7.0f-b.
- `pre-stage-6-documentation-consistency-review.md` — historical audit trail.
- `roadmap-structure-and-process-fitness-review.md` — historical audit trail.
- `global-documentation-quality-review.md` — historical audit trail / partly superseded.
- `documentation-refactoring-plan.md` — reference-only / partly superseded.

## Что изменено

- Создан `docs/reviews/README.md`.
- В reviews index зафиксировано, что `docs/reviews/**` является audit trail, а не primary source of truth.
- В reviews index добавлены current source-of-truth links:
  - `docs/roadmap/roadmap.md`;
  - `docs/product/product-baseline.md`;
  - `docs/architecture/architecture-baseline.md`;
  - `AGENTS.md`;
  - `docs/decisions/README.md`.
- В reviews index выделена текущая documentation cleanup chain:
  - `docs/reviews/documentation-redundancy-structure-audit.md`;
  - `docs/reviews/stage-7-status-navigation-sync-cleanup.md`;
  - `docs/reviews/README.md`;
  - `docs/reviews/stage-7-reviews-index-historical-labeling-cleanup.md`.
- Root/navigation/status docs минимально обновлены ссылками на reviews index and Stage 7.0f-b cleanup:
  - `README.md`;
  - `docs/ROADMAP.md`;
  - `docs/roadmap/roadmap.md`;
  - `docs/architecture/README.md`;
  - `docs/architecture/architecture-baseline.md`;
  - `docs/development/roadmap.md`;
  - `docs/development/milestones.md`;
  - `docs/development/implementation-strategy.md`.

## Что намеренно не менялось

- Historical review reports не переписывались.
- Findings, verdicts and recommendations в old reports не менялись.
- Никакие review artifacts не удалялись, не перемещались, не переименовывались и не объединялись.
- `AGENTS.md`, `docs/product/README.md`, `docs/prompts/**` и `.github/**` не менялись: reviews index решает текущую задачу без расширения scope на prompt/governance deduplication.
- Roadmap не переопределялся; обновлен только минимальный status/navigation trace для Stage 7.0f-b.
- Stage 7.2 не начинался.
- Backend/frontend code, OpenAPI, ADR, architecture decisions и MVP scope не менялись.

## Как теперь читать docs/reviews/**

- Начинай с `docs/reviews/README.md`, чтобы понять роль review artifact.
- Для текущего статуса этапов всегда используй `docs/roadmap/roadmap.md`.
- Для текущего product scope используй `docs/product/product-baseline.md`.
- Для architecture/backend stack используй `docs/architecture/architecture-baseline.md`.
- Historical review artifacts читать как state as-of review time, а не как current instructions.
- Superseded или partly superseded reports сохранять как audit trail; не выполнять их recommendations без отдельной явной roadmap-aligned задачи.
- Old Java/Spring Boot mentions в historical reports не являются текущим backend direction.

## Remaining documentation cleanup items

- Style guide stale wording cleanup.
- Prompt/governance deduplication.
- Development docs merge/shortening.
- Product/architecture index role labels.
- Roadmap readability cleanup.
- Broader documentation redundancy cleanup.

Эти items не являются active backlog и требуют отдельных явных roadmap-aligned задач.

## Final verdict

Passed — `docs/reviews/**` получил clear index and role labeling. Review artifacts теперь discoverable as current cleanup context, completed cleanup reports, historical audit trail, partly superseded/reference-only documents.

Reviews больше не должны конкурировать с source-of-truth docs, если Codex начинает чтение с `docs/reviews/README.md` and current baseline documents.

Stage 7.2+ не активированы.

## Scope control confirmation

- Cleanup ограничен reviews index / historical artifact labeling и минимальными navigation/status updates.
- Backend/frontend implementation не создавались.
- Stage 7.2 не начинался.
- Product scope и MVP boundaries не менялись.
- Architecture decisions, ADR, OpenAPI и provider/API contracts не менялись.
- Historical reports не переписывались.
- Unrelated files не изменялись.
