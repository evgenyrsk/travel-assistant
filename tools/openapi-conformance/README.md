# OpenAPI Conformance Skeleton

`tools/openapi-conformance/` содержит локальный read-only инструмент
классификации OpenAPI/runtime routes и проверки bounded product-client
contract.

Инструмент не генерирует clients, не запускает backend server, не выполняет HTTP requests, не меняет OpenAPI draft и не подключается к CI/Gradle. Текущий статус отчета всегда остается `not_ready`.

## Command

```bash
./tools/openapi-conformance/check
```

Перед первым запуском нужно установить локальные зависимости внутри директории инструмента:

```bash
cd tools/openapi-conformance
npm install
./check
```

Локальные команды из директории `tools/openapi-conformance`:

```bash
npm run check
npm test
npm run build
```

`./tools/openapi-conformance/check` и `npm run check` формируют JSON report в `stdout`.

## Как читать output

| Поле | Интерпретация |
|---|---|
| `status: "not_ready"` | Ожидаемое состояние generated-client/OpenAPI contract, а не ошибка команды. Tool намеренно не подтверждает readiness. |
| `readinessClaim: false` | Generated-client readiness, manifest expansion readiness и final Stage 7 readiness не заявлены. |
| `blockingFindings` | Static/schema/manifest drift, который требует отдельного решения или исправления. Наличие finding не меняет `status` на `ready` и само по себе не делает tool CI gate. |
| `advisoryFindings` | Наблюдения и ограничения текущего read-only/static режима. Они не являются blocking findings. |
| `checks` | Результаты выполненных static/advisory checks; каждый check нужно читать вместе с его `status` и `summary`. |
| `futureOnlyChecks` | Проверки, которые не выполняются в текущем scope, например generated-client compile и runtime HTTP contract tests. |

Успешный exit code `0` означает, что JSON report сформирован. Он не означает generated-client readiness или завершение Stage 7. Для текущего repository state ожидаются `blockingFindings: []`, `status: "not_ready"` и `readinessClaim: false`.

### Product-client checks

- `platform_client_endpoint_inventory` — enforced static check четырёх
  assistant/offers/details candidates в OpenAPI и Ktor inventories.
- `platform_client_contract_shape` — enforced bounded static check актуальных
  assistant/search/offers/details schemas, обязательного error `requestId` и
  `X-Request-ID` response headers.
- `platform_client_runtime_semantics` — advisory-only observation; runtime
  HTTP behavior подтверждается backend tests.

Enforced static checks могут добавить finding в `blockingFindings`. Advisory checks и observations остаются advisory и не должны трактоваться как runtime failure или readiness evidence.

### Что tool не делает

- не запускает backend server;
- не выполняет HTTP/network calls;
- не валидирует live runtime behavior;
- не генерирует и не компилирует clients;
- не расширяет generated-client-ready subset manifest;
- не меняет OpenAPI source;
- не является CI/Gradle gate;
- не заявляет generated-client, manifest expansion, final Stage 7 или runtime HTTP validation readiness.

## Что проверяется

- определяется OpenAPI source, по умолчанию `docs/architecture/stage-6/openapi-draft.yaml`;
- OpenAPI YAML парсится и проверяется на минимальную структуру `openapi` / `paths`;
- извлекается OpenAPI path/method inventory;
- статически сканируются Ktor route files в `services/backend/src/main/kotlin/com/travelassistant/backend/api`;
- проверяется наличие будущего subset manifest path `docs/architecture/stage-7/generated-client-ready-subset.yaml`;
- выводится read-only `manifestDetection` section с ожидаемым `manifestPath`, признаком наличия manifest и detection status;
- если manifest существует, выполняется skeleton-level `manifestValidation`: YAML parse, минимальная проверка Stage 7 schema contract и guardrails против преждевременного readiness promotion;
- если manifest отсутствует, `manifestValidation` остается `not_run`, а report сохраняет `status: "not_ready"` и `readinessClaim: false`;
- placeholder endpoints остаются видимыми как excluded/foundation-only;
- выводится `endpointClassificationSummary` для
  `platform_client_candidate`, `operational`, `diagnostic_excluded`,
  `placeholder_excluded`, `runtime_only` и `unclassified` endpoints;
- root `/health/live`, `/health/ready` и `/metrics` видимы как operational
  runtime routes и намеренно отсутствуют в product OpenAPI/client subset;
- статически проверяются presence/classification четырёх bounded
  product-client candidates и актуальный contract shape, включая correlation;
- runtime semantics остаются advisory и подтверждаются backend HTTP tests;
- generated-client compile checks и runtime HTTP contract tests выводятся как `future_only` / `not_run`.

## Поведение manifest

Путь manifest:

```text
docs/architecture/stage-7/generated-client-ready-subset.yaml
```

Инструмент не создает этот файл и не требует его наличия в текущем skeleton mode.
Если файл существует, он читается как non-readiness manifest candidate, а не как readiness certificate.

Если manifest отсутствует:

- command завершается с exit code `0`;
- `manifestDetection.status` равен `missing`;
- `manifestValidation.status` равен `not_run`;
- report содержит advisory finding `manifest_missing`;
- `status` остается `"not_ready"`;
- `readinessClaim` остается `false`.

Если manifest существует:

- файл читается read-only;
- YAML парсится без перезаписи или форматирования;
- проверяются обязательные top-level fields Stage 7 на skeleton depth;
- `readinessClaim: true`, `status: "ready"`, endpoint-level `readiness: "ready"` и readiness-like true gates в `readinessCriteria` считаются blocking findings в текущем scope;
- invalid YAML или schema issues попадают в structured findings;
- endpoint references проверяются против OpenAPI и runtime inventories;
- passing validation не является generated-client readiness.

Текущий candidate manifest:

- должен сохранять `status: "not_ready"` и `readinessClaim: false`;
- может перечислять endpoints только как `readiness: "not_ready"` candidates;
- candidate endpoint не считается ready endpoint и не является частью approval list;
- не является approval list для generated clients;
- может фиксировать планируемый generated-client target, но не считает его
  проверенным или готовым;
- не запускает generated-client generation, compile checks или runtime HTTP contract tests.

Ограничители readiness:

- tool не может вывести `readinessClaim: true`;
- tool не может вывести `status: "ready"`;
- manifest с `readinessClaim: true` или `status: "ready"` получает blocking validation finding;
- manifest endpoint с `readiness: "ready"` получает blocking validation finding;
- endpoint-level readiness остается `"not_ready"`;
- generated-client generation/compile и runtime HTTP contract tests не запускаются.

## Local tests

```bash
cd tools/openapi-conformance
npm test
```

Тесты проверяют report semantics, bounded contract drift и operational route
classification. Они не запускают backend server, HTTP requests,
generated-client generation или OpenAPI finalization.

## Exit codes

- `0` — инструмент успешно сформировал JSON report, даже если `status = not_ready`;
- `2` — ошибка запуска инструмента, например отсутствуют зависимости, некорректные arguments или required source не читается/не парсится.
