# Эксплуатация Travel Assistant backend

Этот runbook описывает deployment-neutral контракт Kotlin + Ktor backend для
внутренней инфраструктуры. Он не выбирает orchestrator, collector, dashboard,
alert manager или поставщика мониторинга.

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

Для `OPENROUTER` требуются `OPENROUTER_API_KEY` и `OPENROUTER_MODEL`; target и
timeout задаются `OPENROUTER_BASE_URL` и `OPENROUTER_TIMEOUT_MS`. Для Hotels API
используются `HOTELS_API_PUBLIC_BASE_URL`, `HOTELS_API_PUBLIC_TIMEOUT_MS` и
необязательный `HOTELS_API_USER_LANGUAGE`. REAL-режимы требуют исходящего HTTPS
доступа к настроенным target. Health checks никогда не вызывают эти зависимости.

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

До включения alert необходимо согласовать routing, maintenance windows и
ответственных операторов во внутренней инфраструктуре.

## Быстрая диагностика

1. Проверить liveness и readiness; `503` readiness во время shutdown ожидаем.
2. Найти `request_id` из error response/header в stdout events.
3. Проверить `http.request.completed`, затем соответствующие assistant,
   search/details и `dependency.call.completed` outcomes.
4. Для неожиданной ошибки использовать `exception_class`, `cause_classes` и
   bounded `stack_frames`; raw exception text намеренно отсутствует.
5. При restart считать прежние session/search IDs недействительными и начать
   новую session.

## Вне этого runbook

Docker/Kubernetes manifests, collector, retention policy, dashboard,
vendor-specific alert configuration, distributed tracing, raw conversation
capture, durable storage, multi-instance coordination, auth и CORS не входят в
Stage 15.
