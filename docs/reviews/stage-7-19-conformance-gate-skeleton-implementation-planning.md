# Stage 7.19 — Conformance Gate Skeleton Implementation Planning / Tooling Decision

## 1. Цель Stage 7.19

Цель Stage 7.19 — перевести conceptual tooling shape из Stage 7.18 в concrete implementation decisions для будущего generated-client/OpenAPI conformance gate skeleton.

Stage 7.19 выбирает рекомендуемую будущую форму skeleton implementation, но не реализует сам skeleton.

Документ фиксирует:

- какие implementation options рассмотрены;
- какой future approach рекомендуется;
- какой runtime/language, command name, directory layout, subset manifest path и output format стоит использовать в первой implementation task;
- какие checks должны быть blocking или advisory;
- какие checks должны остаться future-only;
- какие prerequisites остаются перед фактической реализацией.

Stage 7.19 не заявляет generated-client readiness, OpenAPI finalization readiness, наличие conformance gate, наличие conformance gate skeleton или наличие generated-client-ready subset.

## 2. Проверенные источники

- `AGENTS.md`
- `docs/prompts/codex-task-template.md`
- `docs/prompts/codex-review-template.md`
- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/product/product-baseline.md`
- `docs/architecture/architecture-baseline.md`
- `docs/development/README.md`
- `docs/development/coding-standards.md`
- `docs/development/testing-strategy.md`
- `docs/development/documentation-guidelines.md`
- `docs/development/definition-of-done.md`
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
- `docs/reviews/stage-7-18-conformance-gate-skeleton-planning-to-tooling.md`
- `docs/architecture/stage-6/openapi-draft.yaml`
- `docs/architecture/stage-6/openapi-contract-notes.md`

В корне репозитория отдельный `openapi-draft.yaml` не найден; актуальный Stage 6 draft находится в `docs/architecture/stage-6/openapi-draft.yaml`.

## 3. Использование task/review templates

`docs/prompts/codex-task-template.md` был прочитан до repository inspection и использован как структура выполнения: source review, scope boundaries, documentation expectations, validation expectations и final reporting.

`docs/prompts/codex-review-template.md` был прочитан до repository inspection и использован для self-review перед завершением: проверены scope drift, roadmap/status consistency, documentation consistency, stale active wording, historical report handling, recommendations not implemented и отсутствие out-of-scope implementation.

## 4. Текущее состояние

Stage 7 завершен through Stage 7.18 до начала Stage 7.19. Stage 7.19 является отдельной planning/decision задачей.

Текущее состояние:

- generated-client/OpenAPI readiness не заявлена;
- generated-client-ready subset не создан;
- conformance gate не реализован;
- conformance gate skeleton не реализован;
- generated clients не созданы;
- OpenAPI finalization не заявлена;
- placeholder hotel search, offers, shortlist и explanations endpoints still return `501 NOT_IMPLEMENTED`;
- OpenAPI draft описывает success schemas для будущих resource flows, которые runtime пока не производит;
- error taxonomy остается foundation-only там, где runtime еще не имеет real resource semantics.

## 5. Почему implementation ещё не выполняется

Implementation еще не выполняется, потому что Stage 7.19 должен принять tooling decision, а не создать tooling.

До фактической реализации нужно явно согласовать:

- runtime/language будущего skeleton;
- command name и invocation style;
- file/directory layout;
- subset manifest path;
- report output format;
- OpenAPI validation approach;
- runtime route inventory approach;
- blocking/advisory mode;
- CI/build integration boundary;
- first implementation slice;
- deferred checks, которые не должны притворяться выполненными.

Реализация до этих решений могла бы создать не тот tool boundary, подключить gate слишком рано к build, либо дать ложное ощущение generated-client/OpenAPI readiness.

## 6. Implementation options considered

Рассмотрены четыре варианта.

1. Standalone repository-local tool under `tools/`.

   Преимущества: изолирован от backend runtime, может быть read-only/static, не требует менять Ktor behavior, не заставляет Gradle считать gate активным quality gate слишком рано, хорошо подходит для OpenAPI parsing и отчетов.

   Риски: будущая task должна отдельно добавить tooling runtime, зависимости и правила запуска.

2. Gradle-integrated task.

   Преимущества: естественно ложится рядом с backend checks и может позже стать частью локального/CI workflow.

   Риски: изменение build files слишком рано может выглядеть как уже активированный conformance gate; Gradle task может смешать planning/static contract checks с backend test lifecycle.

3. Backend test-based implementation.

   Преимущества: может использовать existing Kotlin/Ktor test infrastructure и runtime route context.

   Риски: слишком тесно связывает contract policy с backend tests, может начать runtime behavior validation до появления real resource semantics, повышает риск false readiness claims.

4. Documentation-only/manual checklist.

   Преимущества: самый безопасный вариант без tooling changes.

   Риски: не дает automated drift detection и не является настоящим skeleton для будущего conformance gate.

## 7. Recommended future implementation approach

Рекомендуемый подход для будущей first implementation task: standalone repository-local read-only tool under `tools/openapi-conformance/`.

Этот вариант безопаснее для текущего состояния, потому что:

- не меняет backend code или Ktor routes;
- не меняет Gradle/build lifecycle на первом шаге;
- не запускает backend server;
- не генерирует clients;
- может работать как static/reporting guardrail;
- может явно выводить `not_ready`, пока blockers остаются unresolved;
- сохраняет conformance gate skeleton отдельно от future generated-client generation.

Gradle/CI integration следует добавить только после того, как standalone skeleton стабилизирует inputs, report schema и blocking/advisory semantics.

## 8. Recommended runtime/language

Рекомендуемый runtime/language для first skeleton: Node.js + TypeScript inside standalone `tools/openapi-conformance/` package.

Причины:

- зрелая экосистема для OpenAPI/YAML parsing и validation;
- удобно выпускать JSON и Markdown reports;
- tool можно держать изолированным от `services/backend` и Gradle;
- TypeScript подходит для будущей generated-client/OpenAPI tooling области;
- отсутствие связи с backend runtime снижает риск ложного вывода, что runtime behavior уже contract-ready.

Будущая task должна добавить зависимости минимально и локально внутри `tools/openapi-conformance/`, не в корень проекта, если отдельное решение не потребует shared tooling workspace.

## 9. Recommended command name

Рекомендуемое имя команды для будущего skeleton:

```bash
./tools/openapi-conformance/check
```

Команда должна быть read-only и по умолчанию выводить отчет в stdout. Future aliases вроде `npm run openapi:conformance` или `./gradlew openApiConformanceCheck` допустимы только после отдельного решения о build/CI integration.

## 10. Recommended file/directory layout

Рекомендуемый future layout:

```text
tools/openapi-conformance/
├── README.md
├── check
├── package.json
├── tsconfig.json
└── src/
    ├── cli.ts
    ├── openapi.ts
    ├── route-inventory.ts
    ├── subset-manifest.ts
    ├── placeholder-policy.ts
    └── report.ts
