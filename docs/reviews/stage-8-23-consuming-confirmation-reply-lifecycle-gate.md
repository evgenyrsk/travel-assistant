# Stage 8.23 — Consuming confirmation reply lifecycle gate

## Цель Stage 8.23

Определить, когда и как безопасно подключать `PlanPostConfirmationDecisionUseCase` к assistant route для обработки confirmation reply, как выполнять `markConsumed`, и какой public response возвращать для `Confirmed(criteria)` без search creation.

Stage 8.23 является review/design-only gate. Он не меняет production code, tests, runtime behavior, routes, public API, OpenAPI, frontend, generated clients или roadmap/root status files.

## Текущая точка входа

- Stage 8.15 отображает confirmation prompt через existing public response shape:
  - `nextAction=ask_clarification`;
  - `assistantMessage.content`;
  - без новых public fields;
  - без `hotelSearchId`.
- Stage 8.17 добавил process-local `PendingConfirmationStore`.
- Stage 8.19 добавил conservative `ClassifyConfirmationReplyUseCase`.
- Stage 8.20 добавил `PlanPostConfirmationDecisionUseCase`.
- Stage 8.22 сохраняет pending confirmation state при `ConfirmationRequired`.
- `PlanPostConfirmationDecisionUseCase` пока не подключен к routes.
- Confirmation reply consuming пока не реализован.
- Search creation после confirmation не реализован.
- Stage 7 strict `hotel-search;` handoff остается единственным automatic search creation path.

## Что уже есть после Stage 8.22

Route-level prompt path уже умеет:

1. получить `ProceedWithCandidate`;
2. построить `ProceedWithCandidateConfirmationPlan`;
3. для `ConfirmationRequired` сохранить `PendingProceedWithCandidateConfirmation`;
4. вернуть text-only confirmation prompt;
5. оставить `hotelSearchId` absent.

Saved pending state содержит только safe internal data:

- `sessionId`;
- typed `ProceedWithCandidateCriteria`;
- safe `ProceedWithCandidateConfirmationProposal`;
- `createdAt`;
- `updatedAt`;
- `expiresAt`;
- `PENDING` status.

Saved state не содержит raw `LlmCandidate`, raw validation details, provider/model metadata, hotel search request DTO или `hotelSearchId`.

## Readiness assessment для consuming confirmation reply handling

Verdict: **условно готово только для отдельного backend-only non-search wiring step**.

После Stage 8.22 route уже может иметь active pending state, поэтому future route integration может безопасно вызывать `PlanPostConfirmationDecisionUseCase`, если соблюдены условия:

- consuming path выполняется только при active pending confirmation для текущей session;
- generic positive reply без active pending state не дает `Confirmed`;
- `Confirmed(criteria)` не создает search;
- `Confirmed(criteria)` не создает `hotelSearchId`;
- `Confirmed(criteria)` не возвращает `show_hotel_results`;
- route mapping остается text-only через existing public response shape;
- lifecycle state меняется только после final internal decision;
- Stage 7 `hotel-search;` handoff остается единственным automatic search creation trigger.

Route integration **не готова** для search creation после confirmation. Для этого нужен отдельный future contract/runtime step.

## Lifecycle rules для pending confirmation

| Ситуация | Future lifecycle decision | Обоснование |
|---|---|---|
| `Confirmed(criteria)` | Mark consumed после final decision и перед возвратом safe non-search response. | Повторное “да” не должно повторно использовать те же criteria. |
| `Declined` | Mark consumed. | Пользователь явно отменил предложенные criteria. |
| `NeedsReplanning` | Mark consumed. | Пользователь меняет criteria; old pending state больше не должен быть active. |
| `NeedsClarification` | Не mark consumed. | Reply ambiguous; пользователь может уточнить или подтвердить до expiry. |
| `Unknown` | Не mark consumed. | Reply не является явным lifecycle signal; pending state остается active до expiry. |
| `NoActivePendingConfirmation` | Не mark consumed. | Active state отсутствует, expired или уже consumed. |
| Expired state | Не создавать search; попросить повторить параметры или заново пройти confirmation. | `findActiveBySession` не возвращает expired state. |
| Missing state | Не создавать search; использовать safe clarification/boundary response. | Generic “yes” без active context не является confirmation. |
| Already consumed state | Не создавать search; не переиспользовать criteria. | Consumed state не active. |

