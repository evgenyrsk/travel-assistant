# Stage 8.1 — Design внутренней границы LlmClient

## 1. Цель Stage 8.1

Stage 8.1 определяет внутреннюю provider-independent границу `LlmClient` для будущей AI/LLM-оркестрации hotel-only MVP.

Документ фиксирует ответственность границы, допустимые данные, ожидаемый результат, валидацию, fallback и роль fake LLM. Это design/review artifact, а не новый источник статуса, API contract или план подключения реальной LLM.

## 2. Входные ограничения

- Stage 7 закрыт как ограниченная process-local основа без production readiness.
- Stage 8 остается запланированным; этот документ не меняет его статус.
- LLM не является источником hotel facts.
- Application layer владеет orchestration, проверкой результата и решением о вызове hotel provider.
- MVP v1 остается hotel-only; flights, combined itinerary, booking и payment исключены.
- Реальная LLM, SDK, secrets, сеть, runtime wiring и public API changes не входят в Stage 8.1.

## 3. Назначение LlmClient

`LlmClient` — внутренний контракт, которым application layer запрашивает ограниченную языковую интерпретацию без зависимости от конкретного LLM provider.

Граница может поддерживать отдельные задачи:

- интерпретацию hotel-related сообщения;
- извлечение кандидатных hotel-search constraints;
- подготовку короткого уточняющего вопроса;
- подготовку объяснения или сравнения только по переданным provider facts.

Результат `LlmClient` всегда является кандидатом. Он не меняет session/search state, не вызывает provider, не подтверждает ограничения пользователя и не становится provider fact без проверки application layer.

В границу не входят:

- управление диалогом или состоянием сессии;
- правила достаточности данных для hotel search;
- вызов hotel provider или других tools;
- ранжирование как обязательное решение LLM;
- HTTP/API mapping;
- хранение данных;
- выбор SDK, модели, vendor-specific параметров или prompt format.

## 4. Данные, допустимые для передачи в LLM

Передается только минимальный контекст, необходимый для конкретной задачи:

| Категория | Допустимое содержание |
|---|---|
| Пользовательский ввод | Текущее сообщение и только необходимый фрагмент подтвержденного контекста текущей сессии |
| Hotel-search context | Подтвержденные destination, dates, guests, rooms, budget, preferences и hard constraints |
| Оркестрационный контекст | Известные missing fields, conflicts, unsupported-scope markers и тип запрошенной задачи |
| Объяснение результатов | Только нормализованные hotel offers и provider facts, уже полученные через provider boundary |
| Неопределенность | Явно помеченные assumptions, unknowns и source/freshness limitations |
| Язык ответа | Нужный язык пользовательского ответа, если он известен без дополнительного профилирования |

Принцип минимизации: полная история разговора не передается по умолчанию. Provider facts, user constraints, assumptions и unknowns должны оставаться раздельными.

## 5. Данные, запрещенные для передачи в LLM на этом этапе

- API keys, credentials, auth/session tokens и secrets;
- provider-specific DTO, raw provider payloads и внутренние поля отсутствующего real provider contract;
- account history, profile, contact, payment и booking data;
- полные database records, server logs, telemetry dumps или скрытая runtime configuration;
- данные других пользователей или сессий;
- несвязанные части истории диалога;
- flights, combined itinerary, booking или payment context;
- значения, представленные как provider facts без подтверждения provider boundary.

Если пользователь сам включил чувствительные данные в сообщение, boundary не должен дополнять их данными из других источников. Политика очистки и хранения таких сообщений требует отдельного решения до подключения внешней LLM.

## 6. Ожидаемый результат от LlmClient

Результат должен быть структурированным и provider-independent. Ниже указаны смысловые части, а не Kotlin type, DTO или сериализуемая schema:

| Часть результата | Назначение |
|---|---|
| Task outcome | Успешная интерпретация, требуется уточнение, unsupported intent или невозможность надежной интерпретации |
| Intent candidate | `hotel_search`, unsupported или unknown |
| Extracted constraints | Кандидатные значения с указанием происхождения: user-provided или assumption |
| Missing required fields | Обязательные данные, которых не хватает для hotel search |
| Conflicts and ambiguities | Противоречия и неоднозначности, требующие решения пользователя |
| Clarification draft | Один короткий вопрос, если без него нельзя безопасно продолжить |
| Explanation draft | Текст, основанный только на переданных provider facts, assumptions и unknowns |
| Warnings | Неполный контекст, низкая уверенность или невозможность подтвердить часть интерпретации |

