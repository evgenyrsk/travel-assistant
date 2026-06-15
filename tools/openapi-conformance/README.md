# OpenAPI Conformance Skeleton

`tools/openapi-conformance/` содержит локальный read-only skeleton для будущего generated-client/OpenAPI conformance gate.

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

## Что проверяется

- определяется OpenAPI source, по умолчанию `docs/architecture/stage-6/openapi-draft.yaml`;
- OpenAPI YAML парсится и проверяется на минимальную структуру `openapi` / `paths`;
- извлекается OpenAPI path/method inventory;
- статически сканируются Ktor route files в `services/backend/src/main/kotlin/com/travelassistant/backend/api`;
- проверяется наличие будущего subset manifest path `docs/architecture/stage-7/generated-client-ready-subset.yaml`;
- выводится read-only `manifestDetection` section с ожидаемым `manifestPath`, признаком наличия manifest и detection status;
- если manifest существует, выполняется skeleton-level `manifestValidation`: YAML parse и минимальная проверка Stage 7.23 schema contract;
- если manifest отсутствует, `manifestValidation` остается `not_run`, а report сохраняет `status: "not_ready"` и `readinessClaim: false`;
- placeholder endpoints остаются видимыми как excluded/foundation-only;
- выводится `endpointClassificationSummary` с количеством `foundation_candidate`, `placeholder_excluded`, `runtime_only` и `unclassified` endpoints;
- generated-client compile checks и runtime HTTP contract tests выводятся как `future_only` / `not_run`.

## Поведение manifest

Путь manifest:

```text
docs/architecture/stage-7/generated-client-ready-subset.yaml
```

Инструмент не создает этот файл и не требует его наличия в текущем skeleton mode.

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
- проверяются обязательные top-level fields Stage 7.23 на skeleton depth;
- invalid YAML или schema issues попадают в structured findings;
- endpoint reference validation остается `future_only`;
- passing validation не является generated-client readiness.

Ограничители readiness:

- tool не может вывести `readinessClaim: true`;
- tool не может вывести `status: "ready"`;
- endpoint-level readiness остается `"not_ready"`;
- generated-client generation/compile и runtime HTTP contract tests не запускаются.

## Local tests

```bash
cd tools/openapi-conformance
npm test
```

Тесты проверяют tool-local report semantics и не запускают backend server, HTTP requests, generated-client generation или OpenAPI finalization.

## Exit codes

- `0` — инструмент успешно сформировал JSON report, даже если `status = not_ready`;
- `2` — ошибка запуска инструмента, например отсутствуют зависимости, некорректные arguments или required source не читается/не парсится.
