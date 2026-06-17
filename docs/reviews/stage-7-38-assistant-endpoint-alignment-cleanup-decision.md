# Stage 7.38 — Assistant Endpoint Contract/Runtime Alignment Cleanup Decision

## 1. Назначение Stage 7.38

Stage 7.38 фиксирует decision-only / review-only cleanup decision по gaps, найденным в Stage 7.37 для двух Assistant endpoint candidates:

- `POST /api/v1/assistant/sessions`;
- `POST /api/v1/assistant/sessions/{sessionId}/messages`.

Цель этапа — классифицировать gaps и определить, какие из них закрываются документацией, какие требуют OpenAPI/contract cleanup, какие требуют backend/runtime tests, какие относятся к conformance/tooling follow-up, а какие остаются future-only.

Stage 7.38 не выполняет cleanup implementation, не меняет backend behavior, OpenAPI contracts, `tools/openapi-conformance/**`, generated clients, `generated-client-ready-subset.yaml`, CI/Gradle или frontend code.

## 2. Baseline после Stage 7.37

Baseline после Stage 7.37:

- Stage 7.37 зафиксировал contract/runtime alignment notes по двум Assistant endpoint candidates.
- Оба endpoint существуют в OpenAPI draft и backend runtime.
- Базовые status codes и response shape в целом совпадают.
- Gaps остаются по request validation semantics, malformed/unknown JSON behavior, `message.maxLength`, `clientContext`, `nextAction`, optional/always-present response fields и lifecycle/security/session ownership assumptions.
- Assistant endpoints не включены в `docs/architecture/stage-7/generated-client-ready-subset.yaml`.
- Generated-client/OpenAPI readiness не заявлена.

## 3. Source-of-truth и прочитанные правила/шаблоны

Прочитанные правила, шаблоны и source-of-truth документы:

- `AGENTS.md`;
- `docs/prompts/codex-rules.md`;
- `docs/prompts/review-template.md`;
- `docs/prompts/codex-review-template.md`;
- `docs/guides/documentation-style-guide.md`;
- `README.md`;
- `docs/ROADMAP.md`;
- `docs/roadmap/roadmap.md`;
- `docs/reviews/README.md`.

Примененные правила:

- `docs/roadmap/roadmap.md` остается primary roadmap/status source of truth.
- `README.md` и `docs/ROADMAP.md` остаются navigation docs.
- Review artifacts являются audit trail, а не active backlog.
- Historical review artifacts не переписываются.
- Active human-readable documentation пишется на русском языке.
- Технические identifiers, paths, commands, statuses, field names, JSON/YAML values, stage/report names и commit messages не переводятся.
- Documentation-only validation требует `git diff --check`.

## 4. Какие Stage 7 reports были прочитаны

Прочитанные Stage 7 reports:

- `docs/reviews/stage-7-24-openapi-conformance-manifest-validation-design.md`;
- `docs/reviews/stage-7-25-openapi-conformance-manifest-detection-validation.md`;
- `docs/reviews/stage-7-26-documentation-quality-calibration-audit.md`;
- `docs/reviews/stage-7-27-documentation-governance-rules-cleanup.md`;
- `docs/reviews/stage-7-28-roadmap-structure-refactor.md`;
- `docs/reviews/stage-7-29-active-documentation-language-normalization.md`;
- `docs/reviews/stage-7-30-documentation-final-quality-gate.md`;
- `docs/reviews/stage-7-31-resume-development-handoff.md`;
- `docs/reviews/stage-7-32-resume-stage-7-technical-context-review.md`;
- `docs/reviews/stage-7-33-ready-subset-manifest-candidate-definition.md`;
- `docs/reviews/stage-7-34-manifest-candidate-validation-hardening.md`;
- `docs/reviews/stage-7-35-endpoint-candidate-review.md`;
- `docs/reviews/stage-7-36-assistant-endpoint-candidate-clarification.md`;
- `docs/reviews/stage-7-37-assistant-endpoint-contract-runtime-alignment-notes.md`.

## 5. Stage 7.38 documented или inferred scope

Stage 7.38 documented в `docs/roadmap/roadmap.md` как следующий шаг: `Assistant Endpoint Contract/Runtime Alignment Cleanup Decision`.

Roadmap задает только название и boundary. Детальный scope взят из текущей явной задачи: классифицировать gaps из Stage 7.37 и принять cleanup decision без выполнения самих cleanup changes.

## 6. Scope source

