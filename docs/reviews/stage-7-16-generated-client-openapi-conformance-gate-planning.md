# Stage 7.16 — Generated-client / OpenAPI Conformance Gate Planning

## 1. Цель Stage 7.16

Цель Stage 7.16 — зафиксировать план будущего generated-client / OpenAPI conformance gate без реализации gate, без генерации клиентов, без изменения OpenAPI draft и без изменения backend runtime behavior.

Документ определяет:

- какие цели должен закрывать будущий conformance gate;
- какие проверки являются кандидатами для gate;
- что можно проверять уже сейчас как foundation subset;
- что должно ждать реального hotel search/resource behavior;
- какие blockers остаются перед generated-client readiness и OpenAPI finalization.

Stage 7.16 не заявляет generated-client readiness и не финализирует OpenAPI.

## 2. Проверенные источники

- `AGENTS.md`
- `docs/prompts/codex-task-template.md`
- `docs/prompts/codex-review-template.md`
- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/product/product-baseline.md`
- `docs/architecture/architecture-baseline.md`
- `docs/development/README.md`
- `docs/development/documentation-guidelines.md`
- `docs/development/quality-gates.md`
- `docs/decisions/README.md`
- `docs/reviews/README.md`
- `services/backend/README.md`
- `docs/reviews/stage-7-13-generated-client-openapi-readiness-checkpoint.md`
- `docs/reviews/stage-7-14-generated-client-openapi-readiness-cleanup.md`
- `docs/reviews/stage-7-14-generated-client-openapi-readiness-cleanup-review.md`
- `docs/reviews/stage-7-15-assistant-response-semantics-search-readiness-boundary-cleanup.md`
- `docs/reviews/stage-7-15-assistant-response-semantics-search-readiness-boundary-cleanup-review.md`
- `docs/reviews/stage-7-15b-stage-7-13-7-15-documentation-status-sync.md`
- `docs/architecture/stage-6/openapi-draft.yaml`
- `docs/architecture/stage-6/openapi-contract-notes.md`

В корне репозитория отдельный `openapi-draft.yaml` не найден; актуальный Stage 6 draft находится в `docs/architecture/stage-6/openapi-draft.yaml`.

## 3. Использование task/review templates

`docs/prompts/codex-task-template.md` был прочитан до repository inspection и использован как структура выполнения: task intake, source review, scope control, implementation plan, validation и final reporting.

`docs/prompts/codex-review-template.md` был прочитан до repository inspection и использован для self-review перед завершением: проверены scope, source-of-truth alignment, отсутствие out-of-scope реализации, documentation language policy и validation reporting.

## 4. Текущее состояние

Stage 7 завершен through Stage 7.15b до начала Stage 7.16. Stage 7.16 является отдельной planning/documentation задачей.

Текущее backend состояние остается foundation-only:

- health endpoint реализован;
- assistant session creation и message intake существуют как локальные foundation boundaries;
- assistant response shape публично ограничен `session`, `assistantMessage`, `nextAction`;
- `assistantMessage` остается placeholder-only;
- `nextAction` вычисляется через application-level assistant response semantics;
- placeholder hotel-only endpoints остаются runtime placeholders и возвращают `501 NOT_IMPLEMENTED`;
- generated clients отсутствуют;
- OpenAPI/runtime conformance gate отсутствует;
- generated-client-ready subset отсутствует.

Stage 6 OpenAPI draft описывает полный hotel-only frontend/backend API contract, включая `POST /hotel-searches`, `GET /hotel-searches/{searchId}/offers`, shortlist и explanations. Runtime для этих resource flows еще не реализует contract-aligned success behavior.

## 5. Почему generated-client/OpenAPI finalization ещё рано

Generated-client/OpenAPI finalization еще рано, потому что runtime и draft находятся на разных уровнях готовности:

- placeholder endpoints возвращают `501 NOT_IMPLEMENTED`, а OpenAPI draft описывает success schemas для hotel search, offers, shortlist и explanations;
- `NOT_IMPLEMENTED` и generic `NOT_FOUND` остаются foundation-only runtime codes и не входят в final generated-client taxonomy;
- нет generated-client-ready subset config, который явно отделяет проверяемые endpoints от placeholder endpoints;
- нет runtime/OpenAPI conformance gate;
- нет real `HotelSearchRequest` production flow, provider facts, hotel offers, shortlist/ranking/explanation/resource semantics;
- `show_boundary_message` в assistant response semantics не означает real hotel search readiness;
- `ready_for_hotel_search` намеренно не используется runtime behavior;
- `hotelSearchRequest` намеренно не добавлен в public runtime responses.

Если сейчас сгенерировать clients по полному draft, clients будут ожидать success/resource behavior, которого runtime сознательно не предоставляет.

## 6. Future conformance gate goals

Будущий generated-client/OpenAPI conformance gate должен:

- предотвращать ложное заявление generated-client readiness;
- фиксировать, какой subset endpoint-ов является generated-client-ready;
- явно исключать placeholder endpoints до появления contract-aligned behavior;
- проверять drift между OpenAPI path/method inventory и runtime route inventory;
- проверять response shape для включенного subset;
- проверять error taxonomy для включенного subset;
- отделять foundation-only runtime behavior от final client-facing contract behavior;
- давать понятный fail-fast сигнал перед OpenAPI finalization и client generation;
- сохранять hotel-only MVP scope и не активировать provider/frontend/booking/payment/flight work.

## 7. Candidate gate checks

Кандидаты для будущего gate:

- OpenAPI schema validity: OpenAPI 3.1 document parses and validates.
- Path/method inventory comparison: runtime Ktor routes match expected path/method inventory for included subset.
- Generated-client-ready subset validation: config explicitly lists included endpoints and excluded placeholder endpoints.
- Response shape comparison: included runtime responses match required OpenAPI response schemas.
- Error taxonomy comparison: included runtime error codes match OpenAPI `ErrorResponse` / `ValidationErrorResponse` expectations.
- Placeholder endpoint exclusion strategy: known placeholder endpoints fail the gate if they are included before contract-aligned behavior exists.
- Contract/runtime drift detection: added runtime routes or OpenAPI paths require explicit classification.
- Assistant response shape check: public assistant response remains compatible with the current allowed subset.
- `hotelSearchRequest` exposure check: no runtime exposure until real search/value boundary exists.
- Generated-client compile check: future-only after subset and generator config are safe.
- Runtime contract tests: future-only after behavior exists for the endpoint slice being checked.

## 8. What can be checked now

Без изменения runtime и без генерации clients сейчас можно проверять только foundation-level readiness signals:

- наличие и расположение Stage 6 OpenAPI draft;
- ручной или будущий static path/method inventory для OpenAPI draft;
- ручной или будущий static inventory текущих Ktor routes;
- список placeholder endpoints, которые должны быть excluded from generated-client-ready subset;
- отсутствие generated-client-ready claim в active docs;
- отсутствие generated clients в repository scope;
- assistant public response shape для текущего foundation subset;
- наличие documented blockers перед generated-client/OpenAPI finalization.

Эти проверки не должны создавать иллюзию, что весь OpenAPI draft уже runtime-conformant.

## 9. What must wait

Следующие проверки должны ждать более позднего behavior:

- full generated-client compile check по full OpenAPI draft;
- runtime schema conformance для hotel search success responses;
- runtime schema conformance для offers, shortlist и explanations;
- resource-specific not-found taxonomy для real resources;
- `HotelSearchRequest` lifecycle и accepted/searching/completed/failed search states;
- provider facts, assumptions, unknowns и metadata semantics;
- shortlist item resource semantics;
- explanation/comparison grounding semantics;
- generated clients для frontend integration;
- end-to-end runtime contract tests.

## 10. Generated-client blockers

- Нет generated-client-ready subset config.
- Placeholder endpoints still return `501 NOT_IMPLEMENTED`.
- Placeholder endpoints do not match OpenAPI success schemas.
- Runtime error taxonomy contains foundation-only `NOT_IMPLEMENTED` and generic `NOT_FOUND`.
- Generated-client generation is not configured or validated.
- No generated-client compile gate exists.
- No frontend/client integration target is active.

## 11. OpenAPI finalization blockers

- Stage 6 draft still describes future resource flows that runtime has not implemented.
- Runtime/OpenAPI error taxonomy is not final.
- Assistant response semantics are foundation-only and do not expose `hotelSearchRequest`.
- `ready_for_hotel_search` is intentionally not active.
- Real hotel search/value/resource semantics are absent.
- No OpenAPI/runtime conformance gate exists.

## 12. Runtime behavior blockers

- No real hotel search orchestration.
- No `HotelSearchRequest` creation from confirmed criteria.
- No provider-backed hotel facts or hotel offers.
- No ranking, shortlist behavior or explanation/comparison behavior.
- No resource persistence, search state, offer identity or lifecycle semantics.
- No DB/storage, Redis/cache or durable session/message history.
- No LLM orchestration or requirements extraction.

## 13. Documentation/navigation blockers

No active documentation/navigation blocker was found for Stage 7.16 planning after Stage 7.15b.

Documentation still needs future bounded updates when implementation changes are actually made:

- generated-client-ready subset policy must be documented when created;
- conformance gate usage must be documented when implemented;
- OpenAPI finalization status must be updated only after a real finalization task passes.

## 14. Tooling blockers

- No OpenAPI validation command is documented as an active gate.
- No route inventory comparison tool exists.
- No schema conformance test harness exists.
- No generated-client generation task is configured as safe for current subset.
- No generated-client compile check exists.
- No CI or local quality gate enforces OpenAPI/runtime drift.

## 15. Proposed staged path

Рекомендуемый будущий путь:

1. Readiness cleanup: уточнить generated-client-ready subset policy и placeholder exclusion policy.
2. Conformance gate skeleton: добавить минимальный gate, который валидирует OpenAPI file presence/parseability, route inventory classification и explicit exclusions без генерации clients.
3. Generated-client subset definition: зафиксировать config для endpoints, которые реально готовы к generated clients.
4. Runtime/OpenAPI alignment by endpoint slices: доводить endpoints до contract-aligned behavior постепенно.
5. OpenAPI update/finalization: обновить draft только после runtime alignment и accepted taxonomy decisions.
6. Generated-client generation: включить generation и compile checks только после passing conformance gate.

Stage 7.16 не выполняет ни один из этих шагов как implementation; он только фиксирует план.

## 16. Что было изменено

- Создан Stage 7.16 planning report.
- Добавлена узкая запись Stage 7.16 в `docs/reviews/README.md`.
- Активное status wording в `README.md`, `docs/ROADMAP.md` и `docs/roadmap/roadmap.md` синхронизировано с фактом завершения Stage 7.16 planning task.

## 17. Созданные файлы

- `docs/reviews/stage-7-16-generated-client-openapi-conformance-gate-planning.md`

## 18. Изменённые файлы

- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/reviews/README.md`