Ambiguous reply может повторно использовать pending state только до TTL. После expiry пользователь должен заново предоставить или подтвердить criteria.

## `markConsumed` decision rules

`markConsumed` в future wiring должен вызываться только после того, как:

1. route нашел active pending confirmation;
2. `PlanPostConfirmationDecisionUseCase` вернул final decision;
3. route выбрал safe public mapping без search creation.

`markConsumed` не должен вызываться:

- до classification/decision;
- при missing, expired или already consumed state;
- при `NeedsClarification`;
- при `Unknown`;
- как side effect для ordinary natural-language message без active pending state;
- как часть search creation, потому что search creation остается вне scope.

Для Stage 8.24 достаточно process-local semantics: `markConsumed` меняет status на `CONSUMED` и обновляет `updatedAt`. Durable или cross-instance guarantees не заявляются.

## Outcome-to-public mapping assessment

| `PostConfirmationDecision` outcome | Safe future public mapping без search creation | Lifecycle |
|---|---|---|
| `Confirmed(criteria)` | `nextAction=ask_clarification`; `assistantMessage.content` подтверждает, что confirmation получено, но automatic search start еще не подключен; `hotelSearchId` absent. | Mark consumed. |
| `NeedsClarification` | `nextAction=ask_clarification`; попросить явное confirmation, cancel или corrected criteria. | Keep pending active. |
| `Declined` | `nextAction=ask_clarification`; neutral text: поиск не запускается, можно прислать новые параметры. | Mark consumed. |
| `NeedsReplanning` | `nextAction=ask_clarification`; попросить прислать corrected destination/dates/guests/rooms. | Mark consumed old state. |
| `NoActivePendingConfirmation` | `nextAction=ask_clarification` или `show_boundary_message`; объяснить, что active confirmation нет, и попросить повторить hotel request. | No state change. |
| `Unknown` | `nextAction=ask_clarification` или `show_boundary_message`; не раскрывать internal reason и не создавать search. | Keep pending active until expiry. |

Для `Confirmed(criteria)` предпочтительный Stage 8.24 mapping — `ask_clarification`, потому что existing public contract не содержит отдельного acknowledged/confirmed action. `show_boundary_message` остается допустимым fallback, если wording должен явно показать feature boundary.

## Public contract / OpenAPI / frontend assessment

Existing public response shape достаточен для non-search consuming reply handling:

- `session`;
- `assistantMessage.content`;
- `nextAction`;
- optional `hotelSearchId`, который должен отсутствовать.

Новые public fields, новые `nextAction` values, OpenAPI changes или frontend changes не нужны для Stage 8.24, если он остается non-search.

Existing public contract недостаточен для прозрачного search creation после confirmation:

- frontend не видит structured confirmation lifecycle;
- public contract не содержит отдельный confirmation id;
- `nextAction=ask_clarification` не выражает search-start acknowledgement как отдельный state;
- search creation после `Confirmed(criteria)` требует отдельного future contract/runtime review.

## Process-local / in-memory limitation

`InMemoryPendingConfirmationStore` остается process-local.

Ограничения:

- pending state теряется при process restart;
- parallel runtime instances не разделяют pending state;
- отсутствует durable lifecycle audit;
- отсутствует cross-device/session ownership beyond current `sessionId`;
- TTL является internal safety guard, а не продуктовой гарантией.

