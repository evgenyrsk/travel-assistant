# Stage 8.22 — Save-only pending confirmation route wiring

## Цель Stage 8.22

Подключить process-local `PendingConfirmationStore` к confirmation prompt route path только для сохранения pending confirmation state.

Stage 8.22 не обрабатывает следующий confirmation reply, не подключает `PlanPostConfirmationDecisionUseCase`, не создает hotel search, не создает `hotelSearchId` и не меняет public response shape.

## Что было изменено

- `AssistantLlmRouteWiringUseCase` получил save-only dependency на `PendingConfirmationStore`.
- `ConfirmationRequired` теперь сохраняет pending confirmation state перед возвратом existing public `ask_clarification` response.
- `ProceedWithCandidateConfirmationPlan.ConfirmationRequired` несет accepted typed `ProceedWithCandidateCriteria`, чтобы pending state не нуждался в raw `LlmCandidate`.
- `Application.kt` создает process-local `InMemoryPendingConfirmationStore` и передает его в assistant LLM route wiring.
- `moduleWithAssistantLlm` получил internal test seam для deterministic `PendingConfirmationStore` и `Clock`.

## Production files

Изменены:

- `services/backend/src/main/kotlin/com/travelassistant/backend/Application.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantLlmRouteWiringUseCase.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ProceedWithCandidateConfirmationPlan.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/PlanProceedWithCandidateConfirmationUseCase.kt`.

Новые production files не создавались.

## Tests

Изменены:

- `services/backend/src/test/kotlin/com/travelassistant/backend/api/AssistantSessionRoutesTest.kt`;
- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/PlanProceedWithCandidateConfirmationUseCaseTest.kt`.

Покрытие добавлено или усилено для:

- `ConfirmationRequired` сохраняет active pending confirmation для текущей session;
- сохраненный state содержит typed criteria и safe proposal;
- public response остается `nextAction=ask_clarification`;
- public response не содержит `hotelSearchId`;
- `ClarificationRequired` не сохраняет pending state;
- `Fallback` не сохраняет pending state;
- explicit `hotel-search;` handoff не сохраняет pending confirmation и по-прежнему создает search как Stage 7 behavior;
- reply `да` после prompt не consuming pending state и не создает search;
- saved state не содержит raw candidate, provider/model metadata, validation issues или `hotelSearchId`.

## Save-only wiring flow

Flow для complete safe `ProceedWithCandidate`:

1. Assistant route принимает user message.
2. Existing Stage 7 explicit `hotel-search;` guard проверяется до LLM path.
3. LLM decision дает `ProceedWithCandidate`.
4. `PlanProceedWithCandidateConfirmationUseCase` возвращает `ConfirmationRequired(criteria, proposal)`.
5. `AssistantLlmRouteWiringUseCase` сохраняет `PendingProceedWithCandidateConfirmation`.
6. Public response остается text-only confirmation prompt:
   - `assistantMessage.content`;
   - `nextAction=ask_clarification`;
   - без `hotelSearchId`;
   - без новых public fields.

Save не выполняется для `ClarificationRequired`, `Fallback` или explicit Stage 7 `hotel-search;` handoff.

## Saved pending state content

Сохраняется только internal safe data:

- current `AssistantSessionId`;
- accepted typed `ProceedWithCandidateCriteria`;
- safe `ProceedWithCandidateConfirmationProposal`;
- `createdAt`;
- `updatedAt`;
- `expiresAt`;
- `PENDING` status.

Не сохраняется:

- raw `LlmCandidate`;
- raw candidate payload;
- `candidatePayload`;
- `modelResponse`;
- raw validation issues;
- provider/model metadata;
- `hotelSearchId`;
- public hotel search request DTO.

## TTL / expiry behavior

Stage 8.22 использует minimal internal TTL:

- `15 minutes`;
- `createdAt` и `updatedAt` берутся из injected `Clock`;
- `expiresAt = createdAt + 15 minutes`;
- tests используют fixed `Clock`, а не real time.

Expiry mechanics остаются в `PendingProceedWithCandidateConfirmation` / `PendingConfirmationStore`: active state возвращается только пока status `PENDING` и current time раньше `expiresAt`.

## Confirmation reply consuming boundary

Stage 8.22 не:

- читает следующий user reply как confirmation;
- вызывает `PlanPostConfirmationDecisionUseCase`;
- вызывает `ClassifyConfirmationReplyUseCase`;
- вызывает `PendingConfirmationStore.markConsumed`;
- возвращает `Confirmed`;
- меняет response для generic `yes`.

Reply consuming остается future step.

## Hotel search creation boundary

Confirmation prompt path не:

- создает hotel search;
- создает `hotelSearchId`;
- вызывает hotel provider;
- создает `CreateHotelSearchCommand`;
- возвращает `show_hotel_results`.

Stage 7 explicit `hotel-search;` handoff остается единственным automatic search creation path.

## Public API / OpenAPI / frontend / generated clients verdict

- Public API request/response shape не изменен.
- Новые public fields не добавлены.
- Новые `nextAction` values не добавлены.
- OpenAPI contracts не менялись.
- Frontend не менялся.
- Generated clients не создавались и не обновлялись.

## Stage 7 strict handoff compatibility

Совместимо.

Stage 8.22 сохраняет Stage 7 behavior:

- explicit complete `hotel-search;` request создает search;
- confirmation prompt не создает search;
- save-only pending state не подменяет deterministic handoff;
- LLM path не получает automatic search creation.

## Durable storage limitation

`InMemoryPendingConfirmationStore` остается process-local.

Stage 8.22 не добавляет:

- database;
- filesystem persistence;
- Redis/cache service;
- cross-instance synchronization;
- account/user ownership;
- recovery after restart.

Это не durable storage и не production persistence claim.

## Real provider / network / API keys verdict

Stage 8.22 не добавляет:

- real LLM provider;
- real hotel provider;
- network calls;
- API keys, secrets или environment variables;
- provider-specific configuration.

## Риски и ограничения

- Pending confirmation теряется при process restart.
- Parallel runtime instances не разделяют pending state.
- Public contract не сообщает frontend о structured confirmation lifecycle.
- Следующий reply пока не consuming state.
- `Confirmed(criteria)` и search creation остаются будущими отдельными steps.
- TTL выбран минимально для internal safety, но не является product-level expiry policy.

## Рекомендуемый Stage 8.23

Stage 8.23 — review/design-only lifecycle gate для consuming confirmation reply.

Минимальная цель:

- определить, когда можно подключать `PlanPostConfirmationDecisionUseCase` к route;
- зафиксировать safe public response для `Confirmed(criteria)` без search creation;
- решить, когда и как вызывать `markConsumed`;
- подтвердить missing/expired/consumed behavior;
- сохранить запрет на hotel search creation до отдельного future search-creation stage.

Immediate post-confirmation search creation не рекомендуется.

## Verdict

Stage 8.22 выполнен как backend-only save-only route wiring. Pending confirmation state сохраняется только при `ConfirmationRequired`, public response shape не изменен, confirmation reply consuming не добавлен, `PlanPostConfirmationDecisionUseCase` не подключен к routes, hotel search и `hotelSearchId` из confirmation prompt не создаются, Stage 7 strict `hotel-search;` handoff сохранен.
