# Stage 8.16 — Post-confirmation handling boundary review

## Цель Stage 8.16

Определить, как в будущем безопасно распознавать явное подтверждение пользователя после confirmation prompt без изменения public contract, без скрытой ветки search creation и без обхода Stage 7 strict `hotel-search;` handoff.

Stage 8.16 является review/design-only шагом. Он не меняет production code, tests, runtime behavior, routes, public API, OpenAPI или frontend.

## Текущая точка входа

- Stage 8.15 отображает `ConfirmationRequired` через existing public response shape:
  - `nextAction=ask_clarification`;
  - `assistantMessage.content`;
  - без новых public fields;
  - без `hotelSearchId`.
- `ProceedWithCandidate -> hotel search` не реализован.
- Search creation после confirmation отложен.
- Stage 7 strict `hotel-search;` handoff остается единственным automatic search creation path.

## Что уже есть после Stage 8.15

Текущий runtime умеет:

- построить confirmation prompt из accepted `ProceedWithCandidate`;
- показать prompt как ordinary clarification text;
- сохранить `hotelSearchId` absent;
- вернуть safe fallback для unsafe candidate;
- сохранить explicit `hotel-search;` priority перед LLM remapping.

Текущий runtime не умеет:

- хранить pending validated criteria;
- отличать ordinary clarification от confirmation state на public contract level;
- распознавать следующий user message как подтверждение;
- создавать search после confirmation.

## Options for explicit confirmation recognition

| Вариант | Assessment |
|---|---|
| Text-only interpretation следующего user message | Небезопасно само по себе: generic "да" / "confirm" / "ищи" не должно создавать search без bounded pending context. |
| Internal pending confirmation state | Рекомендуемый foundation: хранит только validated criteria/proposal state внутри session boundary. |
| Session-bound confirmation marker | Допустимо как internal marker, если связан с конкретной session, criteria snapshot и expiration/consumed rules. |
| Confirmation id | Полезен для будущего structured contract, но без нового public field не должен требоваться для Stage 8.17. |
| Новый public field или `nextAction` | Не входит в текущий путь; потребует отдельного OpenAPI/frontend contract step. |
| Сохранить `hotel-search;` как единственный automatic trigger | Обязательно для текущей совместимости: LLM confirmation не должен становиться silent automatic trigger. |

## Public contract assessment

Текущий response shape достаточно выразителен для prompt:

- `assistantMessage.content` несет text-only confirmation question;
- `nextAction=ask_clarification` уже существует;
- `hotelSearchId` может отсутствовать.

Текущий request/response contract недостаточен для безопасного post-confirmation search creation:

- нет public confirmation state;
- нет public confirmation id;
- нет отдельного `nextAction` для confirmation;
- frontend не отличает confirmation prompt от ordinary clarification;
- generic positive reply нельзя надежно связать с validated criteria без internal pending state.

Verdict: без изменения public contract можно спроектировать internal pending state и future text-only confirmation handling, но нельзя безопасно создавать search только по следующему текстовому "yes" без bounded state.

## State / storage assessment

Без pending state post-confirmation handling небезопасен.

Минимальный safe internal state должен хранить:

- session id;
- accepted typed hotel criteria, а не raw candidate;
- human-readable proposal summary/question, если нужно повторить context;
- created/updated timestamp;
- consumed/expired marker;
- marker, что criteria прошли validation и ожидают explicit confirmation.

State не должен хранить:

- raw `LlmCandidate`;
- extracted constraints map как public-facing source;
- validation issue names;
- internal warnings/conflicts;
- provider/model metadata;
- `hotelSearchId` до фактического future search creation step.

Durable storage не требуется для Stage 8.17 skeleton, но process-local pending state должен явно считаться non-production и теряться при restart. Durable/session persistence остается отдельной будущей задачей.

## Guardrails for future confirmation handling

- No search creation without explicit user confirmation.
- No confirmation handling without pending validated criteria.
- No reuse of stale, consumed or expired criteria.
- No search creation from generic "yes" without bounded session context.
- No raw candidate persistence.
- No `hotelSearchId` before future search creation step.
- No `show_hotel_results` from confirmation prompt itself.
- No bypass of Stage 7 strict `hotel-search;` handoff until a separate contract/runtime step explicitly changes this.
- No new public fields or `nextAction` values without OpenAPI/frontend review.
- No real provider, external calls, durable storage, auth or booking flow.

## Stage 7 strict handoff compatibility

Совместимо только если:

- `hotel-search;` остается единственным current automatic search creation trigger;
- pending confirmation state не создает search сам по себе;
- confirmation reply сначала проходит bounded session/pending-state validation;
- future search creation выделяется в отдельный implementation stage с route tests;
- ordinary natural-language messages без pending state продолжают идти через clarification/fallback path.

## Что не входит в Stage 8.16

- production code;
- tests;
- route wiring;
- изменение `Application.kt`;
- изменение `AssistantLlmRouteWiringUseCase`;
- изменение runtime behavior;
- pending confirmation model implementation;
- распознавание confirmation reply;
- создание hotel search или `hotelSearchId`;
- изменение public request/response shape;
- OpenAPI, frontend, generated clients или CI gate;
- внешний LLM-сервис, network calls, ключи доступа или provider-specific настройки;
- durable storage, auth, booking flow или расширение hotel-only MVP.

## Риски преждевременного search creation

- Generic "yes" может случайно запустить search для неверного или старого prompt.
- Пользователь мог ответить на другой вопрос, а не подтвердить hotel search.
- Process-local session может потерять context, если pending state не определен явно.
- Raw/internal candidate details могут попасть в state или public response.
- Search может быть создан без OpenAPI/frontend readiness.
- Stage 7 strict `hotel-search;` guardrail может быть обойден LLM path.
- Bounded hotel-only MVP может незаметно расшириться до general travel planning.

## Рекомендуемый Stage 8.17

Stage 8.17 — backend-only internal pending confirmation state model skeleton без route wiring и без search creation.

Минимальная цель:

- определить internal pending confirmation model;
- хранить только validated hotel criteria/proposal data;
- добавить typed status: pending / consumed / expired;
- добавить deterministic tests для stale/consumed/no-raw-candidate boundaries;
- не подключать model к routes;
- не создавать `hotelSearchId`;
- не менять public API, OpenAPI или frontend.

Immediate post-confirmation search creation не рекомендуется.

## Verdict

Пройдено с ограничениями.

Explicit confirmation recognition небезопасен как plain text-only interpretation без pending validated criteria. Текущий public contract может нести prompt, но не является достаточным contract для safe search creation после generic positive reply. Следующий безопасный шаг — internal pending confirmation state skeleton без route wiring, public contract changes или hotel search creation.