Scope source:

- explicit user task для Stage 7.38;
- current primary roadmap/status в `docs/roadmap/roadmap.md`;
- Stage 7.37 findings;
- Stage 7.35 и Stage 7.36 candidate clarification reports;
- Stage 7.24-7.25 manifest/conformance baseline;
- OpenAPI draft и фактические backend/runtime sources, прочитанные read-only.

## 7. Краткое резюме Stage 7.37 findings

Stage 7.37 подтвердил:

- оба Assistant endpoints существуют в OpenAPI draft и backend runtime;
- `POST /api/v1/assistant/sessions` возвращает `201` при создании session;
- `POST /api/v1/assistant/sessions/{sessionId}/messages` возвращает `200` при принятии message;
- runtime response содержит `session`, `assistantMessage` и `nextAction`;
- current-session-only модель соответствует Stage 6/7 boundary.

Stage 7.37 не подтвердил:

- enforcement для `message.maxLength`;
- contract-aligned behavior для malformed JSON и unknown JSON fields;
- фактическое использование или validation для `clientContext`;
- финальное решение, должен ли `nextAction` быть optional или always present;
- финальное решение по `hotelSearchRequest` и `searchIntentSummary` в foundation responses;
- lifecycle/security/session ownership semantics, достаточные для readiness claim.

## 8. Gap classification table

| Gap | Source/evidence | Category | Severity | Recommended cleanup type | Blocking before readiness claim | Recommended stage candidate |
|---|---|---|---|---|---|---|
| Request validation semantics для create-session: no-body разрешен, body-without-message дает `400`, а OpenAPI schema внутри optional body требует `message`. | Stage 7.36 contract shape review; Stage 7.37 risks; `openapi-draft.yaml`; `AssistantPlaceholderRoutes.kt`. | OpenAPI/contract | Высокая | OpenAPI/contract cleanup | Да | Stage 7.39 |
| Request validation semantics для message-intake: body required, runtime blank/missing `message` дает `400`, но malformed body behavior не зафиксирован contract-first. | Stage 7.37 alignment notes; `AssistantPlaceholderRoutes.kt`; `ValidationErrorResponse`. | OpenAPI/contract + backend/runtime test | Высокая | Сначала contract cleanup, затем runtime test cleanup после contract decision | Да | Stage 7.39 / Stage 7.40 |
| Malformed/unknown JSON behavior: `receiveNullable<AssistantMessageRequest>()` wrapped in `runCatching`; decode failure может схлопнуться до `null` request. | Stage 7.37 not confirmed behavior; `AssistantPlaceholderRoutes.kt`; `Serialization.kt`. | Backend/runtime test | Высокая | Backend/runtime test cleanup после contract decision | Да | Stage 7.40 |
| `message.maxLength` declared в OpenAPI, но runtime enforcement не подтвержден. | Stage 7.37 not confirmed behavior; `openapi-draft.yaml`; `AssistantPlaceholderRoutes.kt`. | OpenAPI/contract + backend/runtime test | Средняя | Contract decision плюс runtime test cleanup | Да | Stage 7.39 / Stage 7.40 |
| `clientContext` accepted by schema и DTO, но не используется и не валидируется runtime. | Stage 7.37 not confirmed behavior; `AssistantMessageRequest`; `AssistantClientContext`. | Documentation + OpenAPI/contract | Средняя | Зафиксировать как ignored/future-only или сузить schema wording | Да, если endpoint входит в generated-client subset | Stage 7.39 |
| `nextAction` optional в OpenAPI, но always present в runtime DTO. | Stage 7.37 not confirmed behavior; `AssistantMessageResponse` schema; runtime DTO. | OpenAPI/contract | Средняя | OpenAPI/contract cleanup | Да | Stage 7.39 |
| Optional/always-present response fields: `hotelSearchRequest` и `searchIntentSummary` optional/future-facing, но runtime foundation responses их не возвращают. | Stage 7.37 not confirmed behavior; OpenAPI response schemas; backend response DTOs. | Documentation + OpenAPI/contract | Средняя | Documentation wording плюс contract cleanup, если generated-client-facing shape сужается | Да, если endpoint входит в generated-client subset | Stage 7.39 |
| Lifecycle/security/session ownership assumptions остаются current-session-only и process-local. | Stage 6.8 notes; Stage 7.36 product/security needs; Stage 7.37 risks; `AssistantSessionStateStore.kt`. | Future-only | Высокая для production, не блокирует текущий decision report | Future-only documentation/carryover; без current runtime implementation | Да для production/readiness; не blocker Stage 7.38 | Будущий explicit security/session stage |
| OpenAPI/runtime mismatch risk для error taxonomy: runtime foundation codes включают `NOT_FOUND` и `NOT_IMPLEMENTED`, а Assistant candidate POST paths в основном ожидают `VALIDATION_ERROR`, `SESSION_NOT_FOUND`, `INTERNAL_ERROR`. | `ErrorResponse.kt`; `ErrorHandling.kt`; OpenAPI errors; `services/backend/README.md`. | OpenAPI/contract + documentation | Средняя | Зафиксировать foundation-only codes и выровнять candidate error taxonomy перед readiness | Да | Stage 7.39 |
| Generated-client readiness implications: assistant endpoints являются foundation candidates в tool classification, но отсутствуют в manifest и не runtime-contract validated. | Stage 7.35; Stage 7.37; `placeholder-policy.ts`; `generated-client-ready-subset.yaml`. | Conformance/tooling | Высокая | Conformance/tooling follow-up только после contract/runtime tests | Да | Более поздний Stage 7 candidate после Stage 7.39/7.40 |

