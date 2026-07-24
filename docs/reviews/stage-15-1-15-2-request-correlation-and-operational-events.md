# Stage 15.1–15.2 — request correlation и operational events

## Scope

- добавить Ktor `CallId` с safe inbound `X-Request-ID`;
- использовать один request ID в response header, error body и operational logs;
- ввести application-owned `OperationalEventSink` без logging dependency в Domain;
- писать JSON Lines в stdout с bounded context и без raw payload;
- покрыть service, HTTP, assistant, confirmation, search, details, LLM,
  provider и unexpected-error outcomes.

## Out of scope

- metrics, probes и scrape endpoint Stage 15.3;
- OpenAPI/proxy/runbook синхронизация Stage 15.4;
- raw conversation capture, distributed tracing и external log collector;
- изменение business behavior, ranking и provider mapping.

## Request correlation

`CallId` принимает inbound `X-Request-ID` только по regex
`[A-Za-z0-9._-]{1,128}`. Пустое, malformed или слишком длинное значение
заменяется UUID. Runtime всегда отвечает `X-Request-ID`; в любом error body
`requestId` обязателен и равен response header.

HTTP events используют fixed operation names. Opaque path segments не становятся
operation и не попадают в log context.

## JSON contract

Каждая stdout строка — самостоятельный JSON object с:

- `schema_version`, UTC `timestamp`, `level`, `service`, `version`, `event`, `component`;
- optional bounded fields `operation`, `method`, `status_code`, `dependency`, `outcome`,
  `diagnostic`, `duration_ms`, `offer_count`;
- optional correlation fields `request_id`, `session_id`, `hotel_search_id`.

Session/search identifiers остаются opaque. `offerId`, provider IDs, message text,
destination/hotel name, criteria values, bodies, headers, URLs, model slug, secrets и raw
exception messages не моделируются в operational event. Unexpected error содержит
только exception class, до четырёх cause classes и до восьми stack frames
без message. Sink и его composite adapter подавляют собственные ошибки.

## События

| Event | Корреляция | Bounded outcome |
|---|---|---|
| `service.lifecycle` | нет | startup, startup failure, stopping, stopped |
| `http.request.completed` | request ID | fixed operation, method, status, duration |
| `assistant.session.created` | request + session ID | created |
| `assistant.turn.completed` | request + session/search ID | clarification, results, unsupported |
| `assistant.confirmation.outcome` | session ID | required, confirmed, declined, clarification, replanning, unknown |
| `hotel.search.completed` | session + optional search ID | results, no offers, rejected, unavailable |
| `hotel.details.completed` | search ID | success, not found, rejected, unavailable |
| `dependency.call.completed` | optional session/search ID | LLM/provider bounded outcome + duration |
| `llm.diagnostic` | нет | существующие fixed diagnostic categories |
| `error.unhandled` | request ID | sanitized throwable metadata |

## Проверки

- safe reuse, UUID generation и replacement malformed/oversized request ID;
- equality response header, error `requestId` и JSON `request_id`;
- JSON schema, UTC timestamp, concurrency isolation и non-throwing writer;
- sensitive fixtures и exception messages отсутствуют в lines;
- fixed HTTP operation и outcomes для `400/404/500/502/503`;
- session/turn/confirmation, results/no-offers/provider failure/details failure и LLM dependency outcomes;
- полный `./gradlew test` — 585 tests, passed;
- `git diff --check`.

## Verdict

`PASSED`.

Stage 15.3 может добавить probes и OpenMetrics adapter к тому же application-owned
event stream без IDs и raw paths в metric labels.
