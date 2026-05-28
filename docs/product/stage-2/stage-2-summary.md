# Stage 2 — резюме

> MVP v1 scope update: Stage 2 сохраняется как historical traceability. Актуальный MVP v1 ограничен hotel search. Flight search является next expansion после hotel flow; combined hotel+flight — later expansion после flight flow. Прежние Stage 2 MVP recommendations по flight/combined superseded для MVP v1.

## Что было зафиксировано

- Use cases UC-01 - UC-15 на основе Stage 1 scenarios S-01 - S-10.
- Edge cases EC-001 - EC-035 по missing data, ambiguous request, contradictory constraints, provider/data problems, LLM/assistant risks, session data и unsupported actions.
- Assistant behaviour rules ABR-001 - ABR-018.
- Четыре уровня combined search и рекомендация по MVP-scope.
- Продуктовые data requirements без database schema, API schema, DTO, OpenAPI или provider adapter design.

## Use cases

Stage 2 покрывает:

- hotel search;
- flight search как future expansion после MVP v1;
- combined search request как later expansion после flight flow;
- clarification;
- comparison;
- explanation;
- save selected result;
- resume current search session;
- no results found;
- contradictory request;
- open destination request;
- change constraints during search;
- partial provider data;
- unsupported actions;
- real provider/API data as source of travel offers.

## Edge cases

Основные группы:

- missing required data;
- ambiguous natural language;
- contradictory constraints;
- provider/API data problems;
- LLM/assistant hallucination risks;
- unsupported booking/payment/legal actions;
- stale session/provider data.

## Assistant behaviour rules

Правила фиксируют:

- когда задавать уточняющий вопрос;
- когда можно и нельзя делать search;
- как объяснять и сравнивать offers;
- как разделять provider facts, assistant assumptions и unknown data;
- как реагировать на provider errors;
- как не обещать prices/availability без provider confirmation;
- как отвечать на booking/payment requests.

## Решение по combined search

- **Level 1 — Combined intent recognition:** superseded for MVP v1; future expansion.
- **Level 2 — Same-dialog hotel and flight assistance:** superseded for MVP v1; future expansion after flight flow.
- **Level 3 — Coordinated combined search:** superseded for MVP v1; later expansion after flight flow.
- **Level 4 — Full combined package ranking:** Post-MVP or Open.

Это закрывает MJ-S1-001 на уровне Stage 2 recommendation: combined search больше не трактуется как единый неделимый объем.

## Data requirements for MVP

Для MVP v1 нужны:

- search request data;
- hotel offer data;
- search session data;
- provider/API data handling;

Flight offer data и combined search data перенесены в future expansion.

## Учет Stage 1 Scope Correction

Stage 2 явно фиксирует:

- интеграция с существующим travel API входит в MVP;
- финальный MVP должен использовать предоставленный API-контракт для получения реальных travel offers;
- mock/fake providers, provider abstractions и contract placeholders допустимы только как промежуточные средства разработки;
- отсутствие API-контракта на Stage 2 является Open input для будущих технических этапов, а не основанием переносить интеграцию в Post-MVP.

## Provider/API data handling

- **Provider facts:** цены, availability, расписания, параметры offers, restrictions, source/provider и freshness из provider/API.
- **Assistant assumptions:** интерпретации ассистента, не подтвержденные provider data.
- **Unknown data:** отсутствующие, неполные, устаревшие или противоречивые данные.

LLM/assistant может структурировать, объяснять и сравнивать provider data, но не должен генерировать provider facts.

## Что остается Open

- Минимальный required field set для hotel и flight search.
- Уровень Level 3 coordinated combined search для MVP.
- Поддержка open destination discovery.
- Конкретный API-контракт существующего travel API.
- Adapter design, error handling taxonomy, reliability и production-hardening.
- Долгосрочная история, авторизация и account-level storage.

## Что переносится на Stage 3

- Финализация hotel-only MVP v1 boundaries и acceptance criteria.
- Уточнение required fields per intent.
- Уточнение supported fallback для open destination и partial provider data.
- Разделение MVP/Post-MVP для session persistence и resume behaviour, если это остается relevant для hotel-only MVP v1.

## Readiness for Stage 3

Stage 2 готовит Stage 3: use cases, edge cases, behaviour rules и data requirements достаточно структурированы для финализации MVP boundaries и acceptance criteria.

## Новые BR/FR

Новые BR/FR не добавлялись. Stage 2 развернул существующие BR-001 - BR-016 и FR-001 - FR-014, а UC-15 следует из Stage 1 Scope Correction и BR-015/FR-004/FR-005/FR-014.
