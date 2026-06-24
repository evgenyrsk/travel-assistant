# Stage 8.9 — ProceedWithCandidate criteria contract review

## Цель Stage 8.9

Проверить будущий путь `ProceedWithCandidate -> hotel search` до любой реализации.

Stage 8.9 является review/design-only шагом. Он не меняет код, тесты, runtime behavior, routes, public API, OpenAPI, frontend или roadmap status.

## Текущая точка входа

- Stage 8.8 подключил internal LLM pipeline к Assistant runtime только для `AskClarification` и `Fallback`.
- `ProceedWithCandidate` сейчас не создает hotel search, не возвращает `hotelSearchId` и не раскрывает internal candidate в public response.
- Stage 7 strict `hotel-search;` handoff остается единственным путем, который автоматически создает hotel search из Assistant message.
- Runtime использует deterministic fake path; внешний LLM-сервис, внешние вызовы и ключи доступа не добавлены.

## Что уже есть после Stage 8.8

Текущий internal путь:

```text
LlmCandidateRequest
  -> GenerateLlmCandidateUseCase
  -> PlanAssistantLlmDecisionUseCase
  -> AssistantCandidateDecision
  -> AssistantLlmRouteWiringUseCase
```

Текущий public mapping:

| Internal decision | Текущий public outcome |
|---|---|
| `AskClarification` | `nextAction=ask_clarification`, clarification text в `assistantMessage.content`, без `hotelSearchId`. |
| `Fallback` | `nextAction=show_boundary_message`, safe boundary message, без `hotelSearchId`. |
| `ProceedWithCandidate` | `nextAction=show_boundary_message`, hotel search не создается, internal candidate не раскрывается. |

## Current ProceedWithCandidate behavior

`ProceedWithCandidate` означает только то, что `LlmCandidateValidator` принял structured candidate как внутренне согласованный.

Этого пока недостаточно для hotel search:

- candidate хранит `extractedConstraints` как `Map<String, String>`, а не typed `HotelSearchCriteria`;
- нет отдельной проверки hotel-search required fields;
- нет проверки domain-форматов для дат, гостей и комнат;
- нет confidence marker или explicit safety marker;
- `warnings` не превращены в blocking/non-blocking policy;
- user confirmation перед созданием search не определен.

## Required hotel-search criteria

Для будущего handoff обязательными должны быть:

| Критерий | Правило |
|---|---|
| Intent | Только `HOTEL_SEARCH`; `UNKNOWN` или `UNSUPPORTED` не могут создавать search. |
| Outcome | Только complete interpreted result без missing fields, conflicts и clarification question. |
| Destination/location | Непустое destination value; ambiguous destination требует уточнения. |
| Dates | Check-in и check-out должны быть parseable ISO dates; check-out должен быть позже check-in. |
| Guests | Должен быть указан хотя бы один adult; children count, если есть, не может быть отрицательным. |
| Rooms | Для LLM handoff лучше требовать explicit rooms count; hidden room assumption не должен запускать first search. |
| Budget/price | Не является обязательным для текущего `HotelSearchCriteria`; если присутствует, должно оставаться user constraint или visible assumption, а не provider fact. |
| Ambiguity | Любая неоднозначность, conflict или missing required field блокирует search и ведет к clarification/fallback. |
| Safety marker | Нужен отдельный internal validation verdict вроде `safeToCreateSearch`; одного `ProceedWithCandidate` недостаточно. |

## Complete vs partial candidate rules

Complete candidate:

- intent — hotel search;
- required fields присутствуют и проходят typed validation;
- dates валидны и образуют корректный range;
- adults >= 1, children >= 0;
- rooms задан явно и >= 1;
- нет conflicts, missing required fields, clarification question и blocking warnings;
- unsupported non-hotel scope отсутствует.

Partial candidate:

- не хватает destination, dates, guests или rooms;
- dates относительные, неполные или не приводятся к safe ISO range;
- destination неоднозначен;
- rooms только предполагается, но не подтвержден;
- есть conflicts, unsupported intent, unknown intent или blocking warnings;
- candidate смешивает hotel-only request с flights, booking, payment или broader travel planning.

Partial candidate не должен создавать hotel search. Безопасный outcome: `ask_clarification` или safe boundary message, `hotelSearchId` отсутствует.

