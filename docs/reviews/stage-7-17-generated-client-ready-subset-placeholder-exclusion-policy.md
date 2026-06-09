# Stage 7.17 — Generated-client-ready Subset / Placeholder Exclusion Policy

## 1. Цель Stage 7.17

Цель Stage 7.17 — зафиксировать policy для будущего `generated-client-ready subset` и placeholder exclusion до реализации conformance gate, генерации клиентов, OpenAPI finalization или изменения backend behavior.

Документ определяет:

- как будущие задачи должны решать, безопасно ли включать endpoint в generated-client-ready subset;
- как placeholder endpoints должны исключаться до alignment runtime behavior, OpenAPI success schemas и error semantics;
- какой conceptual subset manifest shape может быть использован позднее;
- какие blockers остаются перед generated-client generation и OpenAPI finalization.

Stage 7.17 не заявляет generated-client readiness, не создает subset config и не реализует conformance gate.

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
- `docs/reviews/README.md`
- `services/backend/README.md`
- `docs/reviews/stage-7-13-generated-client-openapi-readiness-checkpoint.md`
- `docs/reviews/stage-7-14-generated-client-openapi-readiness-cleanup.md`
- `docs/reviews/stage-7-14-generated-client-openapi-readiness-cleanup-review.md`
- `docs/reviews/stage-7-15-assistant-response-semantics-search-readiness-boundary-cleanup.md`
- `docs/reviews/stage-7-15-assistant-response-semantics-search-readiness-boundary-cleanup-review.md`
- `docs/reviews/stage-7-15b-stage-7-13-7-15-documentation-status-sync.md`
- `docs/reviews/stage-7-16-generated-client-openapi-conformance-gate-planning.md`
- `docs/architecture/stage-6/openapi-draft.yaml`
- `docs/architecture/stage-6/openapi-contract-notes.md`

В корне репозитория отдельный `openapi-draft.yaml` не найден; актуальный Stage 6 draft находится в `docs/architecture/stage-6/openapi-draft.yaml`.

## 3. Использование task/review templates

`docs/prompts/codex-task-template.md` был прочитан до repository inspection и использован как структура выполнения: task intake, allowed/forbidden scope, documentation expectations, validation expectations и final reporting.

`docs/prompts/codex-review-template.md` был прочитан до repository inspection и использован для self-review перед завершением: проверены scope drift, roadmap/status consistency, documentation consistency, source-of-truth duplication, stale active wording, historical report handling и recommendations not implemented.

## 4. Текущее состояние

Stage 7 завершен through Stage 7.16 до начала Stage 7.17. Stage 7.17 является отдельной policy/documentation задачей.

Текущее состояние:

- generated-client/OpenAPI readiness не заявлена;
- conformance gate не реализован;
- generated clients не созданы;
- OpenAPI finalization не заявлена;
- machine-readable generated-client-ready subset config отсутствует;
- placeholder hotel search, offers, shortlist и explanations endpoints остаются `501 NOT_IMPLEMENTED`;
- runtime не производит OpenAPI success schemas для hotel search, offers, shortlist и explanations;
- assistant response semantics остаются foundation-only и не создают `hotelSearchRequest`.

## 5. Почему subset policy нужен до conformance gate implementation

Subset policy нужен до implementation conformance gate, потому что gate должен проверять уже определенную классификацию endpoints, а не принимать продуктовые или contract decisions сам.

Без policy conformance gate мог бы:

- случайно включить placeholder endpoints в generated-client-ready scope;
- принять `501 NOT_IMPLEMENTED` как допустимый final runtime behavior;
- сгенерировать clients против OpenAPI success schemas, которые runtime не умеет возвращать;
- смешать foundation-only runtime behavior с final client-facing contract behavior;
- создать ложное впечатление, что generated-client/OpenAPI readiness достигнута.

Stage 7.17 фиксирует decision layer для будущего Stage 7 conformance gate skeleton, но не реализует сам gate.

## 6. Generated-client-ready subset policy

