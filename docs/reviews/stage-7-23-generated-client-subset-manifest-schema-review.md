# Stage 7.23 — Схема generated-client-ready subset manifest

## 1. Назначение Stage 7.23

Stage 7.23 фиксирует точный schema contract для будущего `generated-client-ready-subset.yaml` и правила будущей read-only validation в standalone OpenAPI conformance tool.

Это planning/review artifact. Stage 7.23 не создает реальный manifest, не меняет tool code, не меняет OpenAPI draft, не генерирует clients, не запускает backend server, не выполняет HTTP requests, не добавляет CI/Gradle integration и не меняет readiness semantics.

## 2. Baseline после Stage 7.22

Baseline перед Stage 7.23:

- Stage 7.19 выбрал `tools/openapi-conformance/`, command `./tools/openapi-conformance/check`, Node.js + TypeScript, JSON stdout и future manifest path `docs/architecture/stage-7/generated-client-ready-subset.yaml`.
- Stage 7.20 реализовал standalone read-only conformance skeleton. Tool читает статические inputs, выводит JSON report и сохраняет `status: "not_ready"` и `readinessClaim: false`.
- Stage 7.20a подтвердил, что skeleton остается isolated/read-only и не заявляет generated-client/OpenAPI readiness.
- Stage 7.21 добавил advisory `endpointClassificationSummary` и tool-local tests, не меняя readiness semantics.
- Stage 7.22 зафиксировал planning по назначению и минимальной будущей форме manifest, но оставил точную schema и validation behavior на следующий этап.

На начало Stage 7.23:

- generated-client/OpenAPI readiness не заявлена;
- OpenAPI finalization не заявлена;
- generated clients не создавались;
- `docs/architecture/stage-7/generated-client-ready-subset.yaml` не существует;
- conformance tool остается standalone/read-only и isolated under `tools/openapi-conformance/**`;
- full OpenAPI/runtime conformance gate не реализован.

## 3. Source-of-truth и прочитанные правила

Перед изменениями были проверены mandatory baseline commands:

- `git status --short --untracked-files=all` — clean.
- `git log --oneline -6` — latest commit `6c07947 docs: plan stage 7.22 generated-client subset manifest`.
- `git diff --stat` — clean.

Прочитанные governance, roadmap и style sources:

- `AGENTS.md`
- `docs/prompts/codex-rules.md`
- `docs/prompts/review-template.md`
- `docs/guides/documentation-style-guide.md`
- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/reviews/README.md`
- `docs/prompts/README.md`
- `docs/prompts/codex-task-template.md`
- `docs/prompts/codex-review-template.md`
- `docs/development/README.md`
- `docs/development/coding-standards.md`
- `docs/development/testing-strategy.md`
- `docs/development/documentation-guidelines.md`
- `docs/development/definition-of-done.md`
- `docs/development/quality-gates.md`
- `docs/architecture/architecture-baseline.md`
- `docs/decisions/README.md`

Прочитанный Stage 7 context:

- `docs/reviews/stage-7-16-generated-client-openapi-conformance-gate-planning.md`
- `docs/reviews/stage-7-17-generated-client-ready-subset-placeholder-exclusion-policy.md`
- `docs/reviews/stage-7-18-conformance-gate-skeleton-planning-to-tooling.md`
- `docs/reviews/stage-7-19-conformance-gate-skeleton-implementation-planning.md`
- `docs/reviews/stage-7-20-standalone-read-only-conformance-gate-skeleton-implementation.md`
- `docs/reviews/stage-7-20a-standalone-read-only-conformance-gate-skeleton-implementation-review.md`
- `docs/reviews/stage-7-21-openapi-conformance-report-depth-tests.md`
- `docs/reviews/stage-7-22-generated-client-ready-subset-manifest-planning.md`
- `tools/openapi-conformance/README.md`
- `tools/openapi-conformance/src/types.ts`
- `tools/openapi-conformance/src/report.ts`
- `tools/openapi-conformance/src/cli.ts`
- `tools/openapi-conformance/src/report.test.ts`
- `tools/openapi-conformance/src/subset-manifest.ts`
- `tools/openapi-conformance/src/placeholder-policy.ts`
- `tools/openapi-conformance/src/paths.ts`

Standalone accepted ADR files отсутствуют.

## 4. Явно описанный или inferred scope

Stage 7.23 не был явно описан в active roadmap как отдельный готовый scope. Active roadmap фиксировал только, что Stage 7.23+ не начаты и требуют отдельной явной roadmap-aligned задачи.

Scope Stage 7.23 inferred из явного запроса текущей задачи и Stage 7.22 recommendation: выполнить узкий planning/review шаг для точной manifest schema и conformance-tool validation behavior перед созданием реального `docs/architecture/stage-7/generated-client-ready-subset.yaml`.

Stage 7.23 не расширяет Stage 7.22 до implementation и не начинает Stage 7.24.

## 5. Будущий путь manifest

Будущий manifest path:

```text
docs/architecture/stage-7/generated-client-ready-subset.yaml
```

Stage 7.23 только фиксирует schema contract для этого path. Реальный файл по этому path не создается.

## 6. Предлагаемый schema contract

### 6.1 Обязательные top-level fields

Будущий manifest должен иметь следующие обязательные top-level fields:

| Поле | Тип | Требование |
|---|---|---|
| `manifestVersion` | string | Должно быть `"stage-7-generated-client-ready-subset-v1"` для первой schema. |
| `scopeName` | string | Непустое имя subset scope, например `"travel-assistant-stage-7-foundation-subset"`. |
| `openApiSource` | string | Repository-relative path к OpenAPI source. На текущем baseline ожидается `docs/architecture/stage-6/openapi-draft.yaml`. |
| `validationStatus` | object | Обязательный status/readiness блок, описан ниже. |
| `includedEndpoints` | array | Явный список endpoints, включенных в future subset candidate scope. |
| `excludedEndpoints` | array | Явный список endpoints, оставленных outside generated-client-ready scope. |
| `classificationPolicy` | object | Policy для placeholder, foundation, runtime-only и unclassified endpoints. |
| `readinessCriteria` | object | Gate criteria, которые должны сохранять readiness blocked до фактической проверки. |
| `knownLimitations` | array | Явные limitations текущего validation подхода. |
| `generatedClientTargets` | array | Список future generated-client targets. До отдельного решения должен быть пустым. |

### 6.2 Optional top-level fields

Optional fields допустимы, если future schema validation явно разрешит их:

| Поле | Тип | Назначение |
|---|---|---|
| `notes` | array of string | Дополнительные non-claim notes. Не заменяет `validationStatus` и `readinessCriteria`. |
| `reviewReferences` | array of string | Ссылки на review/planning artifacts, если future task сочтет это полезным. |
| `schemaNotes` | array of string | Короткие пояснения по интерпретации schema. Не должно становиться backlog. |

Unknown top-level fields должны быть advisory на skeleton этапах и blocking в future strict mode, если они могут скрыть readiness ambiguity.

### 6.3 Enum-like values

Для первой schema фиксируются следующие enum-like values:

- `manifestVersion`: только `"stage-7-generated-client-ready-subset-v1"`.
- `validationStatus.status`: `"not_ready"` на текущем и skeleton-stage baseline.
- `validationStatus.readinessClaim`: только `false`, пока нет фактической readiness promotion.
- `validationStatus.schemaValidation`: `"not_run"`, `"advisory_passed"` или `"failed"`.
- `validationStatus.endpointReferenceValidation`: `"not_run"`, `"advisory_passed"` или `"failed"`.
- `validationStatus.generatedClientCompile`: `"not_run"` или `"passed"`; `"passed"` запрещен до реального compile check.
- `validationStatus.runtimeContractValidation`: `"not_run"` или `"passed"`; `"passed"` запрещен до реальных runtime contract checks.
- `classificationPolicy.placeholderEndpoints`: `"exclude_until_contract_aligned"`.
- `classificationPolicy.foundationCandidates`: `"candidate_only_not_ready"`.
- `classificationPolicy.runtimeOnlyRoutes`: `"must_be_classified_before_readiness"`.
- `classificationPolicy.unclassifiedEndpoints`: `"block_readiness"`.
- endpoint `method`: `"GET"`, `"POST"`, `"PUT"`, `"DELETE"` или `"PATCH"`.
- endpoint `readiness`: `"not_ready"` на текущем baseline.
- endpoint `classification`: `"foundation_candidate"` или `"placeholder_excluded"` для явно известных текущих endpoints; future schema может добавить `"generated_client_ready"` только после отдельной readiness promotion task.
- `knownLimitations[].severity`: `"advisory"` или `"blocking_before_readiness"`.

Manifest может использовать uppercase HTTP methods. Future tool validation должна normalize их к internal lowercase `HttpMethod` перед сравнением с текущим `tools/openapi-conformance` inventory.

### 6.4 Endpoint entry contract

Каждый `includedEndpoints` entry должен содержать:

- `method` — uppercase HTTP method из допустимого enum.
- `path` — normalized full API path, включая `/api/v1`.
- `operationId` — string, если OpenAPI operation имеет `operationId`.
- `classification` — на текущем baseline ожидается `"foundation_candidate"`.
- `readiness` — должно быть `"not_ready"`, пока readiness не promoted.
- `inclusionReason` — непустая причина inclusion.
- `requiredChecks` — array of string, не пустой.
- `unresolvedBlockers` — array of string. До readiness promotion должен быть не пустым, если endpoint не прошел все checks.

Каждый `excludedEndpoints` entry должен содержать:

- `method` — uppercase HTTP method из допустимого enum.
- `path` — normalized full API path, включая `/api/v1`.
- `operationId` — string, если OpenAPI operation имеет `operationId`.
- `classification` — на текущем baseline ожидается `"placeholder_excluded"` для known placeholder endpoints.
- `readiness` — `"not_ready"`.
- `exclusionReason` — непустая machine-readable reason.
- `requiredBeforeInclusion` — array of string, не пустой.

Допустимые endpoint entry optional fields:

- `notes` — array of string.
- `source` — string, если future schema захочет указать source artifact.

### 6.5 Минимальные требования к `includedEndpoints`

На skeleton/planning этапах `includedEndpoints` может быть пустым или содержать только explicit candidate endpoints, которые не являются readiness claim.

Для будущей strict validation перед readiness claim:

- список должен быть явным;
- каждый included endpoint должен существовать в OpenAPI inventory;
- каждый included endpoint должен существовать в runtime route inventory или в более строгом future runtime inventory;
- endpoint не должен быть classified as `placeholder_excluded`;
- endpoint не должен возвращать `501 NOT_IMPLEMENTED` для основного flow;
- endpoint должен иметь stable request/response semantics для generated clients;
- success response должен быть проверен against OpenAPI success schema;
- error response shape и taxonomy должны быть достаточно стабильны;
- generated-client compile и runtime contract checks должны быть фактически запущены и пройдены.

На текущем baseline `GET /api/v1/health` остается low-risk candidate, но не становится generated-client-ready автоматически. Assistant endpoints могут быть только explicit foundation candidates после отдельного conformance decision; Stage 7.23 не включает их в реальный subset.

### 6.6 Минимальные требования к `excludedEndpoints`

`excludedEndpoints` должен явно перечислять endpoints, которые не входят в generated-client-ready scope.

Минимально excluded должны оставаться known placeholder endpoints:

- `POST /api/v1/hotel-searches`
- `GET /api/v1/hotel-searches/{searchId}/offers`
- `GET /api/v1/assistant/sessions/{sessionId}/shortlist`
- `PUT /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`
- `DELETE /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`
- `POST /api/v1/assistant/sessions/{sessionId}/explanations`

Endpoint должен оставаться excluded, если:

- runtime behavior placeholder-only;
- основной flow возвращает `501 NOT_IMPLEMENTED`;
- OpenAPI success schema существует, но runtime не может вернуть matching response;
- endpoint зависит от future hotel search/resource semantics;
- endpoint зависит от provider facts, ranking, shortlist behavior, explanation behavior, booking, payment, flights, LLM orchestration или durable persistence;
- endpoint может пройти только через fake payloads или temporary compatibility exceptions.

### 6.7 Требования к `readinessCriteria`

`readinessCriteria` должен быть object с boolean fields. Для skeleton-stage manifest значения должны удерживать readiness blocked.

Required fields:

- `openApiSourceValidated`
- `manifestSchemaValidated`
- `allIncludedEndpointsInOpenApi`
- `allIncludedEndpointsInRuntimeInventory`
- `noPlaceholderEndpointsIncluded`
- `allRuntimeOnlyRoutesClassified`
- `allUnclassifiedEndpointsResolved`
- `includedEndpointSuccessSchemasValidated`
- `includedEndpointErrorTaxonomyValidated`
- `generatedClientTargetDeclared`
- `generatedClientGenerationConfigured`
- `generatedClientCompilePassed`
- `runtimeContractChecksPassed`

До readiness promotion все поля, которые реально не проверялись, должны быть `false`. Нельзя ставить `true` на основании документационного намерения, route presence или OpenAPI parse alone.

### 6.8 Требования к `validationStatus`

`validationStatus` должен содержать:

- `readinessClaim` — boolean; на текущем baseline только `false`.
- `status` — string; на текущем baseline только `"not_ready"`.
- `schemaValidation` — `"not_run"`, `"advisory_passed"` или `"failed"`.
- `endpointReferenceValidation` — `"not_run"`, `"advisory_passed"` или `"failed"`.
- `generatedClientCompile` — `"not_run"` или `"passed"`.
- `runtimeContractValidation` — `"not_run"` или `"passed"`.
- `lastValidatedBy` — string или `null`; на skeleton этапах допустимо `null`.
- `lastValidatedAt` — string ISO datetime или `null`; на skeleton этапах допустимо `null`.

`status: "ready"` и `readinessClaim: true` запрещены до отдельной readiness promotion task, которая фактически запускает все required gates.

### 6.9 Требования к `knownLimitations`

`knownLimitations` должен быть array of objects. Каждый entry должен содержать:

- `code` — machine-readable string.
- `severity` — `"advisory"` или `"blocking_before_readiness"`.
- `description` — человекочитаемое описание на русском языке, технические identifiers не переводятся.
- `blocksReadiness` — boolean.

Минимальные limitations для текущего baseline:

- static runtime route inventory является advisory и не доказывает runtime contract conformance;
- generated-client compile не настроен и не запущен;
- runtime HTTP contract tests не настроены и не запущены;
- placeholder endpoints остаются excluded;
- generated-client targets не выбраны.

## 7. Иллюстративный YAML example

Следующий YAML является только примером будущей формы. Это не real manifest, не readiness certificate и не файл, который Stage 7.23 создает.

```yaml
manifestVersion: "stage-7-generated-client-ready-subset-v1"
scopeName: "travel-assistant-stage-7-foundation-subset"
openApiSource: "docs/architecture/stage-6/openapi-draft.yaml"
validationStatus:
  readinessClaim: false
  status: "not_ready"
  schemaValidation: "not_run"
  endpointReferenceValidation: "not_run"
  generatedClientCompile: "not_run"
  runtimeContractValidation: "not_run"
  lastValidatedBy: null
  lastValidatedAt: null