## Public contract / OpenAPI assessment

Текущая public response shape уже поддерживает:

- `session`;
- `assistantMessage`;
- `nextAction`;
- optional `hotelSearchId`.

Если когда-нибудь backend будет создавать hotel search из fully validated candidate, существующий shape теоретически может отразить это через `nextAction=show_hotel_results` и `hotelSearchId`.

Но Stage 8.9 не рекомендует такой route wiring прямо сейчас, потому что:

- нет internal criteria validation gate;
- нет политики confirmation step;
- нет тестов, которые защищают candidate-to-criteria mapping;
- раскрывать candidate summary наружу нельзя без отдельного contract step;
- новый public action value или новый response field потребовали бы отдельной OpenAPI задачи.

Для clarification и fallback current shape достаточен. Для automatic search creation нужен отдельный contract/runtime checkpoint.

## Stage 7 strict handoff compatibility assessment

Оценка вариантов:

| Вариант | Verdict |
|---|---|
| Сохранить `hotel-search;` как единственный automatic handoff trigger | Самый безопасный текущий режим; он уже реализован и покрыт тестами. |
| Разрешить LLM handoff только через validation gate | Условно безопасно как future path, но сначала нужен internal validator без route wiring. |
| Требовать explicit user confirmation перед созданием search | Предпочтительно для первого будущего LLM-driven search creation; можно использовать clarification outcome без нового public field. |
| Оставить `ProceedWithCandidate` без runtime search creation | Текущее корректное поведение Stage 8.8. |

Stage 7 strict handoff совместим с будущим LLM handoff только если LLM path не обходит validation gate и не подменяет explicit `hotel-search;` trigger молчаливым созданием search.

## Recommended validation gate

Перед любым route wiring нужен internal validator, который принимает `ProceedWithCandidate` и возвращает один из outcomes:

| Outcome | Значение |
|---|---|
| `CompleteForHotelSearch` | Typed `HotelSearchCriteria` можно передать в будущий handoff step, но Stage 8.10 еще не должен создавать search. |
| `NeedsClarification` | Есть один безопасный вопрос; `hotelSearchId` не создается. |
| `RejectToFallback` | Candidate небезопасен, unsupported или неполон; вернуть safe fallback. |

Validator должен проверять typed fields, required criteria, ambiguity, conflicts, unsupported scope и room-count policy.

## Что не входит в Stage 8.9

- production code;
- tests;
- route wiring;
- изменение runtime behavior;
- создание hotel search из `ProceedWithCandidate`;
- изменение Assistant routes или hotel search handoff;
- изменение public request/response shape;
- OpenAPI, frontend, generated clients или CI gate;
- внешний LLM-сервис, внешние вызовы, ключи доступа или provider-specific настройки;
- durable storage, auth, booking flow или расширение hotel-only MVP.

## Риски преждевременного handoff

- LLM hallucination в destination, dates, guests или rooms.
- Silent hotel search по неверным критериям.
- Создание `hotelSearchId` до проверки required fields.
- Расхождение runtime behavior с OpenAPI и frontend assumptions.
- Раскрытие internal candidate details через public response.
- Смешивание fake path с пользовательской runtime semantics.
- Утечка чувствительных пользовательских данных в будущий внешний слой без отдельной data policy.
- Обход deterministic Stage 7 `hotel-search;` handoff.
- Расширение за пределы bounded hotel-only MVP.

## Рекомендуемый Stage 8.10

Stage 8.10 должен быть backend-only internal criteria validator skeleton без route wiring и без search creation.

Минимальная цель:

- добавить internal validator для `ProceedWithCandidate`;
- вернуть typed complete/clarification/fallback validation result;
- покрыть destination, dates, guests, rooms, ambiguity и unsupported intent targeted tests;
- подтвердить, что public API, OpenAPI, frontend и Stage 7 strict handoff не меняются.

Route wiring для создания hotel search должно оставаться отложенным до отдельного contract/runtime readiness step после Stage 8.10.

## Verdict

Direct `ProceedWithCandidate -> hotel search` пока не готов.

Условно безопасный путь существует только через отдельный internal criteria validation gate и, вероятно, explicit user confirmation перед первым LLM-driven search creation. Stage 8.9 не меняет runtime behavior и рекомендует Stage 8.10 как validator-only step без route wiring.