Endpoint может входить в будущий `generated-client-ready subset` только если выполнены все условия:

- endpoint имеет real runtime behavior, а не placeholder-only boundary;
- endpoint не возвращает `501 NOT_IMPLEMENTED` для своего основного flow;
- runtime success response соответствует OpenAPI success schema для включаемого endpoint;
- request schema стабильна для generated clients;
- response schema стабильна для generated clients;
- error response shape documented и stable;
- error taxonomy compatible с текущей OpenAPI direction или явно согласованной future-final taxonomy;
- endpoint semantics не являются foundation-only или placeholder-only;
- endpoint не зависит от отсутствующих provider facts, ranking, shortlist behavior, explanation behavior, booking, payment, flights, LLM orchestration или future resource semantics;
- endpoint может быть безопасно проверен будущим conformance gate.

Включение endpoint в subset должно быть явным. Наличие route в runtime или path в OpenAPI draft само по себе не делает endpoint generated-client-ready.

## 7. Placeholder exclusion policy

Placeholder endpoint должен быть исключен из generated-client-ready subset, пока отдельная roadmap-aligned задача не заменит placeholder behavior на contract-aligned behavior.

Обязательные правила exclusion:

- все endpoints, возвращающие `501 NOT_IMPLEMENTED`, исключаются;
- endpoint исключается, если OpenAPI success schema существует, но runtime не может ее произвести;
- foundation-only semantics не считаются readiness;
- temporary runtime error taxonomy не считается final generated-client taxonomy;
- fake success payloads запрещены как способ пройти subset policy;
- optional omitted `hotelSearchRequest` не считается search readiness;
- exclusion снимается только после появления real runtime behavior, matching success schema и stable error semantics.

Placeholder endpoints остаются полезными как route inventory и visible foundation boundaries, но не являются generated-client-ready API.

## 8. Endpoint inclusion criteria

Endpoint можно рассматривать для inclusion только если:

- route существует в runtime и OpenAPI draft или future OpenAPI source;
- request validation соответствует documented request schema;
- successful HTTP status и payload соответствуют OpenAPI success response;
- error statuses, `code`, `message`, `fields` или `details` documented и стабильны;
- operationId, path parameters и request body semantics не требуют будущего redesign;
- endpoint не требует missing provider/source data;
- endpoint не требует missing resource lifecycle semantics;
- endpoint не требует fake destination, dates, guests, hotel offers, shortlist items или explanations;
- endpoint покрыт или может быть покрыт future conformance checks без special-case assumptions;
- inclusion не меняет roadmap scope и не запускает Stage 7.18+.

Если хотя бы один критерий не выполнен, endpoint остается outside generated-client-ready subset.

## 9. Endpoint exclusion criteria

Endpoint должен быть excluded, если выполняется хотя бы одно условие:

- endpoint является placeholder endpoint;
- endpoint возвращает `501 NOT_IMPLEMENTED`;
- OpenAPI success schema существует, но runtime не может вернуть matching response;
- response semantics intentionally foundation-only;
- request/response lifecycle не стабилен;
- error taxonomy не final или расходится с OpenAPI direction;
- endpoint зависит от future hotel search/resource semantics;
- endpoint зависит от provider facts, hotel offers, ranking, shortlist behavior, explanations, booking, payment, flights или LLM orchestration;
- endpoint требует generated-client contract assumptions, которые runtime еще не выполняет;
- endpoint может пройти только через fake payloads или temporary compatibility exceptions.

Exclusion не является дефектом само по себе; для Stage 7 foundation это безопасная защита от ложной readiness.

## 10. Conceptual subset manifest shape

Stage 7.17 не создает machine-readable subset config. Будущая task может создать manifest только после отдельного roadmap-aligned решения.

Концептуальная форма manifest:

```yaml
version: "stage-7-generated-client-subset-policy-v1"
openApiSource: "docs/architecture/stage-6/openapi-draft.yaml"
readinessStatus: "not_ready"
includedEndpoints:
  - method: "GET"
    path: "/api/v1/health"
    reason: "candidate for future low-risk foundation subset validation"
    requiredChecks:
      - "openapi_schema_validity"
      - "runtime_response_shape"
      - "error_taxonomy_review"
excludedEndpoints:
  - method: "POST"
    path: "/api/v1/hotel-searches"
    exclusionReason: "placeholder_501_not_implemented"
notes:
  - "Generated-client readiness is not claimed by this manifest."
```

Required conceptual fields:

- `version`;
- `openApiSource`;
- `readinessStatus`;
- `includedEndpoints`;
- `excludedEndpoints`;
- `exclusionReason`;
- `requiredChecks`;
- `notes`.

Machine-readable manifest, schema validation и enforcement остаются future work.

## 11. Relationship to Stage 7.16 conformance gate planning

Stage 7.16 определил цели и candidate checks будущего conformance gate. Stage 7.17 добавляет policy layer, который этот будущий gate должен использовать.

Связь:

- Stage 7.16 говорит, что gate должен проверять generated-client-ready subset;
- Stage 7.17 говорит, какие endpoints можно включать и какие нужно исключать;
- Stage 7.16 говорит, что placeholder endpoints должны fail gate if included too early;
- Stage 7.17 фиксирует причины и критерии такого fail;
- Stage 7.16 оставляет generated-client compile check future-only;
- Stage 7.17 подтверждает, что compile check нельзя запускать до real subset config и passing conformance checks.

Stage 7.17 не заменяет Stage 7.16 и не реализует его tooling.

## 12. What can be included now

Сейчас Stage 7.17 не создает generated-client-ready subset. Можно говорить только о documentation-level candidates для future subset discussion.

Текущий low-risk candidate:

- `GET /api/v1/health` — единственный очевидный runtime endpoint candidate for future subset discussion, потому что он имеет real foundation behavior и не зависит от hotel search/resource semantics.

Assistant session endpoints остаются foundation-only candidates requiring explicit conformance decision:

- `POST /api/v1/assistant/sessions`;
- `POST /api/v1/assistant/sessions/{sessionId}/messages`.

Они близки к Stage 6-like response shape, но не должны автоматически считаться generated-client-ready, потому что assistant semantics остаются placeholder/foundation-only, `assistantMessage` статический, `hotelSearchRequest` отсутствует, а search readiness не активирована.

## 13. What must remain excluded

До отдельной contract-aligned behavior task должны оставаться excluded:

- `POST /api/v1/hotel-searches`;
- `GET /api/v1/hotel-searches/{searchId}/offers`;
- `GET /api/v1/assistant/sessions/{sessionId}/shortlist`;
- `PUT /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`;
- `DELETE /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`;
- `POST /api/v1/assistant/sessions/{sessionId}/explanations`.

Также excluded должен оставаться любой endpoint или flow, которому нужны:

- `HotelSearchRequest`;
- provider facts;
- hotel offers;
- ranking;
- shortlist resource semantics;
- explanation/comparison grounding;
- real resource identity;
- final resource-specific error taxonomy;
- booking, payment, flights или LLM orchestration.

## 14. Remaining generated-client blockers

- Нет machine-readable generated-client-ready subset config.
- Нет conformance gate implementation.
- Placeholder endpoints still return `501 NOT_IMPLEMENTED`.
- Placeholder endpoints do not match OpenAPI success schemas.
- Assistant endpoints remain foundation-only and not automatically generated-client-ready.
- Error taxonomy still includes foundation-only `NOT_IMPLEMENTED` and generic `NOT_FOUND`.
- Generated-client generation and compile checks are not configured.

## 15. Remaining OpenAPI finalization blockers

- Stage 6 OpenAPI draft описывает future resource flows that runtime has not implemented.
- OpenAPI success schemas для hotel search, offers, shortlist и explanations не совпадают с placeholder runtime.
- Error taxonomy is not final for resource-specific behavior.
- `ready_for_hotel_search` не используется runtime behavior.
- `hotelSearchRequest` intentionally absent until real search/value boundary exists.
- No OpenAPI/runtime conformance gate exists.

