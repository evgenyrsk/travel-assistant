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
- placeholder endpoints остаются видимыми как excluded/foundation-only;
- generated-client compile checks и runtime HTTP contract tests выводятся как `future_only` / `not_run`.

## Exit codes

- `0` — инструмент успешно сформировал JSON report, даже если `status = not_ready`;
- `2` — ошибка запуска инструмента, например отсутствуют зависимости, некорректные arguments или required source не читается/не парсится.
