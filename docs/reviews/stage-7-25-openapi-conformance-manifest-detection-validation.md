# Stage 7.25 — OpenAPI conformance manifest detection/validation

## 1. Назначение Stage 7.25

Stage 7.25 добавляет read-only `manifestDetection` и skeleton-level `manifestValidation` в standalone OpenAPI conformance tool в `tools/openapi-conformance/**`.

Этап реализует только tool-local detection/validation reporting. Он не создает реальный `generated-client-ready-subset.yaml`, не меняет readiness semantics, не генерирует clients, не меняет OpenAPI contracts, не меняет backend/frontend code и не начинает Stage 7.26.

## 2. Исходный baseline

Baseline перед Stage 7.25:

- Stage 7.20 реализовал standalone read-only conformance skeleton в `tools/openapi-conformance/**`.
- Stage 7.21 добавил `endpointClassificationSummary` и tool-local tests.
- Stage 7.22 спланировал будущий generated-client-ready subset manifest.
- Stage 7.23 зафиксировал schema contract для будущего `generated-client-ready-subset.yaml`.
- Stage 7.24 зафиксировал design будущего `manifestDetection` / `manifestValidation`.
- `docs/architecture/stage-7/generated-client-ready-subset.yaml` не существовал.
- generated-client/OpenAPI readiness не была заявлена.
- OpenAPI finalization не была заявлена.
- generated clients не создавались.
- conformance tool оставался standalone/read-only и isolated в `tools/openapi-conformance/**`.

Обязательные baseline commands перед изменениями:

- `git status --short --untracked-files=all` — clean.
- `git log --oneline -8` — последний commit `e29a686 docs: design stage 7.24 manifest validation flow`.
- `git diff --stat` — clean.

## 3. Источник scope

Активный roadmap не содержал явного scope для Stage 7.25. Roadmap фиксировал, что Stage 7.25+ не начаты и требуют отдельной явной roadmap-aligned задачи.

Scope Stage 7.25 был inferred из текущего запроса и Stage 7.24 design:

- реализовать read-only `manifestDetection`;
- реализовать skeleton-level `manifestValidation`;
- добавить JSON report sections;
- добавить tool-local tests;
- обновить tool README и narrow status/docs;
- создать этот Stage 7.25 report.

## 4. Созданные файлы

- `docs/reviews/stage-7-25-openapi-conformance-manifest-detection-validation.md`

## 5. Измененные файлы

- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/reviews/README.md`
- `tools/openapi-conformance/README.md`
- `tools/openapi-conformance/src/cli.ts`
- `tools/openapi-conformance/src/report.ts`
- `tools/openapi-conformance/src/report.test.ts`
- `tools/openapi-conformance/src/subset-manifest.ts`
- `tools/openapi-conformance/src/types.ts`

## 6. Сводка реализации

Stage 7.25 расширил существующий conformance report без изменения readiness semantics.

Что добавлено:

- top-level `manifestDetection`;
- top-level `manifestValidation`;
- read-only detection для `docs/architecture/stage-7/generated-client-ready-subset.yaml`;
- skeleton-level YAML parse для present manifest;
- minimal schema validation по Stage 7.23 top-level fields;
- structured findings для missing, YAML parse и schema validation cases;
- tests для missing/present/invalid manifest behavior и `--bad-arg` behavior;
- tool README section про manifest behavior и readiness guardrails.

Что намеренно не добавлено:

- real subset manifest;
- strict mode;
- endpoint reference enforcement;
- generated-client generation;
- generated-client compile gate;
- runtime HTTP contract tests;
- CI/Gradle integration.

## 7. Поведение manifest detection

Default значение `manifestPath`:

```text
docs/architecture/stage-7/generated-client-ready-subset.yaml
```

Если manifest отсутствует:

- command завершается с exit code `0`;
- `manifestDetection.exists` равен `false`;
- `manifestDetection.status` равен `missing`;
- `manifestValidation.status` равен `not_run`;
- report содержит advisory finding `manifest_missing`;
- top-level `status` остается `"not_ready"`;
- top-level `readinessClaim` остается `false`.

Если manifest существует по explicit path или default path:

- файл читается read-only;
- `manifestDetection.status` равен `present`;
- `manifestValidation` выполняет skeleton-level YAML/schema validation;
- passing validation не является readiness claim.

## 8. Поведение manifest validation

Skeleton-level validation для present manifest проверяет:

- top-level root является object;
- required top-level fields существуют:
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
- `includedEndpoints` является array;
- `excludedEndpoints` является array;
- `readinessCriteria` является object;
- `generatedClientTargets` является array;
- `validationStatus` является object, а `validationStatus.status` является string.

Структурированные категории:

- `manifest_missing`;
- `yaml_parse_error`;
- `schema_violation`;
- `endpoint_reference_validation_future_only`;
- `readiness_promotion_blocked`.

Endpoint reference validation остается `future_only` на Stage 7.25.

## 9. Тестовое покрытие

Tool-local tests расширены в `tools/openapi-conformance/src/report.test.ts`.

Покрыто:

- missing manifest detection;
- missing manifest сохраняет `readinessClaim: false`;
- missing manifest сохраняет `status: "not_ready"`;
- valid skeleton manifest shape дает validation result без readiness promotion;
- invalid skeleton manifest дает structured `schema_violation`;
- YAML parse error дает structured `yaml_parse_error`;
- `--bad-arg` сохраняет exit code `2` и structured JSON с `status: "not_ready"` / `readinessClaim: false`.

Tests используют temporary directories и не создают `docs/architecture/stage-7/generated-client-ready-subset.yaml`.

## 10. Подтверждение readiness semantics

Readiness semantics не изменены:

- top-level `status` остается `"not_ready"`;
- top-level `readinessClaim` остается `false`;
- endpoint-level readiness остается `"not_ready"`;
- generated-client generation остается `future_only`;
- generated-client compile остается `not_run`;
- runtime HTTP contract tests остаются `not_run`;
- valid manifest schema не означает generated-client readiness;
- missing manifest не является execution failure в skeleton mode.

Stage 7.25 не заявляет:

- generated-client readiness;
- OpenAPI finalization;
- generated clients;
- backend/runtime validation;
- Gradle/CI integration;
- real subset manifest.

## 11. Validation commands/results

Команды validation:

- `npm install` из `tools/openapi-conformance/` — пройдено; output: `up to date`.
- `npm run build` из `tools/openapi-conformance/` — пройдено.
- `npm test` из `tools/openapi-conformance/` — пройдено; 7 tests passed.
- `./tools/openapi-conformance/check` — пройдено с exit code `0`; JSON содержит `status: "not_ready"`, `readinessClaim: false`, `manifestDetection.status: "missing"` и `manifestValidation.status: "not_run"`.
- `./tools/openapi-conformance/check --bad-arg` — пройдено как negative smoke check с ожидаемым exit code `2`; structured JSON содержит `status: "not_ready"` и `readinessClaim: false`.

Финальная diff/status validation:

- `git diff --check` — пройдено.
- `git status --short --untracked-files=all` — показал только разрешенные Stage 7.25 files и новый untracked Stage 7.25 report.
- `git diff --stat` — показал только tracked Stage 7.25 tool/docs updates.
- `git diff --name-only` — показал только tracked Stage 7.25 tool/docs updates.
- `test ! -e docs/architecture/stage-7/generated-client-ready-subset.yaml` — пройдено; real subset manifest отсутствует.

Backend Gradle tests не запускались, потому что backend files не менялись.

## 12. Оценка scope-control

Stage 7.25 остался внутри разрешенного scope:

- tool changes остались в `tools/openapi-conformance/**`;
- status/navigation updates были узкими;
- Stage 7.25 report добавлен в `docs/reviews/**`;
- backend files не менялись;
- frontend files не менялись;
- OpenAPI contract files не менялись;
- Gradle/CI files не менялись;
- generated clients не создавались;
- `docs/architecture/stage-7/generated-client-ready-subset.yaml` не создан.

## 13. Риски и открытые вопросы

Риски:

- Будущие tasks могут ошибочно прочитать skeleton-level schema validation как readiness evidence.
- Endpoint reference validation остается `future_only`, поэтому manifest endpoint drift пока не enforcement.
- Exit code behavior для invalid present manifest остается report-based, не strict-gate-based.

Открытые вопросы:

- Должен ли future Stage 7.26 добавить endpoint reference validation как advisory или strict mode?
- Должен ли strict manifest-required mode использовать новый flag или отдельную command?
- Должен ли будущий real manifest начинаться только с `GET /api/v1/health` или включать assistant endpoints как `foundation_candidate`?

## 14. Рекомендуемый следующий этап

Рекомендуемый следующий этап: Stage 7.26 как отдельная явная roadmap-aligned task.

Кандидатный scope:

- добавить advisory endpoint reference validation для present manifest;
- сохранить readiness blocked;
- не создавать generated clients;
- не выполнять OpenAPI finalization;
- не добавлять CI/Gradle integration без явного запроса.

Рекомендуемый commit message:

```text
tools: add stage 7.25 manifest detection validation
```
