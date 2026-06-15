# Stage 7.24 — Дизайн manifest detection/schema validation для OpenAPI conformance tool

## 1. Назначение Stage 7.24

Stage 7.24 фиксирует future implementation design для tool-local `manifestDetection` и `manifestValidation` в standalone OpenAPI conformance tool.

Это documentation-only planning/design этап. Он описывает будущие module boundaries, функции, типы, validation flow, error/warning categories, JSON report extension и test plan для Stage 7.25, но не меняет tool behavior сейчас.

Stage 7.24 не создает реальный `generated-client-ready-subset.yaml`, не меняет `tools/openapi-conformance/**`, не начинает Stage 7.25 и не заявляет generated-client/OpenAPI readiness.

## 2. Baseline после Stage 7.23

Baseline после Stage 7.23:

- Stage 7.19 выбрал `tools/openapi-conformance/`, command `./tools/openapi-conformance/check`, Node.js + TypeScript, JSON stdout и future manifest path `docs/architecture/stage-7/generated-client-ready-subset.yaml`.
- Stage 7.20 реализовал standalone read-only conformance skeleton. Tool читает статические inputs, выводит JSON report и сохраняет `status: "not_ready"` и `readinessClaim: false`.
- Stage 7.20a подтвердил, что skeleton остается isolated/read-only и не заявляет generated-client/OpenAPI readiness.
- Stage 7.21 добавил advisory `endpointClassificationSummary` и tool-local tests, не меняя readiness semantics.
- Stage 7.22 зафиксировал planning по назначению и минимальной будущей форме manifest.
- Stage 7.23 зафиксировал schema contract и future validation behavior для будущего `generated-client-ready-subset.yaml`.

На начало Stage 7.24:

- generated-client/OpenAPI readiness не заявлена;
- OpenAPI finalization не заявлена;
- generated clients не создавались;
- `docs/architecture/stage-7/generated-client-ready-subset.yaml` не существует;
- conformance tool остается standalone/read-only и isolated under `tools/openapi-conformance/**`;
- tool только проверяет наличие future manifest path и возвращает `missing_not_created` или `present_not_evaluated`;
- full OpenAPI/runtime conformance gate не реализован;
- Stage 7.25 не начат.

## 3. Source-of-truth и прочитанные правила

Перед изменениями были выполнены mandatory baseline commands:

- `git status --short --untracked-files=all` — clean.
- `git log --oneline -7` — latest commit `14ae174 docs: define stage 7.23 subset manifest schema`.
- `git diff --stat` — clean.

Прочитанные governance, roadmap, style и review sources:

- `AGENTS.md`
- `docs/prompts/codex-rules.md`
- `docs/prompts/review-template.md`
- `docs/guides/documentation-style-guide.md`
- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/reviews/README.md`

Прочитанный Stage 7 context:

- `docs/reviews/stage-7-17-generated-client-ready-subset-placeholder-exclusion-policy.md`
- `docs/reviews/stage-7-19-conformance-gate-skeleton-implementation-planning.md`
- `docs/reviews/stage-7-20-standalone-read-only-conformance-gate-skeleton-implementation.md`
- `docs/reviews/stage-7-20a-standalone-read-only-conformance-gate-skeleton-implementation-review.md`
- `docs/reviews/stage-7-21-openapi-conformance-report-depth-tests.md`
- `docs/reviews/stage-7-22-generated-client-ready-subset-manifest-planning.md`
- `docs/reviews/stage-7-23-generated-client-subset-manifest-schema-review.md`

Прочитанный current tool context:

- `tools/openapi-conformance/README.md`
- `tools/openapi-conformance/package.json`
- `tools/openapi-conformance/src/types.ts`
- `tools/openapi-conformance/src/report.ts`
- `tools/openapi-conformance/src/cli.ts`
- `tools/openapi-conformance/src/report.test.ts`
- `tools/openapi-conformance/src/subset-manifest.ts`
- `tools/openapi-conformance/src/placeholder-policy.ts`

Примененные правила:

- reviews являются audit trail, а не active backlog;
- `docs/roadmap/roadmap.md` остается primary roadmap и source of truth по статусам;
- Stage 7.24 должен быть узким documentation-only этапом;
- человекочитаемая проектная документация пишется на русском языке;
- технические identifiers, paths, commands, field names, statuses и example values не переводятся;
- historical review artifacts не переписываются;
- generated-client readiness, OpenAPI finalization, generated clients, real subset manifest, backend/runtime validation и CI/Gradle integration не заявляются.

## 4. Явно documented или inferred scope

Active roadmap не содержит отдельного явного описания Stage 7.24. В roadmap и navigation docs Stage 7.24+ были отмечены как не начатые и требующие отдельной явной roadmap-aligned задачи.

Scope Stage 7.24 inferred из явного запроса текущей задачи и recommendation Stage 7.23: выполнить tool-local implementation planning for manifest detection/schema validation до создания real manifest или изменения conformance tool behavior.

Stage 7.24 не расширяет Stage 7.23 до implementation и не начинает Stage 7.25.

## 5. Что Stage 7.24 проектирует, но не реализует

Stage 7.24 проектирует:

- future `manifestDetection` для обнаружения manifest path и explicit override;
- future `manifestValidation` для YAML parse, schema validation, endpoint references, consistency и readiness guardrails;
- будущие TypeScript modules/functions/types;
- future JSON report extension;
- error/warning categories;
- advisory vs blocking behavior;
- Stage 7.25 implementation candidate;
- validation commands для будущей implementation task.

Stage 7.24 не реализует:

- parsing содержимого `generated-client-ready-subset.yaml`;
- schema validation в tool;
- новые report fields в runtime output;
- tests в `tools/openapi-conformance/**`;
- real manifest;
- generated-client generation или compile checks;
- runtime HTTP contract checks.

## 6. Будущий manifest path

Будущий manifest path:

```text
docs/architecture/stage-7/generated-client-ready-subset.yaml
```

Stage 7.24 только проектирует будущую обработку этого path. Файл не создается.

## 7. Будущий implementation design для conformance tool

### 7.1 Предполагаемые module boundaries

Будущая implementation должна остаться tool-local under `tools/openapi-conformance/src/`.

Рекомендуемые boundaries:

- `subset-manifest.ts` — оставить как entry point для manifest path inspection и расширить до orchestration layer.
- `manifest-detection.ts` — вынести path resolution и detection candidates, если `subset-manifest.ts` станет слишком широким.
- `manifest-schema.ts` — определить schema constants, enum-like values и shape validation helpers.
- `manifest-validation.ts` — агрегировать YAML parse result, schema validation, endpoint reference validation, consistency checks и readiness guardrails.
- `endpoint-key.ts` или локальные helpers — нормализация `method` + `path`, если текущая логика в `placeholder-policy.ts` начнет дублироваться.
- `types.ts` — добавить public report-facing TypeScript types для `manifestDetection`, `manifestValidation`, `schemaValidation` и validation findings.
- `report.ts` — только встраивать results в JSON report, без чтения файлов и без business validation logic.
- `cli.ts` — только передавать `--subset-manifest` и входные inventories в validation flow.

Stage 7.25 не должен переносить логику в backend, Gradle или root tooling.

### 7.2 Будущие функции

Минимальные future functions:

- `detectSubsetManifest(repositoryRoot, explicitManifestPath?)` — возвращает detection state, выбранный `manifestPath`, candidates и existence.
- `loadSubsetManifest(repositoryRoot, manifestPath)` — read-only чтение YAML, если файл exists.
- `parseSubsetManifestYaml(rawYaml)` — YAML parse без изменения файла.
- `validateManifestSchema(parsed)` — проверка required fields, types, enum-like values и unknown fields policy.
- `validateManifestEndpointReferences(manifest, openApiInventory, runtimeRoutes)` — сверка `includedEndpoints` и `excludedEndpoints` с inventories.
- `validateManifestConsistency(manifest, endpointReports)` — duplicate/conflict checks и included/excluded consistency.
- `validateClassificationPolicy(manifest, endpointReports)` — проверка `classificationPolicy` against skeleton classifications.
- `validateReadinessCriteria(manifest, actualCheckState)` — проверка, что `readinessCriteria` не claiming checks that did not run.
- `validateValidationStatus(manifest, actualCheckState)` — защита `readinessClaim`, `status: "not_ready"` и `not_run` semantics.
- `buildManifestValidationReport(validationResult)` — преобразование validation result в JSON report shape.

Функции должны быть pure там, где возможно. File I/O должен быть ограничен detection/load layer.

### 7.3 Будущие типы

Кандидаты TypeScript types:

```ts
export interface ManifestDetectionReport {
  manifestPath: string;
  exists: boolean;
  explicitPathProvided: boolean;
  candidates: SourceCandidate[];
  status: "missing_not_created" | "present_not_evaluated" | "present_evaluated";
}

export interface ManifestValidationReport {
  status: "not_run" | "advisory_passed" | "failed";
  mode: "advisory" | "strict";
  schemaValidation: ManifestCheckReport;
  endpointReferenceValidation: ManifestCheckReport;
  consistencyValidation: ManifestCheckReport;
  readinessGuardrailValidation: ManifestCheckReport;
  findings: Finding[];
}

export interface ManifestCheckReport {
  name: string;
  status: "not_run" | "advisory" | "passed" | "failed";
  summary: string;
}
```

Кандидаты parsed manifest types должны отражать поля Stage 7.23:

- `manifestVersion`
- `scopeName`
- `openApiSource`
- `validationStatus`
- `includedEndpoints`
- `excludedEndpoints`
- `classificationPolicy`
- `readinessCriteria`
- `knownLimitations`
- `generatedClientTargets`

Будущая implementation может разделить parsed YAML types и report types, чтобы не считать untrusted YAML валидной schema слишком рано.

### 7.4 Как tool будет обнаруживать manifest

Поток detection:

1. Принять optional `--subset-manifest <path>`.
2. Если explicit path задан, использовать его как primary `manifestPath`.
3. Если explicit path не задан, использовать default `docs/architecture/stage-7/generated-client-ready-subset.yaml`.
4. Возвращать candidates с `exists`.
5. Если manifest отсутствует, сохранять current skeleton semantics: `missing_not_created`, `requiredForSkeleton: false`, `status: "not_ready"`, `readinessClaim: false`.
6. Не создавать directories или files.
7. Не fallback к другим manifest names без отдельного решения.

Отсутствующий manifest на Stage 7.25 должен оставаться advisory для skeleton mode и blocking только для explicit future strict/readiness mode.

### 7.5 Как tool будет парсить YAML

Поток YAML parse:

1. Читать файл как UTF-8.
2. Использовать существующую tool-local dependency `yaml`.
3. Parse errors переводить в structured finding, например `MANIFEST_YAML_PARSE_ERROR`.
4. Не форматировать и не переписывать YAML.
5. Не выполнять schema validation, если parse failed.
6. Не считать successful parse proof of readiness.

На Stage 7.25 parse error для существующего manifest должен быть blocking input finding, но top-level report должен оставаться `status: "not_ready"` и `readinessClaim: false`.

### 7.6 Как tool будет валидировать schema

Schema validation должна проверять:

- required top-level fields from Stage 7.23;
- type checks для string, boolean, array, object и nullable fields;
- enum-like values;
- endpoint entry required fields;
- `knownLimitations` object shape;
- `generatedClientTargets` как array;
- unknown top-level fields policy.

На skeleton stage unknown fields могут быть advisory, если они не создают readiness ambiguity. Значения вроде `readinessClaim: true` или `status: "ready"` должны быть blocking даже в advisory mode.

Рекомендуется начать Stage 7.25 с hand-written TypeScript validation helpers, а не добавлять external JSON Schema dependency. Это сохраняет tool-local scope маленьким и не создает новый schema artifact раньше времени.

### 7.7 Как tool будет валидировать endpoint references

Проверка endpoint references:

- normalize manifest `method` к lowercase `HttpMethod`;
- требовать normalized full `path` включая `/api/v1`;
- сопоставлять `method` + `path` с OpenAPI operations;
- если указан `operationId`, сверять его с OpenAPI operation;
- сопоставлять included endpoints с runtime route inventory;
- для excluded endpoints разрешать presence в OpenAPI, runtime, known placeholder policy или explicit exclusion reason;
- report unknown endpoint reference как `UNKNOWN_ENDPOINT_REFERENCE`;
- не запускать backend server и не делать HTTP requests.

На skeleton stage unknown excluded endpoint может быть advisory, если он явно объяснен. Unknown included endpoint должен быть blocking для manifest validation, но не должен менять top-level readiness на ready.

### 7.8 Как tool будет валидировать included/excluded consistency

Проверки consistency:

- один endpoint не может быть одновременно в `includedEndpoints` и `excludedEndpoints`;
- duplicate endpoint внутри одного списка должен report `DUPLICATE_ENDPOINT_REFERENCE`;
- included endpoint не может иметь `classification: "placeholder_excluded"`;
- excluded endpoint должен иметь non-empty `exclusionReason`;
- included endpoint должен иметь non-empty `requiredChecks`;
- included endpoint с unresolved blockers должен иметь `readiness: "not_ready"`;
- placeholder endpoint не может быть included;
- all known placeholder endpoints должны оставаться excluded или явно classified outside readiness.

`included/excluded conflict` должен быть blocking для manifest validation.

### 7.9 Как tool будет валидировать `classificationPolicy`

Validation должна сверять `classificationPolicy` с Stage 7.23 contract:

- `placeholderEndpoints` должен оставаться `"exclude_until_contract_aligned"`;
- `foundationCandidates` должен оставаться `"candidate_only_not_ready"`;
- `runtimeOnlyRoutes` должен оставаться `"must_be_classified_before_readiness"`;
- `unclassifiedEndpoints` должен оставаться `"block_readiness"`.

Policy, которая ослабляет placeholder exclusion или разрешает unclassified endpoints before readiness, должна давать blocking finding.

### 7.10 Как tool будет валидировать `readinessCriteria`

Validation должна проверять, что `readinessCriteria` не утверждает checks, которые tool не выполнял.

На Stage 7.25 должны оставаться false:

- `includedEndpointSuccessSchemasValidated`
- `includedEndpointErrorTaxonomyValidated`
- `generatedClientTargetDeclared`, если `generatedClientTargets` пустой;
- `generatedClientGenerationConfigured`
- `generatedClientCompilePassed`
- `runtimeContractChecksPassed`

Нельзя ставить `true` на основании documentation intent, OpenAPI parse success или static route inventory alone. Любой такой mismatch должен report `READINESS_CRITERIA_INCOMPLETE` или `VALIDATION_STATUS_MISMATCH`.

### 7.11 Как tool будет валидировать `validationStatus`

Validation должна защищать:

- `readinessClaim` остается `false`, пока есть unresolved blockers или future-only checks;
- `status` остается `"not_ready"`, пока generated-client compile и runtime contract checks имеют `not_run`;
- `schemaValidation` может быть `"not_run"`, `"advisory_passed"` или `"failed"`;
- `generatedClientCompile` не может быть `"passed"` без фактического compile check;
- `runtimeContractValidation` не может быть `"passed"` без фактических runtime contract checks;
- `lastValidatedBy` и `lastValidatedAt` не заменяют actual checks.

`readinessClaim: true` и `status: "ready"` должны быть rejected до отдельной readiness promotion task.

## 8. Будущее JSON report extension

### 8.1 Suggested field name

Рекомендуется добавить nested top-level field:

```json
{
  "manifestDetection": {},
  "manifestValidation": {}
}
```

Это лучше, чем перегружать существующий `subsetManifest`, потому что current `subsetManifest` уже означает simple path inspection. В Stage 7.25 можно сохранить backward-readable `subsetManifest` и добавить новые sections рядом с ним.

### 8.2 Ожидаемые fields

Кандидаты `manifestDetection` fields:

- `manifestPath`
- `exists`
- `explicitPathProvided`
- `candidates`
- `status`

Кандидаты `manifestValidation` fields:

- `status`
- `mode`
- `schemaValidation`
- `endpointReferenceValidation`
- `consistencyValidation`
- `classificationPolicyValidation`
- `readinessCriteriaValidation`
- `validationStatusValidation`
- `findings`

Допустимо также добавить summary checks в существующий `checks`, но detailed manifest result должен жить в `manifestValidation`.

### 8.3 Statuses для отсутствующего manifest

Если manifest отсутствует:

- `manifestDetection.status`: `"missing_not_created"`;
- `manifestValidation.status`: `"not_run"`;
- `schemaValidation.status`: `"not_run"`;
- top-level `status`: `"not_ready"`;
- top-level `readinessClaim`: `false`;
- advisory finding `MANIFEST_MISSING`.

Отсутствие manifest не является execution error в skeleton mode.

### 8.4 Statuses для invalid manifest

Если manifest exists, но invalid:

- YAML parse error: `manifestValidation.status`: `"failed"`, finding `MANIFEST_YAML_PARSE_ERROR`;
- schema violation: `manifestValidation.status`: `"failed"`, finding `MANIFEST_SCHEMA_VIOLATION`;
- endpoint conflict или invalid readiness claim: `manifestValidation.status`: `"failed"`;
- top-level `status`: `"not_ready"`;
- top-level `readinessClaim`: `false`;
- exit code может оставаться `0`, если tool сформировал report, или `2`, если выбранная future task решит считать invalid explicit input execution error.

Для Stage 7.25 предпочтительнее сохранить exit code `0` для structured report в default skeleton mode и использовать findings для failure semantics. Это не превращает skeleton в blocking CI gate раньше времени.

### 8.5 Statuses для valid-but-not-ready manifest

Если manifest parseable и schema-valid, но readiness gates не выполнены:

- `manifestValidation.status`: `"advisory_passed"`;
- `schemaValidation.status`: `"passed"` или `"advisory"`;
- `readinessCriteriaValidation.status`: `"advisory"` или `"failed"`, если manifest claiming false facts;
- top-level `status`: `"not_ready"`;
- top-level `readinessClaim`: `false`;
- generated-client compile и runtime contract checks остаются `future_only` / `not_run`.

Валидный manifest не является readiness certificate.

### 8.6 Почему это не должно менять `readinessClaim` в Stage 7.25

Stage 7.25 сможет только добавить detection/schema validation implementation. Даже passing schema validation не выполняет generated-client generation, compile checks, runtime HTTP contract tests, response schema validation или OpenAPI finalization review.

Поэтому `readinessClaim` должен остаться `false`, а `status: "not_ready"` должен сохраниться. Manifest validation повышает видимость и защищает от false readiness, но не доказывает generated-client readiness.

## 9. Будущие error/warning categories

Рекомендуемые categories:

- `MANIFEST_MISSING` — manifest отсутствует по `manifestPath`.
- `MANIFEST_YAML_PARSE_ERROR` — YAML parse failed.
- `MANIFEST_SCHEMA_VIOLATION` — required field, type или enum-like value invalid.
- `UNKNOWN_ENDPOINT_REFERENCE` — endpoint reference отсутствует в OpenAPI/runtime/policy context.
- `DUPLICATE_ENDPOINT_REFERENCE` — duplicate endpoint в `includedEndpoints` или `excludedEndpoints`.
- `INCLUDED_EXCLUDED_CONFLICT` — endpoint одновременно included и excluded.
- `UNCLASSIFIED_ENDPOINT_DRIFT` — runtime-only или OpenAPI-only endpoint не классифицирован.
- `READINESS_CRITERIA_INCOMPLETE` — `readinessCriteria` missing или claiming unverified checks.
- `VALIDATION_STATUS_MISMATCH` — `validationStatus` расходится с actual check state.
- `PLACEHOLDER_ENDPOINT_INCLUDED` — placeholder endpoint попал в `includedEndpoints`.
- `CLASSIFICATION_POLICY_WEAKENED` — `classificationPolicy` ослабляет Stage 7 guardrails.
- `OPENAPI_SOURCE_MISMATCH` — manifest `openApiSource` не совпадает с выбранным tool source.

Уровни severity:

- advisory на skeleton/tooling этапах для missing manifest, static scan limitations и unclassified drift outside included subset;
- blocking для YAML parse error, schema violation, included/excluded conflict, included placeholder и false readiness values;
- blocking before readiness для incomplete criteria, generated-client checks `not_run`, runtime contract checks `not_run` и unresolved `knownLimitations`.

## 10. Advisory vs blocking behavior

### 10.1 Advisory на skeleton/tooling этапах

Advisory checks на skeleton/tooling этапах:

- manifest отсутствует, пока default skeleton mode не требует manifest;
- static runtime route inventory limitations;
- `endpointClassificationSummary` counts;
- runtime-only или unclassified drift outside readiness subset;
- generated-client compile не настроен;
- runtime HTTP contract tests не настроены;
- OpenAPI success schemas существуют для excluded future resource endpoints;
- valid-but-not-ready manifest present.

### 10.2 Blocking на skeleton/tooling этапах

Blocking checks даже до readiness promotion:

- YAML parse error для existing explicit manifest input;
- `readinessClaim: true`;
- `status: "ready"`;
- required schema fields отсутствуют в explicit validation flow;
- included endpoint classified as `placeholder_excluded`;
- endpoint одновременно included и excluded;
- duplicate included endpoint, если duplicate делает readiness ambiguity;
- `openApiSource` mismatch без explicit override.

### 10.3 Blocking только перед readiness promotion

Могут стать blocking только перед readiness promotion:

- manifest отсутствует;
- all runtime-only routes не classified;
- all unclassified endpoints не resolved;
- included endpoint success schemas не validated;
- included endpoint error taxonomy не validated;
- `generatedClientTargets` пустой;
- generated-client generation/compile не configured или не passed;
- runtime contract checks не run или не passed;
- unresolved `knownLimitations` with `blocksReadiness: true`.

Stage 7.25 не должен превращать эти readiness-promotion checks в CI/Gradle gate.

## 11. Stage 7.25 implementation candidate

### 11.1 Минимальный кодовый scope

Кандидат Stage 7.25:

- расширить tool-local manifest inspection до read-only YAML parse и schema validation;
- добавить `manifestDetection` и `manifestValidation` в JSON report;
- сохранить `status: "not_ready"` и `readinessClaim: false`;
- добавить tests только внутри `tools/openapi-conformance`;
- не создавать real manifest;
- не менять OpenAPI, backend, frontend, Gradle, CI или generated clients.

### 11.2 Ожидаемые файлы

Ожидаемые modified files:

- `tools/openapi-conformance/src/subset-manifest.ts`
- `tools/openapi-conformance/src/types.ts`
- `tools/openapi-conformance/src/report.ts`
- `tools/openapi-conformance/src/cli.ts`, только если нужно передать expanded validation result.
- `tools/openapi-conformance/src/report.test.ts` или новый `tools/openapi-conformance/src/subset-manifest.test.ts`
- `tools/openapi-conformance/README.md`
- `docs/reviews/stage-7-25-...md`
- narrow status/navigation sync docs, если task завершает Stage 7.25.

Ожидаемые new files, если потребуются:

- `tools/openapi-conformance/src/manifest-schema.ts`
- `tools/openapi-conformance/src/manifest-validation.ts`

Не ожидаются:

- `docs/architecture/stage-7/generated-client-ready-subset.yaml`
- backend files;
- OpenAPI contract files;
- Gradle/CI files;
- generated clients.

### 11.3 Ожидаемые tests

Tests Stage 7.25 должны покрывать:

- manifest missing -> `manifestDetection.status` `"missing_not_created"`, `manifestValidation.status` `"not_run"`;
- valid minimal parsed manifest -> schema advisory/pass result, top-level `status: "not_ready"`;
- YAML parse error -> structured finding `MANIFEST_YAML_PARSE_ERROR`;
- missing required field -> `MANIFEST_SCHEMA_VIOLATION`;
- `readinessClaim: true` -> blocking finding, top-level `readinessClaim: false`;
- `status: "ready"` -> blocking finding, top-level `status: "not_ready"`;
- unknown included endpoint -> `UNKNOWN_ENDPOINT_REFERENCE`;
- duplicate endpoint -> `DUPLICATE_ENDPOINT_REFERENCE`;
- included/excluded conflict -> `INCLUDED_EXCLUDED_CONFLICT`;
- placeholder included -> `PLACEHOLDER_ENDPOINT_INCLUDED`;
- incomplete `readinessCriteria` -> `READINESS_CRITERIA_INCOMPLETE`;
- validation status mismatch -> `VALIDATION_STATUS_MISMATCH`.

Tests must use fixtures or inline objects, not create the real Stage 7 manifest.

### 11.4 Validation commands

Ожидаемая validation Stage 7.25:

```bash
cd tools/openapi-conformance
npm run build
npm test
./check
git diff --check
git status --short --untracked-files=all
git diff --stat
git diff --name-only
```

Backend Gradle tests нужно не запускать, если backend files не менялись.

## 12. Readiness guardrails

`readinessClaim` обязан оставаться `false`, если выполняется хотя бы одно условие:

- manifest отсутствует;
- `schemaValidation` равно `"not_run"` или `"failed"`;
- endpoint reference validation не запускалась или failed;
- included/excluded consistency не проверена;
- included endpoint является placeholder;
- runtime-only или unclassified endpoints unresolved;
- `generatedClientTargets` пустой;
- generated-client generation не configured;
- generated-client compile не запускался;
- runtime HTTP contract checks не запускались;
- included endpoint success/error semantics не проверены against runtime;
- OpenAPI finalization не была отдельно approved.

`status` обязан оставаться `"not_ready"`, если:

- `readinessClaim` равен `false`;
- любой `readinessCriteria` gate остается `false`;
- любой `knownLimitations` entry имеет `blocksReadiness: true`;
- generated-client compile или runtime contract validation имеют `"not_run"`;
- conformance tool работает в current `classification` mode;
- Stage 7.25 выполняет только manifest detection/schema validation.

Stage 7.24 не меняет readiness semantics, потому что он не создает manifest, не запускает новые checks, не меняет report output и не добавляет implementation enforcement.

## 13. Non-claims

Stage 7.24 не заявляет:

- generated-client readiness claim;
- OpenAPI finalization claim;
- generated clients;
- real subset manifest;
- backend/runtime validation;
- Gradle/CI integration;
- conformance tool behavior change;
- frontend code;
- backend behavior change;
- OpenAPI contract changes;
- provider integration;
- runtime HTTP contract tests.

## 14. Риски и открытые вопросы

Риски:

- Будущую manifest validation могут ошибочно прочитать как readiness gate, хотя Stage 7.25 должен оставаться skeleton/tooling step.
- Hand-written schema validation может разрастись, если Stage 7.25 попытается покрыть full readiness mode.
- Exit code semantics для invalid manifest нужно выбрать аккуратно, чтобы не подключить blocking CI behavior раньше времени.
- `subsetManifest`, `manifestDetection` и `manifestValidation` могут начать дублировать друг друга, если report shape не будет разделен ясно.
- Static route inventory остается advisory и не должен стать proof of runtime conformance.

Открытые вопросы:

- Должен ли Stage 7.25 сохранять `subsetManifest` как legacy/simple field рядом с `manifestDetection`, или сразу заменить его в report shape?
- Должен ли invalid explicit manifest возвращать exit code `0` со structured failed validation report или exit code `2` как input error?
- Нужен ли отдельный `--manifest-required` или `--strict` flag, или strict mode должен ждать readiness promotion stage?
- Должен ли schema validation оставаться hand-written TypeScript или позже нужен отдельный JSON Schema-like artifact?
- Нужно ли выносить endpoint key normalization из `placeholder-policy.ts`, чтобы избежать hidden duplicate logic?

## 15. Рекомендуемый следующий этап

Рекомендуемый следующий этап: Stage 7.25 as tool-local implementation of manifest detection/schema validation in `tools/openapi-conformance/**`.

Stage 7.25 должен оставаться узким implementation step:

- без real subset manifest;
- без generated-client readiness claim;
- без OpenAPI finalization;
- без generated clients;
- без backend/frontend changes;
- без Gradle/CI integration;
- без Stage 7.26 work.

Recommended commit message:

```text
docs: design stage 7.24 manifest validation flow
```
