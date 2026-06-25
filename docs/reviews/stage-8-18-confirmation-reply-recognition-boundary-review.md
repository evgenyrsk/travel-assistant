# Stage 8.18 — Confirmation reply recognition boundary review

## Цель Stage 8.18

Определить, как в будущем безопасно распознавать явное подтверждение пользователя только при active pending confirmation state.

Stage 8.18 является review/design-only шагом. Он не меняет production code, tests, runtime behavior, routes, public API, OpenAPI или frontend.

## Текущая точка входа

- Stage 8.15 показывает confirmation prompt через existing public response shape:
  - `nextAction=ask_clarification`;
  - `assistantMessage.content`;
  - без новых public fields;
  - без `hotelSearchId`.
- Stage 8.17 добавил internal process-local `PendingConfirmationStore`.
- Pending confirmation state не подключен к routes или runtime composition.
- `ProceedWithCandidate -> hotel search` не реализован.
- Stage 7 strict `hotel-search;` handoff остается единственным automatic search creation path.

## Что уже есть после Stage 8.17

Internal foundation после Stage 8.17:

- `PendingProceedWithCandidateConfirmation`;
- `PendingConfirmationStatus`;
- `PendingConfirmationStore`;
- `InMemoryPendingConfirmationStore`;
- expiry/consumed behavior;
- process-local, non-durable limitation;
- отсутствие raw candidate storage.

Текущий runtime не умеет:

- сохранять pending confirmation при показе prompt;
- читать pending confirmation при следующем user message;
- классифицировать confirmation reply;
- создавать search после confirmation.

## Правила распознавания explicit confirmation

Положительный ответ можно считать explicit confirmation только если он:

- короткий и явно подтверждающий;
- относится к active pending confirmation в той же session;
- не содержит новых критериев, исправлений или вопроса;
- не требует raw candidate;
- не приводит к search creation сам по себе.

Примеры допустимых positive replies для будущего classifier:

- `да`;
- `подтверждаю`;
- `ищи`;
- `yes`;
- `confirm`;
- `ок, ищи`;
- `да, проверь отели`.

Не должны считаться достаточным подтверждением:

- ambiguous short replies: `ок`, `угу`, `давай`, `go`;
- negative replies: `нет`, `не надо`, `cancel`, `no`;
- correction replies: `нет, лучше Париж`, `измени даты`, `для троих`;
- mixed replies с новыми criteria;
- positive reply без active pending state.

## Требования к active pending state

Recognition допустим только если:

- для session найден active pending confirmation;
- state не expired;
- state не consumed;
- state содержит accepted typed `ProceedWithCandidateCriteria`;
- criteria уже прошли validation;
- state не хранит raw `LlmCandidate`;
- confirmation proposal был построен из safe fields;
- reply достаточно explicit;
- reply не содержит corrections или changed criteria.

Если любое условие не выполнено, future runtime не должен создавать hotel search или `hotelSearchId`.

## Outcome handling matrix

| Input | Pending state | Future internal outcome | Public/runtime implication |
|---|---|---|---|
| Explicit positive reply | Active, not expired, not consumed | `Confirmed` candidate for future post-confirmation step | Не создавать search в Stage 8.18; future search creation требует отдельного runtime stage. |
| Ambiguous positive reply | Active | `NeedsClarification` | Спросить явное подтверждение или попросить уточнить; `hotelSearchId` absent. |
| Negative reply | Active | `Declined` / `Cancelled` | Mark consumed только в будущем implementation step; search не создавать. |
| Correction reply | Active | `CorrectionRequired` | Не использовать old criteria; перейти к clarification/re-planning path. |
| Positive reply | Missing active state | `NoPendingConfirmation` | Не создавать search; трактовать как ordinary message или safe clarification. |
| Any reply | Expired state | `Expired` | Не создавать search; попросить заново подтвердить или повторить критерии. |
| Any reply | Consumed state | `AlreadyConsumed` | Не создавать повторный search из stale confirmation. |

Typed outcome names здесь являются design vocabulary для будущей internal модели, а не public DTO и не OpenAPI values.

## Public contract / OpenAPI / frontend assessment

Текущий public response shape достаточен для text-only follow-up:

- `assistantMessage.content`;
- existing `nextAction=ask_clarification`;
- optional `hotelSearchId`, который должен отсутствовать до фактического future search creation.

Текущий public contract недостаточен для безопасного search creation после confirmation reply:

- нет public confirmation id;
- нет structured pending state в response;
- frontend не отличает ordinary clarification от confirmation prompt;
- generic positive reply нельзя надежно привязать к criteria без internal state;
- search creation после confirmation потребует отдельного route/runtime review и tests.

Новый `nextAction`, public confirmation id или frontend state handling не входят в Stage 8.18.

## Stage 7 strict handoff compatibility

Совместимо только если:

- Stage 7 explicit `hotel-search;` остается единственным текущим automatic search creation trigger;
- positive natural-language reply не создает search без active pending state;
- active pending state сам по себе не создает search;
- future confirmation recognition возвращает internal decision, а не public search result;
- future search creation выделяется в отдельный stage с route tests и contract review.

## Что не входит в Stage 8.18

- production code;
- tests;
- route wiring;
- изменение `Application.kt`;
- изменение `AssistantLlmRouteWiringUseCase`;
- подключение `PendingConfirmationStore` к routes;
- изменение runtime behavior;
- создание hotel search или `hotelSearchId`;
- изменение public request/response shape;
- OpenAPI, frontend, generated clients или CI gate;
- внешний LLM-сервис, network calls, ключи доступа или provider-specific настройки;
- durable storage, auth, booking flow или расширение hotel-only MVP.

## Риски преждевременного search creation

- Generic `да` может относиться к другому вопросу.
- Пользователь может исправить criteria внутри positive-looking reply.
- Expired или consumed state может быть повторно использован.
- Frontend не имеет structured confirmation state.
- Search может быть создан без OpenAPI/frontend readiness.
- Stage 7 strict `hotel-search;` guardrail может быть обойден LLM path.
- Process-local state не переживает restart и не является production persistence.

## Рекомендуемый Stage 8.19

Stage 8.19 — backend-only internal confirmation reply classifier skeleton без route wiring и без search creation.

Минимальная цель:

- добавить internal input/result model для confirmation reply classification;
- классифицировать positive / ambiguous / negative / correction / no active state;
- требовать active pending confirmation как input precondition;
- добавить deterministic targeted tests;
- не подключать classifier к routes;
- не создавать `hotelSearchId`;
- не менять public API, OpenAPI или frontend.

Немедленное создание search после confirmation не рекомендуется.

## Verdict

Пройдено с ограничениями.

Explicit confirmation recognition допустим только как internal classification поверх active pending confirmation state. Positive text без active pending state, ambiguous reply, correction reply, expired state или consumed state не должны создавать hotel search. Stage 8.18 не разрешает route wiring, public contract changes, создание `hotelSearchId` или обход Stage 7 strict `hotel-search;` handoff.