```

Stage 7.19 не создает эту директорию и не добавляет эти files. Layout является implementation decision для будущей task.

## 11. Recommended subset manifest path

Рекомендуемый future manifest path:

```text
docs/architecture/stage-7/generated-client-ready-subset.yaml
```

Причины:

- manifest является contract/readiness artifact, а не generated output;
- path отделяет Stage 7 runtime readiness policy от Stage 6 draft;
- документ находится рядом с architecture/API boundary context;
- future conformance tool может ссылаться на него явно.

Stage 7.19 не создает `docs/architecture/stage-7/` и не создает manifest. Manifest должен появиться только в отдельной roadmap-aligned task.

## 12. Recommended report output format

Рекомендуемый primary output: JSON в stdout.

Минимальная conceptual shape:

```json
{
  "status": "not_ready",
  "openApiSource": "docs/architecture/stage-6/openapi-draft.yaml",
  "subsetManifest": "docs/architecture/stage-7/generated-client-ready-subset.yaml",
  "checks": [],
  "blockingFindings": [],
  "advisoryFindings": [],
  "endpoints": [],
  "futureOnlyChecks": []
}
```

Рекомендуемый secondary output: human-readable Markdown summary по optional flag, например `--format markdown`.

Первый skeleton не должен писать report files по умолчанию. Optional `--output` можно добавить позднее, если CI или review workflow потребует artifacts.

## 13. Recommended OpenAPI validation approach

Рекомендуемый approach:

- читать OpenAPI source из `docs/architecture/stage-6/openapi-draft.yaml` по explicit config/default;
- проверять, что файл существует и parseable как YAML;
- валидировать базовую OpenAPI 3.1 structure через selected OpenAPI parser/validator;
- извлекать path/method inventory и `operationId`;
- не переписывать OpenAPI draft;
- не генерировать clients;
- не считать successful parse proof of runtime readiness.

Первый skeleton должен fail только на blocking static issues: missing/unparseable OpenAPI source, invalid required structure или невозможность извлечь inventory.

## 14. Recommended runtime route inventory approach

Рекомендуемый first approach: conservative static source inventory по Ktor route files inside `services/backend/src/main/kotlin/com/travelassistant/backend/api`.

Первый skeleton должен:

- читать только source files;
- извлекать known route declarations для `get`, `post`, `put`, `delete` и nested `route`;
- нормализовать paths под `/api/v1`;
- помечать uncertain extraction как advisory finding, а не silently pass;
- не запускать backend server;
- не выполнять HTTP requests;
- не использовать live runtime state.

Если static extraction окажется слишком хрупким, future task может добавить explicit route inventory input. Это должно быть отдельным decision, чтобы не смешивать route inventory manifest с generated-client-ready subset manifest.

## 15. Recommended blocking/advisory mode

Рекомендуемый режим: explicit modes.

- `classification` mode для первого skeleton: blocking только static/input violations, placeholder inclusion violations и invalid manifest references.
- Future `contract` mode: blocking runtime schema, error taxonomy, generated-client compile и runtime contract checks только после появления prerequisites.

Blocking checks в first skeleton:

- OpenAPI source missing/unparseable.
- Subset manifest missing, если command запущена в mode, который требует manifest.
- Included endpoint отсутствует в OpenAPI inventory.
- Included endpoint отсутствует в runtime route inventory.
- Placeholder endpoint включен в `includedEndpoints`.
- Endpoint остается unclassified в strict mode.
- Report пытается вывести `ready`, когда есть unresolved blockers.

Advisory checks:

- assistant endpoints остаются foundation-only candidates;
- generated-client compile check not configured;
- runtime contract tests not configured;
- error taxonomy для excluded endpoints foundation-only;
- route extraction has uncertainty;
- OpenAPI success schemas существуют для endpoints, которые runtime пока excluded.

## 16. Recommended CI/build integration boundary

Первый skeleton должен быть local/manual command, не обязательным CI gate.

Рекомендуемый staged integration:

1. Local read-only command under `tools/openapi-conformance/`.
2. Documentation of command in future `docs/development/quality-gates.md` only after implementation.
3. Optional non-blocking CI/report mode after report schema стабилизируется.
4. Blocking CI mode только после generated-client-ready subset, endpoint classification и required checks станут устойчивыми.

Stage 7.19 не меняет CI, build files или quality-gates docs.

## 17. First implementation skeleton slice

Рекомендуемый first implementation slice для будущей task:

- создать isolated `tools/openapi-conformance/` tool;
- добавить command `./tools/openapi-conformance/check`;
- parse OpenAPI source;
- extract OpenAPI path/method inventory;
- extract conservative Ktor route inventory statically;
- read future subset manifest, если он создан отдельной задачей, или report `manifest_missing` в non-strict mode;
- enforce placeholder exclusion для classified endpoints;
- emit JSON report with `status = not_ready`;
- separate blocking/advisory/future-only checks;
- exit non-zero only for blocking violations in selected mode.

Этот first slice не должен запускать generation, server, runtime contract tests или backend Gradle tests.

## 18. Deferred future-only checks

Future-only checks:

- generated-client generation;
- generated-client compile check;
- runtime HTTP contract tests;
- response payload schema validation against live runtime;
- final error taxonomy conformance;
- provider-backed hotel search behavior validation;
- hotel offers/result envelope validation;
- shortlist resource behavior validation;
- explanation/comparison grounding validation;
- CI blocking mode;
- full OpenAPI finalization gate.

Эти checks не должны выводиться как `passed`, пока реально не реализованы и не запущены.

## 19. Acceptance criteria for future implementation

Будущая first implementation task считается принятой только если:

- command существует и запускается локально;
- tool read-only и не меняет repository files;
- OpenAPI source parse/inventory проверяются;
- runtime route inventory формируется или uncertainty явно попадает в advisory findings;
- placeholder endpoints fail if included;
- output содержит `status`, `blockingFindings`, `advisoryFindings`, `futureOnlyChecks` и endpoint classification summary;
- default status остается `not_ready`, если prerequisites отсутствуют;
- generated clients не создаются;
- backend behavior и OpenAPI draft не меняются;
- docs не заявляют readiness без отдельного passed gate.

## 20. What the first implementation skeleton must not do

Первый skeleton не должен:

- создавать или изменять generated-client-ready subset manifest без отдельной task;
- генерировать clients;
- запускать OpenAPI generator;
- запускать backend server;
- выполнять HTTP requests к runtime;
- валидировать real hotel search behavior, которого нет;
- создавать fake success payloads;
- изменять OpenAPI draft;
- изменять backend code, tests или build files;
- подключаться к CI как blocking gate без отдельной task;
- заявлять generated-client/OpenAPI readiness.

## 21. False readiness prevention rules

Правила предотвращения false readiness:

- default `status` должен быть `not_ready`;
- `ready` запрещен, если есть blocking findings, future-only checks или missing prerequisites;
- excluded endpoints не считаются passed readiness coverage;
- placeholder endpoints must fail if included;
- generated-client compile status должен быть `future_only` или `not_run`, пока compile реально не выполнялся;
- runtime contract status должен быть `future_only` или `not_run`, пока runtime tests реально не выполнялись;
- report должен явно перечислять unresolved blockers;
- docs/status wording должны говорить, что skeleton exists только после отдельной implementation task.

## 22. Rollback/safety considerations

Future implementation должна быть безопасной для rollback:

- standalone tool должен жить в одной isolated directory;
- first task не должна менять backend runtime или OpenAPI draft;
- first task не должна подключать blocking CI/build integration;
- generated outputs не должны коммититься;
- dependency scope должен быть локальным для tool directory;
- rollback должен сводиться к удалению tool directory и связанной navigation documentation, если implementation task будет отменена.

## 23. Relationship to Stage 7.16

Stage 7.16 определил цели и candidate checks conformance gate. Stage 7.19 принимает implementation decisions для first skeleton, который должен реализовывать только безопасную раннюю часть этих checks.

Связь:

- Stage 7.16 определил goal: prevent contract/runtime drift and false readiness claims.
- Stage 7.19 рекомендует standalone tool и concrete command для будущего skeleton.
- Stage 7.16 оставил generated-client compile и runtime tests future-only.
- Stage 7.19 подтверждает, что они остаются deferred future-only checks.

## 24. Relationship to Stage 7.17

Stage 7.17 определил generated-client-ready subset policy и placeholder exclusion policy. Stage 7.19 решает, как будущий tool должен читать и enforce эту policy.

Связь:

- recommended subset manifest path: `docs/architecture/stage-7/generated-client-ready-subset.yaml`;
- placeholder endpoints fail if included;
- endpoint inclusion должен быть explicit;
- foundation-only assistant endpoints не становятся generated-client-ready автоматически.

## 25. Relationship to Stage 7.18

Stage 7.18 описал conceptual planning-to-tooling shape и оставил выбор runtime/command/layout отдельной decision. Stage 7.19 закрывает этот decision layer.

Связь:

- Stage 7.18 предложил possible locations и commands;
- Stage 7.19 выбирает `tools/openapi-conformance/` и `./tools/openapi-conformance/check`;
- Stage 7.18 описал expected inputs/outputs;
- Stage 7.19 выбирает JSON primary report и conservative static Ktor route inventory approach;
- Stage 7.18 запретил implementation;
- Stage 7.19 также не реализует skeleton.

## 26. Remaining prerequisites before implementation

Перед фактической реализацией остаются prerequisites:

- отдельная explicit Stage 7.20+ implementation task;
- решение, создавать ли subset manifest до skeleton или поддержать non-strict `manifest_missing` mode;
- выбор конкретных OpenAPI/YAML validation libraries;
- определение exact JSON report schema;
- подтверждение Ktor route static extraction limits;
- решение по dependency management внутри `tools/openapi-conformance/`;
- решение, когда документировать command в `docs/development/quality-gates.md`;
- separate review после implementation skeleton.

## 27. Remaining generated-client blockers

- Generated-client-ready subset manifest does not exist.
- Conformance gate skeleton does not exist.
- Conformance gate does not exist.
- Generated-client generator config does not exist.
- Generated-client compile check does not exist.
- Placeholder endpoints still return `501 NOT_IMPLEMENTED`.
- Assistant endpoints remain foundation-only and require explicit conformance decision.

## 28. Remaining OpenAPI finalization blockers

- Stage 6 OpenAPI draft includes future resource success schemas not implemented by runtime.
- Placeholder runtime does not match hotel search, offers, shortlist or explanation success schemas.
- Error taxonomy is not final for real resource endpoints.
- `hotelSearchRequest` remains absent until real search/value boundary exists.
- No OpenAPI/runtime conformance gate exists.
- No final readiness review has approved OpenAPI finalization.

## 29. Remaining runtime behavior blockers

- No real hotel search orchestration.
- No `HotelSearchRequest` construction from confirmed criteria.
- No provider-backed hotel facts.
- No hotel offers/result envelope behavior.
- No shortlist resource behavior.
- No explanation/comparison behavior.
- No durable persistence or resource lifecycle.
- No LLM orchestration, requirements extraction or natural-language slot filling.

## 30. Proposed next staged path

Рекомендуемый staged path:

1. Future Stage 7.20+ task: implement standalone read-only conformance gate skeleton under `tools/openapi-conformance/`.
2. Future subset manifest task: create `docs/architecture/stage-7/generated-client-ready-subset.yaml` after skeleton/report assumptions are stable.
3. Future skeleton review task: verify no false readiness claims and no out-of-scope behavior.
4. Future endpoint-slice alignment tasks: make selected endpoints contract-aligned one slice at a time.
5. Future generated-client generation task: run generation/compile only after subset gate passes.

Этот path не запускается Stage 7.20+ автоматически.

## 31. Что было изменено

- Создан Stage 7.19 implementation planning / tooling decision report.
- Добавлена узкая запись Stage 7.19 в `docs/reviews/README.md`.
- Активное status wording в `README.md`, `docs/ROADMAP.md` и `docs/roadmap/roadmap.md` синхронизировано с фактом завершения Stage 7.19 planning/decision task.

## 32. Созданные файлы

- `docs/reviews/stage-7-19-conformance-gate-skeleton-implementation-planning.md`

## 33. Изменённые файлы

- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/reviews/README.md`