`LlmClient` не возвращает исполняемую команду вызова provider. Следующее действие выбирает application orchestration после валидации.

## 7. Валидация результата

До изменения состояния или вызова provider application layer должен проверить:

- результат присутствует и соответствует запрошенной задаче;
- intent не выходит за hotel-only scope;
- извлеченные значения проходят существующие domain/application validation rules;
- user-provided constraints не смешаны с assumptions;
- unknowns не заполнены догадками;
- explanation не содержит фактов, отсутствующих во входных provider facts;
- missing fields и conflicts не противоречат подтвержденному session context;
- clarification draft относится к decision-critical gap и не активирует future scope;
- результат не содержит provider/tool call, secret или vendor-specific payload.

Невалидный результат не должен частично менять session/search state. Provider call разрешает только application layer после собственной проверки достаточности данных.

## 8. Fallback-поведение

| Ситуация | Безопасный fallback |
|---|---|
| Ошибка или timeout | Сохранить текущее состояние, не вызывать provider, вернуть детерминированное безопасное уточнение |
| Пустой ответ | Считать результат неуспешным и использовать тот же fallback |
| Невалидный ответ | Отбросить результат целиком; не применять извлеченные значения |
| Неоднозначная интерпретация | Задать один вопрос по наиболее важному unresolved constraint |
| Unsupported intent | Сообщить границу hotel-only MVP без запуска future flow |
| Ошибка explanation после получения offers | Сохранить структурированные offers и текущий deterministic `matchSummary`; не выдумывать новое объяснение |

Fallback не должен маскировать provider error, подтверждать догадку пользователя или превращать отсутствие ответа LLM в отсутствие hotel offers.

## 9. Fake LLM testing model

Fake LLM — детерминированный test double будущего `LlmClient`. Он нужен для проверки orchestration без сети и внешних вызовов.

Fake должен позволять явно воспроизводить:

- валидную интерпретацию;
- missing fields и clarification;
- conflicting или ambiguous constraints;
- unsupported intent;
- ошибку, timeout, пустой и невалидный результат;
- explanation, использующее только заданные provider facts.

Fake LLM не является:

- эмулятором качества реальной модели;
- SDK adapter;
- prompt-testing framework;
- источником provider facts;
- основанием для readiness claim.

Real LLM provider в будущем будет внешним infrastructure adapter с network, credentials, latency, cost и provider-specific failure modes. Fake остается локальным и детерминированным. Оба должны подчиняться одному application-owned контракту, но Stage 8.1 не проектирует real adapter.

## 10. Почему runtime behavior не меняется

Stage 8.1 создает только design/review документ. Контракт, fake, validators, wiring и tests не добавляются. Существующие Assistant routes, strict hotel-search handoff, session state, ranking и frontend продолжают работать без изменений.

## 11. Что не входит в Stage 8.1

- production code, backend tests или runtime wiring;
- реальная LLM и provider-specific integration;
- API keys, environment variables или configuration;
- prompt library, model selection, token/cost tuning;
- frontend и chat UI;
- OpenAPI, generated clients, manifest или CI gate;
- real travel provider;
- durable storage, auth, booking, payment, flights и combined itinerary;
- изменение roadmap status или production-readiness claim.

## 12. Риски преждевременной реализации

- Превращение `LlmClient` в обертку конкретного SDK.
- Передача полной истории или лишних персональных данных.
- Разрешение LLM напрямую вызывать provider или менять session state.
- Принятие assumptions за подтвержденные user constraints.
- Создание provider facts или скрытие unknowns.
- Смешение Stage 8 orchestration с real provider work Stage 9.
- Расширение public API до появления внутренней проверенной необходимости.

## 13. Рекомендуемый следующий шаг Stage 8.2

`Stage 8.2 — Minimal Internal LlmClient Contract and Fake LLM Test Boundary`.

Отдельная небольшая implementation-задача может добавить:

- application-owned `LlmClient` contract и минимальные internal input/result types;
- детерминированный fake LLM;
- validation boundary и targeted application tests;
- сценарии error/empty/invalid/ambiguous fallback.

Stage 8.2 не должен подключать контракт к public routes, менять runtime behavior, добавлять real LLM SDK, secrets, OpenAPI или frontend.

## 14. Verdict

Passed — provider-independent граница `LlmClient`, правила данных, validation, fallback и fake LLM testing model определены достаточно для отдельного ограниченного Stage 8.2.

Stage 8.1 завершает только design-задачу. Он не означает завершение Stage 8, готовность реальной LLM или production readiness.
