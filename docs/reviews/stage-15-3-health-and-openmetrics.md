# Stage 15.3 — health и OpenMetrics

## Scope

- сохранить legacy `GET /api/v1/health` без изменения body;
- добавить root operational endpoints `GET /health/live`, `GET /health/ready` и
  `GET /metrics`;
- связать readiness только с локальной runtime-композицией и shutdown;
- экспортировать OpenMetrics 1.0 через Micrometer/Prometheus `1.12.4`;
- сохранить bounded application labels без identifiers и raw paths.

## Out of scope

- внешний collector, retention, dashboard и alert configuration;
- Docker/Kubernetes manifests;
- distributed tracing и durable readiness dependencies;
- upstream polling и изменение business behavior.

## Operational endpoints

| Path | Semantics |
|---|---|
| `/api/v1/health` | Legacy product-adjacent health response без изменений |
| `/health/live` | `200`, если process обрабатывает HTTP |
| `/health/ready` | `200` после успешной composition; `503` после `markNotReady` и перед shutdown |
| `/metrics` | OpenMetrics 1.0, `application/openmetrics-text; version=1.0.0; charset=utf-8` |

Readiness не вызывает Hotels API или OpenRouter. Provider timeout/rate limit/unavailable
остаётся dependency outcome конкретного flow и не делает весь process
unready.

## Metrics

Экспортируются:

- HTTP request count, active requests и duration;
- assistant turns по outcome;
- hotel search/details count и duration;
- LLM/provider dependency count и duration по dependency, operation и outcome;
- unexpected errors;
- readiness и build info;
- aggregate JVM memory/heap/non-heap, GC count/time, live/daemon/peak threads,
  process uptime, CPU count и system load average.

Application metrics используют только labels `operation`, `method`, `status_class`,
`dependency`, `outcome`. Их values берутся из bounded enums и status classes.
Request/session/search IDs, user text, provider data и raw paths в registry не передаются.
Aggregate JVM/process gauges не имеют labels.

`/metrics` всегда исключён из HTTP statistics. Успешные legacy/live/ready
probes также исключены; failed readiness с `503` остаётся в HTTP counter.

## Проверки

- legacy health body, live `200`, ready `200/503`;
- health-check не создаёт и не вызывает real LLM/provider client;
- counters, timers, active gauge и failed-readiness counting;
- OpenMetrics content type и terminal `# EOF`;
- bounded application labels и отсутствие IDs/raw exception text в scrape;
- readiness/build и aggregate JVM/process metrics;
- полный `./gradlew test` — 587 tests, passed;
- `git diff --check`.

## Verdict

`PASSED`.

Stage 15.4 может синхронизировать OpenAPI/proxy/conformance и зафиксировать
deployment-neutral runbook с alert recommendations.
