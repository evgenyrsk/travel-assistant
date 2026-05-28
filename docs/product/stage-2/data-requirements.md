# Stage 2 — Data Requirements

> MVP v1 scope update: этот документ сохраняется как historical traceability. Для MVP v1 required data ограничены hotel search. Flight offer data и combined search data перенесены в future expansion.

## Цель документа

Описать продуктовые требования к данным без database schema, API schema, DTO, OpenAPI или provider adapter implementation.

## Принцип источника данных

- Provider/API data является primary source of truth для цен, availability, расписаний, параметров offers, ограничений и freshness.
- LLM/assistant может интерпретировать, объяснять, сравнивать и структурировать provider data.
- LLM/assistant не должен выдумывать provider facts.
- Если provider data отсутствует, неполная или устаревшая, это должно быть явно отмечено как unknown data или provider limitation.
- Assistant assumptions должны храниться и отображаться отдельно от provider facts.
- Финальный MVP v1 должен использовать предоставленный API-контракт для получения реальных hotel offers.
- Stage 2 не проектирует API-контракт.

## Группы данных

| Data group | Требуется для MVP | Зачем нужно | Связанные use cases | Заметки |
|---|---|---|---|---|
| Search request data | yes | Позволяет определить intent, понять required fields, уточнить пробелы и выполнить поиск. | UC-01, UC-02, UC-03, UC-04, UC-11, UC-12 | Не является API request schema. |
| Hotel offer data | yes | Нужно для hotel search, ranking, comparison и explanation. | UC-01, UC-05, UC-06, UC-13, UC-15 | Финальный MVP получает реальные hotel offers из предоставленного travel API. |
| Flight offer data | no for MVP v1 | Нужно для future flight search, ranking, comparison и explanation. | UC-02, UC-05, UC-06, UC-13, UC-15 | Next expansion после hotel flow. |
| Combined search data | no for MVP v1 | Нужно для future combined behaviour. | UC-03, UC-11, UC-12 | Later expansion после flight flow. |
| Search session data | yes | Нужно для уточнений, сохранения, сравнения, resume и изменений в search. | UC-04, UC-07, UC-08, UC-12 | Не задает storage model. |
| Provider/API data handling | yes | Нужно для anti-hallucination, explainability, unknown handling и real offers в MVP. | UC-06, UC-09, UC-13, UC-15 | API contract остается open input для будущих технических этапов. |

## Search request data

**Требуется для MVP:** yes.

**Поля:**
- origin;
- destination;
- dates;
- duration;
- guests/passengers;
- budget;
- preferences;
- constraints;
- flexibility;
- intent type.

**Зачем нужно:** эти данные определяют, можно ли искать, что уточнять и какие offers релевантны.

**Связанные use cases:** UC-01, UC-02, UC-03, UC-04, UC-10, UC-11, UC-12.

**Заметки:** обязательность отдельных полей зависит от intent и должна быть финализирована на Stage 3.

## Hotel offer data

**Требуется для MVP:** yes.

**Поля:**
- hotel name;
- location;
- price;
- currency;
- dates;
- rating;
- review score;
- amenities;
- cancellation policy availability;
- source/provider;
- data freshness;
- confidence/unknown fields.

**Зачем нужно:** hotel offers должны быть ранжируемыми, сравнимыми и объяснимыми.

**Связанные use cases:** UC-01, UC-05, UC-06, UC-13, UC-15.

**Заметки:** missing rating, cancellation policy или amenities должны быть unknown data, а не assistant assumptions.

## Flight offer data

**Требуется для MVP v1:** no; next expansion после hotel flow.

**Поля:**
- airline;
- origin;
- destination;
- departure/arrival time;
- duration;
- stops;
- baggage availability;
- price;
- currency;
- source/provider;
- data freshness;
- confidence/unknown fields.

**Зачем нужно:** в future flight expansion flight offers должны поддерживать базовое сравнение цены, времени, пересадок и удобства.

**Связанные use cases:** UC-02, UC-05, UC-06, UC-13, UC-15.

**Заметки:** baggage availability и freshness нельзя додумывать, если provider их не вернул.

## Combined search data

**Требуется для MVP v1:** no; later expansion после flight flow.

**Поля:**
- shared dates;
- shared destination;
- total budget;
- hotel budget part;
- flight budget part;
- package assumptions;
- unresolved constraints.

**Зачем нужно:** эти данные нужны для Level 2 same-dialog assistance и возможного Level 3 coordinated combined search.

**Связанные use cases:** UC-03, UC-11, UC-12.

**Заметки:** budget split может быть assistant assumption, если provider/API не подтверждает package price.

## Search session data

**Требуется для MVP:** yes.

**Поля:**
- current user goal;
- extracted parameters;
- missing required parameters;
- user preferences;
- selected offers;
- rejected offers;
- comparison candidates;
- assistant assumptions;
- provider facts;
- unknown fields.

**Зачем нужно:** search session поддерживает clarification, resume, save, compare и изменение constraints.

**Связанные use cases:** UC-04, UC-05, UC-07, UC-08, UC-12.

**Заметки:** долгосрочная история и account-level storage остаются Post-MVP/Open.

## Provider/API data handling

**Требуется для MVP:** yes.

**Provider facts:** данные, полученные из provider/API или другого утвержденного источника travel offers: prices, availability, schedules, hotel attributes, flight attributes, baggage, restrictions, source, freshness.

**Assistant assumptions:** интерпретации ассистента, которые не подтверждены provider facts: предполагаемый budget tier, guessed priority, tentative budget split, likely conflict before provider verification.

**Unknown data:** данные, которых нет, которые неполны, устарели, противоречивы или не имеют freshness/source confirmation.

**Зачем нужно:** это основа anti-hallucination, explainability и доверия к real provider/API data.

**Связанные use cases:** UC-06, UC-09, UC-10, UC-13, UC-15.

**Заметки:** если API-контракт еще не предоставлен, это фиксируется как open input для будущих технических этапов, а не как перенос real API integration в Post-MVP.