includedEndpoints:
  - method: "GET"
    path: "/api/v1/health"
    operationId: "getHealth"
    classification: "foundation_candidate"
    readiness: "not_ready"
    inclusionReason: "candidate for future low-risk foundation subset validation"
    requiredChecks:
      - "openapi_source_validated"
      - "runtime_route_present"
      - "response_schema_validated"
      - "generated_client_compile_passed"
      - "runtime_contract_checks_passed"
    unresolvedBlockers:
      - "generated_client_compile_not_run"
      - "runtime_contract_checks_not_run"
excludedEndpoints:
  - method: "POST"
    path: "/api/v1/hotel-searches"
    operationId: "createHotelSearch"
    classification: "placeholder_excluded"
    readiness: "not_ready"
    exclusionReason: "placeholder_501_not_implemented"
    requiredBeforeInclusion:
      - "real_runtime_behavior"
      - "openapi_success_schema_alignment"
      - "stable_error_taxonomy"
classificationPolicy:
  placeholderEndpoints: "exclude_until_contract_aligned"
  foundationCandidates: "candidate_only_not_ready"
  runtimeOnlyRoutes: "must_be_classified_before_readiness"
  unclassifiedEndpoints: "block_readiness"
readinessCriteria:
  openApiSourceValidated: false
  manifestSchemaValidated: false
  allIncludedEndpointsInOpenApi: false
  allIncludedEndpointsInRuntimeInventory: false
  noPlaceholderEndpointsIncluded: true
  allRuntimeOnlyRoutesClassified: false
  allUnclassifiedEndpointsResolved: false
  includedEndpointSuccessSchemasValidated: false
  includedEndpointErrorTaxonomyValidated: false
  generatedClientTargetDeclared: false
  generatedClientGenerationConfigured: false
  generatedClientCompilePassed: false
  runtimeContractChecksPassed: false