## 19. Что намеренно не реализовывалось

- Generated-client/OpenAPI conformance gate.
- Scripts, tests, build tasks или CI checks.
- Generated-client generation.
- OpenAPI draft updates.
- Backend code или public API behavior changes.
- DB/storage, Redis/cache или durable persistence.
- Provider integration, LLM orchestration или requirements extraction.
- Real hotel search, ranking, shortlist или explanation behavior.
- Frontend, booking, payment или flights.
- Stage 7.17 или любые более поздние этапы.

## 20. Проверки

- `git status --short` — выполнено до изменений; рабочее дерево было чистым.
- `git status --short` — выполнено после изменений; показал только ожидаемые documentation changes:
  - `README.md`
  - `docs/ROADMAP.md`
  - `docs/roadmap/roadmap.md`
  - `docs/reviews/README.md`
  - `docs/reviews/stage-7-16-generated-client-openapi-conformance-gate-planning.md`
- `git diff --check` — passed.

Backend Gradle tests не запускались, потому что Stage 7.16 является documentation/planning задачей и не меняет backend или build files.

## 21. Self-review summary

Self-review по `docs/prompts/codex-review-template.md`:

- scope соответствует Stage 7.16 planning/documentation задаче;
- generated clients не создавались;
- OpenAPI draft не изменялся;
- backend behavior не изменялся;
- Stage 7.17+ не активирован;
- blockers не превращены в active backlog без отдельной будущей задачи;
- документация написана на русском с сохранением технических имен;
- remaining readiness claims сформулированы отрицательно: generated-client/OpenAPI readiness не достигнута.

## 22. Recommended next task

Рекомендуемая следующая задача: отдельный bounded Stage 7.17 task для generated-client-ready subset policy / placeholder exclusion policy, если roadmap решит продолжать generated-client/OpenAPI readiness track.

Эта рекомендация не запускает Stage 7.17 автоматически.

## 23. Scope control confirmation

Stage 7.16 ограничен planning/documentation работой. Реализация conformance gate, OpenAPI finalization, generated clients, backend behavior, frontend, provider integration, LLM orchestration, DB/storage, booking, payment и flights не выполнялись.
