# Эксплуатация Travel Assistant backend

Этот runbook описывает deployment-neutral контракт Kotlin + Ktor backend для
внутренней инфраструктуры. Он не выбирает orchestrator, collector, dashboard,
alert manager или поставщика мониторинга. Конкретные продукты ниже приведены
только как варианты интеграции поверх стабильных stdout, HTTP и OpenMetrics
границ.

## Runtime и запуск

Backend собирается как Java 17 application distribution:

```bash
cd services/backend
./gradlew installDist
HOST=0.0.0.0 PORT=8080 \
  build/install/travel-assistant-backend/bin/travel-assistant-backend
```

| Переменная | Default | Назначение |
|---|---:|---|
| `HOST` | `127.0.0.1` | Адрес, на котором Netty принимает соединения |
| `PORT` | `8080` | HTTP port; нечисловое значение заменяется default |
| `LLM_PROVIDER_MODE` | `FAKE` | `FAKE` или явно включённый `OPENROUTER` |
| `HOTEL_PROVIDER_MODE` | `FAKE` | `FAKE` или явно включённый `REAL` |
| `ACCOMMODATION_ANALYSIS_MODE` | `FAKE` | `FAKE` или policy-gated `OPENROUTER` |

Для `OPENROUTER` требуются `OPENROUTER_API_KEY` и `OPENROUTER_MODEL`; target и
timeout задаются `OPENROUTER_BASE_URL` и `OPENROUTER_TIMEOUT_MS`. Для Hotels API
используются `HOTELS_API_PUBLIC_BASE_URL`, `HOTELS_API_PUBLIC_TIMEOUT_MS` и
необязательный `HOTELS_API_USER_LANGUAGE`. REAL-режимы требуют исходящего HTTPS
доступа к настроенным target. Health checks никогда не вызывают эти зависимости.

Semantic `OPENROUTER` mode использует тот же `OPENROUTER_API_KEY`, отдельные
`ACCOMMODATION_ANALYSIS_MODEL`, `ACCOMMODATION_ANALYSIS_BASE_URL`,
`ACCOMMODATION_ANALYSIS_TIMEOUT_MS`, `ACCOMMODATION_ANALYSIS_BATCH_SIZE` и
exact-host `ACCOMMODATION_ANALYSIS_IMAGE_HOSTS`. Startup требует
`ACCOMMODATION_ANALYSIS_EXTERNAL_CONTENT_APPROVED=true`. Этот flag не заменяет
согласование provider content, controlled ZDR/model probe и quality evaluation;
без них production operator обязан оставить `FAKE`.

Секреты передаются через environment внутренней инфраструктуры и не должны
записываться в image, command line, логи или metrics.

## Ограничение deployment topology

Session, confirmation, execution-attempt и hotel-search stores находятся в
памяти процесса. Поддерживается только один backend instance:

- restart теряет текущие sessions, confirmations, search results и opaque IDs;
- два instance без sticky routing и общего storage видят разное состояние;
- rolling deployment, horizontal scaling и HA требуют отдельного этапа durable
  persistence и multi-instance coordination.

Это ограничение не влияет на переносимость Java 17 process, но должно быть
учтено при выборе внутренней runtime-платформы.

## Probes и scrape

| Метод и path | Ожидаемый ответ | Назначение |
|---|---|---|
| `GET /health/live` | `200` | Process принимает HTTP |
| `GET /health/ready` | `200`, перед shutdown `503` | Runtime composition завершена и process принимает запросы |
| `GET /metrics` | `200`, OpenMetrics 1.0 | Scrape endpoint |
| `GET /api/v1/health` | существующий JSON `200` | Legacy совместимость product API |

Readiness локальна: timeout, rate limit или недоступность Hotels API/OpenRouter
не переводят весь service в `unready`. `/metrics` и успешные health probes не
учитываются в обычной HTTP-статистике; failed readiness `503` учитывается.

## Stdout JSON Lines

Operational events выводятся в stdout по одному JSON object на строку. Сбор,
retention, access control и доставка stdout принадлежат внутренней
инфраструктуре.

