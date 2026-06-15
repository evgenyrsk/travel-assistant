# Stage 7.20 — Standalone Read-only Conformance Gate Skeleton Implementation

## 1. Цель Stage 7.20

Цель Stage 7.20 — реализовать первый standalone read-only skeleton для будущего generated-client/OpenAPI conformance gate под `tools/openapi-conformance/`.

Skeleton должен только читать статические repository inputs и выводить JSON report со статусом `not_ready`. Он не заявляет generated-client readiness, не финализирует OpenAPI, не генерирует clients, не запускает backend server и не меняет backend behavior.

## 2. Проверенные источники

- `AGENTS.md`
- `docs/prompts/codex-task-template.md`
- `docs/prompts/codex-review-template.md`
- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/product/product-baseline.md`
- `docs/architecture/architecture-baseline.md`
- `docs/architecture/backend-layering-rules.md`
- `docs/development/README.md`
- `docs/development/coding-standards.md`
- `docs/development/testing-strategy.md`
- `docs/development/documentation-guidelines.md`
- `docs/development/definition-of-done.md`
- `docs/development/quality-gates.md`
- `docs/prompts/README.md`
- `docs/reviews/README.md`
- `services/backend/README.md`
- `docs/reviews/stage-7-16-generated-client-openapi-conformance-gate-planning.md`
- `docs/reviews/stage-7-17-generated-client-ready-subset-placeholder-exclusion-policy.md`
- `docs/reviews/stage-7-18-conformance-gate-skeleton-planning-to-tooling.md`
- `docs/reviews/stage-7-19-conformance-gate-skeleton-implementation-planning.md`
- `docs/architecture/stage-6/openapi-contract-notes.md`
- `docs/architecture/stage-6/openapi-draft.yaml`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ApiRoutes.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/HealthRoutes.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/AssistantPlaceholderRoutes.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/HotelSearchPlaceholderRoutes.kt`

В корне репозитория отдельный `openapi-draft.yaml` не найден. Актуальный Stage 6 draft находится в `docs/architecture/stage-6/openapi-draft.yaml`.

## 3. Использование task/review templates

`docs/prompts/codex-task-template.md` был прочитан до repository inspection и использован как структура выполнения: source review, scope boundaries, allowed/forbidden changes, validation expectations и final reporting.

`docs/prompts/codex-review-template.md` был прочитан до repository inspection и использован для self-review перед завершением: проверены scope drift, roadmap/status consistency, architecture boundary violations, documentation consistency, missing validation, source-of-truth duplication и false readiness claims.

## 4. Текущее состояние

Stage 7 завершен through Stage 7.19 до начала Stage 7.20. Stage 7.20 реализует только first read-only reporting skeleton.

Текущее состояние после Stage 7.20:

- standalone conformance gate skeleton существует;
- generated-client/OpenAPI readiness не заявлена;
- generated-client-ready subset не создан;
- full conformance gate не реализован;
- generated clients не созданы;
- OpenAPI finalization не заявлена;
- backend runtime behavior не изменялся;
- placeholder hotel search, offers, shortlist и explanations endpoints остаются placeholder-only.

## 5. Что реализовано

Реализован локальный read-only TypeScript CLI, который:

- определяет OpenAPI source path;
- парсит OpenAPI YAML и проверяет минимальную структуру `openapi` / `paths`;
- извлекает OpenAPI path/method inventory;
- статически сканирует Ktor route declarations в backend API source files;
- проверяет наличие будущего subset manifest path;
- классифицирует foundation candidates и known placeholder exclusions;
- выводит JSON report в stdout;
- всегда оставляет readiness status `not_ready`;
- выводит generated-client generation/compile и runtime HTTP contract tests как `future_only` / `not_run`.

## 6. Tool location

Tool location:

```text
tools/openapi-conformance/
```

## 7. Command

Основная команда:

```bash
./tools/openapi-conformance/check
```

