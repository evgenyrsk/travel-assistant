# Stage 17.0 — Corporate semantic portability

## Статус

Завершён 28 июля 2026 года без REAL semantic calls. Корпоративный adapter и
contract готовы к integration testing после предоставления gateway, но runtime
activation остаётся `BLOCKED` до content, infrastructure и quality gates.

## Scope

- [x] Существующий `AccommodationAnalysisClient` сохранён без provider/model
      concepts.
- [x] Добавлен explicit `INTERNAL_GATEWAY` mode; `FAKE` остаётся default.
- [x] Добавлен narrow contract v1 `POST /v1/accommodation-analysis`.
- [x] Model/provider заменены opaque deployment ID, проверяемым в response.
- [x] Startup fail-closed требует internal content approval, exact HTTPS URL,
      bearer secret и exact-host image allowlist.
- [x] Payload bounded и не содержит session/search/offer/provider IDs или
      пользовательский запрос.
- [x] Unknown schema, deployment, verdict/evidence и network failures
      возвращают typed failure без retry/fallback.
- [x] Принят `ADR-0002`; создан transfer и model-selection checklist.

## Contract

Gateway получает managed concept и кандидатов с ephemeral ID, bounded hotel
name, descriptions, amenities и максимум тремя разрешёнными images. Возвращает
только typed verdict/evidence. Free-form rationale запрещён.

Gateway deployment владеет точными model version, inference runtime, hardware,
quantization и provider settings. Travel Assistant видит только opaque
`deploymentId` и поэтому не требует code changes для смены deployment.

## Проверки

- [x] Targeted configuration, factory, adapter и runtime compatibility tests.
- [x] Backend `./gradlew test --rerun-tasks` с Java 17.
- [x] Frontend tests, lint и build.
- [x] Launcher tests и network-free `--fake --check-only` / synthetic
      `--real --check-only`; semantic mode остаётся `FAKE`.
- [x] Semantic evaluation tool tests.
- [x] OpenAPI conformance tests и read-only check со `status=not_ready`,
      `readinessClaim=false` и без blocking findings.
- [x] `git diff --check` и review полного diff.

## Open questions

Техническими блокерами adapter они не являются, но нужны до activation:

- corporate AI/GPU platform и доступные licensed multimodal deployments;
- workload identity/mTLS или замена текущего bearer composition;
- approved image transport и dataset storage;
- capacity, SLO, retention, logging и cost constraints.

## Scope control

Не выполнялись REAL model/provider call, content forwarding, controlled probe,
model bake-off, gateway implementation, deployment manifests, durable storage,
taxonomy/ranking/provider mapping changes, public API/OpenAPI или frontend
changes. `main` не затрагивался.
