# Stage 7.22 — Планирование generated-client-ready subset manifest

## 1. Цель Stage 7.22

Stage 7.22 фиксирует назначение, non-claims, минимальную форму и будущие ожидания валидации для generated-client-ready subset manifest.

Это только planning/review artifact. Stage 7.22 не создает manifest, не меняет OpenAPI draft, не генерирует clients, не меняет backend behavior, не добавляет CI/Gradle integration и не меняет readiness semantics conformance tool.

## 2. Текущий baseline после Stage 7.19-7.21

Текущий baseline:

- Stage 7.19 выбрал `tools/openapi-conformance/` как будущую standalone tool location, `./tools/openapi-conformance/check` как command, Node.js + TypeScript как runtime, JSON stdout как primary report format и `docs/architecture/stage-7/generated-client-ready-subset.yaml` как future manifest path.
- Stage 7.20 реализовал standalone read-only conformance skeleton. Он парсит Stage 6 OpenAPI draft, статически сканирует Ktor route declarations, выводит future-only checks и сохраняет top-level `status` как `"not_ready"` с `readinessClaim: false`.
- Stage 7.20a проверил skeleton и подтвердил отсутствие generated-client/OpenAPI readiness claim, generated-client-ready subset, OpenAPI finalization, backend behavior changes и CI/Gradle integration.
- Stage 7.21 добавил advisory `endpointClassificationSummary` reporting и tool-local tests, сохранив `status: "not_ready"`, `readinessClaim: false` и endpoint-level `readiness: "not_ready"`.

На момент Stage 7.22:

- generated-client/OpenAPI readiness не заявлена;
- generated-client-ready subset manifest не существует;
- generated clients не сгенерированы;
- full OpenAPI/runtime conformance gate не реализован;
- OpenAPI finalization не заявлена;
- backend runtime behavior не менялся;
- `endpointClassificationSummary` остается только advisory/reporting-only.

## 3. Зачем нужен subset manifest до readiness claim

Generated-client-ready subset manifest нужен до любого generated-client readiness claim, потому что текущий OpenAPI draft намеренно содержит future resource flows, которые runtime еще не реализует.

Без explicit manifest будущий tooling может случайно:

- принять все OpenAPI paths как ready for generated clients;
- принять наличие route как доказательство runtime contract readiness;
- включить placeholder endpoints, которые все еще возвращают `501 NOT_IMPLEMENTED`;
- скрыть excluded endpoints вместо документирования причин exclusion;
- смешать foundation-only assistant behavior с финальным client-facing contract behavior;
- заявить readiness до появления generated-client generation, compile checks или runtime contract checks.

Manifest должен быть входом для будущей contract/readiness policy в tooling. Сам по себе он не должен быть readiness certificate.

## 4. Предлагаемая будущая форма manifest

Предлагаемый future location:

```text
docs/architecture/stage-7/generated-client-ready-subset.yaml
```

Предлагаемая conceptual shape:

```yaml
manifestVersion: "stage-7-generated-client-ready-subset-v1"
scopeName: "travel-assistant-stage-7-foundation-subset"
openApiSource: "docs/architecture/stage-6/openapi-draft.yaml"
validationStatus:
  readinessClaim: false
  status: "not_ready"
  lastValidatedBy: null
includedEndpoints:
  - method: "GET"
    path: "/api/v1/health"
    operationId: "getHealth"
    inclusionReason: "candidate for future low-risk foundation subset validation"
    readinessCriteria:
      - "openapi_source_identified"
      - "runtime_route_present"
      - "response_schema_validated"
      - "generated_client_compile_validated"
excludedEndpoints:
  - method: "POST"
    path: "/api/v1/hotel-searches"
    operationId: "createHotelSearch"
    exclusionReason: "placeholder_501_not_implemented"
classificationPolicy:
  placeholderEndpoints: "exclude_until_contract_aligned"
  foundationCandidates: "candidate_only_not_ready"
  runtimeOnlyRoutes: "must_be_classified_before_readiness"
  unclassifiedEndpoints: "block_readiness"
readinessCriteria:
  allIncludedEndpointsRuntimeConformant: false
  noPlaceholderEndpointsIncluded: true
  generatedClientTargetDeclared: false
  generatedClientCompilePassed: false
  runtimeContractChecksPassed: false
knownLimitations:
  - "Static route inventory is advisory until stricter conformance mode exists."
generatedClientTargets: []
notes:
  - "This manifest does not claim generated-client readiness."
```

Эта форма иллюстративная. Будущая implementation task должна определить точную YAML schema и validation rules до создания actual file.

## 5. Предлагаемые поля manifest

Минимальные candidate fields:

