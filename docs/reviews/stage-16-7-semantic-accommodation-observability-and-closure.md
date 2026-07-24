# Stage 16.7 — Observability, evaluation и closure

## Статус

Завершён в разрешённом FAKE scope. REAL rollout не активирован.

## Scope

- [x] Semantic search создаёт отдельный bounded start event и один terminal
  `hotel.search.completed` с operation `semantic_hotel_search`.
- [x] Terminal event содержит только opaque session/search IDs, duration,
  offer count и агрегированные analysis counts.
- [x] Provider search/details и coarse/deep classifier calls публикуют bounded
  dependency/operation/outcome/duration без input или model output.
- [x] OpenMetrics использует существующие dependency/search series и только
  bounded labels; IDs и provider content не становятся labels.
- [x] Deployment runbook описывает semantic config, события, PromQL/dashboard
  integration, диагностику и стартовые alerts.
- [x] Rights-safe quality evaluation CLI, schema и tests добавлены отдельно от
  backend runtime.
- [x] Aggregate evaluation report явно фиксирует `NOT_RUN`, а не создаёт
  ложный quality claim.
- [x] REAL adapter остаётся opt-in и startup-gated; controlled live smoke не
  выполнялся из-за незакрытых policy/model/quality gates.

## Review findings и fixes

- Semantic creation event отделён от terminal event, чтобы hotel-search
  counter не считал один async search дважды.
- Timeout scheduler публикуется как bounded `timeout`; поздний result и
  shutdown cancellation не создают ложный terminal success.
- Details cache hit не публикуется как dependency call, потому что network call
  фактически отсутствует.
- Evaluation dataset schema запрещает raw provider fields и требует две labels
  для каждой borderline записи.

## Проверки

- [x] Semantic evaluation harness `npm test`.
- [x] Backend `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` — passed после финальных telemetry assertions.
- [x] Frontend `npm test`, `npm run lint`, `npm run build`.
- [x] OpenAPI conformance `npm test`, `npm run check`.
- [x] `git diff --check`.
- [ ] REAL smoke — намеренно не выполнен: policy/model/quality gates закрыты не
  были; automatic retry отсутствует.

## Scope control

Не добавлены `APARTMENT`, booking/payment, durable scheduler/cache, raw
conversation capture, distributed tracing, deployment manifests, collector,
dashboard или vendor-specific alert configuration.