Команда собирает локальный TypeScript CLI и запускает `dist/cli.js`. Перед первым запуском нужны локальные зависимости внутри `tools/openapi-conformance/`:

```bash
cd tools/openapi-conformance
npm install
./check
```

## 8. Runtime/language

Runtime/language:

- Node.js;
- TypeScript;
- локальный isolated npm package внутри `tools/openapi-conformance/`.

Root package files, Gradle files и backend build lifecycle не изменялись.

## 9. Dependencies

Добавлены только tool-local dependencies:

- `yaml` — runtime dependency для YAML/OpenAPI parsing;
- `typescript` — dev dependency для сборки TypeScript CLI;
- `@types/node` — dev dependency для типизации Node.js APIs.

Зависимости добавлены в `tools/openapi-conformance/package.json` и зафиксированы в `tools/openapi-conformance/package-lock.json`. Root dependency files не создавались и не изменялись.

## 10. OpenAPI source detection

Default detection проверяет candidates:

1. `docs/architecture/stage-6/openapi-draft.yaml`
2. `openapi-draft.yaml`

В текущем репозитории выбран:

```text
docs/architecture/stage-6/openapi-draft.yaml
```

Report также выводит список candidates и `exists` для каждого.

## 11. OpenAPI inventory behavior

Skeleton:

- читает selected OpenAPI source;
- парсит YAML;
- проверяет наличие `openapi` и `paths`;
- требует OpenAPI version `3.x`;
- извлекает path/method inventory;
- добавляет server base path из `servers[0].url`, сейчас `/api/v1`;
- выводит `operationId`, raw path и full path.

Успешный parse/inventory не считается proof of runtime readiness.

## 12. Runtime route inventory behavior

Runtime route inventory собирается консервативным static scan по:

```text
services/backend/src/main/kotlin/com/travelassistant/backend/api
```

Scanner ищет Ktor declarations `route`, `get`, `post`, `put`, `delete`, `patch`, нормализует nested route paths и добавляет base path `/api/v1`.

Backend server не запускается, HTTP requests не выполняются, live runtime state не используется.

## 13. Subset manifest handling

Будущий subset manifest path:

```text
docs/architecture/stage-7/generated-client-ready-subset.yaml
```

Stage 7.20 не создает этот manifest. Если manifest отсутствует, report выводит:

- `exists: false`;
- `status: "missing_not_created"`;
- `requiredForSkeleton: false`.

Отсутствие manifest не считается execution error для skeleton и сохраняет общий status `not_ready`.

## 14. Placeholder/exclusion handling

Known placeholder/excluded endpoints остаются видимыми в JSON report как `placeholder_excluded`:

- `POST /api/v1/hotel-searches`
- `GET /api/v1/hotel-searches/{searchId}/offers`
- `GET /api/v1/assistant/sessions/{sessionId}/shortlist`
- `PUT /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`
- `DELETE /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`
- `POST /api/v1/assistant/sessions/{sessionId}/explanations`

Foundation candidates выводятся отдельно и не считаются generated-client-ready:

- `GET /api/v1/health`
- `POST /api/v1/assistant/sessions`
- `POST /api/v1/assistant/sessions/{sessionId}/messages`

## 15. JSON report shape

Report выводится в stdout и содержит:

- `tool`;
- `generatedAt`;
- `status`;
- `readinessClaim`;
- `openApiSource`;
- `subsetManifest`;
- `inventories.openapi`;
- `inventories.runtimeRoutes`;
- `endpoints`;
- `checks`;
- `blockingFindings`;
- `advisoryFindings`;
- `futureOnlyChecks`.

Default `status` всегда `not_ready`, а `readinessClaim` всегда `false`.

## 16. Exit code behavior

Exit code behavior:

- `0` — tool успешно сформировал JSON report, даже если report status `not_ready`;
- `2` — tool execution error, например отсутствуют локальные dependencies, invalid command usage, missing/unreadable/unparseable required OpenAPI source.

Текущий запуск `./tools/openapi-conformance/check` завершился с exit code `0`.

