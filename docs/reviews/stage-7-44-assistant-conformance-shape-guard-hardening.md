# Stage 7.44 — Assistant Conformance Shape Guard Hardening

## 1. Verdict

Passed — assistant conformance shape guard hardened.

Stage 7.44 закрывает один Minor finding и одну Note из Stage 7.43. Изменения остаются внутри существующего bounded static conformance check и targeted tool-local tests.

## 2. Scope

Реализовано:

- отдельная static проверка наличия property `message` в `AssistantMessageRequest`;
- сохранена отдельная проверка включения `message` в schema `required`;
- отдельная static проверка наличия property `nextAction` в `AssistantMessageResponse`;
- сохранена отдельная проверка включения `nextAction` в schema `required`;
- targeted negative test для отсутствующего Assistant candidate в static runtime inventory;
- усилен contract-shape drift test для request/response property presence и required membership;
- кратко уточнено описание bounded shape check в `tools/openapi-conformance/README.md`.

Не менялись:

- production backend code и backend tests;
- OpenAPI contracts;
- generated clients;
- `docs/architecture/stage-7/generated-client-ready-subset.yaml`;
- frontend code;
- Gradle/CI configuration;
- runtime HTTP behavior;
- dependencies и command/exit-code policy.

## 3. Findings addressed from Stage 7.43

| Stage 7.43 finding | Stage 7.44 result |
|---|---|
| Required-field checks не подтверждали наличие самих `message` / `nextAction` properties | Добавлены `messagePropertyPresent` и `nextActionPropertyPresent`; bounded shape check теперь отдельно требует property presence и membership в `required`. |
| Отсутствовал отдельный negative test для candidate inventory mismatch | Добавлен test с отсутствующим message endpoint в static runtime inventory; он подтверждает failed check и `ASSISTANT_ENDPOINT_CANDIDATE_INVENTORY_MISMATCH`. |

## 4. Implementation summary

Изменены:

- `tools/openapi-conformance/src/types.ts`;
- `tools/openapi-conformance/src/openapi.ts`;
- `tools/openapi-conformance/src/report.ts`;
- `tools/openapi-conformance/src/report.test.ts`;
- `tools/openapi-conformance/README.md`.

`AssistantContractShape` дополнен двумя bounded boolean observations:

- `messagePropertyPresent`;
- `nextActionPropertyPresent`.

`inspectAssistantContractShape` по-прежнему читает только известные Assistant schemas. `assistant_endpoint_contract_shape` использует новые observations вместе с существующими required-list checks.

Test coverage:

- repository success path сохраняется;
- shape drift test проверяет отсутствие properties и required membership для `message` и `nextAction`;
- candidate inventory mismatch test проверяет отсутствие одного Assistant runtime route;
- advisory runtime-semantics test остается без изменений по смыслу.

## 5. Enforced vs advisory behavior

Enforced static behavior:

- property presence и required membership проверяются только для `AssistantMessageRequest.message` и `AssistantMessageResponse.nextAction`;
- inventory mismatch остается bounded проверкой двух Assistant foundation candidates;
- static drift добавляет существующий blocking finding в report, но не продвигает readiness.

Advisory-only behavior:

- `clientContext` runtime behavior;
- empty-object validation;
- malformed/unknown JSON behavior;
- non-object JSON body behavior;
- runtime enforcement `message.maxLength`;
- live runtime response validation.

`ASSISTANT_RUNTIME_SEMANTICS_NOT_CHECKED` остается advisory finding. Advisory findings не стали blocking.

## 6. Readiness statement

Stage 7.44 сохраняет:

- `status: "not_ready"`;
- `readinessClaim: false`;
- endpoint-level `readiness: "not_ready"`;
- runtime HTTP checks как `not_run`;
- generated-client checks как `future_only` / `not_run`.

Stage 7.44 не заявляет:

- generated-client readiness;
- manifest expansion readiness;
- final Stage 7 readiness;
- OpenAPI finalization;
- CI/Gradle gate readiness;
- runtime HTTP validation readiness;
- Stage 8 activation.

## 7. Validation

Выполнено:

| Command | Result |
|---|---|
| `git status --short` перед изменениями | Passed; working tree clean. |
| `npm test` из `tools/openapi-conformance` | Passed; 16 tests, 0 failures. |
| `npm run build` из `tools/openapi-conformance` | Passed; TypeScript build completed. |
| `./tools/openapi-conformance/check` | Passed; exit code `0`, `assistant_endpoint_candidate_inventory: passed`, `assistant_endpoint_contract_shape: passed`, runtime semantics advisory, `blockingFindings: []`, `status: "not_ready"`, `readinessClaim: false`. |
| `git diff --check` | Passed; whitespace errors отсутствуют. |
| Targeted search и file-existence checks | Passed; Stage 7.44 report зарегистрирован в reviews index и primary roadmap, ссылки указывают на существующие файлы. |
| Final `git status --short --untracked-files=all` / diff scope inspection | Passed; показаны только ожидаемые Stage 7.44 tool/docs files и новый report. |

Backend tests не запускались, потому что backend code и backend tests не менялись. Backend server не запускался. HTTP/network calls не выполнялись.

## 8. Next recommended stage

Рекомендуемый следующий этап:

`Stage 7.45 — Assistant Conformance Output Documentation / Operator Guidance`

Stage 7.45 должен быть отдельным узким documentation-only этапом для пояснения интерпретации Assistant static checks, blocking/advisory findings и non-readiness output. Он не должен добавлять checks, менять tool behavior, manifest, OpenAPI, backend, generated clients, frontend или CI/Gradle и не должен заявлять readiness.
