# Stage 7.41 — Assistant Endpoint Conformance/Tooling Follow-up Decision

## 1. Verdict

Passed — decision-only follow-up complete.

Stage 7.41 завершает review-only решение после Stage 7.39 contract shape cleanup и Stage 7.40 runtime contract test cleanup. Blockers для перехода к маленькому будущему conformance-tool implementation stage не обнаружены, если следующий этап сохранит `not_ready` / advisory semantics и не будет заявлять generated-client readiness.

## 2. Scope

Stage 7.41 является decision-only / review-only этапом.

В рамках Stage 7.41:

- implementation conformance tool не выполнялся;
- backend production behavior не менялся;
- OpenAPI contracts не менялись;
- generated clients не менялись;
- manifest не менялся;
- Gradle/CI не менялись;
- frontend не менялся;
- backend server не запускался;
- HTTP/network calls не выполнялись.

Этап не заявляет generated-client readiness, OpenAPI finalization, manifest expansion readiness или Stage 8 activation.

## 3. Inputs reviewed

Перед изменениями выполнен `git status --short`; working tree был clean.

Inspected files:

- `services/backend/src/test/kotlin/com/travelassistant/backend/api/AssistantSessionRoutesTest.kt`;
- `tools/openapi-conformance/README.md`;
- `tools/openapi-conformance/src/report.ts`;
- `tools/openapi-conformance/src/subset-manifest.ts`;
- `tools/openapi-conformance/src/placeholder-policy.ts`;
- `tools/openapi-conformance/src/route-inventory.ts`;
- `tools/openapi-conformance/src/types.ts`;
- `docs/architecture/stage-7/generated-client-ready-subset.yaml`;
- `docs/reviews/README.md`;
- `docs/roadmap/roadmap.md`.

Stage reports reviewed:

- `docs/reviews/stage-7-39-assistant-endpoint-contract-shape-cleanup.md`;
- `docs/reviews/stage-7-40-assistant-endpoint-runtime-contract-test-cleanup.md`.

Active sequencing context reviewed:

- `docs/roadmap/roadmap.md`;
- `docs/reviews/README.md`;
- `tools/openapi-conformance/README.md`;
- `docs/architecture/stage-7/generated-client-ready-subset.yaml`.

## 4. Coverage summary from Stage 7.39 and Stage 7.40

Stage 7.39 уточнил Assistant endpoint contract shape:

- `POST /api/v1/assistant/sessions` допускает no-body session-only creation;
- если create-session request body передан, он следует `AssistantMessageRequest`;
- `POST /api/v1/assistant/sessions/{sessionId}/messages` требует request body и `message`;
- `message.maxLength` остается contract-level limit без runtime enforcement claim;
- `clientContext` optional и behavior-neutral;
- `nextAction` является required response field;
- `hotelSearchRequest` и `searchIntentSummary` остаются optional/future-facing;
- malformed/unknown JSON behavior не зафиксирован как runtime-validated guarantee.

Stage 7.40 подтвердил runtime contract coverage в backend tests:

- optional behavior-neutral `clientContext` принимается на обоих Assistant endpoints;
- `nextAction` присутствует на message response;
- validation error для empty `{}` body на session creation покрыт;
- `SESSION_NOT_FOUND` behavior для unknown `sessionId` уже покрыт existing/current runtime tests;
- no-body session creation, valid message intake, blank/missing `message` и required response fields покрыты existing/current tests.

Эти backend tests повышают confidence в текущий runtime foundation, но не заменяют conformance tooling, не расширяют manifest и не являются generated-client readiness evidence.

## 5. Future conformance-tool candidates