Эти ограничения допустимы для Stage 8.24 non-search consuming wiring, но недостаточны для утверждения надежного post-confirmation search flow.

## Stage 7 strict handoff compatibility

Совместимо, если future wiring сохраняет правила:

- `hotel-search;` остается единственным automatic search creation path;
- `Confirmed(criteria)` не создает search;
- `Confirmed(criteria)` не создает `hotelSearchId`;
- `Confirmed(criteria)` не возвращает `show_hotel_results`;
- old, stale, expired или consumed criteria не используются;
- ordinary natural-language path не обходит Stage 7 strict handoff.

## Guardrails for future consuming wiring

- No search creation from `Confirmed` until separate explicit search-creation stage.
- No `hotelSearchId` for confirmation acknowledgement.
- No `show_hotel_results` for confirmation acknowledgement.
- No hotel provider call from consuming confirmation reply handling.
- No `Confirmed` without active pending state for the same session.
- No generic “yes” outside active pending context.
- No stale, expired или consumed criteria reuse.
- No `markConsumed` before final decision and safe response mapping.
- No raw `LlmCandidate`, validation issue details, provider/model metadata or internal reasons in public text.
- No public field or `nextAction` changes without OpenAPI/frontend step.
- No durable-storage claim for process-local state.
- Stage 7 strict `hotel-search;` handoff remains the only current automatic search creation path.

## Что не входит в Stage 8.23

- production code;
- tests;
- route wiring;
- изменение `Application.kt`;
- изменение `AssistantLlmRouteWiringUseCase`;
- подключение `PlanPostConfirmationDecisionUseCase` к routes;
- consuming confirmation reply handling;
- вызов `markConsumed`;
- создание hotel search или `hotelSearchId`;
- вызов hotel provider;
- изменение public API request/response shape;
- OpenAPI, frontend, generated clients или CI gate;
- durable storage, auth, booking flow или расширение hotel-only MVP;
- изменение roadmap/root status files.

## Риски преждевременного consuming / search creation

- Generic “yes” может быть ответом на другой assistant question.
- Process-local state может исчезнуть между prompt и reply.
- Parallel instances могут видеть разные pending states.
- `Confirmed(criteria)` можно ошибочно трактовать как разрешение создать search.
- Если `markConsumed` вызвать слишком рано, пользователь потеряет возможность уточнить ambiguous reply.
- Если `markConsumed` не вызвать для confirmed/declined/replanning, stale criteria могут остаться active.
- Public contract не показывает frontend structured confirmation lifecycle.
- Natural-language path может случайно обойти Stage 7 strict handoff.

## Рекомендуемый Stage 8.24

Stage 8.24 — backend-only consuming confirmation reply route wiring, no search creation.

Минимальная цель:

- подключить `PlanPostConfirmationDecisionUseCase` к route/runtime composition только для active pending confirmation;
- вернуть safe public text outcomes через existing response shape;
- вызвать `markConsumed` для `Confirmed`, `Declined` и `NeedsReplanning`;
- не вызывать `markConsumed` для `NeedsClarification`, `Unknown` и `NoActivePendingConfirmation`;
- не создавать search;
- не создавать `hotelSearchId`;
- не возвращать `show_hotel_results`;
- сохранить Stage 7 strict `hotel-search;` handoff;
- добавить targeted route/application tests для lifecycle behavior.

Search creation после `Confirmed(criteria)` должен остаться отдельным future stage.

## Verdict

Stage 8.23 подтверждает conditional readiness для future non-search consuming confirmation reply wiring. Safe Stage 8.24 может подключить `PlanPostConfirmationDecisionUseCase` к routes только для active pending state, с `markConsumed` на confirmed/declined/replanning outcomes и без search creation. Public API shape, OpenAPI и frontend достаточны только для text-only non-search outcomes. `hotelSearchId`, `show_hotel_results` и hotel provider остаются вне confirmation reply lifecycle до отдельного future search-creation stage.
