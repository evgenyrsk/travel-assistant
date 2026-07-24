# Stage 16.3 — Semantic accommodation multimodal adapter

## Статус

Завершён без REAL activation. Следующий разрешённый шаг — Stage 16.4.

## Scope

- [x] Добавлена отдельная конфигурация analysis mode `FAKE|OPENROUTER`;
  default — `FAKE`.
- [x] OpenRouter mode использует существующий `OPENROUTER_API_KEY`, отдельные
  model/base URL/timeout/batch size и обязательный exact image-host allowlist.
- [x] Batch size ограничен диапазоном `1..5`; automatic retries отсутствуют.
- [x] Request отправляет strict JSON Schema и provider routing
  `require_parameters=true`, `data_collection=deny`, `zdr=true`.
- [x] Adapter отправляет только ephemeral candidate IDs, bounded name,
  descriptions, amenities и разрешённые image URLs.
- [x] HTTPS image URL требует exact allowlisted host и запрещает credentials,
  query, fragment и explicit port.
- [x] Response принимает только bounded verdict/evidence/signal enums;
  неизвестный signal даёт typed `INVALID_RESPONSE`.
- [x] HTTP/transport failures преобразуются в typed failure taxonomy без raw
  provider error.
- [x] Provider runtime/factory закрывают собственный `HttpClient`.

## Out of scope

- runtime composition в `Application`;
- provider content и REAL calls;
- подтверждение ZDR для конкретной model/provider route;
- async search lifecycle и job scheduler;
- details enrichment/cache;
- public API/OpenAPI/frontend;
- logs/metrics Stage 16.7.

## Security и privacy review

- Session/search/offer/provider identifiers отсутствуют в application request
  type и не добавляются adapter-ом.
- Query/fragment URL отбрасываются, поэтому signed/tracking parameters не
  передаются внешней модели.
- Raw rationale отсутствует в schema.
- Hotel name, descriptions, amenities, image URLs и model output не логируются;
  adapter не содержит logging path.
- MockEngine tests подтверждают отсутствие запрещённых identifiers и unsafe
  URLs в wire body.
- `OPENROUTER` остаётся opt-in и не читается runtime composition на этом
  sub-stage.

## Проверки

- [x] FAKE default и opt-in config parsing.
- [x] Invalid/missing allowlist, wildcard host и batch > 5.
- [x] Exact-host image URL policy.
- [x] Strict schema и privacy routing fields.
- [x] Multiple image input shape и removal unsafe URLs.
- [x] Batching без retry.
- [x] Unknown signal и rate-limit failure mapping.
- [x] Backend `./gradlew test` — gate sub-stage.
- [x] `git diff --check` — gate commit.

## External gate

Право передавать provider descriptions/images, exact production allowlist и
совместимость выбранного endpoint с ZDR не подтверждены. Controlled REAL probe
не выполнялся. Конфигурация `OPENROUTER` не активирована в application runtime.

## Итог

Transport adapter готов к безопасному runtime wiring после закрытия policy
gate. Stage 16 продолжает работу с `FAKE` default; наличие adapter code не
является разрешением на передачу provider content.