| Candidate check | Category | Decision note |
|---|---|---|
| Assistant endpoints присутствуют в OpenAPI inventory и static runtime route inventory | `Candidate for future enforced check` | Можно проверять в будущем conformance-tool stage как static inventory consistency для foundation candidates, без readiness claim. |
| `POST /api/v1/assistant/sessions` и `POST /api/v1/assistant/sessions/{sessionId}/messages` остаются `foundation_candidate` и `readiness: "not_ready"` | `Candidate for future enforced check` | Можно усилить guardrail, чтобы Assistant candidates не становились ready без отдельного readiness flow. |
| `nextAction` required в `AssistantMessageResponse` contract shape | `Candidate for future enforced check` | Подходит для будущей static OpenAPI schema check, потому что Stage 7.39 contract cleanup и Stage 7.40 runtime tests уже согласованы. |
| `clientContext` optional в `AssistantMessageRequest` | `Candidate for future enforced check` | Static schema optionality можно проверять; behavior-neutral semantics должны оставаться advisory-only. |
| `message` required в `AssistantMessageRequest` | `Candidate for future enforced check` | Static schema check уместен; runtime validation details не должны выводиться из schema check. |
| create-session request body остается optional | `Candidate for future enforced check` | Можно проверять как static OpenAPI shape для no-body session-only creation. |
| `404` / `SESSION_NOT_FOUND` contract presence для message endpoint | `Candidate for future enforced check` | Можно проверять presence в OpenAPI contract; runtime behavior остается покрытием backend tests до отдельного runtime conformance mode. |
| `clientContext` runtime acceptance и отсутствие response echo | `Advisory-only` | Backend tests подтверждают текущую behavior-neutral форму, но conformance tool не должен выводить runtime semantics из static scan. |
| validation error для `{}` body на session creation | `Advisory-only` | Есть backend test evidence; conformance tool без runtime HTTP mode не должен делать это blocking check. |
| malformed JSON behavior | `Advisory-only` | Stage 7.39/7.40 намеренно не закрепляют это как runtime guarantee. |
| unknown JSON fields behavior | `Advisory-only` | Нельзя превращать в enforcement без отдельного contract/runtime decision. |
| non-object JSON body behavior | `Advisory-only` | Not runtime-validated; не должно блокировать future conformance candidate implementation. |
| `message.maxLength` runtime enforcement | `Advisory-only` | `message.maxLength` существует как contract-level limit, но runtime enforcement не заявлен. |
| Full response schema validation against live runtime | `Blocked until manifest expansion` | Runtime HTTP/schema checks нельзя делать blocking до явного включения Assistant endpoints в manifest/subset candidate scope. |
| Runtime HTTP contract checks для Assistant endpoints | `Blocked until manifest expansion` | Текущий tool не запускает backend server и не выполняет HTTP requests; endpoint runtime checks требуют отдельного manifest/runtime-mode решения. |
| Endpoint reference validation для Assistant endpoints в manifest | `Blocked until manifest expansion` | Current manifest включает только `GET /api/v1/health`; Assistant references нельзя валидировать как included subset до manifest update. |
| Generated-client generation for Assistant endpoints | `Blocked until generated-client readiness` | Generated clients не создаются и readiness не заявлена. |
| Generated-client compile compatibility for Assistant endpoints | `Blocked until generated-client readiness` | Compile checks требуют declared generated-client target и readiness flow. |
| Generated-client-ready subset readiness promotion | `Blocked until generated-client readiness` | `status: "not_ready"` и `readinessClaim: false` должны сохраняться до отдельного readiness gate. |
| Hidden CI/Gradle enforcement gate | `Out of scope` | Conformance tooling не должен становиться CI gate без отдельного roadmap decision. |
| Production backend behavior fixes | `Out of scope` | Stage 7.41 не исправляет runtime behavior и не открывает backend implementation work. |
| OpenAPI contract rewrites | `Out of scope` | Stage 7.41 не меняет OpenAPI contracts; найденные будущие checks не являются разрешением переписывать contract. |
| Frontend/generated client adoption | `Out of scope` | UI/client integration не входит в ближайший conformance candidate implementation. |

Decision summary:

- безопасно перейти к будущему маленькому implementation-focused этапу для conformance candidate checks;
- этот будущий этап должен начинаться со static/advisory conformance checks, а не с runtime HTTP enforcement;
- checks, зависящие от manifest inclusion или generated clients, должны оставаться blocked;
- advisory gaps из Stage 7.39/7.40 нельзя превращать в readiness blockers без отдельного contract/runtime decision.

## 6. Recommended next stage

Рекомендуемый следующий этап:

`Stage 7.42 — Assistant Endpoint Conformance Candidate Implementation`

Stage 7.42 должен быть маленьким implementation-focused этапом для `tools/openapi-conformance/**`, но только в пределах candidate/static/advisory checks, которые не требуют manifest expansion, generated-client readiness, backend server, HTTP runtime checks, Gradle/CI integration или OpenAPI contract changes.

Stage 7.42 не должен заявлять generated-client readiness, если readiness явно не реализована и не провалидирована отдельным roadmap-aligned flow.

## 7. Risks / non-goals

Risks:

- schema-vs-runtime checks могут расходиться, если enforcing сделать слишком рано;
- manifest expansion нельзя выводить из backend runtime tests;
- generated-client readiness всё еще не заявлена;
- backend tests не заменяют conformance tooling;
- conformance tooling не должен становиться скрытым CI gate без отдельного решения;
- static route inventory не доказывает live runtime behavior;
- advisory gaps по malformed/unknown JSON и `message.maxLength` нельзя превращать в readiness blockers без отдельного решения.

Non-goals:

- не реализовывать новые conformance checks;
- не запускать backend server;
- не выполнять HTTP/network calls;
- не менять production backend behavior;
- не менять OpenAPI contracts;
- не менять manifest;
- не генерировать clients;
- не менять Gradle/CI integration;
- не начинать Stage 8.

## 8. Validation

Validation выполнена lightweight-only:

- `git status --short` перед изменениями — clean working tree;
- `git diff --check` — passed;
- targeted search по `stage-7-41-assistant-endpoint-conformance-tooling-follow-up-decision`, `Stage 7.41`, `Stage 7.42` и `Assistant Endpoint Conformance` подтвердил, что новый report добавлен в `docs/reviews/README.md` и `docs/roadmap/roadmap.md`;
- `git status --short --untracked-files=all` показал только ожидаемые Stage 7.41 files: `docs/reviews/README.md`, `docs/roadmap/roadmap.md` и новый untracked report;
- `git diff --stat` и `git diff --name-only` показали только tracked status/index changes; новый report остается untracked до отдельного commit-gate.

Backend tests не запускаются, потому что production/backend test code не меняется. `./tools/openapi-conformance/check` не запускается как enforcement gate, потому что Stage 7.41 является decision-only и не меняет OpenAPI/tool files.