Обязательные поля schema `1`:

- `schema_version`, UTC `timestamp`, `level`, `service`, `version`;
- фиксированные `event` и `component`.

Разрешённый контекст: `request_id`, opaque `session_id`, opaque
`hotel_search_id`, bounded `operation`, `method`, `status_code`, `dependency`,
`outcome`, `diagnostic`, `duration_ms`, `offer_count`. Unexpected error может
содержать только `exception_class`, не более четырёх `cause_classes` и не более
восьми ограниченных `stack_frames` без exception message.

Terminal semantic search дополнительно может содержать только агрегированные
`analyzed_count`, `deep_analyzed_count`, `match_count` и `probable_count`.
Dependency events используют bounded operations `accommodation_coarse_analysis`
и `accommodation_deep_analysis` с dependency `accommodation_analyzer`.

Запрещено записывать `offerId`, provider IDs, текст сообщений, destination или
hotel names, значения criteria, request/response bodies, headers, URL, model
slug, secrets и raw exception messages. Ошибка stdout writer не должна менять
пользовательский flow.

Входной `X-Request-ID` переиспользуется только при соответствии
`[A-Za-z0-9._-]{1,128}`; иначе backend создаёт UUID. То же значение возвращается
в response header, обязательном `requestId` error body и связанных событиях.

## OpenMetrics contract

`GET /metrics` возвращает
`application/openmetrics-text; version=1.0.0; charset=utf-8` и заканчивается
`# EOF`. Основные серии:

- `travel_assistant_http_requests_total`,
  `travel_assistant_http_active_requests`,
  `travel_assistant_http_request_duration_seconds_*`;
- `travel_assistant_assistant_turns_total`;
- `travel_assistant_hotel_searches_total`,
  `travel_assistant_hotel_search_duration_seconds_*`;
- `travel_assistant_hotel_details_total`,
  `travel_assistant_hotel_details_duration_seconds_*`;
- `travel_assistant_dependency_calls_total`,
  `travel_assistant_dependency_call_duration_seconds_*`;
- `travel_assistant_unexpected_errors_total`, `travel_assistant_readiness`,
  `travel_assistant_build_info`;
- aggregate JVM memory/GC/thread и process metrics с prefix `jvm_`/`process_`.

Application series используют только bounded labels `operation`, `method`,
`status_class`, `dependency`, `outcome`. Request/session/search IDs, raw paths,
user text и provider data не являются labels. JVM/process gauges labels не
имеют.

Semantic analysis не создаёт ID labels: его calls видны в общих dependency
series по bounded `dependency=accommodation_analyzer` и operation coarse/deep;
terminal search виден как `operation=semantic_hotel_search`.

## Как проверить observability локально

Убедитесь, что активна Java 17, соберите application distribution и сохраните
stdout процесса:

```bash
cd services/backend
java -version
./gradlew installDist
HOST=127.0.0.1 PORT=8080 \
  build/install/travel-assistant-backend/bin/travel-assistant-backend \
  | tee /tmp/travel-assistant-backend.log
```

После запуска в stdout должно появиться событие `service.lifecycle` с
`operation=service_startup` и `outcome=started`.

В другом терминале отправьте запрос с известным идентификатором корреляции:

```bash
curl -sS -D - \
  -H 'X-Request-ID: log-check-001' \
  http://127.0.0.1:8080/api/v1/health

rg 'log-check-001' /tmp/travel-assistant-backend.log
```

Один `log-check-001` должен присутствовать в заголовке ответа и событии
`http.request.completed`. Если установлен `jq`, JSON можно отформатировать:

```bash
rg 'log-check-001' /tmp/travel-assistant-backend.log | jq .
```

Для проверки корреляции ошибки запросите неизвестный search:

```bash
curl -sS -D - \
  -H 'X-Request-ID: error-check-001' \
  http://127.0.0.1:8080/api/v1/hotel-searches/missing/offers

rg 'error-check-001' /tmp/travel-assistant-backend.log
```