## 17. False readiness prevention

False readiness предотвращается так:

- report status hardcoded as `not_ready`;
- `readinessClaim: false`;
- subset manifest absence выводится как `missing_not_created`;
- generated-client generation/compile не выполняются и выводятся как `future_only` / `not_run`;
- runtime HTTP contract tests не выполняются и выводятся как `not_run`;
- placeholder endpoints классифицируются как `placeholder_excluded`;
- foundation candidates не становятся generated-client-ready автоматически;
- OpenAPI parse success не трактуется как runtime conformance.

## 18. What remains future-only

Future-only остаются:

- generated-client generation;
- generated-client compile check;
- runtime HTTP contract tests;
- response payload schema validation against live runtime;
- final error taxonomy conformance;
- provider-backed hotel search behavior validation;
- hotel offers/result envelope validation;
- shortlist resource behavior validation;
- explanation/comparison grounding validation;
- CI/Gradle blocking integration;
- full OpenAPI finalization gate.

## 19. Relationship to Stage 7.16

Stage 7.16 зафиксировал цели будущего conformance gate и candidate checks. Stage 7.20 реализует только безопасную first skeleton часть: static OpenAPI inventory, static runtime route inventory, placeholder visibility и explicit `not_ready` report.

Generated-client compile checks и runtime contract tests остаются future-only, как было определено в Stage 7.16.

## 20. Relationship to Stage 7.17

Stage 7.17 определил generated-client-ready subset и placeholder exclusion policy. Stage 7.20 использует эту policy как встроенную skeleton classification:

- known placeholder endpoints visible as excluded;
- `GET /api/v1/health` и assistant endpoints visible only as foundation candidates;
- subset manifest не создается и не требуется для skeleton.

## 21. Relationship to Stage 7.18

Stage 7.18 описал planning-to-tooling форму будущего skeleton. Stage 7.20 реализует первый read-only/static tooling layer из этой формы без build/CI integration, без backend server startup и без generated clients.

## 22. Relationship to Stage 7.19

Stage 7.19 выбрал concrete implementation direction:

- `tools/openapi-conformance/`;
- `./tools/openapi-conformance/check`;
- Node.js + TypeScript;
- JSON stdout;
- default `not_ready`;
- conservative static Ktor route inventory;
- future subset manifest path.

Stage 7.20 реализует именно этот skeleton slice.

## 23. Remaining generated-client blockers

- Generated-client-ready subset manifest does not exist.
- Generated-client generator config does not exist.
- Generated-client compile check remains future-only/not_run.
- Placeholder endpoints still return `501 NOT_IMPLEMENTED`.
- Assistant endpoints remain foundation-only and require explicit conformance decision.
- No generated-client readiness review has passed.

## 24. Remaining OpenAPI finalization blockers

- Stage 6 OpenAPI draft still includes future resource success schemas not implemented by runtime.
- Placeholder runtime does not match hotel search, offers, shortlist or explanation success schemas.
- Error taxonomy is not final for real resource endpoints.
- `hotelSearchRequest` remains absent until real search/value boundary exists.
- Full OpenAPI/runtime conformance gate is not implemented.
- No final readiness review has approved OpenAPI finalization.

## 25. Remaining runtime behavior blockers

- No real hotel search orchestration.
- No `HotelSearchRequest` construction from confirmed criteria.
- No provider-backed hotel facts.
- No hotel offers/result envelope behavior.
- No shortlist resource behavior.
- No explanation/comparison behavior.
- No durable persistence or resource lifecycle.
- No LLM orchestration, requirements extraction or natural-language slot filling.

## 26. Что было изменено

- Создан standalone read-only conformance skeleton под `tools/openapi-conformance/`.
- Добавлена command `./tools/openapi-conformance/check`.
- Добавлен TypeScript CLI для OpenAPI/static route inventory и JSON report.
- Добавлен short README для tool.
- Создан Stage 7.20 implementation report.
- Добавлена узкая запись Stage 7.20 в `docs/reviews/README.md`.
- Активное status wording в `README.md`, `docs/ROADMAP.md` и `docs/roadmap/roadmap.md` синхронизировано с фактом завершения Stage 7.20.

