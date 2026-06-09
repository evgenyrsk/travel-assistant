# Stage 7.15 — Assistant Response Semantics / Search Readiness Boundary Cleanup

## 1. Цель Stage 7.15

Выполнить bounded assistant response semantics / search readiness boundary cleanup после Stage 7.14a quality gate.

Цель cleanup:

- убрать двусмысленность вокруг static `nextAction`;
- определить deterministic foundation-only boundary для internal search readiness;
- сохранить public assistant response shape close to Stage 6 direction;
- не создавать fake `hotelSearchRequest`, fake hotel search values или real assistant intelligence.

Stage 7.15 не является generated-client readiness completion, OpenAPI finalization, real hotel search, provider integration, LLM behavior, requirements extraction, natural-language slot filling, frontend work или DB/storage task.

## 2. Проверенные источники

- `docs/prompts/codex-task-template.md`
- `docs/prompts/codex-review-template.md`
- `AGENTS.md`
- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/product/product-baseline.md`
- `docs/architecture/architecture-baseline.md`
- `docs/architecture/backend-layering-rules.md`
- `docs/development/README.md`
- `docs/development/coding-standards.md`
- `docs/development/kotlin-backend-style-guide.md`
- `docs/development/testing-strategy.md`
- `docs/development/documentation-guidelines.md`
- `docs/development/definition-of-done.md`
- `docs/development/quality-gates.md`
- `docs/prompts/README.md`
- `docs/decisions/README.md`
- `docs/reviews/README.md`
- `services/backend/README.md`
- `docs/reviews/stage-7-13-generated-client-openapi-readiness-checkpoint.md`
- `docs/reviews/stage-7-14-generated-client-openapi-readiness-cleanup.md`
- `docs/reviews/stage-7-14-generated-client-openapi-readiness-cleanup-review.md`
- `docs/architecture/stage-6/openapi-draft.yaml`
- `docs/architecture/stage-6/openapi-contract-notes.md`
- `services/backend/src/main/kotlin/**`
- `services/backend/src/test/kotlin/**`
- `git status --short`

Standalone accepted ADR files отсутствуют.

## 3. Использование task/review templates

`docs/prompts/codex-task-template.md` использован как execution structure:

- goal: bounded assistant response semantics / search readiness boundary cleanup;
- expected outcome: minimal application/API semantics change, tests, backend README update and new Stage 7.15 report;
- allowed scope: minimal assistant response semantics boundary, route mapping changes, tests, backend README and this report;
- forbidden scope: generated clients, OpenAPI rewrite/finalization, real hotel search, provider/LLM/frontend/DB/storage work, Stage 7.16+ activation;
- validation: `git status --short`, `git diff --check`, backend `./gradlew test`.

`docs/prompts/codex-review-template.md` использован для self-review:

- проверены scope drift, unrelated changes, roadmap/status consistency, architecture/layering, API/contract consistency, test coverage, documentation/navigation и recommendations not implemented;
- self-review summary включен в этот report.

## 4. Что было реализовано / изменено

Реализовано:

- добавлен application-level `AssistantResponseSemantics` boundary;
- добавлен internal foundation-only `AssistantSearchReadiness`;
- добавлен public-response `AssistantNextAction`;
- public `nextAction` больше не является static default на API DTO;
- API response mapper вычисляет `nextAction` через `AssistantResponseSemantics` из internal `hotelRequirementsCoveragePlan`;
- normal public session creation / message intake остаются `ask_clarification`, потому что public message text не парсится и slots не заполняются;
- internal/test-only complete required slots дают `show_boundary_message`, а не `ready_for_hotel_search`;
- tests добавлены/обновлены для response semantics и отсутствия `hotelSearchRequest`;
- `services/backend/README.md` обновлен с foundation-only Stage 7.15 semantics.

Не реализовано:

- `hotelSearchRequest`;
- real hotel search values;
- natural-language slot filling;
- dynamic clarification text;
- generated clients;
- OpenAPI update.

## 5. Созданные файлы

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantNextAction.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantSearchReadiness.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantResponseSemantics.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/AssistantResponseSemanticsTest.kt`
- `docs/reviews/stage-7-15-assistant-response-semantics-search-readiness-boundary-cleanup.md`

## 6. Изменённые файлы

- `services/backend/src/main/kotlin/com/travelassistant/backend/api/AssistantPlaceholderRoutes.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/api/AssistantSessionRoutesTest.kt`
- `services/backend/README.md`

## 7. Assistant response semantics

Assistant response semantics теперь задаются application-level boundary:

- `AssistantResponseSemantics.searchReadinessFor(...)`;
- `AssistantResponseSemantics.nextActionFor(...)`.

Boundary читает только `HotelRequirementsCoveragePlan`. Он не читает user message text, не извлекает values, не строит search request, не вызывает providers и не зависит от Ktor.

`assistantMessage` остается deterministic placeholder clarification message. Dynamic clarification copy не добавлялся.

## 8. Search readiness boundary

Internal search readiness теперь имеет два foundation-only состояния:

- `MISSING_REQUIRED_INPUTS`;
- `REQUIRED_INPUTS_COLLECTED`.

Readiness вычисляется только из `coveragePlan.requiredHotelSearchInputsComplete`.

Это internal signal, а не production search state. Он не означает, что backend умеет выполнить hotel search, потому что values destination/dates/guests не хранятся и не извлекаются.

## 9. `nextAction` behavior

`nextAction` теперь детерминированно маппится из internal readiness:

- `MISSING_REQUIRED_INPUTS` -> `ask_clarification`;
- `REQUIRED_INPUTS_COLLECTED` -> `show_boundary_message`.

Почему не `ready_for_hotel_search`:

- required slot statuses могут быть internally collected только через explicit structured internal input;
- slot values не существуют;
- public hotel search endpoint остается `501 NOT_IMPLEMENTED`;
- returning `ready_for_hotel_search` мог бы выглядеть как обещание real search readiness.

`show_boundary_message` является safe foundation signal: required statuses internally complete, но real hotel search boundary еще не реализован.

## 10. `hotelSearchRequest` behavior

`hotelSearchRequest` не добавлен в public response.

Причина:

- нет сохраненных destination/date/guest values;
- нет requirements extraction;
- нет natural-language slot filling;
- нет real search/value boundary;
- fake `HotelSearchRequest` создал бы ложную generated-client/OpenAPI readiness.

Tests дополнительно проверяют, что normal public assistant responses не содержат `hotelSearchRequest`.

## 11. Public API behavior

Public response shape остается:

- `session`;
- `assistantMessage`;
- `nextAction`.

Normal public behavior:

- session creation without initial message -> `nextAction = ask_clarification`;
- session creation with optional initial message -> `nextAction = ask_clarification`;
- message intake -> `nextAction = ask_clarification`;
- `hotelSearchRequest` absent;
- internal state absent.

Public message intake не парсит текст и не заполняет slots, поэтому обычные user messages не делают session search-ready.

## 12. Internal state exposure

Internal state остается internal:

- `clarificationState` не возвращается в public DTOs;
- `hotelRequirementsState` не возвращается в public DTOs;
- `hotelRequirementsCoveragePlan` не возвращается в public DTOs;
- `AssistantSearchReadiness` не возвращается в public DTOs;
- internal slot update boundary не подключен к public Ktor routes.

## 13. Что осталось foundation-only

- Process-local assistant session state.
- Static placeholder `assistantMessage`.
- `nextAction` derived from metadata only, not from real assistant intelligence.
- `show_boundary_message` as safe boundary signal only.
- Omitted `hotelSearchRequest`.
- Optional initial message as local intake only.
- Internal `clarificationState`.
- Internal `hotelRequirementsState`.
- Internal `hotelRequirementsCoveragePlan`.
- Internal structured slot status update boundary.
- Placeholder hotel/search/shortlist/explanation endpoints returning `501 NOT_IMPLEMENTED`.
- No generated-client-ready subset.
- No OpenAPI/runtime conformance gate.

## 14. Что намеренно не реализовывалось

- Generated clients.
- Client generation command.
- OpenAPI generation.
- OpenAPI draft rewrite.
- OpenAPI finalization.
- Real hotel search.
- Hotel search request/value construction.
- Hotel offers.
- Ranking.
- Shortlist behavior.
- Explanation/comparison behavior.
- Requirements extraction.
- Natural-language slot filling.
- Dynamic clarification.
- Message history.
- Durable persistence.
- DB/storage.
- Redis.
- Provider integration.
- Provider SDKs.
- LLM orchestration.
- OpenAI or external LLM calls.
- Frontend.
- Booking.
- Payment.
- Flights.
- Product baseline rewrite.
- Architecture baseline rewrite.
- Roadmap redefinition.
- Stage 7.16+ activation.
- Broad documentation cleanup.

## 15. Проверки

- `git status --short` — passed, clean before changes.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` из `services/backend` — passed after initial code changes.
- `git diff --check` — passed.
- `git diff --no-index --check /dev/null <new-file>` — no whitespace error output for new untracked files; command exit code `1` is expected for `--no-index` because the compared files differ.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` из `services/backend` — passed after all changes.
- `git status --short` — reviewed after validation; only scoped Stage 7.15 files changed.

## 16. Known limitations

- Runtime is still not ready for generated clients.
- Placeholder endpoints still return `501 NOT_IMPLEMENTED`.
- No generated-client-ready subset config exists.
- Runtime/OpenAPI error taxonomy is still not final.
- No OpenAPI/runtime conformance gate exists.
- `show_boundary_message` is only a foundation boundary signal, not real search readiness.
- `hotelSearchRequest` remains absent.
- No real hotel search/value/resource semantics exist.
- Roadmap still has older Stage 7.12/7.13+ wording; not changed in this bounded cleanup.

## 17. Self-review summary

Self-review выполнен по `docs/prompts/codex-review-template.md`.

- Scope control: passed; no generated clients, OpenAPI finalization, real search or provider/frontend/storage work added.
- Roadmap alignment: passed; Stage 7.16+ not activated.
- Source-of-truth alignment: passed; checked roadmap, baselines, Stage 7.13, Stage 7.14, Stage 7.14a, Stage 6 OpenAPI notes, backend README and development rules.
- Architecture/layering: passed; response semantics live in application layer and do not depend on Ktor.
- API/contract consistency: passed; `nextAction` semantics are clearer without pretending real search readiness exists.
- Test coverage: passed; application semantics tests added and API tests verify no `hotelSearchRequest`.
- Documentation/navigation: passed; backend README updated narrowly.
- Historical docs: passed; no historical reports were rewritten.

## 18. Recommended next task

Recommended next task: **Stage 7.16 — Generated-client/OpenAPI Conformance Gate Planning**, if explicitly requested.

Suggested bounded focus:

- define generated-client-ready endpoint subset;
- keep placeholder endpoints excluded;
- define a read-only or documentation-level conformance strategy before any client generation.

Alternative next task: a real contract-aligned hotel search request/value boundary, still without provider integration unless explicitly activated.

## 19. Scope control confirmation

- Stage 7.15 completed as bounded assistant response semantics cleanup.
- Public assistant response shape remains `session`, `assistantMessage`, `nextAction`.
- `nextAction` is deterministic and foundation-only.
- `hotelSearchRequest` not added.
- Internal state not exposed.
- OpenAPI draft not changed.
- Generated clients not created.
- No real hotel/search/shortlist/explanation behavior added.
- No provider, LLM, DB/storage, Redis or frontend work added.
- Product baseline and architecture baseline not rewritten.
- Roadmap/order not changed.
- Stage 7.16+ not started.
