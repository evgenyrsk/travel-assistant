# Stage 7.0f-e — Product/Architecture Index Role Labels Cleanup

## Цель cleanup

Уточнить роли product и architecture документов через легкие role labels в index/baseline/navigation документах, чтобы будущие contributors и Codex не путали текущие baselines, historical stage artifacts, contract drafts, reviews и superseded context.

## Что было проблемой

В `docs/product/**` и `docs/architecture/**` одновременно находятся current baselines, index-документы, historical stage artifacts, contract drafts, review reports и superseded skeleton context. Без явной role hierarchy старые формулировки могли выглядеть как current product/architecture instructions или как разрешение начинать Stage 7.2 implementation.

## Какие product docs были проверены

- Current product source of truth: `docs/product/product-baseline.md`.
- Product navigation/index: `docs/product/README.md`.
- Historical product stage artifacts: `docs/product/stage-0/**`, `docs/product/stage-1/**`, `docs/product/stage-2/**`, `docs/product/stage-3/**`, `docs/product/stage-4/**`.
- Product review/audit artifacts: `stage-1/stage-1-consistency-review.md`, `stage-2/stage-2-consistency-review.md`, `stage-3/stage-3-hotel-only-consistency-review.md`, `stage-3/stage-3-plan-reconciliation.md`, `stage-4/stage-4-consistency-review.md`.
- Stale/superseded product artifact: `docs/product/stage-3/combined-search-ux-decision.md` для MVP v1.
- Reference-only product artifacts: detailed flow, data, behaviour, acceptance criteria, design foundation и screen specification documents внутри Stage 2-4.
- Unclear role: нет после обновления `docs/product/README.md`.

Полная file-level классификация добавлена в `docs/product/README.md`.

## Какие architecture docs были проверены

- Current architecture source of truth: `docs/architecture/architecture-baseline.md`.
- Architecture navigation/index: `docs/architecture/README.md`.
- Historical architecture stage artifacts: `docs/architecture/stage-5/**`.
- API/contract drafts: `docs/architecture/stage-6/openapi-draft.yaml`, `openapi-contract-notes.md`, `openapi-fixes-summary.md`, `provider-boundary-mapping-notes.md`, `pre-implementation-decisions-cleanup.md`.
- Review/audit artifacts: Stage 5 consistency review, Stage 6 contract/completion reviews and final handoff, Stage 7 skeleton review.
- Stale/superseded architecture artifact: `docs/architecture/stage-7/stage-7-1-backend-skeleton-review.md`, потому что old Java/Spring Boot skeleton context superseded by Stage 7.0b Kotlin + Ktor correction.
- Reference-only architecture artifacts: Stage 5 conceptual details, Stage 6 contract notes and handoff context.
- Unclear role: нет после обновления `docs/architecture/README.md`.

Полная file-level классификация добавлена в `docs/architecture/README.md`.

## Какая source-of-truth hierarchy теперь используется

1. `docs/product/product-baseline.md` — product source of truth для MVP scope, active product framing и product guardrails.
2. `docs/architecture/architecture-baseline.md` — architecture source of truth и backend stack authority.
3. `docs/roadmap/roadmap.md` — primary roadmap и source of truth по stage status, progression, carryover и следующему разрешенному шагу.
4. `AGENTS.md` — canonical repository/agent governance.
5. `docs/product/README.md` — product documentation index/navigation only.
6. `docs/architecture/README.md` — architecture documentation index/navigation only.
7. `docs/product/stage-*/**` — historical product discovery/design artifacts unless explicitly stated otherwise.
8. `docs/architecture/stage-*/**` — historical architecture artifacts, API/contract drafts, review reports или superseded skeleton context according to index labels.

## Что изменено

- `docs/product/README.md` получил role hierarchy и file-level inventory для `docs/product/**`.
- `docs/architecture/README.md` получил role hierarchy и file-level inventory для `docs/architecture/**`.
- `docs/product/product-baseline.md` получил короткую role note как current product source of truth.
- `docs/architecture/architecture-baseline.md` получил короткую role note как current architecture source of truth и backend stack authority.
- `README.md`, `docs/ROADMAP.md`, `docs/roadmap/roadmap.md` и `docs/reviews/README.md` получили минимальную navigation/status синхронизацию для Stage 7.0f-e.

## Что намеренно не менялось

- Historical product stage artifacts не переписывались.
- Historical architecture stage artifacts не переписывались.
- OpenAPI draft, contract reviews и Stage 6 handoff artifacts не менялись.
- Old findings/verdicts в review reports не переписывались.
- MVP scope, roadmap ordering, architecture decisions и backend stack не менялись.
- Stage 7.2 не начинался.
- Backend/frontend code, provider integrations, DB/storage, generated clients и production implementation не создавались.

## Как теперь читать docs/product/**

Для product scope начинать с `docs/product/product-baseline.md`. `docs/product/README.md` использовать как index и role map. Документы `stage-0/**` - `stage-4/**` читать как historical product discovery/design artifacts, detailed context или review trail. Если historical wording шире текущего MVP v1, приоритет имеет `product-baseline.md` вместе с primary roadmap.

## Как теперь читать docs/architecture/**

Для architecture scope начинать с `docs/architecture/architecture-baseline.md`. `docs/architecture/README.md` использовать как index и role map. Stage 5 читать как detailed conceptual baseline context, Stage 6 — как contract/documentation artifacts, Stage 7 old skeleton review — как superseded historical context. Historical Java/Spring Boot mentions не переопределяют Kotlin + Ktor direction.

## Remaining documentation cleanup items

- Style guide stale wording cleanup.
- Roadmap readability cleanup.
- Broader documentation redundancy cleanup, если отдельная roadmap-aligned задача сочтет это нужным.

Эти items не являются active backlog.

## Final verdict

Stage 7.0f-e выполнен как narrow documentation cleanup. Product/architecture documentation hierarchy теперь явно размечена через index role labels и baseline role notes. Current baselines отделены от historical artifacts, contract drafts, review reports и superseded stack context.

## Scope control confirmation

Scope удержан в пределах product/architecture index role labeling и минимальной navigation/status синхронизации. Задача не запускала Stage 7.2, не меняла roadmap ordering, не расширяла MVP scope, не меняла architecture decisions и не создавала implementation artifacts.