## 9. Минимальные категории gaps

| Category | Decision |
|---|---|
| Request validation semantics | Требует OpenAPI/contract cleanup first. Runtime tests должны следовать после contract decision. |
| Malformed/unknown JSON behavior | Требует backend/runtime test cleanup после явного contract decision по expected behavior. |
| `message.maxLength` | Требует contract decision: либо runtime enforcement и tests, либо изменение/уточнение contract. До решения это blocker before readiness. |
| `clientContext` | Сейчас следует трактовать как accepted but behavior-neutral/future-facing field; нужен contract/documentation cleanup before readiness. |
| `nextAction` | Требует OpenAPI/contract cleanup, потому что runtime always-present shape расходится с optional contract. |
| Optional/always-present response fields | Требуют documentation/contract cleanup: `hotelSearchRequest` и `searchIntentSummary` должны быть явно future-only/absent для foundation responses или описаны как optional generated-client fields. |
| Lifecycle/security/session ownership assumptions | Оставить future-only для production/security/session ownership work; текущий scope не должен внедрять auth, persistence или ownership checks. |
| OpenAPI/runtime mismatch risk | Считать contract cleanup blocker before readiness, но не менять OpenAPI в Stage 7.38. |
| Generated-client readiness implications | Не использовать assistant endpoints как readiness evidence; tooling/manifest follow-up возможен только после contract/runtime cleanup. |

## 10. Cleanup decision

### Documentation-only cleanup candidates

- Уточнить в active docs или future report, что current Assistant runtime foundation не означает generated-client readiness.
- Уточнить, что `clientContext` сейчас accepted but behavior-neutral.
- Уточнить, что `hotelSearchRequest` и `searchIntentSummary` отсутствуют в foundation assistant responses и не являются search readiness signal.
- Уточнить, что current-session/process-local session state не означает account history, durable persistence, auth/session ownership или production security readiness.

### OpenAPI/contract cleanup candidates

- Зафиксировать precise request body semantics для `POST /api/v1/assistant/sessions`: no-body allowed, initial-message body allowed, body-without-valid-message behavior explicit.
- Зафиксировать precise request body semantics для `POST /api/v1/assistant/sessions/{sessionId}/messages`.
- Принять решение по `message.maxLength`: runtime enforcement vs contract relaxation/wording.
- Принять решение по `clientContext`: сохранить optional behavior-neutral field, ограничить формат или вынести в future-only.
- Принять решение по `nextAction`: optional vs required generated-client-facing response field.
- Уточнить `hotelSearchRequest` и `searchIntentSummary` для foundation Assistant responses.
- Уточнить error taxonomy для malformed JSON, unknown fields, blank/missing `message`, missing session и internal errors.

### Backend/runtime test cleanup candidates

- Добавить runtime tests только после contract decision по malformed JSON и unknown JSON fields.
- Добавить tests для blank/missing `message` на обоих endpoints.
- Добавить tests для `message.maxLength`, если contract сохраняет hard limit.
- Добавить tests для always-present/optional response fields после contract cleanup.
- Добавить tests для `SESSION_NOT_FOUND` semantics на message-intake path.

Stage 7.38 не создает эти tests и не меняет backend behavior.

### Conformance/tooling candidates

