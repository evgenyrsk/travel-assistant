# Stage 15.4 — operational contract, conformance и runbook

## Scope

- синхронизировать response `X-Request-ID` и обязательный error `requestId` в
  runtime-facing OpenAPI contract;
- безопасно передавать correlation header через local demo proxy;
- классифицировать root probes и metrics как operational runtime routes вне
  product OpenAPI/generated-client subset;
- зафиксировать deployment-neutral Java 17 runbook, stdout/metrics contracts и
  начальные alert-рекомендации;
- закрыть Stage 15 полными quality gates.

## Out of scope

- изменение business behavior, ranking, provider mapping или product success
  JSON;
- Docker/Kubernetes manifests, collector, dashboard и vendor-specific alerts;
- distributed tracing, raw conversation capture, durable storage,
  multi-instance coordination, auth и CORS.

## Результат

- OpenAPI draft требует `X-Request-ID` для каждого product response и
  `requestId` для `ErrorResponse`/`ValidationErrorResponse`; safe pattern
  совпадает с runtime.
- Demo proxy передаёт inbound/outbound request ID только при соответствии
  `[A-Za-z0-9._-]{1,128}`; malformed, oversized и duplicated inbound значения
  не передаются.
- Conformance inventory содержит 13 runtime routes: четыре product-client
  candidate, четыре operational, один diagnostic и четыре placeholder; root
  `/health/live`, `/health/ready`, `/metrics` отсутствуют в product OpenAPI и
  subset manifest.
- Runbook фиксирует Java 17, `HOST`/`PORT`, provider environment, outbound
  requirements, single-instance limitation, probe/scrape paths, JSON Lines,
  bounded labels, sensitive-data запреты и alert thresholds.
- Full OpenAPI/generated-client readiness остаётся `not_ready`; Stage 15 не
  меняет этот статус.

## Проверки

- backend `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test --rerun-tasks` — 587 tests, passed;
- frontend `npm test` — 42 tests, passed;
- frontend `npm run lint` — passed;
- frontend `npm run build` — passed;
- conformance `npm test` — 11 tests, passed;
- conformance `npm run check` — passed, blocking findings отсутствуют;
- `git diff --check` — passed.

Первый backend gate до запуска Gradle обнаружил stale shell `JAVA_HOME`; повторный
запуск с явным Java 17 path прошёл успешно.

## Verdict

`PASSED`.

Stage 15 завершён. Следующий этап не активирован и требует отдельной явной
roadmap-aligned задачи.
