# Stage 7.18 — Conformance Gate Skeleton Planning-to-Tooling

## 1. Цель Stage 7.18

Цель Stage 7.18 — определить, как будущий generated-client/OpenAPI conformance gate skeleton должен быть введен как tooling, не реализуя его в этой задаче.

Документ связывает:

- Stage 7.16 conformance gate planning;
- Stage 7.17 generated-client-ready subset / placeholder exclusion policy;
- текущие OpenAPI/runtime blockers.

Stage 7.18 не реализует gate, не создает scripts, tests, build tasks, subset config files, generated clients, OpenAPI changes или backend behavior changes.

## 2. Проверенные источники

- `AGENTS.md`
- `docs/prompts/codex-task-template.md`
- `docs/prompts/codex-review-template.md`
- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/product/product-baseline.md`
- `docs/architecture/architecture-baseline.md`
- `docs/development/documentation-guidelines.md`
- `docs/development/quality-gates.md`
- `docs/reviews/README.md`
- `services/backend/README.md`
- `docs/reviews/stage-7-13-generated-client-openapi-readiness-checkpoint.md`
- `docs/reviews/stage-7-14-generated-client-openapi-readiness-cleanup.md`
- `docs/reviews/stage-7-14-generated-client-openapi-readiness-cleanup-review.md`
- `docs/reviews/stage-7-15-assistant-response-semantics-search-readiness-boundary-cleanup.md`
- `docs/reviews/stage-7-15-assistant-response-semantics-search-readiness-boundary-cleanup-review.md`
- `docs/reviews/stage-7-15b-stage-7-13-7-15-documentation-status-sync.md`
- `docs/reviews/stage-7-16-generated-client-openapi-conformance-gate-planning.md`
- `docs/reviews/stage-7-17-generated-client-ready-subset-placeholder-exclusion-policy.md`
- `docs/architecture/stage-6/openapi-draft.yaml`
- `docs/architecture/stage-6/openapi-contract-notes.md`

В корне репозитория отдельный `openapi-draft.yaml` не найден; актуальный Stage 6 draft находится в `docs/architecture/stage-6/openapi-draft.yaml`.

## 3. Использование task/review templates

`docs/prompts/codex-task-template.md` был прочитан до repository inspection и использован как структура выполнения: source review, allowed/forbidden scope, documentation expectations, validation expectations и final reporting.

`docs/prompts/codex-review-template.md` был прочитан до repository inspection и использован для self-review перед завершением: проверены scope drift, roadmap/status consistency, documentation consistency, stale active wording, historical report handling, recommendations not implemented и отсутствие out-of-scope implementation.

## 4. Текущее состояние

Stage 7 завершен through Stage 7.17 до начала Stage 7.18.

Текущее состояние:

- generated-client/OpenAPI readiness не заявлена;
- generated-client-ready subset не создан;
- conformance gate не реализован;
- generated clients не созданы;
- OpenAPI finalization не заявлена;
- placeholder hotel search, offers, shortlist и explanations endpoints still return `501 NOT_IMPLEMENTED`;
- OpenAPI draft описывает success schemas для будущих resource flows, которые runtime пока не производит;
- error taxonomy остается foundation-only там, где runtime еще не имеет real resource semantics.

## 5. Почему implementation gate ещё рано

Implementation gate еще рано, потому что у будущего gate пока нет всех устойчивых inputs, которые он должен enforce:

- нет machine-readable generated-client-ready subset manifest;
- нет OpenAPI/runtime route inventory tooling;
- нет stable final error taxonomy для resource endpoints;
- нет runtime behavior для hotel search, offers, shortlist и explanations;
- нет generated-client generator config;
- нет runtime contract test harness;
- нет endpoint slices, которые закрывают full OpenAPI success schemas beyond foundation candidates.

Если реализовать gate сейчас как blocking production readiness check, он либо будет проверять слишком мало, либо создаст ложное ощущение generated-client readiness.

## 6. Future conformance gate skeleton purpose

Будущий conformance gate skeleton должен быть первым tooling layer, который:

- читает OpenAPI source и subset/exclusion policy inputs;
- классифицирует endpoints как included, excluded или unclassified;
- обнаруживает route/path drift между OpenAPI и runtime inventory;
- запрещает включение placeholder endpoints в generated-client-ready subset;
- отделяет blocking readiness violations от advisory planning findings;
- выводит отчет, который не заявляет readiness, пока не выполнены все required checks;
- готовит путь к будущим generated-client compile checks и runtime contract tests.

Первый skeleton должен быть guardrail/reporting tool, а не client generator и не runtime behavior validator для еще не реализованных flows.

## 7. Expected future inputs

Ожидаемые будущие inputs:

- OpenAPI draft/source: например `docs/architecture/stage-6/openapi-draft.yaml` или future finalized source.
- Conceptual или machine-readable generated-client-ready subset manifest.
- Runtime route/path inventory для Ktor routes.
- Placeholder exclusion list из Stage 7.17 policy.
- Documented error taxonomy и distinction between final taxonomy / foundation-only runtime codes.
- Endpoint classification metadata: included, excluded, future-only, unclassified.
- Optional generated-client output — future-only после generator config.
- Optional runtime contract test results — future-only после real behavior slices.

Stage 7.18 не создает эти inputs как files.

## 8. Expected future outputs

Ожидаемые будущие outputs:

- pass/fail summary для выбранного gate mode;
- blocking findings list;
- advisory findings list;
- endpoint inclusion/exclusion report;
- unclassified endpoint report;
- schema/runtime drift report;
- placeholder inclusion violation report;
- error taxonomy compatibility report;
- generated-client compile status — future-only;
- runtime contract conformance status — future-only.

Output должен явно различать `not_ready`, `foundation_only`, `advisory_pass` и future `ready` states, чтобы не возникало ложных readiness claims.

## 9. Proposed future gate phases

Предлагаемые future phases:

1. Static OpenAPI validation: parse/validate OpenAPI source.
2. Path/method inventory extraction: извлечь OpenAPI paths/methods и runtime route inventory.
3. Subset manifest validation: проверить manifest shape, required fields и endpoint classifications.
4. Placeholder exclusion enforcement: fail, если placeholder endpoint включен в generated-client-ready subset.
5. Error taxonomy compatibility check: сравнить included endpoint errors с documented taxonomy.
6. Response schema readiness check: static или contract-level check для included endpoints.
7. Generated-client compile check: future-only после generator config и safe subset.
8. Runtime contract tests: future-only после real endpoint behavior.

Первый implementation skeleton должен начинаться с phases 1-4 и reporting. Phases 5-8 могут быть stubbed as future-only statuses, но не должны притворяться выполненными.

## 10. Blocking vs advisory checks

Blocking checks для первого skeleton:

- OpenAPI source отсутствует или не читается.
- Subset manifest отсутствует в gate mode, который требует manifest.
- Manifest содержит endpoint без explicit classification.
- Placeholder endpoint включен в `includedEndpoints`.
- Included endpoint отсутствует в OpenAPI source или runtime inventory.
- Included endpoint возвращает или классифицирован как `501 NOT_IMPLEMENTED`.
- Gate output пытается заявить readiness при unresolved blockers.

Advisory checks:

- Assistant endpoints требуют explicit conformance decision before inclusion.
- Error taxonomy пока foundation-only для excluded endpoints.
- Generated-client compile check not configured.
- Runtime contract tests not configured.
- OpenAPI success schemas существуют для endpoints, которые остаются excluded.
- Documentation/status wording should be updated after future implementation tasks.

Advisory findings не должны блокировать foundation planning work, но должны оставаться видимыми.

## 11. First implementation skeleton boundaries

Первый implementation skeleton должен:

- быть read-only/static tooling;
- читать OpenAPI source;
- читать future subset manifest, если он уже создан отдельной задачей;
- проверять endpoint classification completeness;
- enforce placeholder exclusion;
- выдавать machine-readable или human-readable report;
- завершаться failure code только для blocking violations внутри выбранного gate mode;
- не генерировать clients;
- не вызывать backend server;
- не изменять OpenAPI или backend files;
- не создавать fake success payloads;
- не считать excluded placeholders contract-ready.

Первый skeleton не должен:

- создавать subset manifest сам;
- добавлять scripts/build tasks без отдельной implementation task;
- запускать OpenAPI generator;
- запускать runtime contract tests;
- проверять real hotel search behavior, которого нет;
- менять public API behavior.

## 12. Conceptual file locations and command names

Stage 7.18 не создает файлы, scripts или commands. Концептуальные future locations:

- `tools/openapi-conformance/` — возможная директория для standalone tooling.
- `docs/development/quality-gates.md` — future documentation location для команды после implementation.
- `docs/reviews/` — audit trail для gate planning/review reports.
- `generated-client-subset.yaml` или аналогичный manifest path — future-only, если отдельная задача создаст config.

Концептуальные future commands:

- `./tools/openapi-conformance/check`
- `./gradlew openApiConformanceCheck`
- `npm run openapi:conformance`

Выбор command name и runtime должен быть отдельным implementation decision. Stage 7.18 только фиксирует варианты, не выбирает и не создает command.

## 13. False readiness claim prevention

Будущий skeleton должен предотвращать false readiness claims так:

- default status должен быть `not_ready`, пока все required checks не определены и не проходят;
- excluded endpoints не должны учитываться как readiness success;
- placeholder endpoints must fail if included;
- future-only checks must appear as `not_run` или `future_only`, not `passed`;
- generated-client compile status нельзя выводить как passed без реального запуска generation/compile;
- runtime contract status нельзя выводить как passed без реальных contract tests;
- reports должны явно перечислять unresolved blockers;
- docs/status wording не должно говорить, что generated-client/OpenAPI readiness достигнута, пока это не подтверждено отдельной readiness/finalization task.

## 14. Relationship to Stage 7.16 conformance gate planning

Stage 7.16 определил цели gate и candidate checks. Stage 7.18 переводит эти цели в future tooling shape:

- Stage 7.16: gate should compare OpenAPI/runtime and subset readiness.
- Stage 7.18: first skeleton should read OpenAPI/source inputs and produce endpoint classification/drift reports.
- Stage 7.16: generated-client compile and runtime tests are future-only.
- Stage 7.18: these phases remain future-only statuses in the skeleton until prerequisites exist.
- Stage 7.16: avoid false readiness claims.
- Stage 7.18: defines explicit `not_ready`, `not_run`, `future_only`, blocking/advisory reporting behavior.

Stage 7.18 не заменяет Stage 7.16; он делает следующий planning-to-tooling bridge.

## 15. Relationship to Stage 7.17 subset/exclusion policy

Stage 7.17 определил inclusion/exclusion criteria. Stage 7.18 описывает, как tooling should enforce them:

- included endpoints must satisfy Stage 7.17 inclusion policy;
- excluded endpoints remain visible in report, not hidden;
- placeholder endpoints returning `501 NOT_IMPLEMENTED` must fail if included;
- assistant endpoints remain foundation-only candidates until explicit conformance decision;
- `GET /api/v1/health` can remain low-risk candidate for future subset discussion, not readiness claim;
- no endpoint becomes generated-client-ready by being present in OpenAPI or runtime route inventory alone.

## 16. Remaining prerequisites before implementation

Before implementing the skeleton, future task should decide:

- tooling runtime/language;
- command name and invocation style;
- whether first skeleton requires a manifest or starts with embedded policy inventory;
- machine-readable report format;
- exact OpenAPI validation library/tool;
- exact runtime route inventory extraction method;
- where future subset manifest will live;
- whether checks run locally only or in CI;
- which failures are blocking in first implementation mode.

These decisions are intentionally not made as repo changes in Stage 7.18.

## 17. Remaining generated-client blockers

- Generated-client-ready subset config does not exist.
- Conformance gate implementation does not exist.
- Generated-client generator config does not exist.
- Generated-client compile check does not exist.
- Placeholder endpoints still return `501 NOT_IMPLEMENTED`.
- Assistant endpoints remain foundation-only, not automatically generated-client-ready.

## 18. Remaining OpenAPI finalization blockers

- Stage 6 OpenAPI draft includes future resource success schemas not implemented by runtime.
- Placeholder runtime does not match hotel search, offers, shortlist or explanation success schemas.
- Error taxonomy is not final for real resource endpoints.
- `hotelSearchRequest` remains absent until real search/value boundary exists.
- No OpenAPI/runtime conformance gate exists.
- No final readiness review has approved OpenAPI finalization.

## 19. Remaining runtime behavior blockers

- No real hotel search orchestration.
- No `HotelSearchRequest` construction from confirmed criteria.
- No provider-backed hotel facts.
- No hotel offers/result envelope behavior.
- No shortlist resource behavior.
- No explanation/comparison behavior.
- No durable persistence or resource lifecycle.
- No LLM orchestration, requirements extraction or natural-language slot filling.

## 20. Proposed next staged path

Рекомендуемый staged path:

1. Stage 7.19 — Conformance gate skeleton implementation planning, if needed, to choose tooling runtime and command shape.
2. Future implementation task — add read-only/static skeleton for OpenAPI presence, path inventory and subset/exclusion classification.
3. Future subset manifest task — create machine-readable manifest after skeleton assumptions are stable.
4. Future endpoint-slice alignment tasks — make selected endpoints contract-aligned.
5. Future generated-client generation task — run generation/compile only after gate passes for safe subset.

Этот path не запускается Stage 7.19 автоматически.

## 21. Что было изменено

- Создан Stage 7.18 planning-to-tooling report.
- Добавлена узкая запись Stage 7.18 в `docs/reviews/README.md`.
- Активное status wording в `README.md`, `docs/ROADMAP.md` и `docs/roadmap/roadmap.md` синхронизировано с фактом завершения Stage 7.18 planning task.

## 22. Созданные файлы

- `docs/reviews/stage-7-18-conformance-gate-skeleton-planning-to-tooling.md`

## 23. Изменённые файлы

- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/reviews/README.md`