## 34. Что намеренно не реализовывалось

- Generated-client/OpenAPI conformance gate.
- Conformance gate skeleton.
- Subset config files.
- Scripts, tests, build tasks или CI checks.
- Generated-client generation.
- OpenAPI draft updates.
- Backend code или public API behavior changes.
- DB/storage, Redis/cache или durable persistence.
- Provider integration, LLM orchestration или requirements extraction.
- Real hotel search, ranking, shortlist или explanation behavior.
- Frontend, booking, payment или flights.
- Stage 7.20 или любые более поздние этапы.

## 35. Проверки

- `git status --short` — выполнено до изменений; рабочее дерево было чистым.
- `git status --short` — выполнено после изменений; показал только ожидаемые documentation/status changes:
  - `README.md`
  - `docs/ROADMAP.md`
  - `docs/roadmap/roadmap.md`
  - `docs/reviews/README.md`
  - `docs/reviews/stage-7-19-conformance-gate-skeleton-implementation-planning.md`
- `git diff --check` — passed.

Backend Gradle tests не запускались, потому что Stage 7.19 является documentation/planning задачей и не меняет backend или build files.

## 36. Self-review summary

Self-review по `docs/prompts/codex-review-template.md`:

- scope соответствует Stage 7.19 planning/documentation задаче;
- conformance gate skeleton не реализован;
- scripts, tests, build tasks и subset config не созданы;
- generated clients не создавались;
- OpenAPI draft не изменялся;
- backend behavior не изменялся;
- Stage 7.20+ не активирован;
- readiness claims сформулированы отрицательно: generated-client/OpenAPI readiness, conformance gate implementation и generated-client-ready subset не достигнуты;
- documentation language policy соблюдена с сохранением технических имен.

## 37. Recommended next task

Рекомендуемый следующий шаг: отдельная Stage 7.20+ implementation task, если roadmap явно активирует ее, для создания standalone read-only conformance gate skeleton under `tools/openapi-conformance/` с `./tools/openapi-conformance/check`, JSON report, OpenAPI inventory, static Ktor route inventory и placeholder exclusion enforcement без generated clients или runtime contract tests.

## 38. Scope control confirmation

Stage 7.19 ограничен planning/decision documentation.

Не были начаты:

- Stage 7.20+;
- generated-client generation;
- OpenAPI finalization;
- conformance gate implementation;
- conformance gate skeleton implementation;
- generated-client-ready subset creation;
- backend behavior changes;
- frontend/provider/LLM/search/booking/payment/flights work.