- Не добавлять assistant endpoints в manifest до contract cleanup и runtime tests.
- После contract/runtime cleanup рассмотреть conformance/tooling follow-up для endpoint reference validation и будущих runtime HTTP contract checks.
- Сохранить `status: "not_ready"`, `readinessClaim: false`, `future_only` и `not_run` semantics до отдельной factual readiness stage.
- Не считать static `endpointClassificationSummary` readiness evidence.

### Future-only candidates

- Auth/security/session ownership.
- Durable persistence, account history, cross-device sync и page-refresh guarantees beyond current-session.
- LLM orchestration, provider-backed hotel search, `hotelSearchRequest` construction и real recommendation behavior.
- Frontend/generated-client integration.
- CI/Gradle conformance gate.
- Stage 8 AI/LLM orchestration work.

## 11. Рекомендуемый порядок

Рекомендуемый порядок:

1. Сначала: Stage 7.39 — Assistant Endpoint Contract Shape Cleanup. Цель: принять и внести narrow OpenAPI/contract/documentation cleanup для request/response/error semantics без backend behavior changes, generated clients или readiness claim.
2. Затем: backend/runtime test cleanup candidate. Цель: покрыть уже принятое contract behavior тестами без расширения product/runtime semantics.
3. Затем: conformance/tooling follow-up candidate. Цель: расширять endpoint reference/runtime contract checks только после contract и runtime behavior stabilization.
4. Позже: manifest update candidate. Assistant endpoints можно рассматривать для manifest только как `not_ready` candidates после contract/runtime cleanup; readiness claim остается отдельным будущим gate.

Нельзя делать до contract decision:

- добавлять assistant endpoints в `generated-client-ready-subset.yaml`;
- генерировать clients;
- запускать generated-client readiness gate;
- менять conformance tool так, будто Assistant endpoints уже contract-stable;
- писать runtime tests, которые закрепляют случайное поведение malformed/unknown JSON без contract decision.

Нельзя трактовать как readiness evidence:

- наличие endpoint в OpenAPI и runtime;
- static runtime route inventory;
- `foundation_candidate` classification;
- passing skeleton manifest validation;
- этот Stage 7.38 decision report.

## 12. Readiness / non-claims

Stage 7.38 явно не заявляет:

- no generated-client readiness claim;
- no OpenAPI finalization claim;
- no generated clients;
- no backend behavior change;
- no runtime validation claim;
- no conformance tool behavior change;
- no manifest expansion;
- no CI/Gradle integration;
- no Stage 8 activation.

## 13. Risks and open questions

| Risk / question | Status |
|---|---|
| OpenAPI может сейчас over-specify fields, которые runtime не enforced, особенно `message.maxLength` и `additionalProperties: false`. | Требуется Stage 7.39 contract cleanup decision. |
| Runtime malformed JSON behavior может случайно стать de facto contract, если покрыть его tests до contract cleanup. | Не добавлять runtime tests до решения contract behavior. |
| `clientContext` может запутать generated-client users, если остается accepted but unused. | Зафиксировать behavior-neutral status или сузить contract. |
| `nextAction` optionality может создать generated-client shape ambiguity. | Решить в OpenAPI/contract cleanup до manifest expansion. |
| Session ownership/security assumptions не являются production-ready. | Оставить future-only, пока отдельный security/session stage явно не активирован. |
| Conformance tool сейчас видит assistant POST endpoints как `foundation_candidate`, но manifest не включает их. | Оставить candidate-only signal, а не readiness evidence. |

## 14. Рекомендуемый следующий этап

Рекомендуемый следующий stage: Stage 7.39 — Assistant Endpoint Contract Shape Cleanup.

Рекомендуемый scope для Stage 7.39:

- выполнить narrow OpenAPI/contract/documentation cleanup для Assistant request/response/error semantics;
- принять решение по no-body vs body-with-message behavior для `POST /api/v1/assistant/sessions`;
- принять решение по malformed/unknown JSON и validation error shape expectations;
- принять решение по `message.maxLength`, `clientContext`, `nextAction`, `hotelSearchRequest` и `searchIntentSummary` contract wording;
- сохранить отсутствие backend behavior changes, если это не запрошено отдельно;
- сохранить no generated-client readiness, no generated clients, no conformance tool behavior changes, no manifest expansion и no Stage 8 activation.

Stage 7.39 должен стартовать только отдельной явной roadmap-aligned задачей.
