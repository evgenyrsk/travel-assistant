# Stage 8.21 — Post-confirmation route integration readiness gate

## Цель Stage 8.21

Проверить, можно ли безопасно подключать `PlanPostConfirmationDecisionUseCase` к assistant route без search creation, либо перед этим нужен отдельный public/runtime contract step.

Stage 8.21 является review/design-only gate. Он не меняет production code, tests, runtime behavior, routes, public API, OpenAPI, frontend или roadmap status.

## Текущая точка входа

- Stage 8.15 уже показывает confirmation prompt через existing public response shape:
  - `nextAction=ask_clarification`;
  - `assistantMessage.content`;
  - без новых public fields;
  - без `hotelSearchId`.
- Stage 8.17 добавил internal process-local `PendingConfirmationStore`.
- Stage 8.19 добавил deterministic `ClassifyConfirmationReplyUseCase`.
- Stage 8.20 добавил `PlanPostConfirmationDecisionUseCase`.
- `PlanPostConfirmationDecisionUseCase`, `PendingConfirmationStore` и confirmation reply classifier не подключены к routes/runtime composition.
- `ProceedWithCandidate -> hotel search` не реализован.
- Stage 7 strict `hotel-search;` handoff остается единственным automatic search creation path.

## Что уже есть после Stage 8.20

Internal foundation:

- `PendingProceedWithCandidateConfirmation`;
- `PendingConfirmationStore`;
- `InMemoryPendingConfirmationStore`;
- `ClassifyConfirmationReplyUseCase`;
- `PlanPostConfirmationDecisionRequest`;
- `PostConfirmationDecision`;
- `PlanPostConfirmationDecisionUseCase`.

`PlanPostConfirmationDecisionUseCase` умеет:

1. искать active pending confirmation по `sessionId` и `now`;
2. возвращать `NoActivePendingConfirmation`, если state отсутствует, expired или consumed;
3. классифицировать reply через `ClassifyConfirmationReplyUseCase`;
4. возвращать typed internal decision;
5. не создавать hotel search или `hotelSearchId`.

## Readiness assessment для route integration

Прямое подключение `PlanPostConfirmationDecisionUseCase` к assistant route для consuming confirmation reply пока **не готово**.

Причины:

- Stage 8.15 показывает prompt, но не сохраняет pending confirmation state;
- без save-step следующий reply почти всегда даст `NoActivePendingConfirmation`;
- route еще не имеет lifecycle policy для `markConsumed`;
- public response не отличает ordinary clarification от confirmation lifecycle;
- `Confirmed(criteria)` не имеет разрешенного search-creation mapping;
- generic `yes` не должен менять behavior без active session-bound state;
- process-local store не является durable или cross-instance механизмом.

Минимально безопасный route-level шаг перед consuming reply — отдельное save-only wiring при показе confirmation prompt.

## Outcome-to-public mapping assessment

| `PostConfirmationDecision` outcome | Future public mapping без search creation | Verdict |
|---|---|---|
| `Confirmed(criteria)` | Не создавать search. Допустим только neutral `ask_clarification` text вроде “Подтверждение получено, но запуск поиска еще не подключен” или defer до search-creation contract step. `hotelSearchId` absent. | Unsafe для полноценного route mapping; conditional only for non-search acknowledgement. |
| `NeedsClarification` | `nextAction=ask_clarification`, safe text asking for explicit confirmation. | Safe if text не раскрывает internal reasons. |
| `Declined` | `nextAction=ask_clarification` или neutral boundary message: “Ок, поиск не запускаю. Можете изменить параметры.” `hotelSearchId` absent. | Safe if future lifecycle handles consumed/cancelled state explicitly. |
| `NeedsReplanning` | `nextAction=ask_clarification`, попросить прислать исправленные criteria. Old criteria не использовать. | Safe if old pending state не используется для search. |
| `NoActivePendingConfirmation` | `nextAction=ask_clarification` или safe boundary message. Не трактовать generic positive reply как confirmation. | Safe. |
| `Unknown` | `nextAction=ask_clarification` или `show_boundary_message`, без raw reason. | Safe. |

Вывод: existing public response shape достаточно для safe text-only clarification/boundary outcomes, но не достаточно для search creation.

## Pending state route wiring assessment

Save-only route wiring при `ConfirmationRequired` выглядит **условно безопасным** для отдельного Stage 8.22, если соблюдены guardrails:

- сохранять только accepted typed `ProceedWithCandidateCriteria` и safe proposal;
- использовать session-bound pending state;
- задавать короткий TTL;
- не создавать search;
- не создавать `hotelSearchId`;
- не подключать consuming reply handling в том же шаге;
- не добавлять public confirmation id или новые fields;
- покрыть route tests для сохранения prompt behavior и отсутствия public shape changes.

Ограничения:

- process-local state теряется при restart;
- parallel instances не синхронизируют pending confirmation;
- отсутствие public confirmation id нормально только пока state не используется для search creation;
- durable semantics не должны заявляться.

## Consuming confirmation reply assessment

Consuming reply route wiring через `PlanPostConfirmationDecisionUseCase` пока **отложен**.

Перед этим нужны:

- save-only pending state wiring при показе prompt;
- lifecycle decision для `markConsumed`;
- tests для missing/expired/consumed state;
- safe public mapping для `Confirmed(criteria)` без search creation;
- проверка, что generic `yes` без active state не меняет behavior;
- отдельное решение, будет ли `Confirmed` только acknowledgement или candidate для future search step.

Даже если consuming будет подключен без search creation, `Confirmed(criteria)` не должен создавать hotel search, `hotelSearchId` или `show_hotel_results`.

## Public contract / OpenAPI / frontend assessment

Existing public response shape достаточно для non-search text outcomes:

- `session`;
- `assistantMessage.content`;
- `nextAction`;
- optional `hotelSearchId`, который должен отсутствовать.

Новые public fields не нужны для save-only pending state wiring.

Existing public contract недостаточен для прозрачного search creation после confirmation:

- нет public confirmation id;
- frontend не видит structured confirmation lifecycle;
- OpenAPI не описывает pending confirmation state;
- `nextAction=ask_clarification` не выражает “confirmed but search not started” как отдельное состояние;
- user-facing search creation требует отдельного contract/runtime review.

OpenAPI, frontend и generated clients не должны меняться в Stage 8.21.

## Stage 7 strict handoff compatibility

Совместимо только если:

- explicit `hotel-search;` остается единственным automatic search creation trigger;
- `Confirmed(criteria)` не создает search;
- save-only pending state не создает `hotelSearchId`;
- consuming reply не подключается без отдельного stage;
- stale/expired/consumed state не может быть переиспользован;
- natural-language LLM path не подменяет deterministic Stage 7 handoff.

## Guardrails for future route wiring

- No search creation from `Confirmed` until explicit search-creation stage.
- No `hotelSearchId` until separate contract/runtime step.
- No `show_hotel_results` for confirmation acknowledgement.
- No confirmed decision without active pending confirmation.
- No generic `yes` without session-bound pending state.
- No stale, expired или consumed criteria reuse.
- No raw `LlmCandidate` storage or public leakage.
- No durable storage claim for process-local state.
- No public field or `nextAction` changes without OpenAPI/frontend step.
- No real provider calls, external calls or access-key configuration.
- Stage 7 strict `hotel-search;` handoff remains the only current automatic search creation path.

## Что не входит в Stage 8.21

- production code;
- tests;
- route wiring;
- изменение `Application.kt`;
- изменение `AssistantLlmRouteWiringUseCase`;
- подключение `PlanPostConfirmationDecisionUseCase` к routes;
- подключение `PendingConfirmationStore` к routes;
- изменение runtime behavior;
- создание hotel search или `hotelSearchId`;
- вызов hotel provider;
- изменение public API request/response shape;
- OpenAPI, frontend, generated clients или CI gate;
- durable storage, auth, booking flow или расширение hotel-only MVP;
- изменение roadmap status.

## Риски преждевременного route wiring / search creation

- `yes` может относиться к другому вопросу.
- Prompt сейчас не сохраняет pending state, поэтому consuming route даст ложное ощущение готовности.
- `Confirmed(criteria)` может быть ошибочно воспринят как разрешение создать search.
- Process-local state может исчезнуть после restart.
- Parallel runtime instances могут видеть разный pending state.
- Stale или consumed criteria могут быть использованы повторно.
- Public contract и frontend не описывают confirmation lifecycle.
- Stage 7 strict `hotel-search;` handoff может быть обойден natural-language path.

## Рекомендуемый Stage 8.22

Stage 8.22 — backend-only save-only pending confirmation route wiring.

Минимальная цель:

- при `ConfirmationRequired` сохранять pending confirmation state;
- не consuming reply;
- не подключать `PlanPostConfirmationDecisionUseCase` к route;
- не создавать search;
- не создавать `hotelSearchId`;
- не менять public response shape, OpenAPI или frontend;
- добавить targeted route/application tests для save-only behavior и отсутствия public changes.

Если перед implementation нужен еще более осторожный шаг, допустим Stage 8.22 как public/runtime contract checkpoint. Немедленное consuming reply wiring или search creation не рекомендуется.

## Verdict

Stage 8.21 подтверждает: direct post-confirmation consuming route integration пока не готова. Safe next step — save-only pending confirmation wiring при показе confirmation prompt, без consuming reply handling и без search creation. Existing public response shape достаточен только для text-only non-search outcomes; search creation после confirmation требует отдельного future contract/runtime step.