## 24. Что намеренно не реализовывалось

- Generated-client/OpenAPI conformance gate.
- Subset config files.
- Scripts, tests, build tasks или CI checks.
- Generated-client generation.
- OpenAPI draft updates или rewrite.
- Backend code или public API behavior changes.
- DB/storage, Redis/cache или durable persistence.
- Provider integration, LLM orchestration или requirements extraction.
- Real hotel search, ranking, shortlist или explanation behavior.
- Frontend, booking, payment или flights.
- Stage 7.19 или любые более поздние этапы.

## 25. Проверки

- `git status --short` — выполнено до изменений; рабочее дерево было чистым.
- `git status --short` — выполнено после изменений; показал только ожидаемые documentation changes:
  - `README.md`
  - `docs/ROADMAP.md`
  - `docs/roadmap/roadmap.md`
  - `docs/reviews/README.md`
  - `docs/reviews/stage-7-18-conformance-gate-skeleton-planning-to-tooling.md`
- `git diff --check` — passed.

Backend Gradle tests не запускались, потому что Stage 7.18 является documentation/planning задачей и не меняет backend или build files.

## 26. Self-review summary

Self-review по `docs/prompts/codex-review-template.md`:

- scope соответствует Stage 7.18 planning/documentation задаче;
- conformance gate skeleton описан как future tooling, не реализован;
- subset config не создан;
- scripts, tests и build tasks не добавлены;
- generated clients не создавались;
- OpenAPI draft не изменялся;
- backend behavior не изменялся;
- Stage 7.19+ не активирован;
- roadmap/status wording не заявляет generated-client readiness, generated-client-ready subset existence, conformance gate implementation или OpenAPI finalization readiness;
- historical reports не переписывались.

## 27. Recommended next task

Рекомендуемая следующая задача: отдельный bounded Stage 7.19 task для conformance gate skeleton implementation planning, если roadmap решит продолжать tooling track.

Эта рекомендация не запускает Stage 7.19 автоматически.

## 28. Scope control confirmation

Stage 7.18 ограничен planning/documentation работой. Не выполнялись conformance gate implementation, subset config creation, scripts, tests, build tasks, generated-client generation, OpenAPI changes, backend behavior changes, frontend, provider integration, LLM orchestration, DB/storage, booking, payment или flights.