## 16. Remaining runtime behavior blockers

- No real hotel search orchestration.
- No `HotelSearchRequest` construction from confirmed criteria.
- No provider-backed hotel facts.
- No hotel offers, ranking or result envelope behavior.
- No shortlist resource behavior.
- No explanation/comparison behavior.
- No durable persistence, DB/storage, Redis/cache or resource lifecycle.
- No LLM orchestration, requirements extraction or natural-language slot filling.

## 17. Proposed next staged path

Рекомендуемый будущий staged path:

1. Conformance gate skeleton planning-to-tooling task: реализовать read-only/static checks для OpenAPI presence, path inventory и explicit subset/exclusion classification без generated clients.
2. Machine-readable subset manifest task: создать config/schema только после согласования minimal included/excluded endpoints.
3. Endpoint-slice runtime alignment: доводить отдельные endpoints до real contract-aligned behavior.
4. OpenAPI/runtime taxonomy alignment: согласовать final error codes только вместе с real resource semantics.
5. Generated-client generation task: запускать generation и compile checks только после passing subset gate.

Этот path не запускается Stage 7.17 автоматически.

## 18. Что было изменено

- Создан Stage 7.17 policy report.
- Добавлена узкая запись Stage 7.17 в `docs/reviews/README.md`.
- Активное status wording в `README.md`, `docs/ROADMAP.md` и `docs/roadmap/roadmap.md` синхронизировано с фактом завершения Stage 7.17 policy task.

## 19. Созданные файлы

- `docs/reviews/stage-7-17-generated-client-ready-subset-placeholder-exclusion-policy.md`

## 20. Изменённые файлы

- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/reviews/README.md`

## 21. Что намеренно не реализовывалось

- Generated-client/OpenAPI conformance gate.
- Machine-readable subset config.
- Scripts, tests, build tasks или CI checks.
- Generated-client generation.
- OpenAPI draft updates или rewrite.
- Backend code или public API behavior changes.
- DB/storage, Redis/cache или durable persistence.
- Provider integration, LLM orchestration или requirements extraction.
- Real hotel search, ranking, shortlist или explanation behavior.
- Frontend, booking, payment или flights.
- Stage 7.18 или любые более поздние этапы.

## 22. Проверки

- `git status --short` — выполнено до изменений; рабочее дерево было чистым.
- `git status --short` — выполнено после изменений; показал только ожидаемые documentation changes:
  - `README.md`
  - `docs/ROADMAP.md`
  - `docs/roadmap/roadmap.md`
  - `docs/reviews/README.md`
  - `docs/reviews/stage-7-17-generated-client-ready-subset-placeholder-exclusion-policy.md`
- `git diff --check` — passed.

Backend Gradle tests не запускались, потому что Stage 7.17 является documentation/policy задачей и не меняет backend или build files.

## 23. Self-review summary

Self-review по `docs/prompts/codex-review-template.md`:

- scope соответствует Stage 7.17 policy/documentation задаче;
- generated-client-ready subset policy определен как documentation policy, не как config implementation;
- placeholder exclusion policy не скрывает current runtime/OpenAPI mismatch;
- generated clients не создавались;
- conformance gate не реализовывался;
- OpenAPI draft не изменялся;
- backend behavior не изменялся;
- Stage 7.18+ не активирован;
- roadmap/status wording не заявляет generated-client readiness или OpenAPI finalization readiness;
- historical reports не переписывались.

## 24. Recommended next task

Рекомендуемая следующая задача: отдельный bounded Stage 7.18 task для conformance gate skeleton planning-to-tooling, если roadmap решит продолжать generated-client/OpenAPI readiness track.

Эта рекомендация не запускает Stage 7.18 автоматически.

## 25. Scope control confirmation

Stage 7.17 ограничен policy/documentation работой. Не выполнялись conformance gate implementation, subset config creation, scripts, tests, build tasks, generated-client generation, OpenAPI changes, backend behavior changes, frontend, provider integration, LLM orchestration, DB/storage, booking, payment или flights.