knownLimitations:
  - code: "STATIC_ROUTE_INVENTORY_ADVISORY"
    severity: "blocking_before_readiness"
    description: "Static route inventory остается advisory до strict conformance mode."
    blocksReadiness: true
  - code: "GENERATED_CLIENT_COMPILE_NOT_RUN"
    severity: "blocking_before_readiness"
    description: "generated-client compile check не настроен и не запускался."
    blocksReadiness: true
generatedClientTargets: []
notes:
  - "This example does not claim generated-client readiness."
```

## 8. Будущее поведение conformance-tool validation

Будущая validation должна оставаться tool-local, standalone и read-only.

### 8.1 Обнаружение manifest

Tool должен определять manifest по default path `docs/architecture/stage-7/generated-client-ready-subset.yaml` или explicit `--subset-manifest`.

Поведение skeleton:

- absence остается `missing_not_created`;
- absence не является execution error;
- report сохраняет `status: "not_ready"` и `readinessClaim: false`.

Будущее strict-поведение:

- absence становится blocking, если task явно активировала manifest-required mode;
- отсутствие manifest не должно превращаться в implicit ready state.

### 8.2 YAML parse

Если manifest exists, tool должен read-only parse YAML.

На skeleton stage:

- parse errors могут быть reported as blocking execution/input findings, но общий readiness остается `not_ready`;
- successful parse не считается readiness.

В future strict mode:

- parse error должен block validation;
- parsed YAML не должен изменяться или форматироваться tool.

### 8.3 Schema validation

Tool должен проверять required top-level fields, endpoint entry fields, enum-like values, типы и unknown fields policy.

На skeleton stage:

- schema validation может быть advisory, если manifest только появился как draft;
- invalid readiness-promoting values должны быть blocking даже на early stage.

В future readiness mode:

- schema validation должна быть blocking;
- missing required fields, invalid enum values, wrong types и ambiguous unknown fields должны block readiness.

### 8.4 Endpoint reference validation

Tool должен сверять endpoint entries с OpenAPI inventory и runtime route inventory:

- `method` + `path` должны match normalized full path;
- `operationId` должен match OpenAPI operation, если указан;
- included endpoint должен быть present in OpenAPI;
- included endpoint должен быть present in runtime inventory или future stricter runtime source;
- excluded endpoint должен быть known to OpenAPI, runtime, policy, или явно объяснен.

На skeleton stage drift может быть advisory, кроме included placeholder или invalid readiness claim. Перед readiness claim drift должен быть blocking.

### 8.5 Included/excluded consistency checks

Tool должен проверять:

- один и тот же endpoint не может одновременно быть in `includedEndpoints` и `excludedEndpoints`;
- included endpoint не может иметь `classification: "placeholder_excluded"`;
- excluded endpoint должен иметь непустой `exclusionReason`;
- every included endpoint должен иметь non-empty `requiredChecks`;
- every included endpoint с unresolved checks должен сохранять `readiness: "not_ready"`;
- every excluded placeholder endpoint должен сохранять `readiness: "not_ready"`.

### 8.6 Classification policy checks

Tool должен сверять `classificationPolicy` с known skeleton classifications:

- placeholder endpoints должны оставаться excluded;
- foundation candidates не становятся ready автоматически;
- runtime-only routes требуют explicit classification before readiness;
- unclassified endpoints block readiness.

Если policy ослабляет эти правила, future validation должна report blocking finding.

### 8.7 Readiness criteria checks

Tool должен агрегировать `readinessCriteria` и actual check results.

Запрещено считать criteria passed, если:

- check не запускался;
- check только future-only;
- check основан только на static route inventory;
- generated-client compile не выполнялся;
- runtime contract tests не выполнялись;
- placeholder endpoint included.

### 8.8 Validation status checks

Tool должен проверять, что `validationStatus` не делает false readiness claim:

- `readinessClaim: false` обязателен, пока есть любой unresolved blocker;
- `status: "not_ready"` обязателен, пока generated-client compile и runtime contract checks остаются `not_run`;
- `status: "ready"` должен быть rejected до отдельной readiness promotion task;
- `lastValidatedBy` и `lastValidatedAt` не заменяют actual checks.

### 8.9 Reporting behavior

Report должен оставаться JSON stdout по умолчанию и сохранять explicit negative readiness semantics.

Будущие report additions могут включать:

- `manifestSchemaValidation`;
- `manifestEndpointReferenceValidation`;
- `manifestConsistencyFindings`;
- `readinessCriteriaSummary`;
- `manifestReadinessBlockers`.

На skeleton этапах findings должны различать `advisoryFindings` и `blockingFindings`, но top-level `status` остается `"not_ready"`.

## 9. Advisory vs blocking classification

### 9.1 Advisory на skeleton этапах

Advisory на текущих skeleton/planning этапах:

- manifest отсутствует, пока task не требует его presence;
- static route inventory limitations;
- `endpointClassificationSummary` counts;
- visible runtime-only или unclassified drift, если endpoint не включен в readiness subset;
- generated-client compile не настроен;
- runtime HTTP contract tests не настроены;
- OpenAPI success schemas существуют для excluded future resource endpoints.

### 9.2 Blocking уже на early validation

Blocking даже до readiness claim:

- manifest exists, но YAML unparseable, если tool получил его как explicit input для validation;
- manifest содержит `readinessClaim: true`;
- manifest содержит `status: "ready"`;
- included endpoint classified as `placeholder_excluded`;
- один endpoint одновременно included и excluded;
- `openApiSource` указывает на отсутствующий или другой source без explicit override;
- required top-level fields отсутствуют в strict/schema-validation task.

### 9.3 Blocking перед readiness claim

Перед readiness claim должны стать blocking:

- manifest отсутствует;
- manifest schema invalid;
- included endpoint отсутствует в OpenAPI inventory;
- included endpoint отсутствует в runtime route inventory или stricter runtime inventory;
- runtime-only routes остаются unclassified;
- OpenAPI-only included endpoints существуют;
- placeholder endpoints included;
- included success schema не проверена against runtime;
- error taxonomy не проверена;
- generated-client target не declared;
- generated-client generation/compile не запускались или failed;
- runtime HTTP contract checks не запускались или failed;
- unresolved `knownLimitations` имеют `blocksReadiness: true`.

## 10. Readiness guardrails

`readinessClaim` обязан оставаться `false`, если выполняется хотя бы одно условие:

- manifest отсутствует;
- schema validation не запускалась или failed;
- endpoint reference validation не запускалась или failed;
- included/excluded consistency не проверена;
- included endpoint является placeholder;
- runtime-only или unclassified endpoints остаются unresolved;
- generated-client targets пустые;
- generated-client generation не настроена;
- generated-client compile не запускался;
- runtime HTTP contract checks не запускались;
- included endpoint success/error semantics не проверены against runtime;
- OpenAPI finalization не была отдельно approved.

`status` обязан оставаться `"not_ready"`, если:

- `readinessClaim` равен `false`;
- любой `readinessCriteria` gate остается `false`;
- любой `knownLimitations` entry имеет `blocksReadiness: true`;
- generated-client compile или runtime contract validation имеют `"not_run"`;
- conformance tool работает в current `classification` mode.

Stage 7.23 не меняет readiness semantics, потому что он только документирует будущую schema и validation behavior. Он не создает manifest, не запускает новые checks и не добавляет implementation enforcement.

Readiness promotion в будущем возможна только после отдельной roadmap-aligned task, которая:

- создает или обновляет manifest;
- запускает schema validation как blocking;
- подтверждает endpoint references и consistency;
- выполняет generated-client generation/compile;
- выполняет runtime contract checks;
- фиксирует отдельный readiness review;
- явно меняет readiness semantics.

Stage 7.23 такую promotion не выполняет и не разрешает.

## 11. Non-claims

Stage 7.23 не заявляет:

- generated-client readiness;
- OpenAPI finalization claim;
- generated clients;
- real subset manifest;
- backend/runtime validation;
- Gradle integration;
- CI integration;
- full conformance gate;
- runtime HTTP contract checks;
- provider-backed hotel behavior;
- frontend integration.

## 12. Риски и открытые вопросы

Риски:

- Schema contract может быть ошибочно прочитан как разрешение создать manifest без отдельного Stage 7.24+ task.
- Слишком широкий future `includedEndpoints` может создать false readiness pressure.
- Static route inventory может остаться слишком слабым signal для strict readiness.
- `operationId` drift между OpenAPI и manifest потребует аккуратной normalization.
- Future tool может смешать advisory skeleton mode и blocking readiness mode без explicit mode boundary.

Открытые вопросы:

- Должен ли первый real manifest включать только `GET /api/v1/health` или также перечислить assistant endpoints как `foundation_candidate` outside readiness?
- Должны ли `generatedClientTargets` оставаться пустыми до выбора конкретного generator или можно использовать generic target name?
- Должен ли future strict mode быть отдельным command flag или отдельной command?
- Нужно ли future schema validation принимать JSON Schema-like schema file или держать validation logic внутри tool-local TypeScript code?

## 13. Рекомендуемый следующий этап

Stage 7.24 может быть одним из двух вариантов, но Stage 7.23 не выполняет ни один из них:

- tool-local implementation planning for manifest detection/schema validation;
- создание skeleton manifest only after schema contract is accepted.

Рекомендуемый следующий шаг: Stage 7.24 как tool-local implementation planning for manifest detection/schema validation, если цель — сначала уточнить enforcement boundary. Альтернативно Stage 7.24 может создать skeleton manifest only after schema contract is accepted, но без readiness claim, generated clients, OpenAPI finalization, backend changes, HTTP requests или CI/Gradle integration.

## 14. Scope control confirmation

Stage 7.23 ограничен documentation-only planning/review.

Не созданы и не изменены:

- `docs/architecture/stage-7/generated-client-ready-subset.yaml`;
- `tools/openapi-conformance/**`;
- backend files;
- frontend files;
- OpenAPI contract files;
- Gradle/CI files;
- generated clients.