- `manifestVersion` — идентификатор schema/version для будущей validation.
- `scopeName` — human-readable имя subset scope.
- `openApiSource` — explicit OpenAPI source path, используемый subset.
- `includedEndpoints` — explicit endpoint list, рассматриваемый для generated-client readiness.
- `excludedEndpoints` — explicit endpoint list, оставленный вне generated-client-ready scope.
- `classificationPolicy` — policy для placeholder, foundation, runtime-only и unclassified endpoints.
- `readinessCriteria` — gate criteria, которые должны быть выполнены до readiness claim.
- `knownLimitations` — задокументированные limitations, например static route scanning.
- `generatedClientTargets` — explicit future generated-client target(s), пустой список до отдельного решения.
- `validationStatus` — текущий validation/readiness state, который должен оставаться `not_ready` / `readinessClaim: false` до прохождения всех gates.

Endpoint entries должны включать минимум:

- `method`;
- `path`;
- `operationId`, если он есть в OpenAPI;
- inclusion или exclusion reason;
- required checks или unresolved blockers.

## 6. Обязательные readiness gates

Generated-client readiness должна оставаться false, пока будущие задачи явно не выполнят все required gates:

- OpenAPI source явно указан.
- Endpoint subset явно перечислен.
- Excluded endpoints заданы намеренно и задокументированы.
- Placeholder endpoints не включены.
- Client-generation target указан явно.
- Runtime-only routes классифицированы.
- Unclassified endpoint drift отсутствует.
- Included endpoints есть в OpenAPI inventory.
- Included endpoints есть в runtime route inventory.
- Runtime success responses для included endpoints соответствуют OpenAPI success schemas.
- Error responses и taxonomy для included endpoints достаточно стабильны для generated clients.
- Generated-client compatibility проверена в future stage.
- Generated-client generation и compile checks действительно запущены и прошли.
- Runtime contract checks действительно запущены и прошли для included subset.

Пока эти gates не выполнены, любой manifest должен сохранять readiness status как false/not ready.

## 7. Что Stage 7.22 не заявляет

Stage 7.22 не заявляет:

- generated-client readiness;
- OpenAPI finalization readiness;
- наличие generated-client-ready subset;
- генерацию generated clients;
- backend behavior validation;
- runtime HTTP contract validation;
- CI/Gradle conformance integration;
- full conformance gate implementation.

Будущий manifest также не должен заявлять readiness только потому, что route существует, OpenAPI operation существует или endpoint присутствует в `endpointClassificationSummary`.

## 8. Будущее поведение conformance tool

Будущее tool-local behavior должно оставаться read-only и staged:

1. Определять наличие manifest по пути `docs/architecture/stage-7/generated-client-ready-subset.yaml`.
2. Парсить manifest read-only.
3. Валидировать basic manifest schema в future tool-local task.
4. Сравнивать manifest `openApiSource` с выбранным OpenAPI source.
5. Проверять included/excluded endpoint entries по OpenAPI inventory.
6. Проверять included/excluded endpoint entries по static runtime route inventory.
7. Fail или block future strict mode, если включены placeholder endpoints.
8. Fail или block future strict mode, если runtime-only или OpenAPI-only endpoints остаются unclassified.
9. Сохранять `readinessClaim: false`, пока все readiness gates не выполнены actual checks.
10. Сохранять generated-client compile и runtime HTTP contract checks как `future_only` / `not_run`, пока они действительно не существуют и не запускаются.

Текущий Stage 7 skeleton должен продолжать reporting `not_ready`, пока prerequisites отсутствуют.

## 9. Риски и открытые вопросы

Риски:

- Manifest могут ошибочно принять за readiness certificate, если non-claims не будут explicit.
- Слишком широкий initial included set может случайно подразумевать unsupported generated-client scope.
- Static route scanning может пропустить runtime behavior details и не должен стать единственным readiness signal.
- Assistant foundation endpoints требуют explicit future conformance decision до inclusion.
- Generated-client targets еще не выбраны, поэтому generated-client compatibility остается future-only.

Открытые вопросы:

- Должен ли первый manifest включать только `GET /api/v1/health` как candidate или также перечислять assistant endpoints как candidate-only/foundation-only?
- Должен ли manifest использовать `manifestVersion` или переиспользовать более раннее conceptual поле `version` из Stage 7.17?
- Должны ли generated-client target names сначала быть generic или привязанными к конкретному frontend/client generator после более позднего решения?
- Должен ли future strict mode жить в existing command или за explicit flag?

## 10. Рекомендуемый кандидат для следующего этапа

Рекомендуемый кандидат для следующего этапа: Stage 7.23 review или planning task для точной manifest schema и conformance-tool validation behavior перед созданием `docs/architecture/stage-7/generated-client-ready-subset.yaml`.

Следующий этап по-прежнему не должен генерировать clients, finalize OpenAPI, запускать backend server, выполнять HTTP requests, добавлять CI/Gradle integration или claim generated-client readiness.

## 11. Файлы Stage 7.22

Создан:

- `docs/reviews/stage-7-22-generated-client-ready-subset-manifest-planning.md`

Изменены:

- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/reviews/README.md`

## 12. Подтверждение scope control

Stage 7.22 является только documentation/planning этапом.

Не созданы и не изменены:

- generated-client-ready subset manifest;
- OpenAPI contract files;
- generated clients;
- backend code или behavior;
- frontend code;
- Gradle integration;
- CI integration;
- conformance tool code или package files;
- readiness semantics.
