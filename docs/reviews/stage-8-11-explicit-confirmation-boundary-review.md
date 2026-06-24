# Stage 8.11 — Explicit confirmation boundary review

## Цель Stage 8.11

Определить, нужен ли явный пользовательский шаг подтверждения перед будущим `ProceedWithCandidate -> hotel search`, и можно ли выразить такой шаг через текущий public assistant contract без OpenAPI и frontend changes.

Stage 8.11 является review/design-only шагом. Он не меняет code, tests, runtime behavior, routes, public API, OpenAPI или frontend.

## Текущая точка входа

- Stage 8.8 подключил LLM pipeline к Assistant runtime только для `AskClarification` и `Fallback`.
- `ProceedWithCandidate` сейчас возвращает safe boundary message и не создает search.
- Stage 8.10 добавил internal `ProceedWithCandidateCriteriaValidator`, но он не подключен к routes или runtime composition.
- Stage 7 strict `hotel-search;` handoff остается единственным автоматическим путем создания search.
- Внешний LLM-сервис, внешние вызовы и ключи доступа не добавлены.

## Что уже есть после Stage 8.10

Internal path доступен только как набор внутренних building blocks:

```text
ProceedWithCandidate
  -> ProceedWithCandidateCriteriaValidator
  -> ProceedWithCandidateValidationResult
```

Validator может отличить complete candidate от partial/unsafe candidate, но не создает `hotelSearchId`, не вызывает hotel provider и не формирует public response.

## Explicit confirmation decision

Verdict: explicit confirmation нужен.

Безопасная политика:

| Вариант | Решение |
|---|---|
| Не разрешать `ProceedWithCandidate -> hotel search` без confirmation | Принять как обязательное правило. |
| Разрешить только после typed criteria validation + confirmation | Условно допустимый future path. |
| Оставить `hotel-search;` как единственный automatic trigger | Сохранить для текущего runtime. |
| Разрешить LLM handoff только как proposal/summary | Рекомендованный следующий шаг перед search creation. |

Даже accepted criteria из Stage 8.10 не должны автоматически запускать search. Они могут стать только основанием для user-facing confirmation question.

## Public contract assessment

Текущий public response shape:

- `session`;
- `assistantMessage`;
- `nextAction`;
- optional `hotelSearchId`.

Текущий contract достаточно выразителен для текстового confirmation question:

- `nextAction=ask_clarification`;
- `assistantMessage.content` содержит human-readable summary и вопрос подтверждения;
- `hotelSearchId` отсутствует.

Текущий contract недостаточен для structured criteria summary:

- нет public field для normalized criteria summary;
- нет public action value вроде `confirm_hotel_search`;
- нет способа машинно отличить ordinary clarification от confirmation prompt;
- frontend может трактовать это как обычный clarification state.

Поэтому первый future implementation не должен добавлять structured public confirmation без отдельного OpenAPI/frontend contract step.

## Safe user-facing summary guidance

Если future step будет формировать confirmation text, безопасно включать только validated fields:

- destination;
- check-in;
- check-out;
- adults;
- children, если значение явно есть или безопасно равно 0;
- rooms;
- budget или preferences только если они уже валидированы и явно отделены от provider facts.

Формат должен быть human-readable, например:

```text
Я могу искать отели в Rome для 2 взрослых, 1 ребенка и 1 номера с 2026-07-01 по 2026-07-04. Запустить этот hotel search?
```

Недопустимо включать:

- internal candidate object;
- validator issue list;
- warnings/conflicts как raw internal values;
- confidence score, если он не является public contract;
- provider facts, prices, availability, ratings или amenities;
- assumptions как подтвержденные user constraints.

## Raw candidate leakage boundary

Raw `LlmCandidate` не должен попадать в public response.

Future summary должен строиться из отдельной internal confirmation proposal model, а не из прямой сериализации candidate. Такая model должна содержать только подтверждаемые поля и не раскрывать internal reasons, warnings или extraction details.

## Stage 7 strict handoff compatibility

Stage 7 compatibility сохраняется при таких условиях:

- `hotel-search;` остается единственным current automatic search creation trigger;
- accepted LLM criteria превращаются только в proposal/confirmation question;
- search creation после user confirmation выделяется в отдельный future stage;
- тихое search creation из LLM candidate запрещено;
- `hotelSearchId` появляется только после явного future implementation step с отдельными tests и contract review.

## Что не входит в Stage 8.11

- production code;
- tests;
- route wiring;
- подключение criteria validator к runtime;
- создание hotel search или `hotelSearchId`;
- изменение Assistant routes или hotel-search handoff;
- изменение public request/response shape;
- OpenAPI, frontend, generated clients или CI gate;
- внешний LLM-сервис, внешние вызовы, ключи доступа или provider-specific настройки;
- durable storage, auth, booking flow или расширение hotel-only MVP.

## Риски преждевременного search creation

- Search по неверно извлеченным destination/dates/guests/rooms.
- Создание `hotelSearchId` без явного согласия пользователя.
- Подмена strict `hotel-search;` handoff скрытым LLM path.
- Расхождение runtime behavior с OpenAPI и frontend assumptions.
- Раскрытие internal candidate details через public response.
- Смешивание assumptions с user-provided constraints.
- Размывание hotel-only MVP в общий travel planning.

## Рекомендуемый Stage 8.12

Stage 8.12 — backend-only internal confirmation proposal model skeleton без route wiring.

Минимальная цель:

- принять `ProceedWithCandidateValidationResult.Accepted`;
- создать internal human-readable confirmation proposal;
- не создавать search;
- не менять public API;
- покрыть summary fields, omission rules и raw-candidate leakage targeted tests.

Немедленное route wiring или search creation не рекомендуются до отдельного contract/runtime readiness step.

## Verdict

Пройдено с ограничениями.

Explicit confirmation обязателен перед future `ProceedWithCandidate -> hotel search`. Текущий public contract может выразить confirmation как `ask_clarification` + text-only `assistantMessage.content`, но не поддерживает structured confirmation summary без отдельного contract step. Stage 7 strict handoff остается совместимым только при запрете silent search creation from LLM candidate.