## 27. Созданные файлы

- `tools/openapi-conformance/README.md`
- `tools/openapi-conformance/check`
- `tools/openapi-conformance/package.json`
- `tools/openapi-conformance/package-lock.json`
- `tools/openapi-conformance/tsconfig.json`
- `tools/openapi-conformance/src/cli.ts`
- `tools/openapi-conformance/src/openapi.ts`
- `tools/openapi-conformance/src/paths.ts`
- `tools/openapi-conformance/src/placeholder-policy.ts`
- `tools/openapi-conformance/src/report.ts`
- `tools/openapi-conformance/src/route-inventory.ts`
- `tools/openapi-conformance/src/subset-manifest.ts`
- `tools/openapi-conformance/src/types.ts`
- `docs/reviews/stage-7-20-standalone-read-only-conformance-gate-skeleton-implementation.md`

## 28. Изменённые файлы

- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/reviews/README.md`

## 29. Что намеренно не реализовывалось

- Full generated-client/OpenAPI conformance gate.
- Generated-client-ready subset manifest.
- Generated-client source code.
- Generated-client configuration.
- Generated-client generation или compile gate.
- OpenAPI draft updates или finalization.
- Backend code, tests, build files или runtime behavior changes.
- Backend server startup или HTTP contract tests.
- Gradle/CI integration.
- DB/storage, Redis/cache или durable persistence.
- Provider integration, LLM orchestration или requirements extraction.
- Real hotel search, ranking, shortlist или explanation behavior.
- Frontend, booking, payment или flights.
- Stage 7.21 или любые более поздние этапы.

## 30. Проверки

- `git status --short` — выполнено до изменений; рабочее дерево было чистым.
- `npm install` из `tools/openapi-conformance/` — passed, установлены локальные tool dependencies.
- `npm run build` из `tools/openapi-conformance/` — passed.
- `./tools/openapi-conformance/check` — passed, emitted JSON report with `status: "not_ready"`.
- `git diff --check` — passed.
- `git status --short` — выполнено после изменений; показал только ожидаемые изменения:
  - `README.md`
  - `docs/ROADMAP.md`
  - `docs/roadmap/roadmap.md`
  - `docs/reviews/README.md`
  - `docs/reviews/stage-7-20-standalone-read-only-conformance-gate-skeleton-implementation.md`
  - `tools/openapi-conformance/`

Backend Gradle tests не запускались, потому что Stage 7.20 не меняет backend source, backend tests, Gradle files или runtime behavior.

## 31. Self-review summary

Self-review по `docs/prompts/codex-review-template.md`:

- scope соответствует Stage 7.20 skeleton implementation task;
- tool isolated under `tools/openapi-conformance/`;
- root package/build files не изменялись;
- backend behavior и OpenAPI draft не изменялись;
- generated clients и subset manifest не создавались;
- report не заявляет readiness и явно выводит `not_ready`;
- future-only checks не выводятся как passed;
- placeholder endpoints remain excluded/foundation-only;
- Stage 7.21+ не начат.

## 32. Recommended next task

Рекомендуемая следующая задача: отдельный Stage 7.20 review или Stage 7.21 planning task для проверки skeleton report schema, route scanner limitations и false-readiness wording перед созданием generated-client-ready subset manifest.

Эта рекомендация не запускает Stage 7.21 автоматически.

## 33. Scope control confirmation

Stage 7.20 ограничен standalone read-only conformance gate skeleton implementation.

Не были начаты:

- generated-client readiness;
- OpenAPI finalization;
- full conformance gate enforcement;
- generated-client-ready subset creation;
- generated-client generation;
- backend behavior changes;
- Gradle/CI blocking integration;
- frontend/provider/LLM/search/booking/payment/flights work.