Значения `X-Request-ID` в заголовке ответа, `requestId` в теле ошибки и
operational event должны совпасть. Некорректное входное значение, например
`invalid request id`, не переиспользуется: backend возвращает сгенерированный
UUID.

Проверьте health endpoints и OpenMetrics:

```bash
curl -sS -D - http://127.0.0.1:8080/health/live
curl -sS -D - http://127.0.0.1:8080/health/ready
curl -sS http://127.0.0.1:8080/metrics \
  | rg 'travel_assistant_|jvm_|process_|# EOF'
```

Минимальный успешный результат: обе проверки возвращают `200`, выдача metrics
содержит `travel_assistant_readiness 1.0`, build/JVM/process series и
завершается строкой `# EOF`.

## Как интегрировать с системой наблюдаемости

Интеграция состоит из трёх независимых потоков:

```text
stdout JSON Lines -> platform log capture -> collector -> log storage -> UI
GET /metrics      -> Prometheus-compatible scraper -> time-series storage -> UI/alerts
health endpoints  -> platform probes or uptime checker -> availability alerts
```

Выбор конкретного продукта не требует изменения backend. Если внутренняя
платформа уже предоставляет log agent, Prometheus-compatible scrape и
dashboard UI, следует использовать её стандартные компоненты.

### Сбор и поиск логов

Collector должен читать stdout процесса, сохранять одну JSON-строку как одно
событие и не удалять поля схемы `1`.

| Runtime | Источник для collector |
|---|---|
| systemd/VM | stdout, обычно доступный через journal |
| container runtime | stdout-поток контейнера |
| Kubernetes | Pod logs через runtime или Kubernetes API |

Для labels/index dimensions хранилища логов подходят только bounded значения:

- `service`, окружение deployment `environment`;
- при необходимости `level`, `component`, `event`.

`request_id`, `session_id` и `hotel_search_id` остаются JSON fields для поиска,
но не становятся labels. Это защищает storage от высокой cardinality. Текст
пользователя, данные provider и запрещённые sensitive fields не должны
добавляться collector-ом.

Если используется Loki, запрос корреляции после JSON parsing выглядит так:

```logql
{service="travel-assistant-backend"} | json | request_id="error-check-001"
```

Для нового open-source контура можно использовать Grafana Alloy как collector,
Loki как хранилище логов и Grafana как UI. Alloy поддерживает файловые,
container и Kubernetes log sources и передачу событий в Loki:

- [Grafana Alloy: collect and forward data](https://grafana.com/docs/alloy/latest/collect/);
- [Grafana Alloy: collect Kubernetes logs](https://grafana.com/docs/grafana-cloud/send-data/alloy/collect/logs-in-kubernetes/).

Если организация уже использует OpenSearch, Elastic, Splunk или другую
платформу, нужно настроить ingestion JSON Lines и применить те же правила
labels/index fields; backend contract остаётся прежним.

### Сбор метрик

Prometheus-compatible scraper должен опрашивать только `/metrics`. Минимальный
статический пример Prometheus:

```yaml
scrape_configs:
  - job_name: travel-assistant
    scrape_interval: 15s
    metrics_path: /metrics
    static_configs:
      - targets:
          - travel-assistant.internal:8080
```

В динамической инфраструктуре `static_configs` заменяется стандартным service
discovery. Доступ к `/metrics` следует ограничить monitoring network plane, не
меняя public product API. Полный формат `scrape_config` описан в
[Prometheus configuration](https://prometheus.io/docs/prometheus/latest/configuration/configuration/).

Начальные PromQL-проверки:

```promql
travel_assistant_readiness

sum(increase(travel_assistant_http_requests_total{status_class="5xx"}[5m]))

sum(increase(travel_assistant_dependency_calls_total{
  outcome=~"timeout|rate_limited|unavailable"
}[10m]))

sum by (operation, outcome) (
  increase(travel_assistant_dependency_calls_total{
    dependency="accommodation_analyzer"
  }[10m])
)

sum by (operation) (
  rate(travel_assistant_http_request_duration_seconds_sum[5m])
)
/
sum by (operation) (
  rate(travel_assistant_http_request_duration_seconds_count[5m])
)
```

Последний запрос показывает среднюю длительность по operation. Текущий backend не
экспортирует histogram buckets, поэтому p50/p95/p99 нельзя достоверно вычислить
без отдельного изменения metrics configuration.

### Probes, dashboards и alerts

Deployment platform должна вызывать `/health/live` для liveness и
`/health/ready` для readiness. Если встроенные probes недоступны, эти пути можно
проверять отдельным uptime/blackbox monitor. Health responses не заменяют сбор
metrics и логов.

Минимальный dashboard должен показывать:

- readiness, process uptime и active HTTP requests;
- количество запросов и среднюю длительность по `operation`;
- количество `4xx`/`5xx` и долю `5xx`;
- assistant/search/details outcomes;
- provider/LLM timeout, rate-limit, unavailable, auth и credit outcomes;
- semantic coarse/deep outcomes и terminal no-semantic-matches/partial/failed;
- unexpected errors, JVM memory, GC и threads;
- последние `service.lifecycle` и `error.unhandled` log events.

Prometheus alert rules могут передаваться в Alertmanager или внутреннюю систему
оповещений. Alertmanager отвечает за grouping, routing и receivers; обзор
находится в [Prometheus alerting documentation](https://prometheus.io/docs/alerting/latest/overview/).

### Checklist подключения

- [ ] stdout backend process попадает в collector без объединения JSON-строк;
- [ ] окружение deployment добавляется как bounded infrastructure label;
- [ ] request/session/search IDs доступны для поиска, но не являются labels;
- [ ] Prometheus target для `/metrics` имеет состояние `UP`;
- [ ] liveness и readiness probes проверяются независимо от provider API;
- [ ] dashboard показывает HTTP, business, dependency и JVM/process signals;
- [ ] пороги alerts ниже реализованы и имеют ответственного/notification route;
- [ ] retention, доступ к логам и удаление данных согласованы внутренней инфраструктурой;
- [ ] тестовый request ID находится от HTTP-ответа до log event;
- [ ] restart проверен с учётом потери process-local sessions/searches.

## Начальные alert-рекомендации

Это vendor-neutral стартовые пороги, а не готовая alert configuration:

- `critical`: три последовательных failed liveness или readiness probe;
- `critical`: любое `service.lifecycle` с `outcome=startup_failed`, включая
  startup/config failure, либо dependency/LLM outcome
  `authentication_failed`/`insufficient_credits`;
- `critical`: доля HTTP `5xx` не меньше 5% и одновременно не меньше пяти `5xx`
  за 5 минут;
- `warning`: не меньше пяти provider/LLM outcomes `timeout`, `rate_limited` или
  `unavailable` суммарно за 10 минут.
- `warning`: не меньше пяти `accommodation_analyzer` outcomes `timeout`,
  `rate_limited` или `unavailable` за 10 минут; `failed` semantic search не
  должен автоматически переключать выдачу на обычные отели.

До включения alert необходимо согласовать routing, maintenance windows и
ответственных операторов во внутренней инфраструктуре.

## Быстрая диагностика

1. Проверить liveness и readiness; `503` readiness во время shutdown ожидаем.
2. Найти `request_id` из error response/header в stdout events.
3. Проверить `http.request.completed`, затем соответствующие assistant,
   search/details и `dependency.call.completed` outcomes.
4. Для semantic search проверить terminal `hotel.search.completed`, затем
   `accommodation_coarse_analysis`, `accommodation_deep_analysis` и bounded
   aggregate counts; отсутствие deep events допустимо при пустом provider
   result или недоступных details.
5. Для неожиданной ошибки использовать `exception_class`, `cause_classes` и
   bounded `stack_frames`; raw exception text намеренно отсутствует.
6. При restart считать прежние session/search IDs недействительными и начать
   новую session.

## Вне этого runbook

Docker/Kubernetes manifests, готовая collector configuration/deployment,
retention policy, готовый dashboard, vendor-specific alert configuration,
distributed tracing, raw conversation capture, durable storage, multi-instance
coordination, auth и CORS не входят в Stage 15–16.
