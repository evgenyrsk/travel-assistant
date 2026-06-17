# Stage 7.37 — Assistant Endpoint Contract/Runtime Alignment Notes

## 1. Назначение Stage 7.37

Stage 7.37 фиксирует review-only notes по alignment между Assistant endpoint contract и фактическим backend/runtime состоянием после Stage 7.36.

Цель этапа — зафиксировать, какие части двух assistant endpoint candidates уже выглядят согласованными на уровне read-only анализа, какие остаются gaps/unknowns и что нельзя трактовать как generated-client readiness.

Stage 7.37 не меняет backend behavior, OpenAPI/API contracts, generated clients, `tools/openapi-conformance/**`, manifest candidate, CI/Gradle integration или frontend code.

## 2. Baseline после Stage 7.36

Baseline после Stage 7.36:

- Stage 7.36 завершил clarification двух assistant endpoint candidates:
  - `POST /api/v1/assistant/sessions`;
  - `POST /api/v1/assistant/sessions/{sessionId}/messages`.
- Оба endpoint остаются candidate-only и не включены в `docs/architecture/stage-7/generated-client-ready-subset.yaml`.
- `generated-client-ready-subset.yaml` сохраняет `status: not_ready` и `readinessClaim: false`.
- OpenAPI finalization, generated-client readiness, generated clients, runtime HTTP contract tests и CI gate не заявлены.

## 3. Почему Stage 7.24-7.25 важны как pre-audit technical baseline

Stage 7.24-7.25 были последним technical anchor перед documentation pause:

- Stage 7.24 спроектировал будущий manifest detection/schema validation flow для standalone OpenAPI conformance tool без создания real subset manifest и без readiness claim.
- Stage 7.25 реализовал tool-local read-only manifest detection/validation, но оставил report advisory/not_ready и не добавил endpoint reference validation, runtime HTTP contract tests, generated-client generation или CI integration.

Из-за этого Stage 7.37 должен оценивать assistant endpoints не как почти готовые generated-client endpoints, а как candidates, которые находятся рядом с manifest/conformance work, но еще не прошли contract/runtime cleanup decision и не получили readiness gate.

## 4. Почему Stage 7.26-7.31 считаются documentation stabilization / handoff

Stage 7.26-7.31 были документационной стабилизацией и handoff, а не active technical implementation:

- Stage 7.26 выполнил documentation quality calibration audit.
- Stage 7.27 усилил governance rules.
- Stage 7.28 реорганизовал roadmap/status структуру.
- Stage 7.29 нормализовал active documentation language.
- Stage 7.30 выполнил final documentation quality gate.
- Stage 7.31 зафиксировал resume-development handoff и guardrails для возвращения к bounded Stage 7 technical work.

Эти этапы не изменяли backend behavior, OpenAPI contracts, conformance tool behavior, manifest readiness или generated clients.

## 5. Связь Stage 7.32-7.36 с возвратом к technical track

Stage 7.32-7.36 вернули Stage 7 к bounded technical track после documentation pause:

- Stage 7.32 восстановил technical context и указал Stage 7.25 как последний pre-pause technical baseline.
- Stage 7.33 создал non-readiness `generated-client-ready-subset.yaml` candidate только для skeleton validation.
- Stage 7.34 усилил manifest validation guardrails против premature readiness promotion signals.
- Stage 7.35 выполнил endpoint candidate review и сохранил manifest/readiness state unchanged.
- Stage 7.36 уточнил условия для двух assistant endpoint candidates и рекомендовал отдельные contract/runtime alignment notes перед любой manifest expansion.

Stage 7.37 продолжает этот track как review-only фиксация notes, а не как cleanup implementation.

## 6. Source-of-truth и прочитанные правила/шаблоны

Прочитанные правила, шаблоны и source-of-truth документы:

- `AGENTS.md`;
- `docs/prompts/codex-rules.md`;
- `docs/prompts/review-template.md`;
- `docs/prompts/README.md`;
- `docs/prompts/codex-review-template.md`;
- `docs/guides/documentation-style-guide.md`;
- `README.md`;
- `docs/ROADMAP.md`;
- `docs/roadmap/roadmap.md`;
- `docs/reviews/README.md`;
- `docs/product/product-baseline.md`;
- `docs/architecture/architecture-baseline.md`;
- `docs/architecture/backend-layering-rules.md`;
- `docs/development/README.md`;
- `docs/development/documentation-guidelines.md`;
- `docs/development/quality-gates.md`;
- `docs/development/definition-of-done.md`.

Примененные правила:

- `docs/roadmap/roadmap.md` остается primary roadmap/status source of truth.
- `README.md` и `docs/ROADMAP.md` остаются navigation docs и не должны дублировать detailed status.
- Review artifacts являются audit trail, а не active backlog.
- Historical reports не переписываются.
- Active human-readable documentation пишется на русском языке; технические identifiers, paths, commands, statuses, field names и example values сохраняются без перевода.
- Documentation-only validation требует `git diff --check`.

## 7. Stage 7.37 documented или inferred scope

Stage 7.37 documented в `docs/roadmap/roadmap.md` как следующий шаг: `Assistant Endpoint Contract/Runtime Alignment Notes`.

Roadmap задает boundary на уровне названия и explicit next-step status. Детальный scope для этого report взят из текущей явной задачи: review-only notes по alignment между Assistant endpoint contract и runtime/backend состоянием без изменения backend behavior, OpenAPI contracts, generated clients, `tools/openapi-conformance/**` или conformance tool behavior.

## 8. Assistant endpoint contract sources reviewed

Прочитанные contract sources:

- `docs/architecture/stage-6/openapi-draft.yaml`;
- `docs/architecture/stage-6/openapi-contract-notes.md`;
- `docs/architecture/stage-6/pre-implementation-decisions-cleanup.md`;
- `docs/architecture/stage-7/generated-client-ready-subset.yaml`;
- `docs/reviews/stage-7-35-endpoint-candidate-review.md`;
- `docs/reviews/stage-7-36-assistant-endpoint-candidate-clarification.md`.

Релевантные contract expectations:

- `POST /api/v1/assistant/sessions` может принимать optional initial message.
- `POST /api/v1/assistant/sessions/{sessionId}/messages` требует `message` request body.
- `AssistantMessageRequest.message` описан как required string с `minLength: 1` и `maxLength: 4000`.
- `AssistantMessageRequest.clientContext` допускает optional `locale` и `timezone`.
- `AssistantMessageResponse` требует `session` и `assistantMessage`, допускает optional `nextAction` и `hotelSearchRequest`.
- OpenAPI contract описывает current-session scope, а не account history или cross-device persistence.

## 9. Assistant endpoint runtime/backend sources reviewed

Прочитанные runtime/backend sources:

- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ApiRoutes.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/AssistantPlaceholderRoutes.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ErrorHandling.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ErrorResponse.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/Serialization.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/assistant/AssistantSessionBoundary.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/assistant/AssistantSessionStateStore.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/assistant/AssistantResponseSemantics.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/assistant/AssistantNextAction.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/assistant/AssistantSearchReadiness.kt`;
- `services/backend/README.md`.

Прочитанный conformance-tool context:

- `tools/openapi-conformance/README.md`;
- `tools/openapi-conformance/src/placeholder-policy.ts`;
- `tools/openapi-conformance/src/report.ts`;
- `tools/openapi-conformance/src/subset-manifest.ts`;
- `tools/openapi-conformance/src/types.ts`.

## 10. Contract/runtime alignment notes

### Что совпадает

- Оба assistant endpoints существуют в OpenAPI draft и backend runtime под `/api/v1`.
- `POST /api/v1/assistant/sessions` возвращает `201` при создании session.
- `POST /api/v1/assistant/sessions/{sessionId}/messages` возвращает `200` при принятии user message.
- Runtime обрабатывает blank/missing `message` как validation error для message intake и возвращает `400`.
- Runtime возвращает `404` для missing assistant session на message intake path.
- Runtime response содержит `session` и `assistantMessage`, что соответствует required response fields в OpenAPI draft.
- Runtime `nextAction` использует значения, которые входят в OpenAPI enum: `ask_clarification` и `show_boundary_message`.
- Current-session-only runtime model соответствует product/architecture boundary: нет account history, cross-device persistence, booking/payment или provider-backed search behavior.

### Что не подтверждено

- Не подтверждено runtime enforcement для `message.maxLength: 4000`.
- Не подтверждено contract-aligned поведение для unknown JSON fields и malformed JSON body. В reviewed route code `receiveNullable<AssistantMessageRequest>()` обернут в `runCatching`, поэтому decode failure может быть сведена к `null` request без отдельной error taxonomy.
- Не подтверждена фактическая runtime валидация `clientContext.locale` и `clientContext.timezone`; DTO принимает эти поля, но reviewed runtime не использует их для behavior или validation.
- Не подтверждено, что `nextAction` должен быть optional или always present в generated-client-facing contract. Runtime фактически всегда формирует `nextAction`, а OpenAPI draft описывает его как optional.
- Не подтверждено, что optional `hotelSearchRequest` и `searchIntentSummary` должны оставаться absent для foundation assistant responses или получить отдельное contract wording до manifest expansion.
- Не подтверждены auth/security/ownership assumptions для session access; current Stage 7 runtime остается local current-session foundation.

### Что отсутствует

- Assistant endpoints отсутствуют в included endpoint list `docs/architecture/stage-7/generated-client-ready-subset.yaml`.
- Runtime HTTP contract tests для assistant endpoints отсутствуют в текущем conformance gate.
- Generated-client targets и generated clients отсутствуют.
- Endpoint reference validation в manifest остаётся future-only.
- Durable persistence, message history retrieval, LLM orchestration, provider-backed hotel search, ranking/recommendation behavior, frontend integration и production auth/session ownership отсутствуют.

### Что является future-only

- Добавление assistant endpoints в real generated-client-ready subset.
- Endpoint reference validation между manifest и OpenAPI/runtime inventory.
- Runtime HTTP contract tests.
- Generated-client generation и generated-client compile.
- OpenAPI finalization.
- CI/Gradle integration для conformance gate.
- Cleanup implementation, который выберет direction между OpenAPI contract shape и runtime behavior.

### Что нельзя трактовать как readiness

- Наличие двух assistant endpoints и в OpenAPI draft, и в runtime не является generated-client readiness.
- Static runtime inventory в conformance report не является runtime contract validation.
- Этот report не является OpenAPI finalization, manifest expansion или разрешением создавать generated clients.
- Stage 7.37 не меняет `status: not_ready`, `readinessClaim: false` или endpoint readiness state.

## 11. OpenAPI/conformance implications

Текущий state не влияет на generated-client readiness:

- `generated-client-ready-subset.yaml` не изменен и продолжает включать только foundation candidate `GET /api/v1/health`.
- Assistant endpoints остаются вне manifest до отдельного explicit cleanup/decision step.
- Stage 7.24-7.25 важны как baseline: conformance tool умеет обнаруживать/валидировать manifest skeleton на read-only уровне, но endpoint reference validation и runtime contract checks остаются future-only.
- Stage 7.34 дополнительно блокирует premature readiness promotion signals, поэтому assistant endpoint notes не могут сами перевести report или manifest в ready state.

Вне scope Stage 7.37:

- OpenAPI edits;
- backend behavior changes;
- manifest edits;
- generated-client target declaration;
- generated-client generation;
- conformance tool behavior changes;
- backend server startup или HTTP/network checks.

## 12. Readiness / non-claims

Stage 7.37 явно не заявляет:

- no generated-client readiness claim;
- no OpenAPI finalization claim;
- no generated clients;
- no backend behavior change;
- no runtime validation claim;
- no manifest expansion;
- no conformance tool behavior change;
- no CI/Gradle integration;
- no Stage 8 activation.

## 13. Risks and open questions

- `message` request semantics требуют отдельного cleanup decision: OpenAPI требует `message`, но create-session route допускает missing body для session-only creation.
- Decode/error handling semantics требуют решения до generated-client readiness: unknown fields и malformed JSON body не имеют явно подтвержденного alignment.
- Нужен отдельный decision по тому, остается ли `nextAction` optional в OpenAPI или фиксируется как always present в runtime-facing response.
- Нужна ясность, должны ли `hotelSearchRequest` и `searchIntentSummary` оставаться future-only/absent в foundation stage или получить explicit non-goal wording.
- Session lifecycle, auth/ownership, expiration и persistence assumptions остаются неготовыми для production/generate-client readiness.

## 14. Recommended next stage candidate

Рекомендуемый следующий candidate: Stage 7.38 — Assistant Endpoint Contract/Runtime Alignment Cleanup Decision.

Этот шаг должен быть отдельной явной roadmap-aligned задачей и не должен автоматически стартовать из этого report. Его безопасная цель: выбрать точное направление cleanup для двух assistant endpoints перед любым manifest expansion:

- request body semantics для create-session и message-intake;
- validation/error behavior для blank, missing, too long, unknown fields и malformed JSON;
- `nextAction`, `hotelSearchRequest` и `searchIntentSummary` response contract expectations;
- current-session lifecycle/security/ownership wording;
- критерии, после которых assistant endpoints можно будет рассматривать для future manifest update.

Stage 7.38 не должен заявлять generated-client readiness, создавать generated clients, начинать Stage 8, включать provider/DB/frontend work или активировать production integration без отдельной явной задачи.
